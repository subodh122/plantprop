package com.example.plantprop

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.plantprop.service.BotanyService
import com.example.plantprop.service.AndroidStorageManager
import com.example.plantprop.service.AndroidTreeService
import com.example.plantprop.service.WikipediaService
import com.example.plantprop.service.AndroidNavigator

class MainActivity : ComponentActivity() {
    private val botanyService = BotanyService(apiKey = "AIzaSyBdv9NzCtfp7bsVAewKVftgh6iAlN6y43A")
    private lateinit var viewModel: MainViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (locationGranted && (viewModel.currentView.value is AppView.LiveTrees)) {
            viewModel.searchTreesLive()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        
        viewModel = MainViewModel(
            botanyService = botanyService,
            storageManager = AndroidStorageManager(this),
            treeService = AndroidTreeService(this),
            wikiService = WikipediaService(),
            navigator = AndroidNavigator(this)
        )
        
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setContent {
            App(viewModel)
        }
    }
}
