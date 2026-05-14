package com.subodhsonar.plantprop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subodhsonar.plantprop.AppView
import com.subodhsonar.plantprop.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val currentView by viewModel.currentView.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentView) {
            AppView.Landing -> LandingScreen(viewModel)
            AppView.Camera -> CameraScreen(viewModel)
            AppView.Results -> ResultScreen(viewModel)
            AppView.Garden -> GardenScreen(viewModel)
            AppView.TreeSearch -> TreeSearchScreen(viewModel)
            AppView.LiveTrees -> LiveTreesScreen(viewModel)
            AppView.TreeDetail -> TreeDetailScreen(viewModel)
            AppView.TreeDex -> TreeDexScreen(viewModel)
        }

        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF22C55E))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Botanical Analysis...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
