# Test script for EPUB conversion
# This script builds the app and then uses adb to push the test HTML and image to the device,
# run the conversion, and pull back the resulting EPUB file for inspection

# Set Java home to match your build environment
$env:JAVA_HOME = 'C:\Users\nalabelle\scoop\apps\android-studio\current\jbr'

# Set ADB path
$adbPath = 'C:\Users\nalabelle\AppData\Local\Android\Sdk\platform-tools\adb.exe'

# Build the app
Write-Host "Building the app..."
Push-Location ..
$env:JAVA_HOME = 'C:\Users\nalabelle\scoop\apps\android-studio\current\jbr'
.\gradlew.bat clean assembleDebug
Pop-Location

# Check if the build was successful
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed. Exiting."
    exit 1
}

Write-Host "Build successful."

# Install the app
Write-Host "Installing the app..."
& $adbPath install -r ..\app\build\outputs\apk\debug\app-debug.apk

# Create a test directory on the device
Write-Host "Creating test directory on device..."
& $adbPath shell "mkdir -p /sdcard/Download/epub_test"

# Push the test HTML and image to the device
Write-Host "Pushing test files to device..."
& $adbPath push test.html /sdcard/Download/epub_test/
& $adbPath push e6a9d63d-eec0-4497-bcfa-062d4ad89aba.png /sdcard/Download/epub_test/

# Launch the app and share the test HTML file
Write-Host "Launching app with test HTML..."
# First, let's check if the app is already running and kill it if needed
& $adbPath shell "am force-stop com.example.androidepub"

# Disable waiting for debugger
& $adbPath shell "am set-debug-app -w --persistent com.example.androidepub"
& $adbPath shell "am clear-debug-app"

# Now launch with the intent using a different approach
& $adbPath shell "am start -n com.example.androidepub/.MainActivity"
Start-Sleep -Seconds 2
& $adbPath shell "am broadcast -a android.intent.action.SEND -e android.intent.extra.TEXT file:///sdcard/Download/epub_test/test.html --es android.intent.extra.STREAM file:///sdcard/Download/epub_test/test.html -t text/html"

Write-Host "Test setup complete."
Write-Host "Please complete these steps manually:"
Write-Host "1. Select an output directory in the app if prompted"
Write-Host "2. The app should automatically process the HTML file"
Write-Host "3. Once conversion is complete, find the EPUB file in your selected output directory"
Write-Host "4. Open the EPUB file with an EPUB reader to verify images are displayed correctly"
