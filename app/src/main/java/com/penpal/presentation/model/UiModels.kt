package com.penpal.presentation.ui.screens

data class StoryUiModel(
    val id: String,
    val title: String,
    val description: String?,
    val sceneCount: Int,
    val recordingCount: Int,
    val characterCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)

data class HomeUiState(
    val stories: List<StoryUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class RecordingUiState(
    val recording: RecordingUiModel? = null,
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false,
    val amplitude: Float = 0f,
    val error: String? = null
)

data class RecordingUiModel(
    val id: String,
    val storyId: String,
    val filePath: String,
    val durationMs: Long,
    val transcribedText: String?,
    val summary: String?,
    val scenes: List<SceneUiModel> = emptyList(),
    val createdAt: Long
)

data class SceneUiModel(
    val id: String,
    val storyId: String,
    val recordingId: String,
    val title: String,
    val content: String,
    val summary: String?,
    val location: String?,
    val mood: String?,
    val orderIndex: Int,
    val characterIds: List<String> = emptyList(),
    val createdAt: Long
)

data class CharacterUiModel(
    val id: String,
    val storyId: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val description: String?,
    val color: Int,
    val sceneIds: List<String> = emptyList()
)

data class GraphUiState(
    val storyId: String = "",
    val storyTitle: String = "",
    val sceneNodes: List<GraphNode> = emptyList(),
    val characterNodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList(),
    val isLoading: Boolean = false,
    val selectedNode: String? = null,
    val error: String? = null
)

data class GraphNode(
    val id: String,
    val type: String,
    val label: String,
    val x: Float,
    val y: Float,
    val color: Int = 0xFF6650a4.toInt(),
    val relatedIds: List<String> = emptyList()
)

data class GraphEdge(
    val fromId: String,
    val toId: String,
    val type: String
)