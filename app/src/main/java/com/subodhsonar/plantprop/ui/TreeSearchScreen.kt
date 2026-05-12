package com.subodhsonar.plantprop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subodhsonar.plantprop.MainViewModel

@Composable
fun TreeSearchScreen(viewModel: MainViewModel) {
    TreeContainerScreen(viewModel, initialTab = 0)
}

@Composable
fun TreeSearchScreenContent(viewModel: MainViewModel) {
    var addressInput by remember { mutableStateOf("") }
    val topPropagatable by viewModel.topPropagatable.collectAsState()
    val closestTrees by viewModel.closestTrees.collectAsState()
    val searchRadius by viewModel.searchRadius.collectAsState()
    val suggestions by viewModel.addressSuggestions.collectAsState()
    val isTreesLoading by viewModel.isTreesLoading.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isTreesLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF22C55E),
                trackColor = Color(0xFF22C55E).copy(alpha = 0.2f)
            )
        }
        // Search Bar with Autocomplete
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Column {
                OutlinedTextField(
                    value = addressInput,
                    onValueChange = { 
                        addressInput = it
                        viewModel.onAddressQueryChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Enter San Francisco Address") },
                    trailingIcon = {
                        IconButton(onClick = { 
                            viewModel.searchTrees(addressInput)
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF22C55E))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF22C55E),
                        unfocusedBorderColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (suggestions.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 8.dp
                    ) {
                        Column {
                            suggestions.forEach { suggestion ->
                                Text(
                                    text = suggestion,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            addressInput = suggestion
                                            viewModel.searchTrees(suggestion)
                                        }
                                        .padding(16.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }
        }

        RadiusControl(searchRadius, viewModel)

        TreeResultsList(
            topPropagatable = topPropagatable,
            closestTrees = closestTrees,
            viewModel = viewModel,
            showTopProp = true,
            showClosest = false,
            modifier = Modifier.weight(1f),
            emptyContent = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Search to find specimens nearby", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun RadiusControl(searchRadius: Float, viewModel: MainViewModel) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Search Radius", color = Color.White, style = MaterialTheme.typography.labelMedium)
            Text("${(searchRadius * 10).toInt() / 10.0} miles", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
        }
        Slider(
            value = searchRadius,
            onValueChange = { viewModel.setRadius(it) },
            valueRange = 0.5f..7.0f,
            steps = 12,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF22C55E),
                activeTrackColor = Color(0xFF22C55E)
            )
        )
    }
}
