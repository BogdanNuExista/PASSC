import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MyReverseEngineeringTool {
    private static final String OUTPUT_FILE = "output.txt";
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
        for (Class<?> clazz : PROCESSED_CLASSES) {
            for (RelationshipType type : RelationshipType.values()) {
                RelationshipDetector detector = factory.createDetector(type);
                detector.detect(clazz, RELATIONSHIPS);
            } 
        }
    }

    private static void writeOutput() throws IOException {
        try (PrintWriter writer = new PrintWriter(config.outputFile)) {
            writer.println("=== Class Diagram ===");
            
            // Write classes and interfaces
            for (Class<?> clazz : PROCESSED_CLASSES) {
                if (clazz.isInterface()) {
                    writer.println("\nInterface: " + getClassName(clazz));
                } else {
                    writer.println("\nClass: " + getClassName(clazz));
                }
                
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
    
            // Write relationships
            writer.println("\n=== Relationships ===");
            for (Map.Entry<String, Set<String>> entry : RELATIONSHIPS.entrySet()) {
                String[] parts = entry.getKey().split(" -> ");
                writer.printf("%s %s %s%n",
                    getClassName(parts[0]),
                    String.join(", ", entry.getValue()),
                    getClassName(parts[1]));
            }
        }
    }

    // Helper methods
    private static String getClassName(Class<?> clazz) {
        return config.qualifiedNames ? clazz.getName() : clazz.getSimpleName();
    }

    private static String getClassName(String fullName) {
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
        String outputFile = OUTPUT_FILE;

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
    
        protected boolean shouldSkipType(Class<?> type) {
            return type.isPrimitive()
                || type.getName().startsWith("java.") 
                || type.isArray();
        }
    
        protected void addRelationship(Class<?> source, Class<?> target, String relType) {
            if (!shouldSkipType(target) && config.ignoredPackages.stream().noneMatch(target.getName()::startsWith)) {
                String key = source.getName() + " -> " + target.getName();
                RELATIONSHIPS.computeIfAbsent(key, _ -> new HashSet<>()).add(relType);
            }
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
                if (fieldType instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) fieldType;
                    Type[] typeArgs = pt.getActualTypeArguments();
                    for (Type typeArg : typeArgs) {
                        if (typeArg instanceof Class) {
                            Class<?> argClass = (Class<?>) typeArg;
                            if (!shouldSkipType(argClass)) {
                                addRelationship(clazz, argClass, "association");
                            }
                        }
                    }
                } else if (fieldType instanceof Class) {
                    Class<?> cls = (Class<?>) fieldType;
                    if (!shouldSkipType(cls)) {
                        addRelationship(clazz, cls, "association");
                    }
                }
            }
        }
    }

    static class DependencyDetector extends BaseDetector {
        DependencyDetector(CommandLineArgs config) { super(config); }
    
        public void detect(Class<?> clazz, Map<String, Set<String>> relationships) {
            if (clazz.isInterface()) return; // Skip interfaces
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
}