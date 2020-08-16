package com.company;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Main {

    private static Map<String, String> idVariableMap = new HashMap<>();
    private static Map<String, String> colorVariableMap = new HashMap<>();
    private static Map<String, String> idColorMap = new HashMap<>();
    private static AtomicInteger nodeCount = new AtomicInteger(0);
    private static AtomicInteger colorCount = new AtomicInteger(0);

    public static void main(String[] args) {

        String filePath = args[0];
        try {
            Document outputXmlDom = xmlProcess(filePath, args[1].equalsIgnoreCase("1"));
            String newFilename = getFilename(filePath);
            writeXml(outputXmlDom, newFilename);
        } catch (IOException e) {
            System.err.println("File " + args[0] + " Not Found");
        } catch (SAXException | ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    public static String getFilename(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        String newFilename = filename.substring(0, lastDotIndex) + "_processed" + filename.substring(lastDotIndex);
        return newFilename;
    }

    public static Document xmlProcess(String input, boolean oneStep) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(input);
        NodeList pathNodes = document.getElementsByTagName("path");
        processElements(pathNodes);
        NodeList rectNodes = document.getElementsByTagName("rect");
        processElements(rectNodes);
        addStyleElement(document, oneStep);
        return document;
    }

    private static void processElements(NodeList nodeList){
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            String attributeValue = node.getAttribute("fill");
            if (attributeValue != null && !attributeValue.isEmpty()) {
                //System.out.println(attributeValue);
                String id = "node_" + nodeCount.incrementAndGet();
                node.setAttribute("id", id);
                idVariableMap.put(id, getColorVariableName(attributeValue));
                node.removeAttribute("fill");
                idColorMap.put("node_"+nodeCount.get(), attributeValue);
            }
        }
    }

    public static void writeXml(Document dom, String fileName) throws IOException, TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty("omit-xml-declaration", "yes");
        DOMSource source = new DOMSource(dom);
        FileWriter writer = new FileWriter(new File(fileName));
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);
    }

    private static String getColorVariableName(String colorName) {
        if(colorVariableMap.containsKey(colorName)){
            return colorVariableMap.get(colorName);
        }
        String variableName = "--node-color-" + colorCount.incrementAndGet();
        colorVariableMap.put(colorName, variableName);
        return variableName;
    }

    private static String createStyleElement(Map.Entry<String, String> entry) {
        //#Vector_2 {fill: var(--vector-2-3-color);}
        StringBuilder sb = new StringBuilder("#");
        sb.append(entry.getKey());
        sb.append(" {fill: var(");
        sb.append(entry.getValue());
        sb.append(");}");
        return sb.toString();
    }

    private static String createStyleVariables(Map.Entry<String, String> entry) {
        // --sun-color: #FFF3D1;
        StringBuilder sb = new StringBuilder(entry.getValue());
        sb.append(": ");
        sb.append(entry.getKey());
        sb.append(";");
        return sb.toString();
    }

    private static String createVars(Map.Entry<String, String> entry) {
        //#CR {fill: #81D2E4;}
        StringBuilder sb = new StringBuilder("#");
        sb.append(entry.getKey());
        sb.append(" {fill: ");
        sb.append(entry.getValue());
        sb.append(";}");
        return sb.toString();
    }

    private static void addStyleElement(Document document, boolean oneStep) {
        Element style = document.createElement("style");
        String styleTagContents = "";
        if(oneStep){
            styleTagContents = idColorMap.entrySet().stream().map(stringStringEntry ->  createVars(stringStringEntry)).collect(Collectors.joining("\n"));
        } else {
            String colorVariables = ".test {".concat(colorVariableMap.entrySet().stream().map(e -> createStyleVariables(e)).collect(Collectors.joining("\n")));
            colorVariables = colorVariables.concat("}");
            String styleCont = idVariableMap.entrySet().stream().map(e -> createStyleElement(e)).collect(Collectors.joining("\n"));
            styleTagContents = colorVariables + "\n"+ styleCont;
        }
        style.appendChild(document.createTextNode(styleTagContents));
        Element root = document.getDocumentElement();
        root.appendChild(style);
    }

}
