#!/usr/bin/env bash
set -e

# Navigate to the native directory
cd "$(dirname "$0")/native"

# Hardcode NDK path for now
NDK_PATH="$(cd .. && pwd)/target/ndk/android-ndk-r26c"

if [ ! -d "$NDK_PATH" ]; then
    echo "NDK path not found at $NDK_PATH. Please run setup-android.sh first."
    exit 1
fi

# Build the Rust code for the host platform (for development and testing)
echo "Building Rust code for host platform..."
cargo build --release

# Create the jniLibs directory structure
echo "Creating jniLibs directory structure..."
JNI_LIBS_DIR="../app/src/main/jniLibs"
mkdir -p "$JNI_LIBS_DIR/arm64-v8a"
mkdir -p "$JNI_LIBS_DIR/armeabi-v7a"
mkdir -p "$JNI_LIBS_DIR/x86"
mkdir -p "$JNI_LIBS_DIR/x86_64"

# Copy the host library to the jniLibs directory for testing
# This is a temporary solution until we get cross-compilation working properly
echo "Copying host library to jniLibs directories..."
cp "target/release/libhub.so" "$JNI_LIBS_DIR/arm64-v8a/"
cp "target/release/libhub.so" "$JNI_LIBS_DIR/armeabi-v7a/"
cp "target/release/libhub.so" "$JNI_LIBS_DIR/x86/"
cp "target/release/libhub.so" "$JNI_LIBS_DIR/x86_64/"

echo "Rust build completed successfully!"
echo "NOTE: Using host library for all architectures temporarily."
