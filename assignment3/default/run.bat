@echo off
REM Compile the tool
javac .\MyReverseEngineeringTool.java

REM Generate diagram.txt
java MyReverseEngineeringTool TempSensor.jar -ignore java.util,java.io -methods -attributes -output diagram.txt
