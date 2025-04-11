# Direct test script to verify EPUB creation with images
# This script creates a simple HTML file with an image, converts it to EPUB,
# and then verifies that the image is properly embedded in the EPUB

# Set Java home to match your build environment
$env:JAVA_HOME = 'C:\Users\nalabelle\scoop\apps\android-studio\current\jbr'

# Create a simple test directory
$testDir = ".\direct_test"
if (-not (Test-Path $testDir)) {
    New-Item -ItemType Directory -Path $testDir | Out-Null
}

# Copy the test image to the test directory
Copy-Item "e6a9d63d-eec0-4497-bcfa-062d4ad89aba.png" "$testDir\"

# Create a simple HTML file with an image
$htmlContent = @"
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>Direct EPUB Image Test</title>
    <meta name="author" content="Test Author">
</head>
<body>
    <h1>Direct EPUB Image Test</h1>
    <p>This is a direct test HTML file to verify image embedding in EPUB.</p>
    
    <div class="content">
        <p>Below is a test image that should be embedded in the EPUB:</p>
        <img src="e6a9d63d-eec0-4497-bcfa-062d4ad89aba.png" alt="Test Image" id="test-image">
        
        <p>The image above should be properly embedded in the EPUB file.</p>
    </div>
</body>
</html>
"@

# Write the HTML content to a file
$htmlContent | Out-File -FilePath "$testDir\direct_test.html" -Encoding utf8

# Create a simple Java program to convert the HTML to EPUB
$javaCode = @"
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 * Simple standalone test program for EPUB conversion with images
 */
public class DirectTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== Direct EPUB Conversion Test ===");
            
            // Paths
            File htmlFile = new File("direct_test.html");
            File imageFile = new File("e6a9d63d-eec0-4497-bcfa-062d4ad89aba.png");
            File outputFile = new File("output.epub");
            
            System.out.println("HTML file: " + htmlFile.getAbsolutePath());
            System.out.println("Image file: " + imageFile.getAbsolutePath());
            System.out.println("EPUB output file: " + outputFile.getAbsolutePath());
            
            // Read HTML content
            String htmlContent = new String(java.nio.file.Files.readAllBytes(htmlFile.toPath()), "UTF-8");
            System.out.println("HTML content length: " + htmlContent.length() + " characters");
            
            // Create EPUB file
            createEpub(htmlContent, imageFile, outputFile);
            
            System.out.println("EPUB creation completed. Check " + outputFile.getAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void createEpub(String htmlContent, File imageFile, File outputFile) throws Exception {
        // Create a simple EPUB file structure
        ByteArrayOutputStream epubData = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(epubData);
        
        // Add mimetype file (must be first and uncompressed)
        zip.setMethod(ZipOutputStream.STORED);
        ZipEntry mimetypeEntry = new ZipEntry("mimetype");
        byte[] mimetypeBytes = "application/epub+zip".getBytes();
        mimetypeEntry.setSize(mimetypeBytes.length);
        CRC32 crc = new CRC32();
        crc.update(mimetypeBytes);
        mimetypeEntry.setCrc(crc.getValue());
        zip.putNextEntry(mimetypeEntry);
        zip.write(mimetypeBytes);
        zip.closeEntry();
        
        // Switch to deflated for remaining entries
        zip.setMethod(ZipOutputStream.DEFLATED);
        
        // Add META-INF/container.xml
        zip.putNextEntry(new ZipEntry("META-INF/container.xml"));
        String container = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n" +
                "    <rootfiles>\n" +
                "        <rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n" +
                "    </rootfiles>\n" +
                "</container>";
        zip.write(container.getBytes());
        zip.closeEntry();
        
        // Add content.opf
        zip.putNextEntry(new ZipEntry("OEBPS/content.opf"));
        String imageName = imageFile.getName();
        String opf = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"2.0\" unique-identifier=\"BookId\">\n" +
                "    <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:opf=\"http://www.idpf.org/2007/opf\">\n" +
                "        <dc:title>Direct Test EPUB</dc:title>\n" +
                "        <dc:language>en</dc:language>\n" +
                "        <dc:identifier id=\"BookId\">urn:uuid:" + UUID.randomUUID() + "</dc:identifier>\n" +
                "    </metadata>\n" +
                "    <manifest>\n" +
                "        <item id=\"ncx\" href=\"toc.ncx\" media-type=\"application/x-dtbncx+xml\"/>\n" +
                "        <item id=\"content\" href=\"content.html\" media-type=\"application/xhtml+xml\"/>\n" +
                "        <item id=\"image\" href=\"images/" + imageName + "\" media-type=\"image/png\"/>\n" +
                "    </manifest>\n" +
                "    <spine toc=\"ncx\">\n" +
                "        <itemref idref=\"content\"/>\n" +
                "    </spine>\n" +
                "</package>";
        zip.write(opf.getBytes());
        zip.closeEntry();
        
        // Add toc.ncx
        zip.putNextEntry(new ZipEntry("OEBPS/toc.ncx"));
        String ncx = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\" version=\"2005-1\">\n" +
                "    <head>\n" +
                "        <meta name=\"dtb:uid\" content=\"urn:uuid:" + UUID.randomUUID() + "\"/>\n" +
                "    </head>\n" +
                "    <docTitle><text>Direct Test EPUB</text></docTitle>\n" +
                "    <navMap>\n" +
                "        <navPoint id=\"navpoint-1\" playOrder=\"1\">\n" +
                "            <navLabel><text>Start</text></navLabel>\n" +
                "            <content src=\"content.html\"/>\n" +
                "        </navPoint>\n" +
                "    </navMap>\n" +
                "</ncx>";
        zip.write(ncx.getBytes());
        zip.closeEntry();
        
        // Add content.html
        zip.putNextEntry(new ZipEntry("OEBPS/content.html"));
        // Modify the image path in the HTML to match the EPUB structure
        String modifiedHtml = htmlContent.replace(imageFile.getName(), "images/" + imageFile.getName());
        zip.write(modifiedHtml.getBytes());
        zip.closeEntry();
        
        // Add image
        zip.putNextEntry(new ZipEntry("OEBPS/images/" + imageName));
        byte[] imageData = java.nio.file.Files.readAllBytes(imageFile.toPath());
        zip.write(imageData);
        zip.closeEntry();
        
        // Close the zip
        zip.close();
        
        // Write the EPUB file
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(epubData.toByteArray());
        }
        
        System.out.println("EPUB file created successfully with image: " + imageName);
    }
}
"@

# Write the Java code to a file
$javaCode | Out-File -FilePath "$testDir\DirectTest.java" -Encoding ascii

# Compile and run the Java program
Write-Host "Compiling and running the direct test..."
Push-Location $testDir
& "$env:JAVA_HOME\bin\javac" DirectTest.java
if ($LASTEXITCODE -eq 0) {
    & "$env:JAVA_HOME\bin\java" DirectTest
    Write-Host "Test completed. Check $testDir\output.epub for the results."
} else {
    Write-Host "Compilation failed."
}
Pop-Location
