package com.subodhsonar.plantprop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subodhsonar.plantprop.model.*
import com.subodhsonar.plantprop.service.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
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
    data object TreeDex : AppView()
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

    private val _selectedExplorerTab = MutableStateFlow(0)
    val selectedExplorerTab: StateFlow<Int> = _selectedExplorerTab

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

    private val _isTreesLoading = MutableStateFlow(true)
    val isTreesLoading: StateFlow<Boolean> = _isTreesLoading

    private var allStreetTrees: List<StreetTree> = emptyList()

    init {
        viewModelScope.launch {
            try {
                allStreetTrees = treeService.loadTreesFromAssets()
                updateDex()
            } finally {
                _isTreesLoading.value = false
            }
        }
    }

    private fun updateDex() {
        val collectedNames = _garden.value.map { it.scientificName.lowercase() }.toSet()
        val uniqueSpecies = allStreetTrees
            .distinctBy { it.scientificName.lowercase() }
            .filter { it.scientificName.isNotEmpty() }
            .map { tree ->
                val isCollected = tree.scientificName.lowercase() in collectedNames
                DexEntry(
                    scientificName = tree.scientificName,
                    commonName = tree.commonName,
                    isCollected = isCollected,
                    count = _garden.value.count { it.scientificName.lowercase() == tree.scientificName.lowercase() },
                    firstCollectedDate = _garden.value.find { it.scientificName.lowercase() == tree.scientificName.lowercase() }?.date
                )
            }
            .sortedBy { it.commonName }
        
        _treeDex.value = uniqueSpecies
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
                // Wait for trees to load if they haven't yet
                if (_isTreesLoading.value) {
                    while (_isTreesLoading.value) {
                        delay(500)
                    }
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
                // 1. Fetch AI info and Image URL in parallel
                val aiInfoDeferred = async { 
                    try {
                        botanyService.getInfoByName(treeDistance.tree.scientificName)
                    } catch (e: Exception) { null }
                }
                
                val wikiImgDeferred = async {
                    // Force a deep search for the image before moving to the next screen
                    wikiService.fetchPlantImage(treeDistance.tree.scientificName, treeDistance.tree.commonName)
                }

                val aiInfoResult = aiInfoDeferred.await()
                val wikiImg = wikiImgDeferred.await()
                
                // We only navigate once we have at least tried to fetch the image and info
                // If AI fails but we have an image, we still show the screen with fallback info
                // If we have neither, we'll show a generic error rather than a blank screen
                
                if (aiInfoResult != null || wikiImg != null) {
                    _wikiInfo.value = WikiInfo(
                        title = aiInfoResult?.commonName ?: treeDistance.tree.commonName,
                        extract = aiInfoResult?.summary ?: "Botany information for ${treeDistance.tree.scientificName} is being researched.",
                        thumbnailUrl = wikiImg ?: "https://images.unsplash.com/photo-1542273917363-3b1817f69a2d?q=80&w=1200",
                        propagationSteps = aiInfoResult?.propagationSteps ?: defaultPropagationSteps(treeDistance.tree.commonName)
                    )
                    _currentView.value = AppView.TreeDetail
                } else {
                    _error.value = "Unable to find botanical records or images for this specimen."
                }
                
            } catch (e: Exception) {
                _error.value = "An error occurred while loading tree details."
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

    private val _treeDex = MutableStateFlow<List<DexEntry>>(emptyList())
    val treeDex: StateFlow<List<DexEntry>> = _treeDex

    fun setView(view: AppView) {
        _currentView.value = view
    }

    fun setExplorerTab(tab: Int) {
        _selectedExplorerTab.value = tab
    }

    fun navigateToTree(tree: StreetTree) {
        navigator.openNavigation(tree.latitude, tree.longitude)
    }

    fun openWikipedia(scientificName: String) {
        navigator.openBrowser("https://en.wikipedia.org/wiki/$scientificName")
    }

    fun openBrowser(url: String) {
        navigator.openBrowser(url)
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
                // Identify and get clean botanical info in one go
                val result = botanyService.analyzePlant(imageBytes)
                _analysisResult.value = result
                _currentView.value = AppView.Results
                
                // Fetch high-quality reference image in background after navigating
                val img = wikiService.fetchPlantImage(result.scientificName, result.commonName)
                _referenceImageUrl.value = img
                
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred during analysis."
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
        val dateStr = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
        val id = Random.nextLong().toString(16)
        
        val newPlant = result.toSavedPlant(id = id, imagePath = imagePath, date = dateStr)
        val updatedGarden = listOf(newPlant) + _garden.value
        _garden.value = updatedGarden
        storageManager.saveGarden(updatedGarden)
        updateDex()
        _currentView.value = AppView.Garden
    }

    fun deleteFromGarden(id: String) {
        val updatedGarden = _garden.value.filter { it.id != id }
        _garden.value = updatedGarden
        storageManager.saveGarden(updatedGarden)
        updateDex()
        if (_selectedGardenPlant.value?.id == id) { _selectedGardenPlant.value = null }
    }

    fun openGardenPlant(plant: SavedPlant) {
        _selectedGardenPlant.value = plant
        _referenceImageUrl.value = null
        _analysisResult.value = PlantAnalysis(plant.commonName, plant.scientificName, plant.confidence, plant.summary, plant.propagationSteps, plant.tips)
        _currentView.value = AppView.Results
        viewModelScope.launch {
            _referenceImageUrl.value = wikiService.fetchPlantImage(plant.scientificName, plant.commonName)
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

    private fun defaultPropagationSteps(name: String) = listOf(
        PropagationStep("Sourcing Cuttings", "Identify a healthy, disease-free parent $name. Take a 5- to 7-inch cutting from a semi-hardwood branch during the active growing season."),
        PropagationStep("Preparation", "Strip away the leaves from the bottom half of the cutting to prevent rot. Optionally, dip the bottom 1 inch of the stem into a rooting hormone."),
        PropagationStep("Setting & Humidity", "Insert into a moist, sterile rooting medium. Keep in a bright spot with indirect light and maintain humidity.")
    )
}
