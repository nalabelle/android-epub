package com.example.androidepub.utils

import android.util.Log
import nl.siegmann.epublib.domain.Author
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.MediaType
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubWriter
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.File
import java.io.OutputStream
import java.net.URL
import java.util.UUID

object EpubCreator {
    private const val TAG = "EpubCreator"

    data class Result(
        val success: Boolean,
        val filePath: String? = null,
        val message: String? = null
    )

    /**
     * Creates an EPUB file from a URL and writes it to the provided OutputStream
     * @param url The URL to fetch content from
     * @param outputStream The OutputStream to write the EPUB to
     * @return Result object with success status and error message if applicable
     */
    fun createEpubFromUrl(url: String, outputStream: OutputStream): Result {
        try {
            // Validate URL
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return Result(false, null, "Invalid URL format. URL must start with http:// or https://")
            }

            try {
                // Fetch and parse the HTML content
                val connection = Jsoup.connect(url)
                    .userAgent("AndroidEpub/1.0 (EPUB Converter; https://github.com/example/android-epub; contact@example.com)")
                    .timeout(10000)
                    .ignoreHttpErrors(true)  // Don't throw exceptions for HTTP errors
                
                // Execute the request and get the response
                val response = connection.execute()
                
                // Only log detailed headers for non-success responses
                val statusCode = response.statusCode()
                if (statusCode >= 300) {
                    logResponseHeaders(url, response)
                } else {
                    // For successful responses, just log a brief message
                    Log.d(TAG, "Successful response (${statusCode}) for URL: $url")
                }
                
                // Check for HTTP errors
                if (statusCode >= 400) {
                    val errorMessage = "HTTP error $statusCode (${response.statusMessage()}): Unable to access the webpage. The site may be blocking automated access."
                    Log.e(TAG, errorMessage)
                    
                    // Log the full response body in chunks to avoid log truncation
                    val responseBody = response.body()
                    Log.e(TAG, "Response body length: ${responseBody.length} characters")
                    
                    // Log the body in chunks of 4000 characters (logcat has a limit)
                    responseBody.chunked(4000).forEachIndexed { index, chunk ->
                        Log.e(TAG, "Response body [part ${index+1}]: $chunk")
                    }
                    
                    return Result(false, null, errorMessage)
                }
                
                // Get the document from the response
                val document = response.parse()

                // Extract metadata
                val title = document.title() ?: "Untitled"
                val author = document.select("meta[name=author]").attr("content") ?: "Unknown Author"
                
                // Create the EPUB book
                val book = Book()
                
                // Set metadata
                book.metadata.addTitle(title)
                book.metadata.addAuthor(Author(author))
                
                // Process HTML and download images
                val (content, imageResources) = processHtmlContent(document, url)
                
                // Add the main content
                book.addSection(title, Resource(content.toByteArray(), "content.html"))
                
                // Add all downloaded images to the EPUB
                imageResources.forEach { (_, resource) ->
                    try {
                        // Add the resource to the book
                        book.resources.add(resource)
                        Log.d(TAG, "Added image to EPUB: ${resource.href}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to add image to EPUB: ${resource.href}", e)
                    }
                }
                
                // Write the EPUB file
                EpubWriter().write(book, outputStream)
                
                return Result(true, null, "EPUB created successfully")
            } catch (e: java.net.UnknownHostException) {
                Log.e(TAG, "Unknown host for URL: $url", e)
                return Result(false, null, "Unknown host: Could not connect to the website. Please check your internet connection or the URL.")
            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "Connection timed out for URL: $url", e)
                return Result(false, null, "Connection timed out: The website took too long to respond.")
            }
        } catch (e: Exception) {
            // Log the full exception details
            Log.e(TAG, "Error creating EPUB from URL: $url", e)
            
            // Create a more detailed error message
            val errorDetails = "Exception type: ${e.javaClass.simpleName}, Message: ${e.message}"
            Log.e(TAG, errorDetails)
            
            return Result(false, null, "Error creating EPUB: ${e.message}")
        }
    }
    
    /**
     * Process the HTML content to make it suitable for EPUB
     * @param document The Jsoup document
     * @param baseUrl The base URL for resolving relative links
     * @return Pair of processed HTML content as a string and a map of image resources
     */
    private fun processHtmlContent(document: Document, baseUrl: String): Pair<String, Map<String, Resource>> {
        Log.d(TAG, "Processing HTML content from $baseUrl")
        // Create a copy to avoid modifying the original
        val processedDoc = document.clone()
        
        // Remove unnecessary elements
        processedDoc.select("script, style, iframe, nav, footer, header, aside, .ads, .comments").remove()
        
        // Find the main content (this is a simple heuristic, can be improved)
        val mainContent = processedDoc.select("article, .article, .post, .content, main").first()
            ?: processedDoc.body()
        
        // Map to store downloaded images
        val imageResources = mutableMapOf<String, Resource>()
        
        // Process images - convert relative URLs to absolute and download images
        mainContent.select("img[src]").forEach { img ->
            processImage(img, baseUrl, imageResources)
        }
        
        // Create a simple HTML wrapper
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>${processedDoc.title()}</title>
            </head>
            <body>
                <h1>${processedDoc.title()}</h1>
                ${mainContent.html()}
            </body>
            </html>
        """.trimIndent()
        
        return Pair(htmlContent, imageResources)
    }
    
    /**
     * Logs the response headers for debugging purposes
     * @param url The URL that was requested
     * @param response The JSoup Connection.Response object
     */
    private fun logResponseHeaders(url: String, response: Connection.Response) {
        val statusCode = response.statusCode()
        val statusMessage = response.statusMessage()
        val contentType = response.contentType()
        
        Log.d(TAG, "Response for URL: $url")
        Log.d(TAG, "Status: $statusCode $statusMessage")
        Log.d(TAG, "Content-Type: $contentType")
        Log.d(TAG, "Headers:")
        
        // Log all headers
        response.headers().forEach { (name, value) ->
            Log.d(TAG, "  $name: $value")
        }
    }
    
    /**
     * Process an image element - convert relative URLs to absolute and download the image
     * @param img The image element to process
     * @param baseUrl The base URL for resolving relative links
     * @param imageResources Map to store downloaded image resources
     */
    private fun processImage(img: Element, baseUrl: String, imageResources: MutableMap<String, Resource>) {
        val src = img.attr("src")
        
        try {
            // Skip empty sources
            if (src.isBlank()) {
                return
            }
            
            // Handle data URLs separately
            if (src.startsWith("data:")) {
                // Data URLs are already embedded, no need to download
                return
            }
            
            // Skip javascript and other special protocols
            if (src.startsWith("javascript:") || (src.contains("://") && !src.startsWith("http"))) {
                return
            }
            
            // Convert relative URLs to absolute
            val absoluteUrl = if (!src.startsWith("http")) {
                try {
                    URL(URL(baseUrl), src).toString()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to convert image URL: $src", e)
                    return
                }
            } else {
                src
            }
            
            // Download the image if we haven't already
            if (!imageResources.containsKey(absoluteUrl)) {
                try {
                    // Generate a unique filename for the image
                    val extension = getFileExtension(absoluteUrl)
                    val filename = "image_${UUID.randomUUID().toString().substring(0, 8)}$extension"
                    
                    // Download the image
                    val imageBytes = downloadImage(absoluteUrl)
                    if (imageBytes != null) {
                        // Create a resource for the image with appropriate media type and href
                        // Extract just the filename from the source URL, removing any query parameters
                        val originalFilename = src.substringAfterLast('/').substringBefore('?')
                        
                        // Check if this looks like a hash (no extension, alphanumeric, 20+ chars)
                        val isLikelyHash = !originalFilename.contains('.') && 
                                           originalFilename.matches(Regex("^[a-zA-Z0-9]+$")) && 
                                           originalFilename.length >= 20
                        
                        // Ensure the filename has a valid extension
                        val filenameWithExtension = when {
                            // For hash-like filenames, add a more descriptive name with extension
                            isLikelyHash -> "image_${originalFilename.take(8)}.jpg"
                            // For filenames without extension, add .jpg
                            !originalFilename.contains('.') -> "$originalFilename.jpg"
                            // Otherwise use the original filename
                            else -> originalFilename
                        }
                        
                        // Create a resource with the filename
                        val resource = Resource(imageBytes, filenameWithExtension)
                        
                        // Add to our resources map
                        imageResources[absoluteUrl] = resource
                        
                        // Always update the src attribute to match the resource href
                        // This ensures the HTML reference matches the resource name in the EPUB
                        img.attr("src", filenameWithExtension)
                        
                        Log.d(TAG, "Downloaded image: $absoluteUrl as $filename")
                    } else {
                        Log.e(TAG, "Failed to download image: $absoluteUrl")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing image: $absoluteUrl", e)
                }
            } else {
                // Image already downloaded, just update the src attribute if needed
                val resource = imageResources[absoluteUrl]!!
                // Only update if the current src doesn't match the resource href
                if (img.attr("src") != resource.href) {
                    img.attr("src", resource.href)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image element with src: $src", e)
        }
    }
    
    /**
     * Download an image from a URL
     * @param imageUrl The URL of the image to download
     * @return The image bytes or null if download failed
     */
    private fun downloadImage(imageUrl: String): ByteArray? {
        try {
            val connection = Jsoup.connect(imageUrl)
                .userAgent("AndroidEpub/1.0 (EPUB Converter; https://github.com/example/android-epub; contact@example.com)")
                .timeout(10000)
                .ignoreContentType(true) // Important for binary content like images
                .maxBodySize(10 * 1024 * 1024) // 10MB max size
                .execute()
            
            if (connection.statusCode() == 200) {
                return connection.bodyAsBytes()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image: $imageUrl", e)
        }
        return null
    }
    
    /**
     * Get the file extension from a URL
     * @param url The URL to extract extension from
     * @return The file extension including the dot, or .jpg as default
     */
    private fun getFileExtension(url: String): String {
        // Remove query parameters before processing the URL
        val urlWithoutQuery = url.substringBefore('?')
        
        val path = try {
            URL(urlWithoutQuery).path
        } catch (e: Exception) {
            return ".jpg" // Default extension
        }
        
        val lastDotIndex = path.lastIndexOf('.')
        if (lastDotIndex != -1 && lastDotIndex < path.length - 1) {
            val extension = path.substring(lastDotIndex)
            // Validate it's a common image extension
            return when (extension.lowercase()) {
                ".jpg", ".jpeg", ".png", ".gif", ".svg", ".webp" -> extension
                else -> ".jpg" // Default extension
            }
        }
        return ".jpg" // Default extension
    }
}
