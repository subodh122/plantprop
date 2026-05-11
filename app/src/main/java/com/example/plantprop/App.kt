package com.example.plantprop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.plantprop.ui.MainScreen

@Composable
fun App(viewModel: MainViewModel) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFF0F172A) 
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
