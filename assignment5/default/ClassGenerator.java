import org.w3c.dom.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class ClassGenerator {
    private Map<String, Element> namedComplexTypes;
    private Map<String, ClassInfo> classInfoMap;
    private Queue<ClassInfo> queue;
    private Set<String> generatedClasses;

    public ClassGenerator(Map<String, Element> namedComplexTypes) {
        this.namedComplexTypes = namedComplexTypes;
        this.classInfoMap = new HashMap<>();
        this.queue = new LinkedList<>();
        this.generatedClasses = new HashSet<>();
    }

    public void generateClass(String className, Element complexTypeElement) {
        if (generatedClasses.contains(className)) {
            return;
        }

        ClassInfo classInfo = new ClassInfo(className, complexTypeElement);
        classInfoMap.put(className, classInfo);
        queue.add(classInfo);

        while (!queue.isEmpty()) {
            ClassInfo current = queue.poll();
            if (generatedClasses.contains(current.className)) {
                continue;
            }
            processComplexType(current);
            generateJavaFile(current);
            generatedClasses.add(current.className);
        }
    }

    private void processComplexType(ClassInfo classInfo) {
        List<Field> fields = new ArrayList<>();
        Element complexTypeElement = classInfo.complexTypeElement;
        NodeList children = complexTypeElement.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElem = (Element) child;
                String localTag = getLocalTag(childElem.getTagName());

                if (localTag.equals("sequence") || localTag.equals("all") || localTag.equals("choice")) {
                    processCompositor(childElem, fields, classInfo.className);
                } else if (localTag.equals("attribute")) {
                    fields.add(processAttribute(childElem));
                }
            }
        }
        classInfo.fields = fields;
    }

    private void processCompositor(Element compositor, List<Field> fields, String parentClassName) {
        NodeList compositorChildren = compositor.getChildNodes();
        for (int i = 0; i < compositorChildren.getLength(); i++) {
            Node node = compositorChildren.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) node;
                String localTag = getLocalTag(elem.getTagName());
                if (localTag.equals("element")) {
                    fields.add(processElement(elem, parentClassName));
                }
            }
        }
    }

    private Field processElement(Element element, String parentClassName) {
        String fieldName = element.getAttribute("name");
        String typeAttr = element.getAttribute("type");
        String minOccurs = element.getAttribute("minOccurs");
        String maxOccurs = element.getAttribute("maxOccurs");

        boolean isList = "unbounded".equals(maxOccurs) || 
                        (!maxOccurs.isEmpty() && Integer.parseInt(maxOccurs) > 1);

        Element nestedComplexType = null;
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElem = (Element) child;
                String localTag = getLocalTag(childElem.getTagName());
                if (localTag.equals("complexType")) {
                    nestedComplexType = childElem;
                    break;
                }
            }
        }

        String fieldType;
        if (nestedComplexType != null) {
            fieldType = capitalize(fieldName);
            if (!generatedClasses.contains(fieldType) && !classInfoMap.containsKey(fieldType)) {
                ClassInfo nestedClass = new ClassInfo(fieldType, nestedComplexType);
                classInfoMap.put(fieldType, nestedClass);
                queue.add(nestedClass);
            }
        } else if (!typeAttr.isEmpty()) {
            fieldType = mapXsdTypeToJava(typeAttr);
            if (fieldType == null) {
                fieldType = capitalize(typeAttr);
                if (namedComplexTypes.containsKey(typeAttr) && 
                    !generatedClasses.contains(fieldType) && 
                    !classInfoMap.containsKey(fieldType)) {
                    ClassInfo namedClass = new ClassInfo(fieldType, namedComplexTypes.get(typeAttr));
                    classInfoMap.put(fieldType, namedClass);
                    queue.add(namedClass);
                }
            }
        } else {
            fieldType = "String";
        }

        if (isList) {
            fieldType = "List<" + fieldType + ">";
        }

        return new Field(fieldName, fieldType, isList);
    }

    private Field processAttribute(Element attribute) {
        String attrName = attribute.getAttribute("name");
        String typeAttr = attribute.getAttribute("type");
        String javaType = mapXsdTypeToJava(typeAttr);
        if (javaType == null) {
            javaType = "String";
        }
        return new Field(attrName, javaType, false);
    }

    private String mapXsdTypeToJava(String xsdType) {
        String type = xsdType.contains(":") ? 
                     xsdType.substring(xsdType.indexOf(':') + 1) : 
                     xsdType;
        switch (type) {
            case "string": return "String";
            case "float": return "float";
            case "double": return "double";
            case "integer": return "int";
            case "boolean": return "boolean";
            default: return null;
        }
    }

    private void generateJavaFile(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        boolean needsListImport = classInfo.fields.stream().anyMatch(f -> f.isList);

        if (needsListImport) {
            sb.append("import java.util.ArrayList;\n");
            sb.append("import java.util.List;\n\n");
        }

        sb.append("public class ").append(classInfo.className).append(" {\n");
        for (Field field : classInfo.fields) {
            if (field.isList) {
                sb.append("    public ").append(field.type)
                  .append(" ").append(field.name)
                  .append(" = new ArrayList<>();\n");
            } else {
                sb.append("    public ").append(field.type)
                  .append(" ").append(field.name)
                  .append(";\n");
            }
        }
        sb.append("}\n");

        try {
            Files.write(Paths.get(classInfo.className + ".java"), 
                       sb.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getLocalTag(String tag) {
        return tag.contains(":") ? tag.substring(tag.indexOf(':') + 1) : tag;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}