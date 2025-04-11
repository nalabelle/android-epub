use std::path::Path;
use anyhow::{Result, Context, Error};
use thiserror::Error;

// Include the generated bindings
uniffi::include_scaffolding!("hub");

// Define custom error types that map to the UDL errors
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

// Simple struct to hold the conversion parameters
struct EpubConversionRequest {
    url: String,
    output_path: String,
    title: Option<String>,
}

// Implementation of the url_to_epub function defined in the UDL
pub fn url_to_epub(url: String, output_path: String, title: Option<String>) -> Result<String, EpubError> {
    // Create a request structure to hold the parameters
    let request = EpubConversionRequest {
        url,
        output_path,
        title,
    };
    
    // Call our internal implementation and map errors to the appropriate types
    match create_epub_internal(&request) {
        Ok(path) => Ok(path),
        Err(e) => {
            // Map anyhow errors to our specific error types for better error reporting
            if e.to_string().contains("URL") {
                Err(EpubError::InvalidUrl)
            } else if e.to_string().contains("download") {
                Err(EpubError::DownloadFailed(e.to_string()))
            } else if e.to_string().contains("file") || e.to_string().contains("permission") {
                Err(EpubError::FileSystemError(e.to_string()))
            } else {
                Err(EpubError::ProcessingFailed(e.to_string()))
            }
        }
    }
}

// Internal function to handle EPUB creation
fn create_epub_internal(request: &EpubConversionRequest) -> Result<String> {
    // Create the output directory if it doesn't exist
    let output_path = Path::new(&request.output_path);
    if let Some(parent) = output_path.parent() {
        if !parent.exists() {
            std::fs::create_dir_all(parent)
                .context("Failed to create output directory")?;
        }
    }

    // Call the http-epub library to create the EPUB
    let path = http_epub::url_to_epub(
        &request.url,
        Some(&output_path.to_path_buf()),
        request.title.as_deref()
    ).context("Failed to create EPUB")?;

    Ok(path.to_string_lossy().to_string())
}

// Implementation of the create_epub_from_url function that returns bytes
pub fn create_epub_from_url(url: String, title: Option<String>) -> Result<Vec<u8>, EpubError> {
    // Create a temporary directory for the EPUB file
    let temp_dir = tempfile::tempdir()
        .map_err(|e| EpubError::FileSystemError(format!("Failed to create temp directory: {}", e)))?;
    
    // Generate a temporary file path
    let output_path = temp_dir.path().join("output.epub");
    let output_path_str = output_path.to_str()
        .ok_or_else(|| EpubError::FileSystemError("Invalid temp path".to_string()))?
        .to_string();
    
    // Create the EPUB file
    url_to_epub(url, output_path_str, title)?;
    
    // Read the file into memory
    let bytes = std::fs::read(&output_path)
        .map_err(|e| EpubError::FileSystemError(format!("Failed to read EPUB file: {}", e)))?;
    
    Ok(bytes)
}
