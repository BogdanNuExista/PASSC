import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class SchemaToJava {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java SchemaToJava <xsd file>");
            return;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(args[0]));
            processSchema(doc);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void processSchema(Document doc) {
        Element schemaRoot = doc.getDocumentElement();
        Map<String, Element> namedComplexTypes = new HashMap<>();
        NodeList topLevelNodes = schemaRoot.getChildNodes();

        for (int i = 0; i < topLevelNodes.getLength(); i++) {
            Node node = topLevelNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String tag = element.getTagName();
                String localTag = getLocalTag(tag);

                if (localTag.equals("complexType")) {
                    String name = element.getAttribute("name");
                    if (!name.isEmpty()) {
                        namedComplexTypes.put(name, element);
                    }
                }
            }
        }

        Element rootElement = null;
        for (int i = 0; i < topLevelNodes.getLength(); i++) {
            Node node = topLevelNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String tag = element.getTagName();
                String localTag = getLocalTag(tag);
                if (localTag.equals("element")) {
                    rootElement = element;
                    break;
                }
            }
        }

        if (rootElement == null) {
            throw new RuntimeException("Root element not found in schema");
        }

        String rootElementName = rootElement.getAttribute("name");
        String rootType = rootElement.getAttribute("type");
        Element rootComplexTypeElement = null;

        if (!rootType.isEmpty()) {
            rootComplexTypeElement = namedComplexTypes.get(rootType);
        } else {
            NodeList children = rootElement.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElem = (Element) child;
                    String tag = childElem.getTagName();
                    String localTag = getLocalTag(tag);
                    if (localTag.equals("complexType")) {
                        rootComplexTypeElement = childElem;
                        break;
                    }
                }
            }
        }

        if (rootComplexTypeElement == null) {
            throw new RuntimeException("No complex type associated with root element");
        }

        String rootClassName = rootType.isEmpty() 
            ? capitalize(rootElementName) 
            : capitalize(rootType);

        ClassGenerator generator = new ClassGenerator(namedComplexTypes);
        generator.generateClass(rootClassName, rootComplexTypeElement);
    }

    private static String getLocalTag(String tag) {
        return tag.contains(":") ? tag.substring(tag.indexOf(':') + 1) : tag;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}