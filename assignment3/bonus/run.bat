@echo off
REM Compile the tool
javac .\MyReverseEngineeringTool.java

REM Generate yUML diagram
java MyReverseEngineeringTool EventNotifier.jar -format yuml -ignore java.util,java.lang -methods -attributes -output diagram_yuml.txt

REM Generate PlantUML diagram
java MyReverseEngineeringTool EventNotifier.jar -format plantuml -ignore java.util,java.lang -methods -attributes -output diagram_plantuml.txt

echo Done! Diagrams generated.