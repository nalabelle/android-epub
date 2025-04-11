package com.example.androidepub.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import nl.siegmann.epublib.epub.EpubReader
import org.jsoup.Jsoup
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Helper class to test EPUB conversion with images
 */
object EpubTestHelper {
    private const val TAG = "EpubTestHelper"

    /**
     * Convert a local HTML file to EPUB for testing
     * @param context The Android context
     * @param htmlFile The HTML file to convert
     * @param outputFile The output EPUB file
     * @return A detailed report of the conversion process
     */
    fun testHtmlToEpub(context: Context, htmlFile: File, outputFile: File): String {
        val report = StringBuilder()
        report.appendLine("=== EPUB Conversion Test Report ===")
        report.appendLine("HTML file: ${htmlFile.absolutePath}")
        report.appendLine("EPUB output file: ${outputFile.absolutePath}")
        report.appendLine()

        try {
            // Read the HTML content
            val htmlContent = htmlFile.readText()
            report.appendLine("HTML content length: ${htmlContent.length} characters")
            
            // Parse the HTML to get the image src
            val doc = Jsoup.parse(htmlContent)
            val imgElements = doc.select("img")
            report.appendLine("Found ${imgElements.size} image elements in HTML:")
            
            imgElements.forEach { img ->
                val src = img.attr("src")
                report.appendLine("- Image src: $src")
            }
            report.appendLine()
            
            // Create output directory if it doesn't exist
            outputFile.parentFile?.mkdirs()
            
            // Convert to EPUB
            val outputStream = FileOutputStream(outputFile)
            val baseUrl = "file://${htmlFile.absolutePath}"
            
            report.appendLine("Converting HTML to EPUB...")
            report.appendLine("Base URL: $baseUrl")
            
            // Use our EpubCreator to convert the file
            val result = EpubCreator.createEpubFromUrl(baseUrl, outputStream)
            report.appendLine("Conversion result: ${result.success}, Message: ${result.message}")
            outputStream.close()
            
            if (result.success) {
                // Now read the EPUB back to check image references
                val inputStream = FileInputStream(outputFile)
                val epubReader = EpubReader()
                val book = epubReader.readEpub(inputStream)
                
                report.appendLine("\nEPUB Content:")
                report.appendLine("Title: ${book.title}")
                report.appendLine("Creator: ${book.metadata.authors.joinToString { it.firstname + " " + it.lastname }}")
                report.appendLine("Number of resources: ${book.resources.all.size}")
                
                // List all resources
                report.appendLine("\nResources in EPUB:")
                book.resources.all.forEach { resource ->
                    report.appendLine("- ${resource.href} (${resource.mediaType})")
                }
                
                // Check HTML content for image references
                report.appendLine("\nChecking HTML content for image references:")
                val htmlResources = book.resources.all.filter { it.mediaType.toString().contains("html") }
                htmlResources.forEach { htmlResource ->
                    val content = String(htmlResource.data)
                    val htmlDoc = Jsoup.parse(content)
                    val images = htmlDoc.select("img")
                    
                    report.appendLine("HTML resource: ${htmlResource.href}")
                    report.appendLine("Found ${images.size} image references:")
                    
                    images.forEach { img ->
                        val src = img.attr("src")
                        report.appendLine("  - Image src: $src")
                        
                        // Check if this image exists as a resource
                        val imageResource = book.resources.getByHref(src)
                        if (imageResource != null) {
                            report.appendLine("    ✓ Found matching resource: ${imageResource.href}")
                        } else {
                            report.appendLine("    ✗ No matching resource found for this src")
                            
                            // Try to find a similar resource
                            val similarResources = book.resources.all.filter { 
                                it.mediaType.toString().contains("image") &&
                                (it.href.contains(File(src).name) || File(src).name.contains(it.href))
                            }
                            if (similarResources.isNotEmpty()) {
                                report.appendLine("    Similar resources found:")
                                similarResources.forEach { 
                                    report.appendLine("      - ${it.href} (${it.mediaType})")
                                }
                            }
                        }
                    }
                }
                
                report.appendLine("\nTest completed successfully!")
                inputStream.close()
            }
        } catch (e: Exception) {
            report.appendLine("\nError during conversion: ${e.message}")
            Log.e(TAG, "Error during test conversion", e)
        }
        
        return report.toString()
    }
}
