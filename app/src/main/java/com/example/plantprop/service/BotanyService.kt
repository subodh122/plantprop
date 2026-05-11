package com.example.plantprop.service

import android.util.Base64
import android.util.Log
import com.example.plantprop.model.PlantAnalysis
import com.example.plantprop.model.PropagationStep
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BotanyService(private val apiKey: String) : IBotanyService {
    private val client = HttpClient()
    private val TAG = "BotanyService"

    override suspend fun analyzePlant(imageBytes: ByteArray): PlantAnalysis = withContext(Dispatchers.IO) {
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        
        val prompt = """
            Analyze this image of a plant or tree. Identify the plant (common and scientific name) and provide comprehensive propagation information. 

            Return your response as a JSON object with this exact structure:
            {
              "commonName": "string",
              "scientificName": "string",
              "confidence": number,
              "summary": "string",
              "propagationSteps": [
                { "title": "Step title", "description": "detailed description" }
              ],
              "tips": "string"
            }
            Provide at least 3-5 detailed propagation steps.
        """.trimIndent()

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", prompt))
                    put(JSONObject().put("inline_data", JSONObject().apply {
                        put("mime_type", "image/jpeg")
                        put("data", base64Image)
                    }))
                })
            }))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent?key=$apiKey"
        
        Log.d(TAG, "Calling Gemini API: https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent")
        
        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }

        if (response.status != HttpStatusCode.OK) {
            val errorBody = response.bodyAsText()
            Log.e(TAG, "Gemini Error: $errorBody")
            throw Exception("Gemini API Error (${response.status}): $errorBody")
        }

        val responseText = response.bodyAsText()
        return@withContext parseGeminiResponse(responseText)
    }

    override suspend fun getInfoByName(name: String): PlantAnalysis = withContext(Dispatchers.IO) {
        val prompt = """
            Provide professional botanical information for the plant: $name.
            Identify its common name and scientific name accurately.
            Focus on its unique characteristics and provide very elaborate, detailed, step-by-step instructions on how to propagate it successfully (mentioning timing, soil type, hormone use, and environment).
            
            Return your response as a JSON object with this exact structure:
            {
              "commonName": "string",
              "scientificName": "string",
              "confidence": 1.0,
              "summary": "string",
              "propagationSteps": [
                { "title": "Step title", "description": "detailed description" }
              ],
              "tips": "string"
            }
            Provide at least 3-5 detailed propagation steps.
        """.trimIndent()

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            }))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent?key=$apiKey"
        
        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }

        if (response.status != HttpStatusCode.OK) {
            val errorBody = response.bodyAsText()
            Log.e(TAG, "Gemini Error: $errorBody")
            throw Exception("Gemini API Error: ${response.status}")
        }

        val responseText = response.bodyAsText()
        return@withContext parseGeminiResponse(responseText)
    }

    private fun parseGeminiResponse(responseText: String): PlantAnalysis {
        val root = JSONObject(responseText)
        val candidate = root.getJSONArray("candidates").getJSONObject(0)
        val text = candidate.getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        
        val jsonStart = text.indexOf('{')
        val jsonEnd = text.lastIndexOf('}')
        if (jsonStart == -1 || jsonEnd == -1) throw Exception("Invalid AI Response Text")
        val jsonString = text.substring(jsonStart, jsonEnd + 1)
        
        val obj = JSONObject(jsonString)
        val stepsArray = obj.getJSONArray("propagationSteps")
        val steps = mutableListOf<PropagationStep>()
        for (i in 0 until stepsArray.length()) {
            val stepObj = stepsArray.getJSONObject(i)
            steps.add(PropagationStep(
                stepObj.getString("title"),
                stepObj.getString("description")
            ))
        }
        
        return PlantAnalysis(
            commonName = obj.getString("commonName"),
            scientificName = obj.getString("scientificName"),
            confidence = obj.optDouble("confidence", 1.0),
            summary = obj.getString("summary"),
            propagationSteps = steps,
            tips = obj.getString("tips")
        )
    }
}
