package com.example.androidepub.test

import com.example.androidepub.utils.EpubCreator
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubReader
import org.jsoup.Jsoup
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Simple test program to diagnose image reference issues in EPUB conversion
 */
fun main() {
    val testDir = File("test_epub")
    val htmlFile = File(testDir, "test.html")
    val epubFile = File(testDir, "test_output.epub")
    
    println("Starting EPUB test conversion...")
    println("HTML file: ${htmlFile.absolutePath}")
    println("EPUB output file: ${epubFile.absolutePath}")
    
    // Read the HTML content
    val htmlContent = htmlFile.readText()
    println("HTML content length: ${htmlContent.length} characters")
    
    // Parse the HTML to get the image src
    val doc = Jsoup.parse(htmlContent)
    val imgElement = doc.getElementById("test-image")
    val originalSrc = imgElement?.attr("src") ?: "No image found"
    println("Original image src: $originalSrc")
    
    // Create output directory if it doesn't exist
    epubFile.parentFile?.mkdirs()
    
    // Convert to EPUB
    val outputStream = FileOutputStream(epubFile)
    val baseUrl = htmlFile.toURI().toString()
    
    try {
        // Use our EpubCreator to convert the file
        // Since EpubCreator expects a URL, we'll use the file:// URL
        val result = EpubCreator.createEpubFromUrl(baseUrl, outputStream)
        println("Conversion result: ${result.success}, Message: ${result.message}")
        
        if (result.success) {
            // Now read the EPUB back to check image references
            outputStream.close()
            val inputStream = FileInputStream(epubFile)
            val epubReader = EpubReader()
            val book = epubReader.readEpub(inputStream)
            
            println("\nEPUB Content:")
            println("Title: ${book.title}")
            println("Creator: ${book.metadata.authors.joinToString { it.firstname + " " + it.lastname }}")
            println("Number of resources: ${book.resources.size}")
            
            // List all resources
            println("\nResources in EPUB:")
            book.resources.all.forEach { resource ->
                println("- ${resource.href} (${resource.mediaType})")
            }
            
            // Check HTML content for image references
            println("\nChecking HTML content for image references:")
            val htmlResources = book.resources.all.filter { it.mediaType.toString().contains("html") }
            htmlResources.forEach { htmlResource ->
                val content = String(htmlResource.data)
                val htmlDoc = Jsoup.parse(content)
                val images = htmlDoc.select("img")
                
                println("HTML resource: ${htmlResource.href}")
                println("Found ${images.size} image references:")
                
                images.forEach { img ->
                    val src = img.attr("src")
                    println("  - Image src: $src")
                    
                    // Check if this image exists as a resource
                    val imageResource = book.resources.getByHref(src)
                    if (imageResource != null) {
                        println("    ✓ Found matching resource: ${imageResource.href}")
                    } else {
                        println("    ✗ No matching resource found for this src")
                        
                        // Try to find a similar resource
                        val similarResources = book.resources.all.filter { 
                            it.mediaType.toString().contains("image") &&
                            (it.href.contains(src) || src.contains(it.href))
                        }
                        if (similarResources.isNotEmpty()) {
                            println("    Similar resources found:")
                            similarResources.forEach { 
                                println("      - ${it.href} (${it.mediaType})")
                            }
                        }
                    }
                }
            }
            
            println("\nTest completed successfully!")
        }
    } catch (e: Exception) {
        println("Error during conversion: ${e.message}")
        e.printStackTrace()
    } finally {
        try { outputStream.close() } catch (e: Exception) {}
    }
}
