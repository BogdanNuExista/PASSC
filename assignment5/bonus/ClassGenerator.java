package bonus;

import org.w3c.dom.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class ClassGenerator {
    private Map<String, Element> namedComplexTypes;
    private Map<String, Element> namedElements;
    private Map<String, ClassInfo> classInfoMap;
    private Queue<ClassInfo> queue;
    private Set<String> generatedClasses;

    public ClassGenerator(Map<String, Element> namedComplexTypes, Map<String, Element> namedElements) {
        this.namedComplexTypes = namedComplexTypes;
        this.namedElements = namedElements;
        this.classInfoMap = new HashMap<>();
        this.queue = new LinkedList<>();
        this.generatedClasses = new HashSet<>();
    }

    public String generateClassName(Element element) {
        String name = element.getAttribute("name");
        if (!name.isEmpty()) {
            return capitalize(name);
        }
        return "GeneratedClass";
    }

    public void generateClass(String className, Element element) {
        if (generatedClasses.contains(className)) {
            return;
        }

        Element typeElement = resolveType(element);
        ClassInfo classInfo = new ClassInfo(className, typeElement);
        classInfoMap.put(className, classInfo);
        queue.add(classInfo);

        while (!queue.isEmpty()) {
            ClassInfo current = queue.poll();
            if (generatedClasses.contains(current.className)) {
                continue;
            }
            processType(current);
            generateJavaFile(current);
            generatedClasses.add(current.className);
        }
    }

    private Element resolveType(Element element) {
        String typeName = element.getAttribute("type");
        if (!typeName.isEmpty() && namedComplexTypes.containsKey(typeName)) {
            return namedComplexTypes.get(typeName);
        }

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElem = (Element) child;
                if (getLocalTag(childElem.getTagName()).equals("complexType")) {
                    return childElem;
                }
            }
        }
        return null;
    }

    private void processType(ClassInfo classInfo) {
        if (classInfo.typeElement == null) {
            return;
        }

        List<Field> fields = new ArrayList<>();
        NodeList children = classInfo.typeElement.getChildNodes();

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
        //String minOccurs = element.getAttribute("minOccurs");
        String maxOccurs = element.getAttribute("maxOccurs");

        boolean isList = "unbounded".equals(maxOccurs) || 
                        (!maxOccurs.isEmpty() && Integer.parseInt(maxOccurs) > 1);

        String fieldType;
        Element nestedType = resolveType(element);

        if (nestedType != null) {
            String nestedClassName = generateClassNameForNested(fieldName, parentClassName);
            if (!generatedClasses.contains(nestedClassName) && !classInfoMap.containsKey(nestedClassName)) {
                ClassInfo nestedClass = new ClassInfo(nestedClassName, nestedType);
                classInfoMap.put(nestedClassName, nestedClass);
                queue.add(nestedClass);
            }
            fieldType = nestedClassName;
        } else if (!typeAttr.isEmpty()) {
            if (namedComplexTypes.containsKey(typeAttr)) {
                String namedClassName = capitalize(typeAttr);
                if (!generatedClasses.contains(namedClassName) && !classInfoMap.containsKey(namedClassName)) {
                    ClassInfo namedClass = new ClassInfo(namedClassName, namedComplexTypes.get(typeAttr));
                    classInfoMap.put(namedClassName, namedClass);
                    queue.add(namedClass);
                }
                fieldType = namedClassName;
            } else if (namedElements.containsKey(typeAttr)) {
                String namedElementClassName = capitalize(typeAttr);
                if (!generatedClasses.contains(namedElementClassName) && !classInfoMap.containsKey(namedElementClassName)) {
                    ClassInfo namedClass = new ClassInfo(namedElementClassName, namedElements.get(typeAttr));
                    classInfoMap.put(namedElementClassName, namedClass);
                    queue.add(namedClass);
                }
                fieldType = namedElementClassName;
            } else {
                fieldType = mapXsdTypeToJava(typeAttr);
            }
        } else {
            fieldType = "String";
        }

        if (isList) {
            fieldType = "List<" + fieldType + ">";
        }

        return new Field(fieldName, fieldType, isList);
    }

    private String generateClassNameForNested(String fieldName, String parentClassName) {
        return parentClassName + "_" + capitalize(fieldName);
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