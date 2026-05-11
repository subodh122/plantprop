package com.example.plantprop.model

data class StreetTree(
    val id: String,
    val species: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val propagationEase: Int = 0,
    val commonName: String = "",
    val scientificName: String = "",
    val family: String = "",
    val nativeRange: String = "",
    val description: String = "",
    val uses: String = "",
    val propagationSteps: List<PropagationStepData> = emptyList(),
    val matureImageUrl: String = "",
    val foliageImageUrl: String = ""
)

data class PropagationStepData(
    val step: String,
    val title: String,
    val description: String,
    val imageUrl: String
)

data class TreeDistance(
    val tree: StreetTree,
    val distanceMiles: Double
)

data class GroupedTree(
    val species: String,
    val commonName: String,
    val scientificName: String,
    val family: String,
    val nativeRange: String,
    val description: String,
    val uses: String,
    val propagationTechniques: String,
    val imageUrl: String,
    val foliageImageUrl: String,
    val propagationEase: Int,
    val locations: List<TreeLocation>
)

data class TreeLocation(
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMiles: Double
)
