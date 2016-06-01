/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.oneandone.lshw2json;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author stephan
 */
public class Main {

    private static JsonNode toJson(Document doc) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode result = mapper.createObjectNode();
        return result;
    }

    private static void writeJson(File out, JsonNode json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(out, json);
    }

    public static void main(String args[]) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new File(args[0]));
        JsonNode node = toJson(doc);
        writeJson(new File(args[1]), node);
    }
}
