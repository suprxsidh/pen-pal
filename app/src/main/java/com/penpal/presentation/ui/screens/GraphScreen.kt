package com.penpal.presentation.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.storyTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
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
                Text(
                    text = "Scenes (horizontal)",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "Characters (vertical)",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                GraphCanvas(
                    sceneNodes = sceneNodes,
                    characterNodes = characterNodes,
                    edges = edges,
                    scale = scale,
                    offset = offset,
                    selectedNodeId = uiState.selectedNodeId,
                    onNodeTap = { nodeId ->
                        viewModel.selectNode(nodeId)
                    },
                    onNodeDrag = { nodeId, x, y ->
                        viewModel.updateNodePosition(nodeId, x, y)
                    },
                    onScaleChange = { newScale -> scale = newScale },
                    onOffsetChange = { newOffset -> offset = newOffset },
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

            uiState.selectedNodeId?.let { nodeId ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = uiState.detailTitle ?: "Selected",
                            style = MaterialTheme.typography.titleMedium
                        )
                        uiState.detailSummary?.let { summary ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = summary.take(150),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            TextButton(onClick = { onNavigateToSceneDetail(nodeId) }) {
                                Text("View Detail")
                            }
                            TextButton(onClick = { viewModel.clearSelection() }) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GraphCanvas(
    sceneNodes: List<SceneNodeData>,
    characterNodes: List<CharacterNodeData>,
    edges: List<GraphEdge>,
    scale: Float,
    offset: Offset,
    selectedNodeId: String?,
    onNodeTap: (String) -> Unit,
    onNodeDrag: (String, Float, Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val nodeRadius = 30f
                    for (node in sceneNodes) {
                        val nodeX = node.x * scale + offset.x
                        val nodeY = node.y * scale + offset.y
                        val distance = kotlin.math.sqrt(
                            (offset.x - nodeX) * (offset.x - nodeX) +
                            (offset.y - nodeY) * (offset.y - nodeY)
                        )
                        if (distance < nodeRadius) {
                            onNodeTap(node.id)
                            return@detectTapGestures
                        }
                    }
                    for (node in characterNodes) {
                        val nodeX = node.x * scale + offset.x
                        val nodeY = node.y * scale + offset.y
                        val distance = kotlin.math.sqrt(
                            (offset.x - nodeX) * (offset.x - nodeX) +
                            (offset.y - nodeY) * (offset.y - nodeY)
                        )
                        if (distance < nodeRadius) {
                            onNodeTap(node.id)
                            return@detectTapGestures
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onOffsetChange(Offset(
                        offset.x + dragAmount.x,
                        offset.y + dragAmount.y
                    ))
                }
            }
    ) {
        val sceneRadius = 30f
        val charSize = 25f

        for (edge in edges) {
            val fromNode = sceneNodes.find { it.id == edge.fromId }
            val toNode = sceneNodes.find { it.id == edge.toId }
            if (fromNode != null && toNode != null) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(fromNode.x * scale + offset.x, fromNode.y * scale + offset.y),
                    end = Offset(toNode.x * scale + offset.x, toNode.y * scale + offset.y),
                    strokeWidth = 2f * scale
                )
            }
        }

        for (node in sceneNodes) {
            val isSelected = node.id == selectedNodeId
            val color = if (isSelected) Color(0xFF6650a4) else Color(0xFF2196F3)
            val centerX = node.x * scale + offset.x
            val centerY = node.y * scale + offset.y

            drawCircle(
                color = color,
                radius = sceneRadius * scale,
                center = Offset(centerX, centerY)
            )

            if (isSelected) {
                drawCircle(
                    color = Color.White,
                    radius = sceneRadius * scale,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 3f * scale)
                )
            }

            val textLayoutResult = textMeasurer.measure(
                text = node.title.take(15),
                style = TextStyle(fontSize = (10 * scale).sp)
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    centerX - textLayoutResult.size.width / 2,
                    centerY + sceneRadius * scale + 8
                )
            )
        }

        for (node in characterNodes) {
            val isSelected = node.id == selectedNodeId
            val color = Color(node.color)
            val centerX = node.x * scale + offset.x
            val centerY = node.y * scale + offset.y

            drawRect(
                color = color,
                topLeft = Offset(centerX - charSize * scale, centerY - charSize * scale),
                size = androidx.compose.ui.geometry.Size(charSize * 2 * scale, charSize * 2 * scale)
            )

            if (isSelected) {
                drawRect(
                    color = Color.White,
                    topLeft = Offset(centerX - charSize * scale, centerY - charSize * scale),
                    size = androidx.compose.ui.geometry.Size(charSize * 2 * scale, charSize * 2 * scale),
                    style = Stroke(width = 3f * scale)
                )
            }

            val textLayoutResult = textMeasurer.measure(
                text = node.name.take(15),
                style = TextStyle(fontSize = (10 * scale).sp)
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    centerX - textLayoutResult.size.width / 2,
                    centerY + charSize * scale + 8
                )
            )
        }
    }
}