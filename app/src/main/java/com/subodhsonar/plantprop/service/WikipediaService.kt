package com.subodhsonar.plantprop.service

import com.subodhsonar.plantprop.model.PropagationStep
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.plugins.*
import org.json.JSONObject

data class WikiInfo(
    val title: String,
    val extract: String,
    val thumbnailUrl: String?,
    val propagationSteps: List<PropagationStep>? = null
)

class WikipediaService {
    private val USER_AGENT = "PlantProp/1.0 (https://example.com/plantprop; support@example.com)"
    private val client = HttpClient {
        defaultRequest {
            header(HttpHeaders.UserAgent, USER_AGENT)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 15000
        }
    }

    private suspend fun fetchUrl(url: String): String {
        return client.get(url).bodyAsText()
    }

    suspend fun getTreeInfo(query: String): WikiInfo? {
        try {
            var baseQuery = query.split("::").first()
                .split("'").first()
                .split("(").first()
                .split(":").first()
                .trim()
            
            if (baseQuery.lowercase().contains("patanus")) {
                baseQuery = baseQuery.replace("patanus", "Platanus", ignoreCase = true)
            }

            val info = fetchInfoByTitle(baseQuery)
            if (info?.thumbnailUrl != null) return info

            val searchResults = performSearch(baseQuery)
            for (title in searchResults.take(3)) {
                val searchInfo = fetchInfoByTitle(title)
                if (searchInfo?.thumbnailUrl != null) return searchInfo
            }

            if (query.contains("::")) {
                val commonPart = query.split("::").getOrNull(1)?.split(":")?.first()?.trim()
                if (commonPart != null && commonPart != baseQuery) {
                    val commonInfo = fetchInfoByTitle(commonPart)
                    if (commonInfo?.thumbnailUrl != null) return commonInfo
                }
            }

            if (baseQuery.contains(" ")) {
                val firstWord = baseQuery.split(" ").first()
                val genusInfo = fetchInfoByTitle(firstWord)
                if (genusInfo?.thumbnailUrl != null) return genusInfo
            }

            return info
        } catch (e: Exception) {
            return null
        }
    }

    private suspend fun fetchInfoByTitle(title: String): WikiInfo? {
        try {
            val encodedTitle = title.replace(" ", "_").encodeURLParameter()
            val url = "https://en.wikipedia.org/w/api.php?action=query&format=json&prop=extracts|pageimages|images|info&inprop=url&exintro=1&explaintext=1&piprop=thumbnail|original&pithumbsize=1000&redirects=1&titles=$encodedTitle"
            
            val response = fetchUrl(url)
            val root = JSONObject(response)
            val queryObj = root.optJSONObject("query") ?: return null
            val pages = queryObj.optJSONObject("pages") ?: return null
            val firstKey = pages.keys().asSequence().firstOrNull() ?: return null
            
            if (firstKey == "-1") return null
            
            val page = pages.getJSONObject(firstKey)
            var thumbUrl = page.optJSONObject("thumbnail")?.optString("source")
                ?: page.optJSONObject("original")?.optString("source")
            
            if (thumbUrl == null) {
                val imageList = page.optJSONArray("images")
                if (imageList != null) {
                    for (i in 0 until minOf(10, imageList.length())) {
                        val fileTitle = imageList.getJSONObject(i).optString("title")
                        if (fileTitle != null && isLikelyBotanicalPhoto(fileTitle)) {
                            val resolvedUrl = resolveImageUrl(fileTitle)
                            if (resolvedUrl != null) {
                                thumbUrl = resolvedUrl
                                break
                            }
                        }
                    }
                }
            }
            
            if (thumbUrl != null && thumbUrl.startsWith("//")) {
                thumbUrl = "https:$thumbUrl"
            }

            return WikiInfo(
                title = page.optString("title", title),
                extract = page.optString("extract", ""),
                thumbnailUrl = thumbUrl
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun isLikelyBotanicalPhoto(filename: String): Boolean {
        val lower = filename.lowercase()
        val isPhoto = lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
        val isNotUI = !lower.contains("logo") && !lower.contains("icon") && !lower.contains("stub") && 
                      !lower.contains("medal") && !lower.contains("edit-clear") && !lower.contains("magnify") &&
                      !lower.contains("map") && !lower.contains("distribution") && !lower.contains("range")
        return isPhoto && isNotUI
    }

    private suspend fun resolveImageUrl(fileTitle: String): String? {
        try {
            val encodedFile = fileTitle.encodeURLParameter()
            val url = "https://en.wikipedia.org/w/api.php?action=query&format=json&prop=imageinfo&iiprop=url|size&iiurlwidth=1000&titles=$encodedFile"
            val response = fetchUrl(url)
            val root = JSONObject(response)
            val queryObj = root.optJSONObject("query") ?: return null
            val pages = queryObj.optJSONObject("pages") ?: return null
            val firstKey = pages.keys().asSequence().firstOrNull() ?: return null
            val page = pages.getJSONObject(firstKey)
            val imageInfo = page.optJSONArray("imageinfo")?.optJSONObject(0)
            
            return imageInfo?.optString("thumburl") ?: imageInfo?.optString("url")
        } catch (e: Exception) {
            return null
        }
    }

    private suspend fun performSearch(query: String): List<String> {
        try {
            val encodedQuery = query.encodeURLParameter()
            val searchUrl = "https://en.wikipedia.org/w/api.php?action=query&format=json&list=search&srsearch=$encodedQuery&srlimit=5"
            val response = fetchUrl(searchUrl)
            val root = JSONObject(response)
            val searchResults = root.optJSONObject("query")?.optJSONArray("search")
            val list = mutableListOf<String>()
            if (searchResults != null) {
                for (i in 0 until searchResults.length()) {
                    searchResults.getJSONObject(i).optString("title").let { list.add(it) }
                }
            }
            return list
        } catch (e: Exception) {
            return emptyList()
        }
    }

    suspend fun fetchPlantImage(query: String): String? {
        val info = getTreeInfo(query)
        return info?.thumbnailUrl
    }
}
