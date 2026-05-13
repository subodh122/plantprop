package com.subodhsonar.plantprop.service

import com.subodhsonar.plantprop.model.PropagationStep
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.plugins.*
import org.json.JSONArray
import org.json.JSONObject
import android.util.Log

data class WikiInfo(
    val title: String,
    val extract: String,
    val thumbnailUrl: String?,
    val propagationSteps: List<PropagationStep>? = null
)

class WikipediaService(apiKey: String) {
    private val apiKey = apiKey.trim()
    private val TAG = "WikipediaService"
    private val USER_AGENT = "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36"
    private val client = HttpClient {
        defaultRequest {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header("Referer", "https://commons.wikimedia.org/")
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000 
            connectTimeoutMillis = 60000
            socketTimeoutMillis = 60000
        }
    }

    private suspend fun fetchUrl(url: String): String {
        return try {
            Log.d(TAG, "[FETCH] Requesting URL: $url")
            val response = client.get(url)
            if (response.status == HttpStatusCode.OK) {
                response.bodyAsText()
            } else if (response.status == HttpStatusCode.Found || response.status == HttpStatusCode.MovedPermanently) {
                val location = response.headers[HttpHeaders.Location]
                if (location != null) {
                    Log.d(TAG, "[FETCH] Following redirect to: $location")
                    client.get(location).bodyAsText()
                } else "{}"
            } else {
                Log.e(TAG, "[FETCH] Failed with status: ${response.status}")
                "{}"
            }
        } catch (e: Exception) { 
            Log.e(TAG, "[FETCH] Exception: ${e.message}")
            "{}" 
        }
    }

