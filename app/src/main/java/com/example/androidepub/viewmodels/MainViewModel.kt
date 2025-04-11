package com.example.androidepub.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import uniffi.hub.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel : ViewModel() {
    
    // LiveData for UI state
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String>("") 
    val errorMessage: LiveData<String> = _errorMessage
    
    private val _successMessage = MutableLiveData<String>("") 
    val successMessage: LiveData<String> = _successMessage
    
    private val _statusMessage = MutableLiveData<String>("") 
    val statusMessage: LiveData<String> = _statusMessage
    
    fun createEpubFromUrl(context: Context, url: String, outputUri: Uri) {
        _isLoading.value = true
        _statusMessage.value = "Creating EPUB from $url..."
        
        viewModelScope.launch {
            try {
                // Create a data class to hold the result
                data class ConversionResult(val success: Boolean, val filePath: String?, val message: String?)
                
                val result = withContext(Dispatchers.IO) {
                    // Get the document tree from the URI
                    val treeUri = DocumentsContract.buildDocumentUriUsingTree(
                        outputUri,
                        DocumentsContract.getTreeDocumentId(outputUri)
                    )
                    
                    // Create a new document for the EPUB
                    val filename = "${System.currentTimeMillis()}.epub"
                    val mimeType = "application/epub+zip"
                    
                    val epubUri = DocumentsContract.createDocument(
                        context.contentResolver,
                        treeUri,
                        mimeType,
                        filename
                    )
                    
                    epubUri?.let {
                        // Create a temporary file to store the EPUB
                        val tempFile = File(context.cacheDir, filename)
                        
                        try {
                            // Call the Rust function to create the EPUB
                            // The uniffi-generated function throws exceptions on error
                            val epubBytes = createEpubFromUrl(url, null)
                            
                            // Convert UByte list to ByteArray
                            val byteArray = epubBytes.map { it.toByte() }.toByteArray()
                            
                            // Write the bytes to a file
                            tempFile.writeBytes(byteArray)
                            
                            // Copy the temporary file to the output URI
                            val outputStream = context.contentResolver.openOutputStream(epubUri)
                            if (outputStream != null) {
                                outputStream.use { stream ->
                                    tempFile.inputStream().use { input ->
                                        input.copyTo(stream)
                                    }
                                }
                                // Delete the temporary file
                                tempFile.delete()
                                ConversionResult(
                                    success = true,
                                    filePath = epubUri.toString(),
                                    message = "EPUB created successfully"
                                )
                            } else {
                                // Clean up if we couldn't open the output stream
                                tempFile.delete()
                                try {
                                    DocumentsContract.deleteDocument(context.contentResolver, epubUri)
                                } catch (e: Exception) {
                                    // Ignore errors when trying to delete the file
                                }
                                ConversionResult(
                                    success = false,
                                    filePath = null,
                                    message = "Failed to open output stream"
                                )
                            }
                        } catch (e: EpubException) {
                            // Handle specific errors from Rust
                            // If there was an error, delete the empty file
                            tempFile.delete()
                            try {
                                DocumentsContract.deleteDocument(context.contentResolver, epubUri)
                            } catch (deleteEx: Exception) {
                                // Ignore errors when trying to delete the file
                            }
                            ConversionResult(
                                success = false,
                                filePath = null,
                                message = e.message
                            )
                        } catch (e: Exception) {
                            // Handle other exceptions
                            tempFile.delete()
                            try {
                                DocumentsContract.deleteDocument(context.contentResolver, epubUri)
                            } catch (deleteEx: Exception) {
                                // Ignore errors when trying to delete the file
                            }
                            ConversionResult(
                                success = false,
                                filePath = null,
                                message = "Error: ${e.message}"
                            )
                        }
                    } ?: ConversionResult(
                        success = false,
                        filePath = null,
                        message = "Failed to create output file"
                    )
                }
                
                if (result.success) {
                    _successMessage.value = "EPUB created successfully!"
                    _statusMessage.value = "EPUB created successfully!"
                } else {
                    _errorMessage.value = result.message ?: "Failed to create EPUB"
                    _statusMessage.value = result.message ?: "Failed to create EPUB"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An unexpected error occurred"
                _statusMessage.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun openEpubFile(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/epub+zip")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                _errorMessage.value = "No app found to open EPUB files"
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error opening file: ${e.message}"
        }
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = ""
    }
    
    fun clearSuccessMessage() {
        _successMessage.value = ""
    }
    
    fun clearStatusMessage() {
        _statusMessage.value = ""
    }
}
