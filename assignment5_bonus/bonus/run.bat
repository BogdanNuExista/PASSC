@echo off
echo Compiling Java files...
javac *.java

echo Running TestBooks...
java TestBooks

echo Running TestDots...
java TestDots

echo Cleaning up .class files...
del /s *.class

echo Done!