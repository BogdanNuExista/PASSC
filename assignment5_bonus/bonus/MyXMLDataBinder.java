import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

public class MyXMLDataBinder {

    public static <T> T CreateObjectFromXMLfile(String xmlFile, Class<T> clazz) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(xmlFile));
        return unmarshal(doc.getDocumentElement(), clazz);
    }

    private static <T> T unmarshal(Element element, Class<T> clazz) throws Exception {
        T obj = clazz.getDeclaredConstructor().newInstance();
        NamedNodeMap attributes = element.getAttributes();
        
        // Process attributes
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            String attrName = attr.getNodeName();
            String attrValue = attr.getNodeValue();
            setFieldValue(obj, attrName, attrValue);
        }

        // Process child elements
        NodeList children = element.getChildNodes();
        Map<String, List<Element>> childElements = new HashMap<>();
        
        // Group elements by tag name
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;
                String tagName = child.getTagName();
                childElements.computeIfAbsent(tagName, k -> new ArrayList<>()).add(child);
            }
        }
        
        // Process grouped elements
        for (Map.Entry<String, List<Element>> entry : childElements.entrySet()) {
            String fieldName = entry.getKey();
            List<Element> elements = entry.getValue();
            Field field = getField(clazz, fieldName);
            
            if (field == null) continue;
            field.setAccessible(true); // Make field accessible
            
            Class<?> fieldType = field.getType();
            
            if (List.class.isAssignableFrom(fieldType)) {
                // Handle list fields
                List<Object> list = new ArrayList<>();
                ParameterizedType listType = (ParameterizedType) field.getGenericType();
                Class<?> itemClass = (Class<?>) listType.getActualTypeArguments()[0];
                
                for (Element elem : elements) {
                    Object item;
                    if (isPrimitiveOrWrapper(itemClass)) {
                        item = getSimpleValue(elem, itemClass);
                    } else {
                        item = unmarshal(elem, itemClass);
                    }
                    list.add(item);
                }
                field.set(obj, list);
            } else {
                // Handle single fields
                Element elem = elements.get(0); // Take first element
                Object value;
                
                if (isPrimitiveOrWrapper(fieldType)) {
                    value = getSimpleValue(elem, fieldType);
                } else {
                    value = unmarshal(elem, fieldType);
                }
                
                field.set(obj, value);
            }
        }
        return obj;
    }

    private static boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() || 
               type == String.class ||
               type == Integer.class ||
               type == Float.class ||
               type == Double.class ||
               type == Boolean.class ||
               type == Long.class ||
               type == Short.class ||
               type == Byte.class;
    }

    private static Object getSimpleValue(Element element, Class<?> type) {
        String text = element.getTextContent().trim();
        if (type == String.class) return text;
        if (type == int.class || type == Integer.class) return Integer.parseInt(text);
        if (type == float.class || type == Float.class) return Float.parseFloat(text);
        if (type == double.class || type == Double.class) return Double.parseDouble(text);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(text);
        if (type == long.class || type == Long.class) return Long.parseLong(text);
        return text;
    }

    public static void CreateXMLFromObject(Object object, String rootElementName, String xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        
        Element rootElement = doc.createElement(rootElementName);
        doc.appendChild(rootElement);
        
        marshal(object, rootElement, doc, new HashSet<>());
        
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(xmlFile));
        transformer.transform(source, result);
    }

    private static void marshal(Object object, Element parent, Document doc, Set<Object> visited) throws Exception {
        if (object == null || visited.contains(object)) {
            return; // Prevent infinite recursion
        }
        
        visited.add(object);
        
        Class<?> clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields();
        
        for (Field field : fields) {
            field.setAccessible(true); // Make field accessible
            Object value = field.get(object);
            if (value == null) continue;
            
            if (List.class.isAssignableFrom(field.getType())) {
                List<?> list = (List<?>) value;
                for (Object item : list) {
                    Element element = doc.createElement(field.getName());
                    if (isPrimitiveOrWrapper(item.getClass())) {
                        element.setTextContent(item.toString());
                    } else {
                        Set<Object> childVisited = new HashSet<>(visited);
                        marshal(item, element, doc, childVisited);
                    }
                    parent.appendChild(element);
                }
            } else if (isPrimitiveOrWrapper(field.getType())) {
                Element element = doc.createElement(field.getName());
                element.setTextContent(value.toString());
                parent.appendChild(element);
            } else {
                Element element = doc.createElement(field.getName());
                Set<Object> childVisited = new HashSet<>(visited);
                marshal(value, element, doc, childVisited);
                parent.appendChild(element);
            }
        }
        
        visited.remove(object);
    }

    private static void setFieldValue(Object obj, String fieldName, String value) throws Exception {
        Field field = getField(obj.getClass(), fieldName);
        if (field == null) return;
        
        field.setAccessible(true); // Make field accessible
        Class<?> type = field.getType();
        
        if (type == String.class) {
            field.set(obj, value);
        } else if (type == int.class || type == Integer.class) {
            field.set(obj, Integer.parseInt(value));
        } else if (type == float.class || type == Float.class) {
            field.set(obj, Float.parseFloat(value));
        } else if (type == double.class || type == Double.class) {
            field.set(obj, Double.parseDouble(value));
        } else if (type == boolean.class || type == Boolean.class) {
            field.set(obj, Boolean.parseBoolean(value));
        }
    }

    private static Field getField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}