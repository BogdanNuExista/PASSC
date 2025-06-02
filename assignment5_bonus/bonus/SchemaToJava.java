import org.w3c.dom.*;
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processSchema(Document doc) {
        Element schemaRoot = doc.getDocumentElement();
        Map<String, Element> namedComplexTypes = new HashMap<>();
        Map<String, Element> namedElements = new HashMap<>();
        NodeList topLevelNodes = schemaRoot.getChildNodes();

        // First pass: collect all named complexTypes and elements
        for (int i = 0; i < topLevelNodes.getLength(); i++) {
            Node node = topLevelNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String tag = getLocalTag(element.getTagName());
                String name = element.getAttribute("name");

                if (!name.isEmpty()) {
                    if (tag.equals("complexType")) {
                        namedComplexTypes.put(name, element);
                    } else if (tag.equals("element")) {
                        namedElements.put(name, element);
                    }
                }
            }
        }

        // Find root element
        Element rootElement = null;
        for (int i = 0; i < topLevelNodes.getLength(); i++) {
            Node node = topLevelNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String tag = getLocalTag(element.getTagName());
                if (tag.equals("element")) {
                    rootElement = element;
                    break;
                }
            }
        }

        if (rootElement == null) {
            throw new RuntimeException("Root element not found in schema");
        }

        ClassGenerator generator = new ClassGenerator(namedComplexTypes, namedElements);
        String rootClassName = generator.generateClassName(rootElement);
        generator.generateClass(rootClassName, rootElement);
    }

    private static String getLocalTag(String tag) {
        return tag.contains(":") ? tag.substring(tag.indexOf(':') + 1) : tag;
    }
}