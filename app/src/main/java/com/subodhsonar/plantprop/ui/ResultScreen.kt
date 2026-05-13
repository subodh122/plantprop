package com.subodhsonar.plantprop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import com.subodhsonar.plantprop.AppView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Scale
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import com.subodhsonar.plantprop.MainViewModel
import android.util.Log
import androidx.compose.ui.platform.LocalContext

@Composable
fun ResultScreen(viewModel: MainViewModel) {
    val result by viewModel.analysisResult.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val imageBytes by viewModel.capturedImageBytes.collectAsState()
    val referenceUrl by viewModel.referenceImageUrl.collectAsState()
    val selectedPlant by viewModel.selectedGardenPlant.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            Surface(color = Color(0xFF121212), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.setView(AppView.Camera) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = if (isAnalyzing) "Analyzing..." else result?.commonName ?: "Analysis",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.setView(AppView.TreeSearch) },
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
            // Image Section
            PlantImageGallery(
                capturedImageBytes = imageBytes,
                gardenImagePath = selectedPlant?.imagePath,
                referenceUrl = referenceUrl,
                isAnalyzing = isAnalyzing
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            result?.let { res ->
                if (!isAnalyzing) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = res.commonName,
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = res.scientificName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF22C55E),
                            fontStyle = FontStyle.Italic
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Surface(
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = parseMarkdown(res.summary),
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(16.dp),
                                lineHeight = 22.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                    Text("Propagation Guide", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    res.propagationSteps.forEachIndexed { index, step ->
                        ResultPropagationStep(
                            title = step.title,
                            description = step.description
                        )
                        if (index < res.propagationSteps.size - 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                        
                        Text("Expert Tips", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = parseMarkdown(res.tips),
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 22.sp,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Botanical Research Links
                        Text("Research Resources", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Explore external scientific databases for this species", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        ResearchLinkItem(
                            title = "USDA Plants Database",
                            description = "Detailed profiles, distribution maps, and legal status.",
                            onClick = { 
                                val encoded = res.scientificName.replace(" ", "+")
                                viewModel.openBrowser("https://plants.sc.egov.usda.gov/home/basicSearch?q=$encoded")
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ResearchLinkItem(
                            title = "iNaturalist Observations",
                            description = "Real-world sightings and community identifications.",
                            onClick = { 
                                val encoded = res.scientificName.replace(" ", "+")
                                viewModel.openBrowser("https://www.inaturalist.org/search?q=$encoded") 
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ResearchLinkItem(
                            title = "Kew World Checklist",
                            description = "The definitive international database of plant species.",
                            onClick = { 
                                val encoded = res.scientificName.replace(" ", "+")
                                viewModel.openBrowser("https://powo.science.kew.org/results?q=$encoded") 
                            }
                        )
                    }
                }
            }

            // Bottom Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedPlant == null && result != null && !isAnalyzing) {
                    Button(
                        onClick = { viewModel.saveToGarden() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF22C55E))
                        Spacer(Modifier.width(8.dp))
                        Text("SAVE")
                    }
                }
                Button(
                    onClick = { viewModel.setView(AppView.Camera) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedPlant != null) "NEW SCAN" else "RESCAN")
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun PlantImageGallery(
    capturedImageBytes: ByteArray?,
    gardenImagePath: String?,
    referenceUrl: String?,
    isAnalyzing: Boolean
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Capture Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                val model = gardenImagePath ?: capturedImageBytes
                if (model != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(model)
                            .precision(Precision.EXACT)
                            .scale(Scale.FILL)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Your Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Surface(color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(topStart = 12.dp), modifier = Modifier.align(Alignment.BottomEnd)) {
                    Text("CAPTURE", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                }
            }

            // Reference Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                if (referenceUrl == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF22C55E),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Finding photo...",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp
                        )
                    }
                } else {
                    Log.d("PlantImageGallery", "Displaying reference URL: $referenceUrl")
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(referenceUrl)
                            .httpHeaders(NetworkHeaders.Builder()
                                .set("User-Agent", "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36")
                                .set("Referer", "https://commons.wikimedia.org/")
                                .build())
                            .precision(Precision.EXACT)
                            .scale(Scale.FILL)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Reference Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onState = { state ->
                            when (state) {
                                is coil3.compose.AsyncImagePainter.State.Error -> {
                                    Log.e("PlantImageGallery", "Error loading image: $referenceUrl", state.result.throwable)
                                }
                                is coil3.compose.AsyncImagePainter.State.Success -> {
                                    Log.d("PlantImageGallery", "Successfully loaded image: $referenceUrl")
                                }
                                else -> {}
                            }
                        }
                    )
                    Surface(color = Color(0xFF22C55E).copy(alpha = 0.9f), shape = RoundedCornerShape(topStart = 12.dp), modifier = Modifier.align(Alignment.BottomEnd)) {
                        Text("REFERENCE", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ResultPropagationStep(title: String, description: String) {
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
            text = parseMarkdown(description),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            lineHeight = 20.sp
        )
    }
}

fun parseMarkdown(text: String): AnnotatedString {
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
