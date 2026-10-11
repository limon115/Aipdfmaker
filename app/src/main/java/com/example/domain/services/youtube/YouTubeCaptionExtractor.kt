package com.example.domain.services.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class YouTubeCaptionExtractor {

    suspend fun extractTranscript(youtubeUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val canonicalUrl = normalizeYouTubeUrl(youtubeUrl)
            val html = fetchUrlContent(canonicalUrl)
            
            // Look for "captionTracks":[{...}] in the HTML (handles standard and JSON-escaped formats)
            val regex = """(?:"captionTracks"|\\"captionTracks\\")\s*:\s*\[(.*?)\]""".toRegex(RegexOption.IGNORE_CASE)
            val matchResult = regex.find(html)
            
            if (matchResult != null) {
                val captionTracksJson = matchResult.groupValues[1]
                
                // Extract baseUrl from the JSON string (handles both raw and escaped quotes)
                val baseUrlRegex = """(?:"baseUrl"|\\"baseUrl\\")\s*:\s*(?:\"|\\")([^"\\]+)(?:\"|\\")""".toRegex()
                val baseUrlMatches = baseUrlRegex.findAll(captionTracksJson)
                
                var targetUrl: String? = null
                
                for (match in baseUrlMatches) {
                    val rawUrl = match.groupValues[1]
                    val url = rawUrl
                        .replace("\\u0026", "&")
                        .replace("&amp;", "&")
                        .replace("\\/", "/")
                        .replace("\\", "")

                    if (targetUrl == null) {
                        targetUrl = url
                    }
                    // Prefer English caption track
                    if (url.contains("lang=en") || captionTracksJson.substring(match.range.first).take(300).contains("\"en\"")) {
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
                        return@withContext Result.failure(Exception("Closed captions were found but transcript text was empty."))
                    }
                } else {
                    return@withContext Result.failure(Exception("No readable caption tracks found for this video."))
                }
            } else {
                return@withContext Result.failure(Exception("Could not find closed captions on this YouTube video. The creator might not have enabled subtitles."))
            }
            
        } catch (e: Exception) {
            com.example.utils.AppLogger.e("YouTubeCaptionExtractor", "Error extracting transcript: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun normalizeYouTubeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        val videoIdRegex = """(?:v=|\/shorts\/|\/embed\/|youtu\.be\/|\/v\/|^)([a-zA-Z0-9_-]{11})""".toRegex()
        val match = videoIdRegex.find(trimmed)
        return if (match != null) {
            val videoId = match.groupValues[1]
            "https://www.youtube.com/watch?v=$videoId"
        } else {
            if (!trimmed.startsWith("http")) "https://$trimmed" else trimmed
        }
    }

    private fun fetchUrlContent(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
        // Bypass GDPR consent walls that block bot caption extraction
        connection.setRequestProperty("Cookie", "CONSENT=YES+cb.20210328-17-p0.en+FX+478;")
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                return connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw Exception("HTTP Error ${connection.responseCode} while fetching YouTube data.")
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
                       .replace("\n", " ")
            sb.append(text).append(" ")
        }
        return sb.toString().trim().replace(Regex("\\s+"), " ")
    }
}
