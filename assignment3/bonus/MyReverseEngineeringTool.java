import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MyReverseEngineeringTool {
    private static CommandLineArgs config;
    private static final Set<Class<?>> PROCESSED_CLASSES = new HashSet<>();
    private static final Map<String, Set<String>> RELATIONSHIPS = new HashMap<>();

    public static void main(String[] args) {
        try {
            config = new CommandLineArgs(args);
            processJarFile();
            analyzeClasses();
            writeOutput();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processJarFile() throws Exception {
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{new File(config.jarPath).toURI().toURL()});
            JarFile jar = new JarFile(config.jarPath)) {
            
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    String className = entry.getName()
                        .replace("/", ".")
                        .replace(".class", "");
                    
                    if (shouldIgnore(className)) continue;
                    
                    Class<?> clazz = classLoader.loadClass(className);
                    PROCESSED_CLASSES.add(clazz);
                }
            }
        }
    }

    private static boolean shouldIgnore(String className) {
        return config.ignoredPackages.stream().anyMatch(className::startsWith);
    }

    private static void analyzeClasses() {
        RelationshipDetectorFactory factory = new RelationshipDetectorFactory(config);
        ///
        for (Class<?> clazz : PROCESSED_CLASSES) {
            for (RelationshipType type : RelationshipType.values()) {
                RelationshipDetector detector = factory.createDetector(type);
                detector.detect(clazz, RELATIONSHIPS);
            }
        }
    }

    private static void writeOutput() throws IOException {
        try (PrintWriter writer = new PrintWriter(config.outputFile)) {
            OutputFormatter formatter = OutputFormatterFactory.createFormatter(config);
            formatter.writeHeader(writer);
            
            for (Class<?> clazz : PROCESSED_CLASSES) {
                formatter.writeClass(writer, clazz, config);
            }

            formatter.writeRelationshipsHeader(writer);
            for (Map.Entry<String, Set<String>> entry : RELATIONSHIPS.entrySet()) {
                formatter.writeRelationship(writer, 
                    entry.getKey().split(" -> ")[0], 
                    entry.getKey().split(" -> ")[1], 
                    entry.getValue(), 
                    config);
            }
            
            formatter.writeFooter(writer);
        }
    }

    // Helper methods
    private static String getClassName(Class<?> clazz, CommandLineArgs config) {
        return config.qualifiedNames ? clazz.getName() : clazz.getSimpleName();
    }

    private static String getClassName(String fullName, CommandLineArgs config) {
        return config.qualifiedNames ? fullName : fullName.substring(fullName.lastIndexOf('.') + 1);
    }

    private static String getTypeName(Type type) {
        if (type instanceof Class) {
            return ((Class<?>) type).getSimpleName();
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            StringBuilder sb = new StringBuilder(getTypeName(pt.getRawType()));
            sb.append("<");
            Type[] args = pt.getActualTypeArguments();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(getTypeName(args[i]));
            }
            sb.append(">");
            return sb.toString();
        }
        return type.getTypeName();
    }

    private static String parametersToString(Type[] params) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < params.length; i++) {
            sb.append(getTypeName(params[i]));
            if (i < params.length - 1) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

    // Configuration class
    private static class CommandLineArgs {
        final String jarPath;
        final Set<String> ignoredPackages = new HashSet<>();
        boolean qualifiedNames;
        boolean showMethods;
        boolean showAttributes;
        String outputFile = "output.txt";
        String format = "text";

        CommandLineArgs(String[] args) {
            if (args.length == 0) throw new IllegalArgumentException("Missing JAR file path");
            jarPath = args[0];
            
            for (int i = 1; i < args.length; i++) {
                switch (args[i]) {
                    case "-ignore":
                        ignoredPackages.addAll(Arrays.asList(args[++i].split(",")));
                        break;
                    case "-qualified":
                        qualifiedNames = true;
                        break;
                    case "-methods":
                        showMethods = true;
                        break;
                    case "-attributes":
                        showAttributes = true;
                        break;
                    case "-output":
                        outputFile = args[++i];
                        break;
                    case "-format":
                        format = args[++i].toLowerCase();
                        break;
                }
            }
        }
    }

    // Relationship detection framework
    enum RelationshipType { INHERITANCE, IMPLEMENTATION, ASSOCIATION, DEPENDENCY }

    interface RelationshipDetector {
        void detect(Class<?> clazz, Map<String, Set<String>> relationships);
    }

    static class RelationshipDetectorFactory {
        private final CommandLineArgs config;

        RelationshipDetectorFactory(CommandLineArgs config) {
            this.config = config;
        }

        RelationshipDetector createDetector(RelationshipType type) {
            switch (type) {
                case INHERITANCE: return new InheritanceDetector(config);
                case IMPLEMENTATION: return new ImplementationDetector(config);
                case ASSOCIATION: return new AssociationDetector(config);
                case DEPENDENCY: return new DependencyDetector(config);
                default: throw new IllegalArgumentException("Unknown relationship type");
            }
        }
    }

    static abstract class BaseDetector implements RelationshipDetector {
        protected final CommandLineArgs config;

        BaseDetector(CommandLineArgs config) {
            this.config = config;
        }

        protected void addRelationship(Class<?> source, Class<?> target, String relType) {
            if (config.ignoredPackages.stream().noneMatch(target.getName()::startsWith)) {
                String key = source.getName() + " -> " + target.getName();
                RELATIONSHIPS.computeIfAbsent(key, _ -> new HashSet<>()).add(relType);
            }
        }
        
        protected boolean shouldSkipType(Class<?> type) {
            return type.isPrimitive() 
                || type.getName().startsWith("java.") 
                || type.isArray();
        }
    }

    static class InheritanceDetector extends BaseDetector {
        InheritanceDetector(CommandLineArgs config) { super(config); }

        public void detect(Class<?> clazz, Map<String, Set<String>> relationships) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && !Object.class.equals(superClass)) {
                addRelationship(clazz, superClass, "extends");
            }
        }
    }

    static class ImplementationDetector extends BaseDetector {
        ImplementationDetector(CommandLineArgs config) { super(config); }

        public void detect(Class<?> clazz, Map<String, Set<String>> relationships) {
            for (Class<?> intf : clazz.getInterfaces()) {
                addRelationship(clazz, intf, "implements");
            }
        }
    }

    static class AssociationDetector extends BaseDetector {
        AssociationDetector(CommandLineArgs config) { super(config); }
    
        public void detect(Class<?> clazz, Map<String, Set<String>> relationships) {
            for (Field field : clazz.getDeclaredFields()) {
                Type fieldType = field.getGenericType();
                if (fieldType instanceof Class) {
                    Class<?> cls = (Class<?>) fieldType;
                    if (!shouldSkipType(cls) && !cls.isPrimitive()) {
                        addRelationship(clazz, cls, "association");
                    }
                } else if (fieldType instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) fieldType;
                    Type rawType = pt.getRawType();
                    //System.out.println("Raw type: " + rawType);
                    if (rawType instanceof Class) {
                        Class<?> cls = (Class<?>) rawType;
                        if (!shouldSkipType(cls) && !cls.isPrimitive()) {
                            addRelationship(clazz, cls, "association");
                        }
                    }
                    
                    // Also detect associations with generic type parameters
                    for (Type typeArg : pt.getActualTypeArguments()) {
                        if (typeArg instanceof Class) {
                            Class<?> cls = (Class<?>) typeArg;
                            if (!shouldSkipType(cls)) {
                                addRelationship(clazz, cls, "association");
                            }
                        }
                    }
                }
            }
        }
    }

    static class DependencyDetector extends BaseDetector {
        DependencyDetector(CommandLineArgs config) { super(config); }

        public void detect(Class<?> clazz, Map<String, Set<String>> relationships) {
            
            // skip for interfaces
            if (clazz.isInterface()) return;

            for (Method method : clazz.getDeclaredMethods()) {
                analyzeParameters(method.getGenericParameterTypes(), clazz);
                analyzeReturnType(method.getGenericReturnType(), clazz);
            }
        }

        private void analyzeParameters(Type[] paramTypes, Class<?> source) {
            for (Type paramType : paramTypes) {
                if (paramType instanceof Class) {
                    Class<?> cls = (Class<?>) paramType;
                    if (!shouldSkipType(cls)) {
                        addRelationship(source, cls, "dependency");
                    }
                }
            }
        }

        private void analyzeReturnType(Type returnType, Class<?> source) {
            if (returnType instanceof Class) {
                Class<?> cls = (Class<?>) returnType;
                if (!shouldSkipType(cls)) {
                    addRelationship(source, cls, "dependency");
                }
            }
        }
    }

    // Output formatting framework
    interface OutputFormatter { 
        void writeHeader(PrintWriter writer);
        void writeClass(PrintWriter writer, Class<?> clazz, CommandLineArgs config);
        void writeRelationshipsHeader(PrintWriter writer);
        void writeRelationship(PrintWriter writer, String source, String target, 
                              Set<String> relTypes, CommandLineArgs config);
        void writeFooter(PrintWriter writer);
    }

    static class TextFormatter implements OutputFormatter {
        public void writeHeader(PrintWriter writer) {
            writer.println("=== Class Diagram ===");
        }

        public void writeClass(PrintWriter writer, Class<?> clazz, CommandLineArgs config) {
            writer.println("\nClass: " + getClassName(clazz, config));
            
            if (config.showAttributes) {
                writer.println("Fields:");
                for (Field field : clazz.getDeclaredFields()) {
                    writer.println("  " + getTypeName(field.getGenericType()) + " " + field.getName());
                }
            }
            
            if (config.showMethods) {
                writer.println("Methods:");
                for (Method method : clazz.getDeclaredMethods()) {
                    writer.println("  " + method.getName() + parametersToString(method.getGenericParameterTypes()));
                }
            }
        }

        public void writeRelationshipsHeader(PrintWriter writer) {
            writer.println("\n=== Relationships ===");
        }

        public void writeRelationship(PrintWriter writer, String source, String target, 
                                     Set<String> relTypes, CommandLineArgs config) {
            writer.printf("%s %s %s%n",
                getClassName(source, config),
                String.join(", ", relTypes),
                getClassName(target, config));
        }

        public void writeFooter(PrintWriter writer) {}
    }

    static class PlantUMLFormatter implements OutputFormatter {
        public void writeHeader(PrintWriter writer) {
            writer.println("@startuml");
            //writer.println("set namespaceSeparator none");
            //writer.println("hide empty members\n");
        }

        public void writeClass(PrintWriter writer, Class<?> clazz, CommandLineArgs config) {
            String className = getClassName(clazz, config);
            
            if (clazz.isInterface()) {
                writer.printf("interface \"%s\" {\n", className);
            } else {
                writer.printf("class \"%s\" {\n", className);
            }

            if (config.showAttributes) {
                for (Field field : clazz.getDeclaredFields()) {
                    writer.printf("  -%s: %s\n", 
                        field.getName(), getEnhancedTypeName(field.getGenericType()));
                }
            }

            if (config.showMethods) {
                for (Method method : clazz.getDeclaredMethods()) {
                    writer.printf("  %s()\n", method.getName());
                }
            }
            writer.println("}\n");
        }

        public void writeRelationshipsHeader(PrintWriter writer) {
            writer.println("\n' Relationships");
        }

        public void writeRelationship(PrintWriter writer, String source, String target, 
                            Set<String> relTypes, CommandLineArgs config) {
            String sourceName = getClassName(source, config);
            String targetName = getClassName(target, config);
            
            if (shouldSkipRelationship(targetName)) return;
            
            // Prioritize relationships - only show the strongest one
            if (relTypes.contains("extends")) {
                writer.printf("\"%s\" <|-- \"%s\"\n", targetName, sourceName);
            } else if (relTypes.contains("implements")) {
                writer.printf("\"%s\" <|.. \"%s\"\n", targetName, sourceName);
            } else if (relTypes.contains("association")) {
                writer.printf("\"%s\" --> \"%s\"\n", sourceName, targetName);
            } else if (relTypes.contains("dependency")) {
                writer.printf("\"%s\" ..> \"%s\"\n", sourceName, targetName);
            }
        }

        public void writeFooter(PrintWriter writer) {
            writer.println("@enduml");
        }
        
        private String simplifyType(Type type) {
            String typeName = getTypeName(type)
                .replaceAll("<.*>", "")
                .replace("java.lang.", "");
            return typeName.endsWith("[]") ? "array" : typeName;
        }

        private String getEnhancedTypeName(Type type) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                String baseType = getTypeName(pt.getRawType()).replace("java.util.", "");
                Type[] args = pt.getActualTypeArguments();
                List<String> argNames = new ArrayList<>();
                for (Type arg : args) {
                    argNames.add(getTypeName(arg).replace("java.lang.", ""));
                }
                return baseType + " of " + String.join(", ", argNames);
            }
            return simplifyType(type);
        }

        private boolean shouldSkipRelationship(String target) {
            return target.matches("^(void|int|float|boolean|char|byte|long|short|double)\\b");
        }
    }

    static class YUMLFormatter implements OutputFormatter {
        private final List<String> relationships = new ArrayList<>();

        public void writeHeader(PrintWriter writer) {
            //writer.println("// yUML class diagram generated by MyReverseEngineeringTool");
        }

        public void writeClass(PrintWriter writer, Class<?> clazz, CommandLineArgs config) {
            String className = getClassName(clazz, config);
            StringBuilder sb = new StringBuilder("[");
            sb.append(className);
    
            if (config.showAttributes || config.showMethods) {
                sb.append("|");
                if (config.showAttributes) {
                    for (Field field : clazz.getDeclaredFields()) {
                        // Adjust the format to match expected output
                        String prefix = Modifier.isPrivate(field.getModifiers()) ? "- " : 
                                       Modifier.isPublic(field.getModifiers()) ? "+ " : "# ";
                        sb.append(prefix)
                          .append(field.getName())
                          .append(":")
                          .append(formatType(field.getGenericType()))
                          .append(";");
                    }
                }
                if (config.showMethods) {
                    for (Method method : clazz.getDeclaredMethods()) {
                        sb.append(method.getName()).append("();");
                    }
                }
                // Remove trailing semicolon if exists
                if (sb.charAt(sb.length() - 1) == ';') {
                    sb.setLength(sb.length() - 1);
                }
            }
            sb.append("]");
            writer.println(sb);
        }

        public void writeRelationshipsHeader(PrintWriter writer) {
            // Relationships collected during writeRelationship
        }

        public void writeRelationship(PrintWriter writer, String source, String target, 
                            Set<String> relTypes, CommandLineArgs config) {
            String sourceName = getClassName(source, config);
            String targetName = getClassName(target, config);

            if (shouldSkipRelationship(targetName)) return;

            // Prioritize relationships - only show the strongest one
            String rel;
            
            if (relTypes.contains("extends")) {
                rel = String.format("[%s]-^[%s]", sourceName, targetName);
                relationships.add(0, rel);  // Add inheritance first
            } else if (relTypes.contains("implements")) {
                rel = String.format("[%s]^-.-[%s]", targetName, sourceName);
                relationships.add(rel);
            } else if (relTypes.contains("association")) {
                rel = String.format("[%s]->[%s]", sourceName, targetName);
                relationships.add(rel);
            } else if (relTypes.contains("dependency")) {
                rel = String.format("[%s]-.->[%s]", sourceName, targetName);
                relationships.add(rel);
            }
        }

        // public void writeFooter(PrintWriter writer) {
        //     // Write each relationship on its own line instead of joining them
        //     for (String rel : relationships) {
        //         writer.println(rel);
        //     }
        // }

        public void writeFooter(PrintWriter writer) { // cleaner version
            // Filter out dependency relationships (those with -.->) and keep only specific ones
            for (String rel : relationships) {
                // Include only inheritance, implementation, and association relationships
                if (!rel.contains("-.-") || rel.contains("^-.-")) {
                    writer.println(rel);
                }
            }
        }
        
        private String simplifyType(Type type) {
            String typeName = getTypeName(type)
                .replaceAll("<.*>", "")
                .replace("java.lang.", "");
            return typeName.endsWith("[]") ? "array" : typeName;
        }

        private String formatType(Type type) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                String baseType = getTypeName(pt.getRawType()).replace("java.util.", "");
                return baseType + " of ?";  // Match expected output format
            }
            return simplifyType(type);
        }

        private boolean shouldSkipRelationship(String target) {
            return target.matches("^(void|int|float|boolean|char|byte|long|short|double)\\b");
            // Removed the target.contains(".") condition which was skipping all qualified names
        }
    }

    static class OutputFormatterFactory {
        static OutputFormatter createFormatter(CommandLineArgs config) {
            switch (config.format) {
                case "plantuml": return new PlantUMLFormatter();
                case "yuml": return new YUMLFormatter();
                default: return new TextFormatter();
            }
        }
    }
}