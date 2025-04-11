# Set Java home to match your build environment
$env:JAVA_HOME = 'C:\Users\nalabelle\scoop\apps\android-studio\current\jbr'

# Compile the test program
Write-Host "Compiling the test program..."
Push-Location ..

# Create output directory if it doesn't exist
$outputDir = "app\build\test-output"
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

# Copy the test file to the project directory for easier access
Copy-Item "test_epub\TestEpubCreation.kt" "app\src\main\java\com\example\androidepub\TestEpubCreation.kt"

# Modify the package declaration in the file
$content = Get-Content "app\src\main\java\com\example\androidepub\TestEpubCreation.kt"
$content[0] = "package com.example.androidepub"
$content | Set-Content "app\src\main\java\com\example\androidepub\TestEpubCreation.kt"

# Build the project
.\gradlew.bat clean build

# Run the test program
Write-Host "Running the test program..."
.\gradlew.bat runTest

Pop-Location

Write-Host "Test completed."
