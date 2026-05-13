package com.subodhsonar.plantprop.service

import com.subodhsonar.plantprop.model.*

interface IBotanyService {
    suspend fun analyzePlant(imageBytes: ByteArray): PlantAnalysis
    suspend fun getInfoByName(scientificName: String): PlantAnalysis
}

interface IStorageManager {
    fun saveImage(imageBytes: ByteArray): String
    fun saveGarden(garden: List<SavedPlant>)
    fun loadGarden(): List<SavedPlant>
}

interface ITreeService {
    suspend fun loadTreesFromAssets(): List<StreetTree>
    suspend fun getAddressSuggestions(query: String): List<String>
    suspend fun getCurrentLocation(): PlatformLocation?
    suspend fun findTreesNearAddress(address: String, allTrees: List<StreetTree>, radiusMiles: Double): List<TreeDistance>
    suspend fun findTreesNearLocation(lat: Double, lng: Double, allTrees: List<StreetTree>, radiusMiles: Double): List<TreeDistance>
}

interface IPlatformNavigator {
    fun openNavigation(latitude: Double, longitude: Double)
    fun openBrowser(url: String)
}

data class PlatformLocation(val latitude: Double, val longitude: Double)
