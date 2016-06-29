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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
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

    /** Names which are treated as native JSON numbers. */
    private final Set<String> numberNames;

    /** Names which are treated as native JSON bools. */
    private final Set<String> booleanNames;

    private Main(JsonGenerator generator) {
        this.generator = Objects.requireNonNull(generator);
        
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
                throw new IOException("Root element is '"+list.getNodeName()+"', but expecting 'list'");
            }

            instance.processNodes(list);
        }
        catch (Exception e) {
            // delete output file in case of error
            output.delete();
            throw e;
        }
    }
        
    /** Process all "node" elements in one hierarchy level. 
     * This is a recursive call.
     * @param list a parent of node elements.
     */
    private void processNodes(Element list) throws IOException, XPathExpressionException {
        Objects.requireNonNull(list, "list is null");
        
        getChildElementsWithName(list, "node").forEach(node -> {
            try {
                generator.writeStartObject();
                mapAttributes(node);
                mapElements(node);
                mapIdElements(node, "configuration", "setting", e -> e.getAttribute("value").trim());
                mapIdElements(node, "capabilities", "capability", e -> e.getTextContent().trim());
                
                int count = getChildElementsWithName(node, "node").size();
                if (count > 0) {
                    generator.writeArrayFieldStart("children");
                    processNodes(node);
                    generator.writeEndArray();
                }
                
                generator.writeEndObject();
            } catch (IOException | XPathExpressionException ex) {
                throw new WrappedException(ex);
            }
        }
        );        
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
    
    /** Gets all child elements of a DOM element with a certain name.
     * @param parent the parent to examine the children of.
     * @param name the name of the child elements to search.
     * @return a List of all direct child elements having the given name.
     */
    private static List<Element> getChildElementsWithName(Element parent, String name) {
        List<Element> outer = new NodeListAdapter(parent.getChildNodes()).
            stream().
            filter(e -> e instanceof Element).
            map(e -> (Element) e).
            filter(e -> e.getNodeName().equals(name)).
            collect(Collectors.toList());

        return outer;
    }
    
    /** Maps key value lists to JSON fields. 
     * This happens in the LSHW output for capabilities and configuration items.
     * @param parent the parent of the lists, usually a "node" element.
     * @parma level1name the first level container element name, for example "capabilities".
     * @param level2Name the item element name, for example "capability".
     * @param valueMapper a mapping function for values at item element level.
     */
    private void mapIdElements(Element parent, String level1Name, String level2Name, Function<Element, String> valueMapper) throws IOException, XPathExpressionException {
        
        List<Element> outer = getChildElementsWithName(parent, level1Name);
        
        long count = outer.stream().flatMap(node -> getChildElementsWithName(node, level2Name).stream()).count();
        if (count == 0)
            return;
        
        generator.writeObjectFieldStart(level1Name);

        outer.stream().flatMap(node -> getChildElementsWithName(node, level2Name).stream()).forEach(
            subNode
            -> {
            try {
                if (subNode instanceof Element) {

                    Element subElement = (Element) subNode;
                    String v = valueMapper.apply(subElement);
                    String id = subElement.getAttribute("id");
                    if (id == null) {
                        // this is probably a structural error,
                        // I am going to ignore this
                        return;
                    }
                    if (!v.isEmpty()) {
                        writeField(id, v);
                    } else {
                        generator.writeBooleanField(id, true);
                    }
                }
            } catch (IOException e) {
                throw new WrappedException(e);
            }
        }
        );
        
        generator.writeEndObject();
    }
    
    public static void main(String args[])  {        
        if (args.length == 0) {
            System.err.println("Usage: Main <XML-INPUT-FILE> <...>");
            System.exit(1);
        }
        
        Arrays.asList(args).stream().parallel().forEach(arg -> {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(new File(arg));
                writeJson(doc, new File(arg + ".json"));
            } catch (ParserConfigurationException | SAXException | IOException | WrappedException | XPathExpressionException e) {
                System.err.println("Error with " + arg);
                e.printStackTrace();
            }
        });
    }
}
