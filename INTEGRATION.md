# Rust-Android Integration Guide

This document explains the integration between Rust and Android using UniFFI in the Android EPUB Creator app.

## Overview

We've replaced the previous Java/Kotlin-based EPUB conversion with a Rust implementation using the http-epub library. This provides better performance and more reliable EPUB creation. The integration uses UniFFI to generate Kotlin bindings for the Rust code, allowing seamless communication between the Android UI and Rust backend.

## Key Components

### 1. Rust Library (`native/hub/`)

- **Interface Definition**: `src/hub.udl` defines the API contract between Rust and Kotlin
- **Implementation**: `src/lib.rs` contains the actual EPUB conversion logic
- **Error Handling**: Custom error types that provide clear, specific error messages

### 2. UniFFI Bindings

- Generated Kotlin code that provides a type-safe interface to the Rust functions
- Handles marshaling data between Kotlin and Rust
- Automatically maps Rust errors to Kotlin exceptions

### 3. Android Integration

- Native libraries loaded via JNI
- Kotlin code calls the generated bindings directly
- Error handling propagated through the UI

## How It Works

1. The Android UI collects the URL and output path from the user
2. The ViewModel calls the Rust function through the UniFFI bindings
3. The Rust code downloads and processes the web content
4. The EPUB is created and saved to the specified location
5. Results or errors are returned to the Android UI

## Error Handling

We follow the "fail fast and loud" principle:

```rust
// In Rust
#[derive(Debug, Error)]
pub enum EpubError {
    #[error("Invalid URL provided")]
    InvalidUrl,
    #[error("Failed to download content: {0}")]
    DownloadFailed(String),
    #[error("Failed to process content: {0}")]
    ProcessingFailed(String),
    #[error("File system error: {0}")]
    FileSystemError(String),
}
```

```kotlin
// In Kotlin (ViewModel)
try {
    val outputPath = urlToEpub(url, tempFile.absolutePath, null)
    // Handle success
} catch (e: EpubError) {
    // Handle specific error types
    when (e) {
        is InvalidUrl -> // Handle invalid URL
        is DownloadFailed -> // Handle download failure
        else -> // Handle other errors
    }
}
```

## Build Process

The build process has been streamlined with improved scripts:

1. **Setup**: Run `./setup-android.sh` to install the Android NDK and Rust targets
   - Downloads and verifies the Android NDK
   - Adds required Rust targets for Android
   - Installs cargo-ndk for future cross-compilation

2. **Build**: Run `./build-android.sh` for a complete build
   - This script handles the entire build process:
   - Sets up Android SDK and accepts licenses
   - Builds the Rust library with `build-rust.sh`
   - Generates UniFFI Kotlin bindings
   - Builds the Android APK
   - Verifies the APK was created successfully

3. **Output**: The final APK is available at `app/build/outputs/apk/debug/app-debug.apk`

## Testing

For development and testing, we use the host platform's Rust library. In production, you should cross-compile for all Android architectures using cargo-ndk.

## Benefits Over Previous Implementation

1. **Performance**: Rust is significantly faster than Java/Kotlin for this workload
2. **Memory Safety**: Rust's ownership model prevents memory leaks and crashes
3. **Error Handling**: Clear, specific error messages that fail fast
4. **Maintainability**: Cleaner separation of concerns between UI and backend
5. **Simplicity**: Direct integration without unnecessary layers
