package com.subodhsonar.plantprop.model

data class PropagationStep(
    val title: String,
    val description: String
)

data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val reasoningTokens: Int,
    val totalTokens: Int
)

data class PlantAnalysis(
    val commonName: String,
    val scientificName: String,
    val confidence: Double,
    val summary: String,
    val propagationSteps: List<PropagationStep>,
    val tips: String,
    val usage: TokenUsage? = null
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
