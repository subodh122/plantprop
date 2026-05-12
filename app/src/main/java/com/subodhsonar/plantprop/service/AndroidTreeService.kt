package com.subodhsonar.plantprop.service

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.subodhsonar.plantprop.model.PropagationStepData
import com.subodhsonar.plantprop.model.StreetTree
import com.subodhsonar.plantprop.model.TreeDistance
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import kotlin.coroutines.resume

class AndroidTreeService(private val context: Context) : ITreeService {
    private val geocoder = Geocoder(context, Locale.getDefault())

    private val speciesData = mapOf(
        "Geijera parviflora" to mapOf(
            "propagation" to listOf(
                PropagationStepData("Step 1", "Cutting Selection", "Select semi-hardwood cuttings (about 6 inches long) from mid-summer to early autumn. Cuttings should be firm but still flexible at the tip. Ensure the parent tree is well-hydrated before taking the cut.", "https://images.unsplash.com/photo-1622321481261-267980339906?q=80&w=400"),
                PropagationStepData("Step 2", "Preparation & Hormone", "Remove leaves from the bottom half of the cutting. Make a fresh angled cut just below a node. Dip the base in a high-strength IBA rooting hormone (3000-8000 ppm) to stimulate root development in this stubborn species.", "https://images.unsplash.com/photo-1599591037488-826620577607?q=80&w=400"),
                PropagationStepData("Step 3", "Rooting Environment", "Insert into a sterile, well-draining mix like 50/50 perlite and peat. Australian Willows require bottom heat (around 70°F) and a high-humidity environment (misting or a plastic dome) to root successfully over 8-12 weeks.", "https://images.unsplash.com/photo-1616849411204-63309a96e625?q=80&w=400")
            )
        ),
        "Ficus microcarpa" to mapOf(
            "propagation" to listOf(
                PropagationStepData("Step 1", "Tip Cuttings", "Take 6-8 inch tip cuttings during the active growing season (Spring/Summer). Choose healthy stems with at least 3-4 leaves. Ficus exude a sticky white latex when cut; be careful as it can irritate skin.", "https://images.unsplash.com/photo-1599591037488-826620577607?q=80&w=400"),
                PropagationStepData("Step 2", "Media Selection", "Ficus are vigorous rooters. You can root them directly in a jar of clean water (changed weekly) or a 50/50 mix of perlite and peat moss. Ensure at least two nodes are submerged or buried in the media.", "https://images.unsplash.com/photo-1466692476868-aef1dfb1e735?q=80&w=400"),
                PropagationStepData("Step 3", "Establishment", "Maintain bright, indirect light. Once roots are 2 inches long (usually 3-4 weeks), transplant into rich, organic potting soil. Keep the soil consistently moist but not soggy for the first two weeks while it acclimates.", "https://images.unsplash.com/photo-1528183429188-946d4574b50d?q=80&w=600")
            )
        ),
        "Olea europaea" to mapOf(
            "propagation" to listOf(
                PropagationStepData("Step 1", "Harvesting Semi-Ripe Wood", "Harvest semi-ripe wood cuttings (late summer). The base of the cutting should be starting to turn woody/brown while the tip remains soft and green. Each cutting should be 4-6 inches long.", "https://images.unsplash.com/photo-1589927986089-35812388d1f4?q=80&w=400"),
                PropagationStepData("Step 2", "Wounding & Hormone", "Strip lower leaves and 'wound' the base by removing a 1-inch sliver of bark from one side. This exposes more cambium to the rooting hormone. Dip in a medium-strength IBA rooting powder before sticking.", "https://images.unsplash.com/photo-1464961144732-d74274022802?q=80&w=600"),
                PropagationStepData("Step 3", "Bottom Heat & Drainage", "Use a gritty, extremely well-draining mix (lots of perlite or sand). Olive cuttings root best with consistent 75°F soil temperatures. Avoid overwatering at all costs, as olives are prone to fungal rot during rooting.", "https://images.unsplash.com/photo-1616849411204-63309a96e625?q=80&w=600")
            )
        )
    )

