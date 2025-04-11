import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;
import java.util.zip.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * Simple standalone test program for EPUB conversion with images
 */
public class TestEpubConversion {
    public static void main(String[] args) {
        try {
            System.out.println("=== EPUB Conversion Test ===");
            
            // Paths
            File htmlFile = new File("test.html");
            File imageFile = new File("e6a9d63d-eec0-4497-bcfa-062d4ad89aba.png");
            File outputFile = new File("output.epub");
            
            System.out.println("HTML file: " + htmlFile.getAbsolutePath());
            System.out.println("Image file: " + imageFile.getAbsolutePath());
            System.out.println("EPUB output file: " + outputFile.getAbsolutePath());
            
            // Read HTML content
            String htmlContent = new String(Files.readAllBytes(htmlFile.toPath()));
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
                "        <dc:title>Test EPUB</dc:title>\n" +
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
                "    <docTitle><text>Test EPUB</text></docTitle>\n" +
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
        byte[] imageData = Files.readAllBytes(imageFile.toPath());
        zip.write(imageData);
        zip.closeEntry();
        
        // Close the zip
        zip.close();
        
        // Write the EPUB file
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(epubData.toByteArray());
        }
    }
}
