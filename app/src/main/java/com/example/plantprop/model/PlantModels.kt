package com.example.plantprop.model

data class PropagationStep(
    val title: String,
    val description: String
)

data class PlantAnalysis(
    val commonName: String,
    val scientificName: String,
    val confidence: Double,
    val summary: String,
    val propagationSteps: List<PropagationStep>,
    val tips: String
)

data class SavedPlant(
    val id: String,
    val commonName: String,
    val scientificName: String,
    val confidence: Double,
    val summary: String,
    val propagationSteps: List<PropagationStep>,
    val tips: String,
    val imagePath: String,
    val date: String
)

fun PlantAnalysis.toSavedPlant(id: String, imagePath: String, date: String): SavedPlant {
    return SavedPlant(
        id = id,
        commonName = commonName,
        scientificName = scientificName,
        confidence = confidence,
        summary = summary,
        propagationSteps = propagationSteps,
        tips = tips,
        imagePath = imagePath,
        date = date
    )
}
