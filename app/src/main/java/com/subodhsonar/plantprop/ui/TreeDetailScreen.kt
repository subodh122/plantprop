package com.subodhsonar.plantprop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.subodhsonar.plantprop.AppView
import com.subodhsonar.plantprop.MainViewModel
import com.subodhsonar.plantprop.model.PropagationStep

@Composable
fun TreeDetailScreen(viewModel: MainViewModel) {
    val treeDist by viewModel.selectedTree.collectAsState()
    val wikiInfo by viewModel.wikiInfo.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

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
                    IconButton(onClick = { viewModel.setView(AppView.TreeSearch) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "Tree Information",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.reset() },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search Trees", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            treeDist?.let { item ->
                val tree = item.tree
                val commonName = tree.commonName.ifEmpty { 
                    tree.scientificName.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                }
                
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = commonName,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tree.scientificName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF22C55E),
                        fontStyle = FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Primary Wikipedia Image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color(0xFF22C55E))
                        } else {
                            val imageModel = remember(wikiInfo?.thumbnailUrl) {
                                wikiInfo?.thumbnailUrl?.takeIf { it.isNotEmpty() } 
                                    ?: "https://images.unsplash.com/photo-1542273917363-3b1817f69a2d?q=80&w=1200"
                            }
                            
                            AsyncImage(
                                model = imageModel,
                                contentDescription = tree.commonName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Wikipedia Data
                    Text("Botanical Description", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (wikiInfo == null) {
                                Text(
                                    text = "Botanical data for this specimen is currently unavailable on Wikipedia.",
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                Text(
                                    text = wikiInfo!!.extract,
                                    color = Color.White.copy(alpha = 0.8f),
                                    lineHeight = 22.sp,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                TextButton(
                                    onClick = { viewModel.openWikipedia(tree.scientificName) },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("READ ON WIKIPEDIA", color = Color(0xFF22C55E), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Propagation Guide
                    Text("Propagation Guide", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val stepsToShow = wikiInfo?.propagationSteps ?: tree.propagationSteps.map { 
                        PropagationStep(it.title, it.description) 
                    }

                    stepsToShow.forEachIndexed { index, step ->
                        CleanPropagationStep(
                            title = step.title,
                            description = step.description
                        )
                        if (index < stepsToShow.size - 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Location Action
                    Button(
                        onClick = { viewModel.navigateToTree(tree) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("NAVIGATE TO SPECIMEN", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = tree.address,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(64.dp).navigationBarsPadding())
                }
            }
        }
    }
}

fun parseBotanicalMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { lineIndex, line ->
            var currentLine = line.trim()
            if (currentLine.startsWith("* ")) {
                append("  • ")
                currentLine = currentLine.substring(2)
            }
            val parts = currentLine.split("**")
            parts.forEachIndexed { index, part ->
                if (index % 2 == 1) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) { append(part) }
                } else { append(part) }
            }
            if (lineIndex < lines.size - 1) { append("\n") }
        }
    }
}

@Composable
fun CleanPropagationStep(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = Color(0xFF22C55E),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.size(12.dp)
            ) { }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = parseBotanicalMarkdown(description),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            lineHeight = 20.sp
        )
    }
}
