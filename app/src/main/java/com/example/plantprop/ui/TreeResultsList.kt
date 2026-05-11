package com.example.plantprop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.plantprop.MainViewModel
import com.example.plantprop.model.TreeDistance

@Composable
fun TreeResultsList(
    topPropagatable: List<TreeDistance>,
    closestTrees: List<TreeDistance>,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    showTopProp: Boolean = true,
    showClosest: Boolean = true,
    emptyContent: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No specimens found in this area", color = Color.Gray)
        }
    },
) {
    val showResults = (showTopProp && topPropagatable.isNotEmpty()) || (showClosest && closestTrees.isNotEmpty())
    
    if (!showResults) {
        emptyContent()
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.navigationBarsPadding()
        ) {
            if (showTopProp && topPropagatable.isNotEmpty()) {
                item {
                    Text(
                        "TOP PROPAGATION SPECIMENS",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF22C55E),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(topPropagatable) { item ->
                    TreeResultCard(item) {
                        viewModel.openTreeDetail(item)
                    }
                }
            }

            if (showClosest && closestTrees.isNotEmpty()) {
                item {
                    if (showTopProp && topPropagatable.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Text(
                        "CLOSEST SPECIMENS",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF3B82F6),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(closestTrees) { item ->
                    TreeResultCard(item) {
                        viewModel.openTreeDetail(item)
                    }
                }
            }
        }
    }
}

@Composable
fun TreeResultCard(treeDist: TreeDistance, onClick: () -> Unit) {
    val commonName = treeDist.tree.commonName.ifEmpty { 
        treeDist.tree.scientificName.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = commonName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = treeDist.tree.scientificName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF22C55E),
                        fontStyle = FontStyle.Italic
                    )
                }
                Surface(
                    color = Color(0xFF22C55E).copy(alpha = 0.1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${treeDist.tree.propagationEase}/10 Ease",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color(0xFF22C55E),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = treeDist.tree.address,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = "${((treeDist.distanceMiles * 100).toInt() / 100.0)} miles away",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
        }
    }
}
