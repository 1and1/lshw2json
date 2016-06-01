package com.oneandone.lshw2json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/**
 * Converts a LSHW XML document to a LSHW JSON document.
 * @author Stephan Fuhrmann
 */
public class Main {

    /** The generator for the JSON output. */
    private final JsonGenerator generator;

    /** XPath factory to use. */
    private final XPathFactory factory;

    /** Names which are treated as native JSON numbers. */
    private final Set<String> numberNames;

    /** Names which are treated as native JSON bools. */
    private final Set<String> booleanNames;

    private Main(JsonGenerator generator) {
        this.generator = Objects.requireNonNull(generator);
        this.factory = XPathFactory.newInstance();
        
        numberNames = new HashSet<>(Arrays.asList("size", "capacity", "width", "clock"));
        booleanNames = new HashSet<>(Arrays.asList("claimed"));
    }
        
    /** Writes a JSON file from the given XML document.
     * @param doc the parsed DOM XML lshw document.
     * @param output the file to write the JSON output to.
     */
    private static void writeJson(Document doc, File output) throws IOException, XPathExpressionException {
        try (JsonGenerator generator = new JsonFactory().createGenerator(output, JsonEncoding.UTF8)) {
            generator.setPrettyPrinter(new DefaultPrettyPrinter());
            
            Main instance = new Main(generator);
            
            Element list = doc.getDocumentElement();
            if (!list.getNodeName().equals("list")) {
                throw new IllegalStateException("Root element is '"+list.getNodeName()+"', but expecting 'list'");
            }

            instance.processNodes(list);
        }
    }
    
    /** Process all "node" elements in one hierarchy level. 
     * This is a recursive call.
     * @param list a parent of node elements.
     */
    private void processNodes(Element list) throws IOException, XPathExpressionException {
        Objects.requireNonNull(list, "list is null");
        
        NodeList nodeList = (NodeList) factory.newXPath().evaluate("./node", list, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); i++) {
            generator.writeStartObject();
            Element node = (Element) nodeList.item(i);
            mapAttributes(node);
            mapElements(node);
            mapIdElements(node, "configuration", "setting", e -> e.getAttribute("value").trim());
            mapIdElements(node, "capabilities", "capability", e -> e.getTextContent().trim());
            
            Double count = (Double)factory.newXPath().evaluate("count(./node)", node, XPathConstants.NUMBER);
            if (count.intValue() > 0) {
                generator.writeArrayFieldStart("children");
                processNodes(node);
                generator.writeEndArray();
            }
            
            generator.writeEndObject();
        }
    }
    
    /** Writes a field and honors the JSON type by looking at the field name and
     * trying to determine what type this is.
     * @param key the key to write.
     * @param value the (String) value to write. Might be converted to another type.
     * @see #booleanNames
     * @see #numberNames
     */
    private void writeField(String key, String value) throws IOException {
        if (numberNames.contains(key)) {
            BigDecimal decimal = new BigDecimal(value);
            generator.writeNumberField(key, decimal);
        } else if (booleanNames.contains(key)) {
            boolean boolVal = Boolean.parseBoolean(value);
            generator.writeBooleanField(key, boolVal);
        } else {
            generator.writeStringField(key, value);
        }
    }
    
    /** Maps XML attributes to JSON fields. */
    private void mapAttributes(Element node) throws IOException {
        NamedNodeMap map = node.getAttributes();
        for (int i=0; i < map.getLength(); i++) {
            Attr attribute = (Attr) map.item(i);
            writeField(attribute.getName(), attribute.getValue());
        }
    }
    
    /** Maps simple XML "value elements" to JSON fields. */
    private void mapElements(Element node) throws IOException {
        NodeList list = node.getChildNodes();
        for (int i=0; i < list.getLength(); i++) {
            Node child = list.item(i);
            if (child instanceof Element) {
                Element element = (Element) child;

                if (element.getChildNodes().getLength() == 1 && element.getChildNodes().item(0) instanceof Text) {
                    String content = element.getTextContent().trim();
                    if (!content.isEmpty()) {
                        writeField(element.getTagName(), content);
                    }
                }
            }
        }
    }
    
    /** Maps key value lists to JSON fields. 
     * This happens in the LSHW output for capabilities and configuration items.
     * @param parent the parent of the lists, usually a "node" element.
     * @parma level1name the first level container element name, for example "capabilities".
     * @param level2Name the item element name, for example "capability".
     * @param valueMapper a mapping function for values at item element level.
     */
    private void mapIdElements(Element parent, String level1Name, String level2Name, Function<Element, String> valueMapper) throws IOException, XPathExpressionException {
        NodeList list = (NodeList) factory.newXPath().evaluate(level1Name + "/" + level2Name, parent, XPathConstants.NODESET);
        
        if (list.getLength() == 0)
            return;
        
        generator.writeObjectFieldStart(level1Name);

        for (int j = 0; j < list.getLength(); j++) {
            Node child2 = list.item(j);
            if (child2 instanceof Element) {

                Element element2 = (Element) child2;
                String v = valueMapper.apply(element2);
                String id = element2.getAttribute("id");
                if (id == null) {
                    // this is probably a structural error,
                    // I am going to ignore this
                    continue;
                }
                if (!v.isEmpty()) {
                    writeField(id, v);
                } else {
                    generator.writeBooleanField(id, true);
                }
            }
        }
        generator.writeEndObject();
    }
    
    public static void main(String args[]) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {        
        if (args.length != 2) {
            System.err.println("Usage: Main <XML-INPUT-FILE> <JSON-OUTPUT-FILE>");
            System.exit(1);
        }
        
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new File(args[0]));
        writeJson(doc, new File(args[1]));
    }
}
