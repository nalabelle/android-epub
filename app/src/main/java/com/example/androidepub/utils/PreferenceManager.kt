package com.example.androidepub.utils

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import androidx.core.content.edit
import androidx.preference.PreferenceManager as AndroidPreferenceManager

/**
 * Utility class to manage app preferences
 */
object PreferenceManager {
    private const val KEY_EPUB_STORAGE_LOCATION_URI = "epub_storage_location_uri"
    private const val KEY_INCLUDE_IMAGES = "include_images"
    private const val KEY_DEFAULT_AUTHOR = "default_author"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return AndroidPreferenceManager.getDefaultSharedPreferences(context)
    }

    /**
     * Get the URI string for the EPUB storage location
     */
    fun getEpubStorageLocationUri(context: Context): String {
        return getSharedPreferences(context).getString(KEY_EPUB_STORAGE_LOCATION_URI, "") ?: ""
    }

    /**
     * Set the URI string for the EPUB storage location
     */
    fun setEpubStorageLocationUri(context: Context, uriString: String) {
        getSharedPreferences(context).edit {
            putString(KEY_EPUB_STORAGE_LOCATION_URI, uriString)
        }
    }

    /**
     * Get a user-friendly description of the EPUB storage location
     */
    fun getEpubStorageLocation(context: Context): String {
        val uriString = getEpubStorageLocationUri(context)
        return if (uriString.isEmpty()) {
            // Default location
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        } else {
            try {
                val uri = uriString.toUri()
                val path = uri.path
                if (path != null) {
                    // Try to make the path more readable
                    path.substringAfterLast("/storage/")
                } else {
                    uri.toString()
                }
            } catch (e: Exception) {
                // Fallback to default
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
            }
        }
    }

    /**
     * Get the actual File object for the EPUB storage location
     */
    fun getEpubStorageDirectory(context: Context): java.io.File {
        val uriString = getEpubStorageLocationUri(context)
        if (uriString.isEmpty()) {
            // Default to Downloads directory
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }

        try {
            // Try to use the selected directory
            // Note: This is simplified and may not work for all URI types
            // A more robust implementation would use DocumentFile
            val uri = Uri.parse(uriString)
            val path = uri.path
            if (path != null) {
                val file = java.io.File(path)
                if (file.exists() && file.isDirectory) {
                    return file
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback to Downloads directory
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    }

    /**
     * Check if images should be included in the EPUB
     */
    fun shouldIncludeImages(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_INCLUDE_IMAGES, true)
    }

    /**
     * Get the default author name to use when not available
     */
    fun getDefaultAuthor(context: Context): String {
        return getSharedPreferences(context).getString(KEY_DEFAULT_AUTHOR, "Unknown Author") ?: "Unknown Author"
    }
}
