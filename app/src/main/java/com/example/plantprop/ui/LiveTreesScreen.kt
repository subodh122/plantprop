package com.example.plantprop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.plantprop.MainViewModel
import com.example.plantprop.model.TreeDistance
import com.example.plantprop.ui.*

@Composable
fun LiveTreesScreen(viewModel: MainViewModel) {
    TreeContainerScreen(viewModel, initialTab = 1)
}

@Composable
fun LiveTreesScreenContent(viewModel: MainViewModel) {
    val topPropagatable by viewModel.topPropagatable.collectAsState()
    val closestTrees by viewModel.closestTrees.collectAsState()
    val searchRadius by viewModel.searchRadius.collectAsState()
    val isSearching by viewModel.isSearchingLocation.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (error != null) {
            Surface(
                color = Color.Red.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = error!!,
                    color = Color.Red,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Radius Control with Refresh
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                RadiusControl(searchRadius, viewModel)
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.searchTreesLive() },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .background(if (isSearching) Color.Gray.copy(alpha = 0.2f) else Color(0xFF22C55E).copy(alpha = 0.1f), CircleShape),
                enabled = !isSearching
            ) {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.Gray)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF22C55E))
                }
            }
        }

        TreeResultsList(
            topPropagatable = topPropagatable,
            closestTrees = closestTrees,
            viewModel = viewModel,
            showTopProp = false,
            showClosest = true,
            modifier = Modifier.weight(1f),
            emptyContent = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isSearching) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF22C55E))
                            Spacer(Modifier.height(16.dp))
                            Text("Locating trees near you...", color = Color.Gray)
                        }
                    } else {
                        Text("No trees found. Try increasing the search radius.", color = Color.Gray)
                    }
                }
            }
        )
    }
}
