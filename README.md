# Android EPUB Creator

A minimalist Android application that converts web content to EPUB format. Simply input a URL, and the app will create an EPUB file with enhanced readability.

## Features

- Single-purpose: Convert web content to EPUB
- Enhanced readability using Mozilla's readability algorithm port
- Automatic content extraction and cleanup
- Saves EPUBs directly to your Downloads directory
- No history tracking or storage

## How to Use

1. Open the app
2. Enter a URL
3. Tap "Create EPUB"
4. The EPUB will be saved to your Downloads directory
5. Open with your preferred EPUB reader

## Technical Details

This app is built using:

- Kotlin for the Android UI
- Android Jetpack components
- Rust for the EPUB conversion core functionality
- UniFFI for Rust-Android integration
- http-epub Rust library for web content processing

## Permissions

The app requires the following permissions:

- Internet access: To download content from URLs
- Storage access: To save EPUB files to your device

## Building the Project

### Prerequisites

- Rust toolchain (rustup)
- Android NDK
- Android SDK
- Cargo NDK (`cargo install cargo-ndk`)

### Setup

1. Clone the repository
2. Run `./setup-android.sh` to set up the Android NDK and Rust targets
3. Run `./build-rust.sh` to build the Rust library
4. Run `./build-android.sh` to generate the Kotlin bindings and build the Android app

### Project Structure

- `native/hub/`: Rust code for the EPUB conversion
  - `src/hub.udl`: UniFFI interface definition
  - `src/lib.rs`: Rust implementation
- `app/`: Android application code
  - `src/main/java/com/example/androidepub/generated/`: Generated UniFFI bindings
  - `src/main/jniLibs/`: Native libraries for different architectures

## License

This project is open source and available under the MIT License.
