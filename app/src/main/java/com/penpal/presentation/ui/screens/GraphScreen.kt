package com.penpal.presentation.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    storyId: String,
    viewModel: GraphViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSceneDetail: (String) -> Unit,
    onNavigateToCharacterDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val sceneNodes by viewModel.sceneNodes.collectAsState()
    val characterNodes by viewModel.characterNodes.collectAsState()
    val edges by viewModel.edges.collectAsState()
    val characterEdges by viewModel.characterEdges.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (uiState.viewMode) {
                        GraphViewMode.CHARACTER_JOURNEY -> Text(uiState.characterName ?: "Character Journey")
                        GraphViewMode.SCENE_DETAIL -> Text(uiState.sceneTitle ?: "Scene Details")
                        else -> Text(uiState.storyTitle)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.viewMode != GraphViewMode.TIMELINE) {
                            viewModel.resetView()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            if (uiState.viewMode != GraphViewMode.TIMELINE) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        when (uiState.viewMode) {
            GraphViewMode.CHARACTER_JOURNEY -> {
                CharacterJourneyView(
                    characterName = uiState.characterName ?: "",
                    sceneIds = uiState.journeyScenes,
                    sceneNodes = sceneNodes,
                    onSceneClick = { viewModel.showSceneDetails(it) }
                )
            }

            GraphViewMode.SCENE_DETAIL -> {
                SceneDetailView(
                    title = uiState.sceneTitle ?: "",
                    summary = uiState.sceneSummary ?: "",
                    content = uiState.sceneContent ?: "",
                    location = uiState.sceneLocation,
                    mood = uiState.sceneMood,
                    characters = uiState.sceneCharacters,
                    onEdit = { showEditDialog = true },
                    onDelete = { showDeleteDialog = true },
                    onClose = { viewModel.resetView() }
                )
            }

            GraphViewMode.TIMELINE -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Scenes →") }
                        )
                        AssistChip(
                            onClick = { },
                            label = { Text("Characters ↓") }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        InteractiveGraphCanvas(
                            sceneNodes = sceneNodes,
                            characterNodes = characterNodes,
                            edges = edges,
                            characterEdges = characterEdges,
                            selectedNodeId = uiState.selectedNodeId,
                            onSceneTap = { viewModel.showSceneDetails(it) },
                            onCharacterTap = { viewModel.showCharacterJourney(it) },
                            onNodeDrag = { nodeId, x, y -> viewModel.updateNodePosition(nodeId, x, y) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (sceneNodes.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = "No scenes yet. Record and transcribe your story to see the graph.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog && uiState.viewMode == GraphViewMode.SCENE_DETAIL) {
        EditSceneDialog(
            currentTitle = uiState.sceneTitle ?: "",
            currentSummary = uiState.sceneSummary ?: "",
            currentContent = uiState.sceneContent ?: "",
            currentLocation = uiState.sceneLocation,
            currentMood = uiState.sceneMood,
            onSave = { title, summary, content, location, mood ->
                uiState.selectedSceneId?.let { sceneId ->
                    viewModel.updateScene(sceneId, title, summary, content, location, mood)
                }
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete?") },
            text = { Text("Are you sure you want to delete this? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (uiState.viewMode) {
                            GraphViewMode.SCENE_DETAIL -> {
                                uiState.selectedSceneId?.let { viewModel.deleteScene(it) }
                            }
                            GraphViewMode.CHARACTER_JOURNEY -> {
                                uiState.selectedCharacterId?.let { viewModel.deleteCharacter(it) }
                            }
                            else -> {}
                        }
                        viewModel.resetView()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CharacterJourneyView(
    characterName: String,
    sceneIds: List<String>,
    sceneNodes: List<SceneNodeData>,
    onSceneClick: (String) -> Unit
) {
    val journeyScenes = sceneNodes.filter { it.id in sceneIds }.sortedBy { it.orderIndex }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "$characterName appears in ${journeyScenes.size} scenes:",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(journeyScenes) { scene ->
                Card(
                    onClick = { onSceneClick(scene.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = scene.title,
                                style = MaterialTheme.typography.titleSmall
                            )
                            scene.summary?.let {
                                Text(
                                    text = it.take(100),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable 
fun SceneDetailView(
    title: String,
    summary: String,
    content: String,
    location: String?,
    mood: String?,
    characters: List<String>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (summary.isNotEmpty()) {
            Text(text = "Summary", style = MaterialTheme.typography.titleSmall)
            Text(text = summary, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
        }

        location?.let { Text(text = "Location: $it", style = MaterialTheme.typography.bodyMedium) }
        mood?.let { Text(text = "Mood: $it", style = MaterialTheme.typography.bodyMedium) }
        if (characters.isNotEmpty()) {
            Text(text = "Characters: ${characters.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
        }

        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "Full Content", style = MaterialTheme.typography.titleSmall)
        Text(text = content, style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Edit")
            }
            OutlinedButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun EditSceneDialog(
    currentTitle: String,
    currentSummary: String,
    currentContent: String,
    currentLocation: String?,
    currentMood: String?,
    onSave: (String, String, String, String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }
    var summary by remember { mutableStateOf(currentSummary) }
    var content by remember { mutableStateOf(currentContent) }
    var location by remember { mutableStateOf(currentLocation ?: "") }
    var mood by remember { mutableStateOf(currentMood ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Scene") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Summary") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mood,
                    onValueChange = { mood = it },
                    label = { Text("Mood") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(title, summary, content, location.ifBlank { null }, mood.ifBlank { null })
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun InteractiveGraphCanvas(
    sceneNodes: List<SceneNodeData>,
    characterNodes: List<CharacterNodeData>,
    edges: List<GraphEdge>,
    characterEdges: List<GraphEdge>,
    selectedNodeId: String?,
    onSceneTap: (String) -> Unit,
    onCharacterTap: (String) -> Unit,
    onNodeDrag: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val nodeRadius = 30f
        val charSize = 25f

        for (edge in edges) {
            val fromNode = sceneNodes.find { it.id == edge.fromId }
            val toNode = sceneNodes.find { it.id == edge.toId }
            if (fromNode != null && toNode != null) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(fromNode.x, fromNode.y),
                    end = Offset(toNode.x, toNode.y),
                    strokeWidth = 2f
                )
            }
        }

        for (edge in characterEdges) {
            val charNode = characterNodes.find { it.id == edge.fromId }
            val sceneNode = sceneNodes.find { it.id == edge.toId }
            if (charNode != null && sceneNode != null) {
                drawLine(
                    color = Color(charNode.color).copy(alpha = 0.3f),
                    start = Offset(charNode.x, charNode.y),
                    end = Offset(sceneNode.x, sceneNode.y),
                    strokeWidth = 1f
                )
            }
        }

        for (node in sceneNodes) {
            val isSelected = node.id == selectedNodeId
            val color = if (isSelected) Color(0xFF6650a4) else Color(0xFF2196F3)

            drawCircle(color = color, radius = nodeRadius, center = Offset(node.x, node.y))
            if (isSelected) {
                drawCircle(color = Color.White, radius = nodeRadius, center = Offset(node.x, node.y), style = Stroke(width = 3f))
            }

            val textResult = textMeasurer.measure(text = node.title.take(15), style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp))
            drawText(textLayoutResult = textResult, topLeft = Offset(node.x - textResult.size.width / 2, node.y + nodeRadius + 8))
        }

        for (node in characterNodes) {
            val isSelected = node.id == selectedNodeId
            val color = Color(node.color)

            drawRect(color = color, topLeft = Offset(node.x - charSize, node.y - charSize), size = Size(charSize * 2, charSize * 2))
            if (isSelected) {
                drawRect(color = Color.White, topLeft = Offset(node.x - charSize, node.y - charSize), size = Size(charSize * 2, charSize * 2), style = Stroke(width = 3f))
            }

            val textResult = textMeasurer.measure(text = node.name.take(15), style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp))
            drawText(textLayoutResult = textResult, topLeft = Offset(node.x - textResult.size.width / 2, node.y + charSize + 8))
        }
    }
}