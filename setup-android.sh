#!/usr/bin/env bash
set -e

# Setup script for Android cross-compilation
echo "Setting up Android NDK and tools..."

# Add Android ARM64 target to rustup
rustup target add aarch64-linux-android

# Create NDK directory in target
mkdir -p "$(pwd)/target/ndk"

# Download and extract Android NDK if not already installed
if [ ! -d "$(pwd)/target/ndk/android-ndk-r26c" ]; then
  # Create a temporary directory for the download
  TEMP_DIR=$(mktemp -d)
  
  NDK_ZIP="$TEMP_DIR/android-ndk.zip"
  
  echo "Downloading Android NDK r26c..."
  curl -L https://dl.google.com/android/repository/android-ndk-r26c-linux.zip -o "$NDK_ZIP"
  
  # Verify checksum (SHA-256)
  echo "Verifying download integrity..."
  EXPECTED_CHECKSUM="6d6e659834d28bb24ba7ae66148ad05115ebbad7dabed1af9b3265674774fcf6"
  ACTUAL_CHECKSUM=$(sha256sum "$NDK_ZIP" | cut -d' ' -f1)
  
  if [ "$EXPECTED_CHECKSUM" != "$ACTUAL_CHECKSUM" ]; then
    echo "Checksum verification failed!"
    echo "Expected: $EXPECTED_CHECKSUM"
    echo "Actual: $ACTUAL_CHECKSUM"
    exit 1
  fi
  
  echo "Checksum verified successfully."
  echo "Extracting Android NDK..."
  unzip -q "$NDK_ZIP" -d "$(pwd)/target/ndk"
  
  # Clean up
  rm -rf "$TEMP_DIR"
  
  echo "Android NDK setup complete."
else
  echo "Android NDK already installed."
fi

# Install cargo-ndk if not already installed
if ! command -v cargo-ndk &> /dev/null; then
  echo "Installing cargo-ndk..."
  cargo install cargo-ndk
else
  echo "cargo-ndk already installed."
fi

echo "Android setup complete!"
echo "Run ./build-android.sh to build the application"
