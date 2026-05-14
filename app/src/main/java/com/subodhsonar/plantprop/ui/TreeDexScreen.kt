package com.subodhsonar.plantprop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subodhsonar.plantprop.AppView
import com.subodhsonar.plantprop.MainViewModel
import com.subodhsonar.plantprop.model.DexEntry

@Composable
fun TreeDexScreen(viewModel: MainViewModel) {
    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFF121212),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.setView(AppView.Landing) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "SF TreeDex",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.setView(AppView.Garden) },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Garden", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        },
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            TreeDexScreenContent(viewModel)
        }
    }
}

@Composable
fun TreeDexScreenContent(viewModel: MainViewModel) {
    val treeDex by viewModel.treeDex.collectAsState()
    val totalSpecies = treeDex.size
    val collectedSpecies = treeDex.count { it.isCollected }

    Column(modifier = Modifier.fillMaxSize()) {
        // Progress Summary
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Collection Progress",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "SF TreeDex",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$collectedSpecies / $totalSpecies",
                            color = Color(0xFF22C55E),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Species Found",
                            color = Color(0xFF22C55E).copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val progress = if (totalSpecies > 0) collectedSpecies.toFloat() / totalSpecies else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = Color(0xFF22C55E),
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            val sortedTreeDex = treeDex.sortedWith(
                compareByDescending<DexEntry> { it.isCollected }
                    .thenBy { it.commonName }
            )
            items(sortedTreeDex) { entry ->
                DexItem(entry) {
                    if (entry.isCollected) {
                        val gardenPlant = viewModel.garden.value.find { 
                            it.scientificName.lowercase() == entry.scientificName.lowercase() || 
                            it.commonName.lowercase() == entry.commonName.lowercase() 
                        }
                        if (gardenPlant != null) {
                            viewModel.openGardenPlant(gardenPlant)
                        } else {
                            viewModel.setView(AppView.Garden)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DexItem(entry: DexEntry, onClick: () -> Unit) {
    Surface(
        color = if (entry.isCollected) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.02f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .aspectRatio(0.85f)
            .clickable(enabled = entry.isCollected) { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (entry.isCollected) Color(0xFF22C55E).copy(alpha = 0.2f) 
                        else Color.White.copy(alpha = 0.05f), 
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (entry.isCollected) Icons.Default.CheckCircle else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (entry.isCollected) Color(0xFF22C55E) else Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = if (entry.isCollected) {
                    entry.commonName.split(",").first().trim()
                } else {
                    "???"
                },
                color = if (entry.isCollected) Color.White else Color.White.copy(alpha = 0.3f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 12.sp,
                fontWeight = if (entry.isCollected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
