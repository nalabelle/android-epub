#!/usr/bin/env bash
set -e

# Set up paths
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
NDK_PATH="${PROJECT_ROOT}/target/ndk/android-ndk-r26c"
SDK_PATH="${PROJECT_ROOT}/target/android-sdk"

# Check if setup has been run
if [ ! -d "$NDK_PATH" ]; then
    echo "ERROR: NDK not found at $NDK_PATH"
    echo "Please run ./setup-android.sh first to set up the Android development environment"
    exit 1
fi

# Make sure SDK directory exists
mkdir -p "$SDK_PATH"

# Set up SDK command-line tools (if needed)
CMDLINE_TOOLS_DIR="$SDK_PATH/cmdline-tools/latest"
if [ ! -d "$CMDLINE_TOOLS_DIR" ]; then
    echo "Setting up Android SDK command-line tools..."
    mkdir -p "$SDK_PATH/cmdline-tools"
    
    # Download the command-line tools
    TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip"
    echo "Downloading Android SDK command-line tools..."
    wget -q "$TOOLS_URL" -O /tmp/cmdline-tools.zip
    
    # Extract to a temporary directory
    unzip -q /tmp/cmdline-tools.zip -d /tmp
    
    # Move to the correct location and rename to "latest"
    mv /tmp/cmdline-tools "$CMDLINE_TOOLS_DIR"
    
    # Clean up
    rm /tmp/cmdline-tools.zip
fi

# Set up environment variables for Android SDK
export ANDROID_HOME="$SDK_PATH"
export PATH="$CMDLINE_TOOLS_DIR/bin:$PATH"

# Accept licenses and install SDK components
echo "Accepting Android SDK licenses..."
yes | sdkmanager --licenses

echo "Installing required SDK components..."
yes | sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"

# Create local.properties with SDK path only (NDK is specified in build.gradle.kts)
echo "Setting up local.properties..."
echo "sdk.dir=$SDK_PATH" > "${PROJECT_ROOT}/local.properties"

# First build the Rust code
echo "Building Rust code..."
./build-rust.sh

# Generate Android bindings from Rust code
echo "Generating Android bindings..."
mkdir -p app/src/main/java/com/example/androidepub/generated
cd native/hub

# Use absolute paths
PROJECT_ROOT=$(cd ../.. && pwd)
LIB_PATH="${PROJECT_ROOT}/native/target/release/libhub.so"
OUT_DIR="${PROJECT_ROOT}/app/src/main/java/com/example/androidepub/generated"

# Generate bindings
cargo run --bin uniffi-bindgen -- generate src/hub.udl --lib-file "${LIB_PATH}" --language kotlin --out-dir "${OUT_DIR}"
cd ../..

# Then build the Android app
echo "Building Android app..."
./gradlew assembleDebug

# Get the path to the generated APK
APK_PATH="$(pwd)/app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK_PATH" ]; then
    echo "APK built successfully: $APK_PATH"
else
    echo "ERROR: APK file not found at expected path: $APK_PATH"
    exit 1
fi
