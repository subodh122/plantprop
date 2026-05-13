package com.subodhsonar.plantprop.service

import android.util.Base64
import android.util.Log
import com.subodhsonar.plantprop.model.PlantAnalysis
import com.subodhsonar.plantprop.model.PropagationStep
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * BotanyService - REWRITTEN - 2026-05-15
 * Optimized for Gemini 3.1 Flash Lite via OpenRouter.
 */
class BotanyService(apiKey: String) : IBotanyService {
    private val apiKey = apiKey.trim()
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 60000 
            connectTimeoutMillis = 60000
            socketTimeoutMillis = 60000
        }
    }
    private val TAG = "BotanyService"
    private val MODEL_ID = "google/gemini-3.1-flash-lite"

    override suspend fun analyzePlant(imageBytes: ByteArray): PlantAnalysis = withContext(Dispatchers.IO) {
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        
        val prompt = """
            Identify the plant in this image with high precision.
            Analyze the leaf shape, color, pattern, and stem structure.
            
            Return a JSON object:
            {
              "commonName": "Primary common name",
              "scientificName": "Binomial scientific name",
              "confidence": 0.0 to 1.0,
              "summary": "Professional botanical description (6-8 sentences).",
              "propagationSteps": [
                { "title": "Step title", "description": "detailed instructions" }
              ],
              "tips": "Expert horticultural tips."
            }
            Ensure the response is ONLY the JSON object.
        """.trimIndent()

        val requestBody = JSONObject().apply {
            put("model", MODEL_ID)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().put("type", "text").put("text", prompt))
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$base64Image"))
                        })
                    })
                })
            })
            put("response_format", JSONObject().put("type", "json_object"))
        }
        executeOpenRouter(requestBody)
    }

    override suspend fun getInfoByName(scientificName: String): PlantAnalysis = withContext(Dispatchers.IO) {
        val prompt = "Provide botanical info for: $scientificName. Return ONLY a JSON object with keys: commonName, scientificName, confidence, summary, propagationSteps (with title and description), and tips."

        val requestBody = JSONObject().apply {
            put("model", MODEL_ID)
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            }))
            put("response_format", JSONObject().put("type", "json_object"))
        }
        executeOpenRouter(requestBody)
    }

    private suspend fun executeOpenRouter(requestBody: JSONObject): PlantAnalysis {
        val url = "https://openrouter.ai/api/v1/chat/completions"
        val response = try {
            client.post(url) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                header("HTTP-Referer", "https://subodhsonar.com/plantprop")
                header("X-Title", "PlantProp")
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }
        } catch (e: Exception) {
            throw Exception("Network Error: ${e.message}")
        }

        if (response.status != HttpStatusCode.OK) {
            val errorBody = response.bodyAsText()
            Log.e(TAG, "OpenRouter Error: ${response.status} - $errorBody")
            throw Exception("AI Service Error (${response.status})")
        }

        val responseBody = response.bodyAsText()
        val root = JSONObject(responseBody)
        val content = root.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            
        val usageObj = root.optJSONObject("usage")
        val usage = usageObj?.let {
            com.subodhsonar.plantprop.model.TokenUsage(
                promptTokens = it.optInt("prompt_tokens"),
                completionTokens = it.optInt("completion_tokens"),
                reasoningTokens = it.optInt("reasoning_tokens", 0),
                totalTokens = it.optInt("total_tokens")
            )
        }
            
        return parseJsonResponse(content, usage)
    }

    private fun parseJsonResponse(jsonString: String, usage: com.subodhsonar.plantprop.model.TokenUsage? = null): PlantAnalysis {
        try {
            // Log the incoming content for debugging
            Log.d(TAG, "Parsing AI Response: ${jsonString.take(100)}...")

            // 1. Remove markdown code blocks if present (```json ... ```)
            val cleaned = jsonString
                .replace("```json", "")
                .replace("```", "")
                .trim()

            // 2. Find the JSON object using a more robust regex that finds the first '{' and last '}'
            val firstBrace = cleaned.indexOf('{')
            val lastBrace = cleaned.lastIndexOf('}')
            
            if (firstBrace == -1 || lastBrace == -1 || lastBrace < firstBrace) {
                Log.e(TAG, "Malformed content (no braces found): $jsonString")
                throw Exception("Invalid data format returned by AI.")
            }
            
            val cleanJson = cleaned.substring(firstBrace, lastBrace + 1)
            val obj = JSONObject(cleanJson)
            
            val stepsArray = obj.optJSONArray("propagationSteps") ?: JSONArray()
            val steps = List(stepsArray.length()) { i ->
                val s = stepsArray.getJSONObject(i)
                PropagationStep(s.optString("title", "Step ${i+1}"), s.optString("description", ""))
            }
            
            return PlantAnalysis(
                commonName = obj.optString("commonName", "Unknown Plant"),
                scientificName = obj.optString("scientificName", "Species unknown"),
                confidence = obj.optDouble("confidence", 1.0),
                summary = obj.optString("summary", "Botanical description unavailable."),
                propagationSteps = steps,
                tips = obj.optString("tips", ""),
                usage = usage
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parsing error: ${e.message}. Raw content: $jsonString")
            throw Exception("Invalid data format returned by AI.")
        }
    }
}
