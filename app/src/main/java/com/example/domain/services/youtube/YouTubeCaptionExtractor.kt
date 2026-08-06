package com.example.domain.services.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class YouTubeCaptionExtractor {

    suspend fun extractTranscript(youtubeUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val html = fetchUrlContent(youtubeUrl)
            
            // Look for "captionTracks":[{...}] in the HTML
            // Specifically, we want the baseUrl of the English caption track or the first available one.
            val regex = """"captionTracks":\s*\[(.*?)\]""".toRegex(RegexOption.IGNORE_CASE)
            val matchResult = regex.find(html)
            
            if (matchResult != null) {
                val captionTracksJson = matchResult.groupValues[1]
                
                // Extract baseUrl from the JSON string
                val baseUrlRegex = """"baseUrl":\s*"(.*?)"""".toRegex()
                val baseUrlMatches = baseUrlRegex.findAll(captionTracksJson)
                
                var targetUrl: String? = null
                
                for (match in baseUrlMatches) {
                    val url = match.groupValues[1].replace("\\u0026", "&").replace("\\/", "/")
                    if (targetUrl == null) {
                        targetUrl = url
                    }
                    // Prefer English
                    if (url.contains("lang=en") || captionTracksJson.substring(match.range.first).take(200).contains("\"languageCode\":\"en\"")) {
                        targetUrl = url
                        break
                    }
                }
                
                if (targetUrl != null) {
                    val xml = fetchUrlContent(targetUrl)
                    val transcript = parseXmlToText(xml)
                    if (transcript.isNotEmpty()) {
                        return@withContext Result.success(transcript)
                    } else {
                        return@withContext Result.failure(Exception("Failed to parse transcript XML."))
                    }
                } else {
                    return@withContext Result.failure(Exception("No caption tracks found."))
                }
            } else {
                return@withContext Result.failure(Exception("Could not find caption tracks in the page."))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchUrlContent(urlString: String): String {
        var finalUrl = urlString
        // handle short URL mapping if necessary
        if (!finalUrl.startsWith("http")) {
            finalUrl = "https://$finalUrl"
        }
        val url = URL(finalUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                return connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw Exception("HTTP Error: ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseXmlToText(xml: String): String {
        val textRegex = """<text[^>]*>(.*?)</text>""".toRegex()
        val matches = textRegex.findAll(xml)
        val sb = StringBuilder()
        for (match in matches) {
            var text = match.groupValues[1]
            text = text.replace("&amp;", "&")
                       .replace("&lt;", "<")
                       .replace("&gt;", ">")
                       .replace("&#39;", "'")
                       .replace("&quot;", "\"")
            sb.append(text).append(" ")
        }
        return sb.toString().trim().replace(Regex("\\s+"), " ")
    }
}