    private suspend fun callGeminiToSelectImage(query: String, options: List<String>): String? {
        if (options.isEmpty()) return null
        try {
            Log.d(TAG, "[AI] Starting image selection for '$query' with ${options.size} candidates")
            
            val prompt = """
                You are a botanical image verification system. 
                Goal: Select the single URL that represents a HIGH-QUALITY photograph of the plant species: "$query".
                
                Strict Rules:
                1. VERIFY NAMES: Look for parts of "$query" in the filenames or URLs.
                2. REJECT ANIMALS: Reject any URLs containing words like "python", "snake", "insect", "wasp", etc.
                3. REJECT LOGOS/MAPS: Reject any non-botanical content.
                4. PREFER COMMONS: Prioritize URLs from 'wikimedia.org'.
                
                Candidates:
                ${options.joinToString("\n")}
                
                Instructions:
                - If a URL looks like it matches the name "$query", select it.
                - If multiple look good, pick the most professional-looking URL.
                - If no URL is a botanical match, return "none".
                
                Return ONLY the selected URL as plain text.
            """.trimIndent()

            val url = "https://openrouter.ai/api/v1/chat/completions"
            val requestBody = JSONObject().apply {
                put("model", "google/gemini-3.1-flash-lite")
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                }))
            }
            
            val response = client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                header("HTTP-Referer", "https://subodhsonar.com/plantprop")
                header("X-Title", "PlantProp")
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }
            
            if (response.status != HttpStatusCode.OK) {
                Log.e(TAG, "[AI] OpenRouter Error: ${response.status}")
                return null
            }
            
            val content = JSONObject(response.bodyAsText())
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
            
            Log.d(TAG, "[AI] Selection result: $content")
            if (content.lowercase() == "none" || !content.startsWith("http")) return null
            
            if (isSuspicious(content, query)) {
                Log.e(TAG, "[AI] Selected URL looks suspicious. Rejection triggered.")
                return null
            }
            
            return content
        } catch (e: Exception) { 
            Log.e(TAG, "[AI] Exception: ${e.message}")
            return null 
        }
    }

    private fun isSuspicious(url: String, query: String): Boolean {
        val lUrl = url.lowercase()
        val lQuery = query.lowercase().split(" ").first()
        val animalWords = listOf("python", "snake", "boa", "viper", "wasp", "bee", "bug")
        if (animalWords.any { lUrl.contains(it) } && !lUrl.contains(lQuery)) return true
        return false
    }

    suspend fun fetchPlantImage(scientificName: String, commonName: String): String? {
        Log.d(TAG, "[START] Image acquisition for S:'$scientificName', C:'$commonName'")
        
        val baseScientific = scientificName.split("'").first().split("\"").first().split("(").first().trim()
        
        if (baseScientific.isNotEmpty() && baseScientific.lowercase() != "unknown") {
            Log.d(TAG, "[STEP] Attempting search with base scientific name: $baseScientific")
            val img = scrapeImageExhaustive(baseScientific)
            if (img != null) {
                Log.d(TAG, "[SUCCESS] Found image via scientific name: $img")
                return img
            }
        }
        
        val cleanCommon = commonName.split(",").first().split("(").first().trim()
        if (cleanCommon.isNotEmpty() && cleanCommon.lowercase() != "unknown plant") {
            Log.d(TAG, "[STEP] Attempting search with common name: $cleanCommon")
            val img = scrapeImageExhaustive(cleanCommon)
            if (img != null) {
                Log.d(TAG, "[SUCCESS] Found image via common name: $img")
                return img
            }
        }
        
        Log.w(TAG, "[FAIL] No image found for either name.")
        return null
    }

    private suspend fun scrapeImageExhaustive(query: String): String? {
        Log.d(TAG, "[SCRAPE] Exhaustive search for: $query")
        
        val info = getTreeInfo(query)
        if (info?.thumbnailUrl != null) {
            Log.d(TAG, "[SCRAPE] Found verified URL: ${info.thumbnailUrl}")
            return info.thumbnailUrl
        }
        
        return null
    }

    suspend fun getTreeInfo(query: String): WikiInfo? {
        try {
            val baseQuery = query.split(",").first().split("(").first().split("::").first().trim()
            if (baseQuery.isEmpty()) return null
            
            Log.d(TAG, "[WIKI] Starting deep collection for: $baseQuery")
            val candidates = mutableListOf<String>()

            // A. Check main title
            val mainInfo = fetchInfoByTitle(baseQuery, fullExtract = true)
            mainInfo?.thumbnailUrl?.let { candidates.add(it) }
            
            // B. Search for alternates
            val searchResults = performSearch("$baseQuery plant")
            for (title in searchResults.take(3)) {
                fetchInfoByTitle(title)?.thumbnailUrl?.let { candidates.add(it) }
            }
            
            // C. External fallbacks
            fetchImageFromDDG(baseQuery)?.let { candidates.add(it) }
            
            // D. Wikimedia Commons
            fetchImageFromCommonsCategory(baseQuery)?.let { candidates.add(it) }

            // E. Bing Images (Expanded Scraper)
            fetchImageFromBing("$baseQuery plant").forEach { candidates.add(it) }

            // F. Smart Filtering
            val genus = baseQuery.split(" ").first().lowercase()
            val filtered = candidates.map { if (it.startsWith("//")) "https:$it" else it }
                .distinct()
                .filter { url ->
                    val l = url.lowercase()
                    val isJunk = l.contains("logo") || l.contains("icon") || l.contains("stub") || l.contains("map")
                    val likelyRelevant = l.contains(genus) || l.contains("plant") || l.contains("flower") || l.contains("leaf") || l.contains("tree") || l.contains("shrub") || l.contains("specimen")
                    !isJunk && likelyRelevant
                }

            Log.d(TAG, "[FILTER] Candidates after filter: ${filtered.size}")
            filtered.forEach { Log.d(TAG, "[FILTER] Candidate: $it") }
            
            // G. AI Selection with Fallback
            val selected = callGeminiToSelectImage(baseQuery, filtered)
            
            // If AI failed but we have a highly likely candidate from Wikipedia main search, use it
            val finalUrl = selected ?: if (filtered.contains(mainInfo?.thumbnailUrl)) mainInfo?.thumbnailUrl else filtered.firstOrNull()
            
            Log.d(TAG, "[RESULT] Final selection: ${finalUrl ?: "NONE"}")
            
            return (mainInfo ?: WikiInfo(baseQuery, "", null)).copy(thumbnailUrl = finalUrl)
        } catch (e: Exception) { 
            Log.e(TAG, "[WIKI] Error in getTreeInfo: ${e.message}")
            return null 
        }
    }

    private suspend fun fetchImageFromCommonsCategory(query: String): String? {
        try {
            val url = "https://commons.wikimedia.org/w/api.php?action=query&format=json&generator=categorymembers&gcmtitle=Category:${query.replace(" ", "_").encodeURLParameter()}&gcmtype=file&prop=imageinfo&iiprop=url&iiurlwidth=1200&gcmlimit=5"
            val responseText = fetchUrl(url)
            val root = JSONObject(responseText)
            val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: return null
            for (key in pages.keys()) {
                val img = pages.getJSONObject(key).optJSONArray("imageinfo")?.optJSONObject(0)
                val thumb = img?.optString("thumburl") ?: img?.optString("url")
                if (thumb != null) return thumb
            }
        } catch (_: Exception) { }
        return null
    }

    private suspend fun fetchImageFromDDG(query: String): String? {
        try {
            val url = "https://api.duckduckgo.com/?q=${query.replace(" ", "+")}&format=json&no_html=1&skip_disambig=1"
            val responseText = fetchUrl(url)
            val root = JSONObject(responseText)
            val img = root.optString("Image", "")
            if (img.isNotEmpty()) {
                return if (img.startsWith("/")) "https://duckduckgo.com$img" else img
            }
        } catch (_: Exception) { }
        return null
    }

    private suspend fun fetchImageFromBing(query: String): List<String> {
        val urls = mutableListOf<String>()
        try {
            val searchUrl = "https://www.bing.com/images/search?q=${query.encodeURLParameter()}&first=1"
            val responseText = fetchUrl(searchUrl)
            
            // Improved regex for media URLs
            val regex = Regex("\"murl\":\"(https://[^\"]+)\"")
            val matches = regex.findAll(responseText)
            
            matches.take(8).forEach { match ->
                val imgUrl = match.groupValues[1]
                    .replace("\\u003a", ":")
                    .replace("\\u002f", "/")
                    .replace("\\u002e", ".")
                if (imgUrl.isNotEmpty() && !imgUrl.contains("bing.net")) {
                    urls.add(imgUrl)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[BING] Error: ${e.message}")
        }
        return urls
    }

    private suspend fun fetchInfoByTitle(title: String, fullExtract: Boolean = false): WikiInfo? {
        try {
            val url = "https://en.wikipedia.org/w/api.php?action=query&format=json&prop=extracts|pageimages|images|info&inprop=url&${if (fullExtract) "explaintext=1" else "exintro=1&explaintext=1"}&piprop=thumbnail|original&pithumbsize=1000&redirects=1&titles=${title.replace(" ", "_").encodeURLParameter()}"
            val responseText = fetchUrl(url)
            val root = JSONObject(responseText)
            val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: return null
            val firstKey = pages.keys().asSequence().firstOrNull() ?: return null
            if (firstKey == "-1") return null
            
            val page = pages.getJSONObject(firstKey)
            var thumb = page.optJSONObject("thumbnail")?.optString("source") 
                ?: page.optJSONObject("original")?.optString("source")
            
            if (thumb == null) {
                val imgs = page.optJSONArray("images")
                if (imgs != null) {
                    for (i in 0 until minOf(10, imgs.length())) {
                        val t = imgs.getJSONObject(i).optString("title")
                        if (t != null && isLikelyBotanicalPhoto(t)) {
                            thumb = resolveImageUrl(t)
                            if (thumb != null) break
                        }
                    }
                }
            }
            
            val extract = page.optString("extract", "")
            return WikiInfo(page.optString("title", title), if (fullExtract) extract.take(5000) else extract, thumb, if (fullExtract) extractPropagationSteps(extract) else null)
        } catch (e: Exception) {
            return null 
        }
    }

    private fun extractPropagationSteps(full: String): List<PropagationStep>? {
        for (s in listOf("Propagation", "Cultivation", "Reproduction", "Growth", "Planting")) {
            val h = "== $s =="
            val i = full.indexOf(h, ignoreCase = true)
            if (i != -1) {
                val next = full.indexOf("==", i + h.length)
                val c = if (next == -1) full.substring(i + h.length) else full.substring(i + h.length, next)
                if (c.trim().length > 50) {
                    val sentences = c.trim().split(Regex("(?<=[.!?])\\s+"))
                        .filter { it.length > 30 }
                        .map { it.trim() }
                    
                    if (sentences.isNotEmpty()) {
                        return sentences.take(8).mapIndexed { idx, t -> 
                            PropagationStep("Step ${idx + 1}", t) 
                        }
                    }
                }
            }
        }
        return null
    }

    private fun isLikelyBotanicalPhoto(f: String): Boolean {
        val l = f.lowercase()
        if (l.contains("python") || l.contains("snake") || l.contains("insect") || l.contains("wasp")) return false
        return (l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".png")) && !l.contains("logo") && !l.contains("icon") && !l.contains("stub") && !l.contains("map")
    }

    private suspend fun resolveImageUrl(t: String): String? {
        try {
            val url = "https://en.wikipedia.org/w/api.php?action=query&format=json&prop=imageinfo&iiprop=url|size&iiurlwidth=1200&titles=${t.encodeURLParameter()}"
            val responseText = fetchUrl(url)
            val page = JSONObject(responseText).optJSONObject("query")?.optJSONObject("pages")?.let { it.getJSONObject(it.keys().next()) }
            return page?.optJSONArray("imageinfo")?.optJSONObject(0)?.let { it.optString("thumburl") ?: it.optString("url") }
        } catch (e: Exception) { return null }
    }

    private suspend fun performSearch(q: String): List<String> {
        try {
            val url = "https://en.wikipedia.org/w/api.php?action=query&format=json&list=search&srsearch=${q.encodeURLParameter()}&srlimit=5"
            val responseText = fetchUrl(url)
            val results = JSONObject(responseText).optJSONObject("query")?.optJSONArray("search") ?: return emptyList()
            return List(results.length()) { results.getJSONObject(it).getString("title") }
        } catch (e: Exception) { return emptyList() }
    }
}