    override suspend fun loadTreesFromAssets(): List<StreetTree> = withContext(Dispatchers.IO) {
        val trees = mutableListOf<StreetTree>()
        try {
            val inputStream = context.assets.open("data/Street_Tree_List_20260426.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine()
            reader.forEachLine { line ->
                val tokens = line.split("\",\"").map { it.trim().removeSurrounding("\"") }
                if (tokens.size >= 17) {
                    val speciesRaw = tokens[2].trim()
                    var scientificName = speciesRaw.split("::").getOrNull(0)?.trim() ?: ""
                    var commonName = speciesRaw.split("::").getOrNull(1)?.trim() ?: ""
                    val lat = tokens[15].trim().toDoubleOrNull() ?: 0.0
                    val lng = tokens[16].trim().toDoubleOrNull() ?: 0.0
                    
                    val standardizedScientific = scientificName.split("'").first().split("(").first().trim()

                    if (commonName.isEmpty()) {
                        commonName = when {
                            standardizedScientific.contains("Platanus", true) -> "California Sycamore"
                            standardizedScientific.contains("Ginkgo", true) -> "Maidenhair Tree"
                            else -> standardizedScientific.split(" ").joinToString(" ") { 
                                it.replaceFirstChar { char -> char.uppercase() } 
                            }
                        }
                    }

                    val customData = speciesData[scientificName] ?: speciesData.entries.find { scientificName.contains(it.key, ignoreCase = true) }?.value
                    
                    if (lat != 0.0 && lng != 0.0) {
                        trees.add(StreetTree(
                            id = tokens[0].trim(), species = speciesRaw, latitude = lat, longitude = lng, address = tokens[3].trim(),
                            propagationEase = if (customData != null) 8 else 3,
                            commonName = commonName,
                            scientificName = scientificName,
                            propagationSteps = (customData?.get("propagation") as? List<PropagationStepData>) ?: defaultPropagation(commonName)
                        ))
                    }
                }
            }
        } catch (_: Exception) { }
        return@withContext trees
    }

    private fun defaultPropagation(name: String) = listOf(
        PropagationStepData("Step 1", "Sourcing Cuttings", "Identify a healthy, disease-free parent $name. Take a 5- to 7-inch cutting from a semi-hardwood branch during the active growing season.", "https://images.unsplash.com/photo-1622321481261-267980339906?q=80&w=400"),
        PropagationStepData("Step 2", "Preparation & Hormone", "Strip away the leaves from the bottom half of the cutting to prevent rot. Dip the bottom 1 inch of the stem into a rooting hormone.", "https://images.unsplash.com/photo-1599591037488-826620577607?q=80&w=400"),
        PropagationStepData("Step 3", "Setting & Humidity", "Insert into a moist, sterile rooting medium. Cover with a clear plastic bag to maintain high humidity.", "https://images.unsplash.com/photo-1616849411204-63309a96e625?q=80&w=600")
    )

    override suspend fun getAddressSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val searchAddress = if (query.contains(",") || query.lowercase().contains("san francisco")) {
            query 
        } else {
            "$query, San Francisco, CA"
        }
        
        return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                try {
                    geocoder.getFromLocationName(searchAddress, 5, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: List<Address>) {
                            continuation.resume(addresses.mapNotNull { it.getAddressLine(0) })
                        }
                        override fun onError(errorMessage: String?) {
                            continuation.resume(emptyList())
                        }
                    })
                } catch (e: Exception) {
                    continuation.resume(emptyList())
                }
            }
        } else {
            try {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(searchAddress, 5)?.mapNotNull { it.getAddressLine(0) } ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getCurrentLocation(): PlatformLocation? = withContext(Dispatchers.IO) {
        try {
            // Check Google Play Services availability
            val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
            if (availability != ConnectionResult.SUCCESS) {
                return@withContext null
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val lastLocation = try { fusedLocationClient.lastLocation.await() } catch (_: Exception) { null }
            if (lastLocation != null && (System.currentTimeMillis() - lastLocation.time) < 300000) {
                return@withContext PlatformLocation(lastLocation.latitude, lastLocation.longitude)
            }

            val cts = CancellationTokenSource()
            val freshLocation = withTimeoutOrNull(15000) {
                try {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await()
                } catch (_: Exception) { null }
            }
            
            val finalLoc = freshLocation ?: lastLocation ?: Location("provider").apply {
                latitude = 37.7881
                longitude = -122.4075
            }
            PlatformLocation(finalLoc.latitude, finalLoc.longitude)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun findTreesNearAddress(address: String, allTrees: List<StreetTree>, radiusMiles: Double): List<TreeDistance> = withContext(Dispatchers.IO) {
        val searchAddress = if (address.contains(",") || address.lowercase().contains("san francisco")) {
            address
        } else {
            "$address, San Francisco, CA"
        }
        val locationList = try { geocoder.getFromLocationName(searchAddress, 1) } catch (_: Exception) { null }
        if (locationList.isNullOrEmpty()) return@withContext emptyList()
        val centerLat = locationList[0].latitude
        val centerLng = locationList[0].longitude
        return@withContext findTreesNearLocation(centerLat, centerLng, allTrees, radiusMiles)
    }

    override suspend fun findTreesNearLocation(lat: Double, lng: Double, allTrees: List<StreetTree>, radiusMiles: Double): List<TreeDistance> = withContext(Dispatchers.IO) {
        return@withContext allTrees.mapNotNull { tree ->
            val results = FloatArray(1)
            Location.distanceBetween(lat, lng, tree.latitude, tree.longitude, results)
            val dist = results[0] / 1609.34
            if (dist <= radiusMiles) TreeDistance(tree, dist) else null
        }.sortedWith(compareByDescending<TreeDistance> { it.tree.propagationEase }.thenBy { it.distanceMiles })
    }
}
