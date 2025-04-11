package com.example.androidepub

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.androidepub.utils.EpubCreator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Activity for testing EPUB conversion locally without relying on external intents
 */
class LocalTestActivity : AppCompatActivity() {
    private val TAG = "LocalTestActivity"
    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_test)

        resultTextView = findViewById(R.id.resultTextView)
        val runTestButton = findViewById<Button>(R.id.runTestButton)

        runTestButton.setOnClickListener {
            runTest()
        }
    }

    private fun runTest() {
        resultTextView.text = "Starting test..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create test directory
                val testDir = File(getExternalFilesDir(null), "epub_test")
                testDir.mkdirs()
                
                // Create test HTML file
                val htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="utf-8">
                        <title>Local EPUB Image Test</title>
                        <meta name="author" content="Test Author">
                    </head>
                    <body>
                        <h1>Local EPUB Image Test</h1>
                        <p>This is a local test HTML file to verify image embedding in EPUB.</p>
                        
                        <div class="content">
                            <p>Below is a test image that should be embedded in the EPUB:</p>
                            <img src="test_image.jpg" alt="Test Image" id="test-image">
                            
                            <p>The image above should be properly embedded in the EPUB file.</p>
                        </div>
                    </body>
                    </html>
                """.trimIndent()
                
                val htmlFile = File(testDir, "test.html")
                htmlFile.writeText(htmlContent)
                
                // Create a test image (a simple 1x1 pixel)
                val imageFile = File(testDir, "test_image.jpg")
                if (!imageFile.exists()) {
                    // Simple 1x1 JPEG image data
                    val imageData = byteArrayOf(
                        -1, -40, -1, -32, 0, 16, 74, 70, 73, 70, 0, 1, 1, 1, 0, 96, 0, 96, 0, 0, -1, -31, 
                        0, 60, 69, 120, 105, 102, 0, 0, 77, 77, 0, 42, 0, 0, 0, 8, 0, 1, 1, 31, 0, 2, 0, 0, 
                        0, 16, 0, 0, 0, 26, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 
                        1, 0, 0, 0, 1, -1, -37, 0, 67, 0, 8, 6, 6, 7, 6, 5, 8, 7, 7, 7, 9, 9, 8, 10, 12, 20, 
                        13, 12, 11, 11, 12, 25, 18, 19, 15, 20, 29, 26, 31, 30, 29, 26, 28, 28, 32, 36, 46, 
                        39, 32, 34, 44, 35, 28, 28, 40, 55, 41, 44, 48, 49, 52, 52, 52, 31, 39, 57, 61, 56, 
                        50, 60, 46, 51, 52, 50, -1, -37, 0, 67, 1, 9, 9, 9, 12, 11, 12, 24, 13, 13, 24, 50, 
                        33, 28, 33, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 
                        50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50, 
                        50, 50, 50, 50, 50, 50, 50, 50, 50, 50, -1, -64, 0, 17, 8, 0, 1, 0, 1, 3, 1, 34, 0, 
                        2, 17, 1, 3, 17, 1, -1, -60, 0, 31, 0, 0, 1, 5, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 
                        0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, -1, -60, 0, 31, 1, 0, 3, 1, 1, 1, 1, 1, 1, 
                        1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, -1, -60, 0, 20, 16, 1, 
                        1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, -60, 0, 20, 17, 1, 1, 0, 0, 
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, -1, -38, 0, 12, 3, 1, 0, 2, 17, 3, 17, 0, 
                        63, 0, 100, 118, -1, -39
                    )
                    imageFile.writeBytes(imageData)
                }
                
                // Create output EPUB file
                val epubFile = File(testDir, "output.epub")
                val outputStream = FileOutputStream(epubFile)
                
                // Convert HTML to EPUB
                val baseUrl = "file://${htmlFile.absolutePath}"
                val result = EpubCreator.createEpubFromUrl(baseUrl, outputStream)
                outputStream.close()
                
                // Update UI with results
                withContext(Dispatchers.Main) {
                    if (result.success) {
                        resultTextView.text = """
                            Test completed successfully!
                            
                            HTML file: ${htmlFile.absolutePath}
                            EPUB file: ${epubFile.absolutePath}
                            
                            Please check the EPUB file to verify that the image is properly displayed.
                        """.trimIndent()
                    } else {
                        resultTextView.text = """
                            Test failed!
                            
                            Error: ${result.message}
                            
                            Please check the logs for more details.
                        """.trimIndent()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error running test", e)
                withContext(Dispatchers.Main) {
                    resultTextView.text = "Test failed with error: ${e.message}"
                }
            }
        }
    }
}
