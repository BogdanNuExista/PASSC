@echo off
REM Compile the Java files
javac SchemaToJava.java ClassGenerator.java ClassInfo.java Field.java

REM Run the SchemaToJava program with dots.xsd
java SchemaToJava dots.xsd

REM Delete the .class files
del *.class

echo Done.