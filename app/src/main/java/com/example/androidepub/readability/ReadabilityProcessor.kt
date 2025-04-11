package com.example.androidepub.readability

import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.safety.Safelist

/**
 * A processor that implements readability features similar to Firefox's Reader View.
 * This class helps clean up HTML content and make it more readable.
 */
class ReadabilityProcessor {
    companion object {
        private val UNLIKELY_CANDIDATES = setOf(
            "banner", "breadcrumb", "combx", "comment", "community", "cover-wrap",
            "disqus", "extra", "foot", "header", "legends", "menu", "related",
            "remark", "replies", "rss", "shoutbox", "sidebar", "skyscraper",
            "social", "sponsor", "supplemental", "ad-break", "agegate",
            "pagination", "pager", "popup", "yom-remote"
        )

        private val LIKELY_CANDIDATES = setOf(
            "article", "body", "content", "entry", "hentry", "h-entry",
            "main", "page", "pagination", "post", "text", "blog", "story"
        )
    }

    /**
     * Process HTML content to make it more readable
     * @param html The HTML content to process
     * @return Cleaned up and more readable HTML content
     */
    fun makeReadable(html: String): String {
        val doc = Jsoup.parse(html)
        
        // Remove unlikely candidates
        doc.select("[class]").forEach { element ->
            val classNames = element.classNames()
            if (classNames.any { it in UNLIKELY_CANDIDATES } && 
                classNames.none { it in LIKELY_CANDIDATES }) {
                element.remove()
            }
        }

        // Remove known clutter elements
        doc.select("script, style, iframe, nav, header, footer, aside").remove()

        // Find the main content
        val mainContent = findMainContent(doc)
        
        // Clean up the content
        cleanupContent(mainContent)

        return mainContent.html()
    }

    /**
     * Find the main content element in the document
     */
    private fun findMainContent(doc: Document): Element {
        // First try to find article element
        doc.select("article").firstOrNull()?.let { return it }

        // Then try main element
        doc.select("main").firstOrNull()?.let { return it }

        // Look for the element with most paragraphs
        var bestElement = doc.body()
        var maxParagraphs = countParagraphs(doc.body())

        doc.select("div, section").forEach { element ->
            val paragraphCount = countParagraphs(element)
            if (paragraphCount > maxParagraphs) {
                maxParagraphs = paragraphCount
                bestElement = element
            }
        }

        return bestElement
    }

    /**
     * Count the number of paragraphs in an element
     */
    private fun countParagraphs(element: Element): Int {
        return element.select("p").size
    }

    /**
     * Clean up the content by removing unnecessary elements and formatting
     */
    private fun cleanupContent(element: Element) {
        // Remove empty elements
        element.select("*").forEach { el ->
            if (el.text().trim().isEmpty() && el.tagName() != "img" && el.tagName() != "br") {
                el.remove()
            }
        }

        // Convert relative URLs to absolute
        element.select("img").forEach { img ->
            img.attr("src")?.let { src ->
                if (!src.startsWith("http")) {
                    img.attr("src", src.removePrefix("./"))
                }
            }
        }

        // Clean up text nodes
        cleanTextNodes(element)
    }

    /**
     * Clean up text nodes by removing extra whitespace and normalizing text
     */
    private fun cleanTextNodes(node: Node) {
        node.childNodes().forEach { child ->
            when (child) {
                is TextNode -> {
                    child.text(normalizeText(child.text()))
                }
                is Element -> {
                    cleanTextNodes(child)
                }
            }
        }
    }

    /**
     * Normalize text by cleaning up whitespace and special characters
     */
    private fun normalizeText(text: String): String {
        return StringEscapeUtils.unescapeHtml4(text)
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Calculate the readability score of the text (Flesch Reading Ease score)
     * @param text The text to analyze
     * @return A score between 0 (very difficult) and 100 (very easy)
     */
    fun calculateReadabilityScore(text: String): Double {
        val sentences = text.split(Regex("[.!?]+")).size
        val words = text.split(Regex("\\s+")).size
        val syllables = countSyllables(text)

        if (words == 0 || sentences == 0) return 0.0

        return 206.835 - 1.015 * (words.toDouble() / sentences) - 84.6 * (syllables.toDouble() / words)
    }

    /**
     * Count the approximate number of syllables in text
     */
    private fun countSyllables(text: String): Int {
        var count = 0
        text.lowercase().split(Regex("\\s+")).forEach { word ->
            count += word.count { it in "aeiouy" }
            if (word.endsWith("e")) count--
            if (count <= 0) count = 1
        }
        return count
    }
}
