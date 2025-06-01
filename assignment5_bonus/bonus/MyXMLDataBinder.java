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
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;
                String fieldName = child.getTagName();
                Field field = getField(clazz, fieldName);
                
                if (field == null) continue;
                
                Class<?> fieldType = field.getType();
                Object value;
                
                if (List.class.isAssignableFrom(fieldType)) {
                    value = handleListField(child, field);
                } else if (fieldType.getName().startsWith("java.lang")) {
                    value = getSimpleValue(child, fieldType);
                } else {
                    value = unmarshal(child, fieldType);
                }
                
                field.set(obj, value);
            }
        }
        return obj;
    }

    private static Object handleListField(Element listElement, Field field) throws Exception {
        ParameterizedType listType = (ParameterizedType) field.getGenericType();
        Class<?> itemClass = (Class<?>) listType.getActualTypeArguments()[0];
        List<Object> list = new ArrayList<>();
        
        NodeList children = listElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;
                Object item;
                
                if (itemClass.getName().startsWith("java.lang")) {
                    item = getSimpleValue(child, itemClass);
                } else {
                    item = unmarshal(child, itemClass);
                }
                
                list.add(item);
            }
        }
        return list;
    }

    private static Object getSimpleValue(Element element, Class<?> type) {
        String text = element.getTextContent().trim();
        if (type == String.class) return text;
        if (type == int.class || type == Integer.class) return Integer.parseInt(text);
        if (type == float.class || type == Float.class) return Float.parseFloat(text);
        if (type == double.class || type == Double.class) return Double.parseDouble(text);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(text);
        return text;
    }

    public static void CreateXMLFromObject(Object object, String rootElementName, String xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        
        Element rootElement = doc.createElement(rootElementName);
        doc.appendChild(rootElement);
        
        marshal(object, rootElement, doc);
        
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(xmlFile));
        transformer.transform(source, result);
    }

    private static void marshal(Object object, Element parent, Document doc) throws Exception {
        Class<?> clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields();
        
        for (Field field : fields) {
            Object value = field.get(object);
            if (value == null) continue;
            
            if (List.class.isAssignableFrom(field.getType())) {
                List<?> list = (List<?>) value;
                for (Object item : list) {
                    Element element = doc.createElement(field.getName());
                    if (item.getClass().getName().startsWith("java.lang")) {
                        element.setTextContent(item.toString());
                    } else {
                        marshal(item, element, doc);
                    }
                    parent.appendChild(element);
                }
            } else if (field.getType().getName().startsWith("java.lang")) {
                Element element = doc.createElement(field.getName());
                element.setTextContent(value.toString());
                parent.appendChild(element);
            } else {
                Element element = doc.createElement(field.getName());
                marshal(value, element, doc);
                parent.appendChild(element);
            }
        }
    }

    private static void setFieldValue(Object obj, String fieldName, String value) throws Exception {
        Field field = getField(obj.getClass(), fieldName);
        if (field == null) return;
        
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
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}