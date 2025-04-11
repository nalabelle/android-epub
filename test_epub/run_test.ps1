$env:JAVA_HOME = 'C:\Users\nalabelle\scoop\apps\android-studio\current\jbr'

# Compile the test program
Write-Host "Compiling test program..."
$kotlinc = "$env:JAVA_HOME\bin\java.exe -jar ..\app\libs\kotlin-compiler.jar"

# Create a libs directory if it doesn't exist
if (-not (Test-Path -Path "libs")) {
    New-Item -Path "libs" -ItemType Directory | Out-Null
}

# Copy required libraries from the app
Write-Host "Copying required libraries..."
Copy-Item -Path "..\app\libs\*" -Destination "libs\" -Recurse -Force

# Create a classpath with all the required libraries
$libs = Get-ChildItem -Path "libs" -Filter "*.jar" | ForEach-Object { $_.FullName }
$classpath = $libs -join ";"

# Add the app classes
$classpath = "$classpath;..\app\build\intermediates\javac\debug\classes;..\app\build\tmp\kotlin-classes\debug"

# Compile the test program
Write-Host "Compiling EpubTest.kt..."
& $env:JAVA_HOME\bin\java.exe -jar libs\kotlin-compiler.jar -cp "$classpath" -d "out" "EpubTest.kt"

# Run the test program
Write-Host "Running test program..."
& $env:JAVA_HOME\bin\java.exe -cp "$classpath;out" com.example.androidepub.test.EpubTestKt
