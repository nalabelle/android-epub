package com.example.androidepub

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.androidepub.utils.EpubTestHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class TestActivity : AppCompatActivity() {
    private lateinit var resultTextView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var runTestButton: Button
    
    companion object {
        private const val TAG = "TestActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        
        resultTextView = findViewById(R.id.resultTextView)
        scrollView = findViewById(R.id.scrollView)
        runTestButton = findViewById(R.id.runTestButton)
        
        runTestButton.setOnClickListener {
            runEpubTest()
        }
    }
    
    private fun runEpubTest() {
        resultTextView.text = "Running EPUB test..."
        runTestButton.isEnabled = false
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val report = withContext(Dispatchers.IO) {
                    // Create test directory in external files directory
                    val testDir = File(getExternalFilesDir(null), "epub_test")
                    testDir.mkdirs()
                    
                    // Create HTML test file
                    val htmlFile = File(testDir, "test.html")
                    val htmlContent = createTestHtml()
                    FileOutputStream(htmlFile).use { it.write(htmlContent.toByteArray()) }
                    
                    // Create image file
                    val imageFile = File(testDir, "test_image.png")
                    assets.open("test_image.png").use { input ->
                        FileOutputStream(imageFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    // Output EPUB file
                    val epubFile = File(testDir, "test_output.epub")
                    if (epubFile.exists()) {
                        epubFile.delete()
                    }
                    
                    // Run the test
                    EpubTestHelper.testHtmlToEpub(this@TestActivity, htmlFile, epubFile)
                }
                
                // Display the report
                resultTextView.text = report
                scrollView.post {
                    scrollView.fullScroll(ScrollView.FOCUS_UP)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error running EPUB test", e)
                resultTextView.text = "Error running test: ${e.message}\n\n${e.stackTraceToString()}"
            } finally {
                runTestButton.isEnabled = true
            }
        }
    }
    
    private fun createTestHtml(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>EPUB Image Test</title>
                <meta name="author" content="Test Author">
            </head>
            <body>
                <h1>EPUB Image Test</h1>
                <p>This is a test HTML file to verify image embedding in EPUB.</p>
                
                <div class="content">
                    <p>Below is a test image that should be embedded in the EPUB:</p>
                    <img src="test_image.png" alt="Test Image" id="test-image">
                    
                    <p>The image above should be properly embedded in the EPUB file.</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
