import com.example.androidepub.utils.EpubCreator
import nl.siegmann.epublib.epub.EpubReader
import org.jsoup.Jsoup
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Simple test program to verify EPUB creation with images
 */
fun main() {
    println("=== EPUB Conversion Test ===")
    
    // Set up file paths
    val htmlFile = File("test_epub/test.html")
    val outputFile = File("test_epub/output.epub")
    
    println("HTML file: ${htmlFile.absolutePath}")
    println("EPUB output file: ${outputFile.absolutePath}")
    
    try {
        // Read the HTML content
        val htmlContent = htmlFile.readText()
        println("HTML content length: ${htmlContent.length} characters")
        
        // Parse the HTML to get the image src
        val doc = Jsoup.parse(htmlContent)
        val imgElements = doc.select("img")
        println("Found ${imgElements.size} image elements in HTML:")
        
        imgElements.forEach { img ->
            val src = img.attr("src")
            println("- Image src: $src")
        }
        
        // Create output directory if it doesn't exist
        outputFile.parentFile?.mkdirs()
        
        // Convert to EPUB
        val outputStream = FileOutputStream(outputFile)
        val baseUrl = "file://${htmlFile.absolutePath}"
        
        println("Converting HTML to EPUB...")
        println("Base URL: $baseUrl")
        
        // Use our EpubCreator to convert the file
        val result = EpubCreator.createEpubFromUrl(baseUrl, outputStream)
        println("Conversion result: ${result.success}, Message: ${result.message}")
        outputStream.close()
        
        if (result.success) {
            // Now read the EPUB back to check image references
            val inputStream = FileInputStream(outputFile)
            val epubReader = EpubReader()
            val book = epubReader.readEpub(inputStream)
            
            println("\nEPUB Content:")
            println("Title: ${book.title}")
            println("Creator: ${book.metadata.authors.joinToString { it.firstname + " " + it.lastname }}")
            println("Number of resources: ${book.resources.all.size}")
            
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
                            (it.href.contains(File(src).name) || File(src).name.contains(it.href))
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
            inputStream.close()
        }
    } catch (e: Exception) {
        println("\nError during conversion: ${e.message}")
        e.printStackTrace()
    }
}
