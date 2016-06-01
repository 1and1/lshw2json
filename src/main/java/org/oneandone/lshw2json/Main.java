/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.oneandone.lshw2json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
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
 *
 * @author stephan
 */
public class Main {

    private final JsonGenerator generator;
    private final XPathFactory factory;
    
    
    private Main(JsonGenerator generator) {
        this.generator = Objects.requireNonNull(generator);
        this.factory = XPathFactory.newInstance();
    }
        
    private static void toJson(Document doc, File f) throws IOException, XPathExpressionException {
        JsonGenerator generator = new JsonFactory().createGenerator(f, JsonEncoding.UTF8);
        generator.setPrettyPrinter(new DefaultPrettyPrinter());
        
        Main instance = new Main(generator);       
        
        Element list = doc.getDocumentElement();
        instance.processNodes(list);
        generator.close();
    }
    
    private void processNodes(Element list) throws IOException, XPathExpressionException {
        NodeList nodeList = (NodeList) factory.newXPath().evaluate("./node", list, XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); i++) {
            generator.writeStartObject();
            Element node = (Element) nodeList.item(i);
            mapAttributes(node);
            mapElements(node);
            mapIdElements(node, "configuration", "setting", e -> e.getAttribute("value").trim());
            mapIdElements(node, "capabilities", "capability", e -> e.getTextContent().trim());
            
            NodeList subNodes = (NodeList) factory.newXPath().evaluate("./node", list, XPathConstants.NODESET);
            if (subNodes.getLength() > 0) {
                generator.writeArrayFieldStart("children");
                processNodes(node);
                generator.writeEndArray();
            }
            
            generator.writeEndObject();
        }
    }
    
    private void mapAttributes(Element node) throws IOException {
        NamedNodeMap map = node.getAttributes();
        for (int i=0; i < map.getLength(); i++) {
            Attr n = (Attr) map.item(i);
            generator.writeStringField(n.getName(), n.getValue());
        }
    }
    
    private void mapElements(Element node) throws IOException {
        NodeList list = node.getChildNodes();
        for (int i=0; i < list.getLength(); i++) {
            Node child = list.item(i);
            if (child instanceof Element) {
                Element element = (Element) child;

                if (element.getChildNodes().getLength() == 1 && element.getChildNodes().item(0) instanceof Text) {
                    String content = element.getTextContent().trim();
                    if (!content.isEmpty()) {
                        generator.writeStringField(element.getTagName(), content);
                    }
                }
            }
        }
    }
    
    private void mapIdElements(Element parent, String level1Name, String level2Name, Function<Element, String> func) throws IOException, XPathExpressionException {
        NodeList list = (NodeList) factory.newXPath().evaluate(level1Name + "/" + level2Name, parent, XPathConstants.NODESET);
        
        if (list.getLength() == 0)
            return;
        
        generator.writeObjectFieldStart(level1Name);

        for (int j = 0; j < list.getLength(); j++) {
            Node child2 = list.item(j);
            if (child2 instanceof Element) {

                Element element2 = (Element) child2;
                String v = func.apply(element2);
                if (!v.isEmpty()) {
                    generator.writeStringField(element2.getAttribute("id"), v);
                } else {
                    generator.writeBooleanField(element2.getAttribute("id"), true);
                }
            }
        }
        generator.writeEndObject();
    }
    
    public static void main(String args[]) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new File(args[0]));
        toJson(doc, new File(args[1]));
    }
}
