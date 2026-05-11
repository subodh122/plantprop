package com.example.plantprop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plantprop.model.*
import com.example.plantprop.service.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random

sealed class AppView {
    data object Landing : AppView()
    data object Camera : AppView()
    data object Results : AppView()
    data object Garden : AppView()
    data object TreeSearch : AppView()
    data object LiveTrees : AppView()
    data object TreeDetail : AppView()
}

class MainViewModel(
    private val botanyService: IBotanyService,
    private val storageManager: IStorageManager,
    private val treeService: ITreeService,
    private val wikiService: WikipediaService,
    private val navigator: IPlatformNavigator
) : ViewModel() {
    private val TAG = "MainViewModel"

    private val _currentView = MutableStateFlow<AppView>(AppView.Landing)
    val currentView: StateFlow<AppView> = _currentView

    private val _topPropagatable = MutableStateFlow<List<TreeDistance>>(emptyList())
    val topPropagatable: StateFlow<List<TreeDistance>> = _topPropagatable

    private val _closestTrees = MutableStateFlow<List<TreeDistance>>(emptyList())
    val closestTrees: StateFlow<List<TreeDistance>> = _closestTrees

    private val _selectedTree = MutableStateFlow<TreeDistance?>(null)
    val selectedTree: StateFlow<TreeDistance?> = _selectedTree

    private val _wikiInfo = MutableStateFlow<WikiInfo?>(null)
    val wikiInfo: StateFlow<WikiInfo?> = _wikiInfo

    private val _addressSuggestions = MutableStateFlow<List<String>>(emptyList())
    val addressSuggestions: StateFlow<List<String>> = _addressSuggestions

    private val _searchRadius = MutableStateFlow(2.0f)
    val searchRadius: StateFlow<Float> = _searchRadius

    private val _isSearchingLocation = MutableStateFlow(false)
    val isSearchingLocation: StateFlow<Boolean> = _isSearchingLocation

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private var allStreetTrees: List<StreetTree> = emptyList()

    init {
        viewModelScope.launch {
            allStreetTrees = treeService.loadTreesFromAssets()
        }
    }

    fun onAddressQueryChange(query: String) {
        viewModelScope.launch {
            if (query.length >= 4) {
                _addressSuggestions.value = treeService.getAddressSuggestions(query)
            } else {
                _addressSuggestions.value = emptyList()
            }
        }
    }

    fun setRadius(radius: Float) {
        _searchRadius.value = radius
    }

    fun searchTreesLive() {
        _currentView.value = AppView.LiveTrees
        _isSearchingLocation.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                var attempts = 0
                while (allStreetTrees.isEmpty() && attempts < 5) {
                    delay(1000)
                    attempts++
                }

                val location = treeService.getCurrentLocation()
                if (location != null) {
                    val results = treeService.findTreesNearLocation(location.latitude, location.longitude, allStreetTrees, _searchRadius.value.toDouble())
                    
                    val closestUnique = results
                        .sortedBy { it.distanceMiles }
                        .distinctBy { it.tree.scientificName }
                        .take(10)
                    
                    _closestTrees.value = closestUnique
                } else {
                    _error.value = "GPS timed out. Try moving near a window or using the search bar."
                }
            } catch (e: Exception) {
                _error.value = "Error locating trees: ${e.message}"
            } finally {
                _isSearchingLocation.value = false
            }
        }
    }

    fun searchTrees(query: String) {
        _addressSuggestions.value = emptyList()
        viewModelScope.launch {
            val results = treeService.findTreesNearAddress(query, allStreetTrees, _searchRadius.value.toDouble())
            val topProp = results.distinctBy { it.tree.scientificName }.take(10)
            val topPropIds = topProp.map { it.tree.id }.toSet()
            val closest = results
                .filter { it.tree.id !in topPropIds }
                .sortedBy { it.distanceMiles }
                .take(10)
            
            _topPropagatable.value = topProp
            _closestTrees.value = closest
        }
    }

    fun openTreeDetail(treeDistance: TreeDistance) {
        _isProcessing.value = true
        _selectedTree.value = treeDistance
        _wikiInfo.value = null
        
        viewModelScope.launch {
            try {
                // Try to find the best image and info
                val info = wikiService.getTreeInfo(treeDistance.tree.scientificName) ?: 
                          wikiService.getTreeInfo(treeDistance.tree.commonName)
                
                _wikiInfo.value = info
                
                // If we have no image, try one last AI research attempt
                if (info?.thumbnailUrl == null) {
                    try {
                        val aiInfo = botanyService.getInfoByName(treeDistance.tree.scientificName)
                        _wikiInfo.value = WikiInfo(
                            title = aiInfo.commonName,
                            extract = aiInfo.summary,
                            thumbnailUrl = "https://images.unsplash.com/photo-1542273917363-3b1817f69a2d?q=80&w=1200",
                            propagationSteps = aiInfo.propagationSteps
                        )
                    } catch (e: Exception) {
                        // Keep whatever we got from wiki
                    }
                }
                _currentView.value = AppView.TreeDetail
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private val _isAnalyzing = MutableStateFlow(value = false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing
    private val _analysisResult = MutableStateFlow<PlantAnalysis?>(null)
    val analysisResult: StateFlow<PlantAnalysis?> = _analysisResult
    private val _capturedImageBytes = MutableStateFlow<ByteArray?>(null)
    val capturedImageBytes: StateFlow<ByteArray?> = _capturedImageBytes
    private val _referenceImageUrl = MutableStateFlow<String?>(null)
    val referenceImageUrl: StateFlow<String?> = _referenceImageUrl
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _garden = MutableStateFlow(storageManager.loadGarden())
    val garden: StateFlow<List<SavedPlant>> = _garden
    private val _selectedGardenPlant = MutableStateFlow<SavedPlant?>(null)
    val selectedGardenPlant: StateFlow<SavedPlant?> = _selectedGardenPlant

    fun setView(view: AppView) {
        _currentView.value = view
    }

    fun navigateToTree(tree: StreetTree) {
        navigator.openNavigation(tree.latitude, tree.longitude)
    }

    fun openWikipedia(scientificName: String) {
        navigator.openBrowser("https://en.wikipedia.org/wiki/$scientificName")
    }

    fun handleCapture(imageBytes: ByteArray) {
        _isAnalyzing.value = true
        _isProcessing.value = true
        _capturedImageBytes.value = imageBytes
        _referenceImageUrl.value = null
        _error.value = null
        _selectedGardenPlant.value = null
        
        viewModelScope.launch {
            try {
                val result = botanyService.analyzePlant(imageBytes)
                _analysisResult.value = result
                
                var img = wikiService.fetchPlantImage(result.scientificName)
                if (img == null) {
                    img = wikiService.fetchPlantImage(result.commonName)
                }
                _referenceImageUrl.value = img
                _currentView.value = AppView.Results
                
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
                _currentView.value = AppView.Results
            } finally { 
                _isAnalyzing.value = false
                _isProcessing.value = false
            }
        }
    }

    fun saveToGarden() {
        val result = _analysisResult.value ?: return
        val imageBytes = _capturedImageBytes.value ?: return
        val imagePath = storageManager.saveImage(imageBytes)
        
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val dateStr = "${now.monthNumber}/${now.dayOfMonth}/${now.year}"
        val id = Random.nextLong().toString(16)
        
        val newPlant = result.toSavedPlant(id = id, imagePath = imagePath, date = dateStr)
        val updatedGarden = listOf(newPlant) + _garden.value
        _garden.value = updatedGarden
        storageManager.saveGarden(updatedGarden)
        _currentView.value = AppView.Garden
    }

    fun deleteFromGarden(id: String) {
        val updatedGarden = _garden.value.filter { it.id != id }
        _garden.value = updatedGarden
        storageManager.saveGarden(updatedGarden)
        if (_selectedGardenPlant.value?.id == id) { _selectedGardenPlant.value = null }
    }

    fun openGardenPlant(plant: SavedPlant) {
        _selectedGardenPlant.value = plant
        _referenceImageUrl.value = null
        _analysisResult.value = PlantAnalysis(plant.commonName, plant.scientificName, plant.confidence, plant.summary, plant.propagationSteps, plant.tips)
        _currentView.value = AppView.Results
        viewModelScope.launch {
            _referenceImageUrl.value = wikiService.fetchPlantImage(plant.scientificName) ?: 
                                     wikiService.fetchPlantImage(plant.commonName)
        }
    }

    fun reset() {
        _analysisResult.value = null
        _capturedImageBytes.value = null
        _referenceImageUrl.value = null
        _isAnalyzing.value = false
        _error.value = null
        _selectedGardenPlant.value = null
        _selectedTree.value = null
        _wikiInfo.value = null
        _topPropagatable.value = emptyList()
        _closestTrees.value = emptyList()
        _currentView.value = AppView.Landing
    }
}
