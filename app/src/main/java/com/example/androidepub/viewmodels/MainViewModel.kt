package com.example.androidepub.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidepub.utils.EpubCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                        val outputStream = context.contentResolver.openOutputStream(it)
                        if (outputStream != null) {
                            val result = outputStream.use { stream ->
                                EpubCreator.createEpubFromUrl(url, stream)
                            }
                            if (result.success) {
                                EpubCreator.Result(true, epubUri.toString(), result.message)
                            } else {
                                // If there was an error, delete the empty file
                                try {
                                    DocumentsContract.deleteDocument(context.contentResolver, epubUri)
                                } catch (e: Exception) {
                                    // Ignore errors when trying to delete the file
                                }
                                result
                            }
                        } else {
                            EpubCreator.Result(false, null, "Failed to open output stream")
                        }
                    } ?: EpubCreator.Result(false, null, "Failed to create output file")
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
