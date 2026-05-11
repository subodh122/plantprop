package com.example.plantprop.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.edit
import com.example.plantprop.model.PropagationStep
import com.example.plantprop.model.SavedPlant
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AndroidStorageManager(private val context: Context) : IStorageManager {
    private val prefs = context.getSharedPreferences("plant_garden", Context.MODE_PRIVATE)

    override fun saveImage(imageBytes: ByteArray): String {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val fileName = "plant_${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }

    override fun saveGarden(garden: List<SavedPlant>) {
        val jsonArray = JSONArray()
        garden.forEach { plant ->
            val plantJson = JSONObject().apply {
                put("id", plant.id)
                put("commonName", plant.commonName)
                put("scientificName", plant.scientificName)
                put("confidence", plant.confidence)
                put("summary", plant.summary)
                put("tips", plant.tips)
                put("imagePath", plant.imagePath)
                put("date", plant.date)
                
                val stepsArray = JSONArray()
                plant.propagationSteps.forEach { step ->
                    stepsArray.put(JSONObject().apply {
                        put("title", step.title)
                        put("description", step.description)
                    })
                }
                put("propagationSteps", stepsArray)
            }
            jsonArray.put(plantJson)
        }
        prefs.edit { putString("garden_list", jsonArray.toString()) }
    }

    override fun loadGarden(): List<SavedPlant> {
        val data = prefs.getString("garden_list", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(data)
            val list = mutableListOf<SavedPlant>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val stepsArray = obj.getJSONArray("propagationSteps")
                val steps = mutableListOf<PropagationStep>()
                for (j in 0 until stepsArray.length()) {
                    val stepObj = stepsArray.getJSONObject(j)
                    steps.add(PropagationStep(
                        stepObj.getString("title"),
                        stepObj.getString("description")
                    ))
                }
                list.add(SavedPlant(
                    id = obj.getString("id"),
                    commonName = obj.getString("commonName"),
                    scientificName = obj.getString("scientificName"),
                    confidence = obj.getDouble("confidence"),
                    summary = obj.getString("summary"),
                    propagationSteps = steps,
                    tips = obj.getString("tips"),
                    imagePath = obj.getString("imagePath"),
                    date = obj.getString("date")
                ))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
