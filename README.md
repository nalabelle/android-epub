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

- Kotlin
- Android Jetpack components
- Epublib for EPUB creation
- Jsoup for HTML parsing

## Permissions

The app requires the following permissions:

- Internet access: To download content from URLs
- Storage access: To save EPUB files to your device

## Building the Project

1. Clone the repository
2. Open the project in Android Studio
3. Build and run the app on your device or emulator

## License

This project is open source and available under the MIT License.
