@echo off
echo === Compiling and Running EPUB Test ===

set JAVA_HOME=C:\Users\nalabelle\scoop\apps\android-studio\current\jbr

echo Compiling TestEpubConversion.java...
"%JAVA_HOME%\bin\javac" TestEpubConversion.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    exit /b 1
)

echo Running test...
"%JAVA_HOME%\bin\java" TestEpubConversion

echo Test completed.
