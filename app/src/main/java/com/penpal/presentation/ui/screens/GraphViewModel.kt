package com.penpal.presentation.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penpal.domain.model.Character
import com.penpal.domain.model.Scene
import com.penpal.domain.repository.CharacterRepository
import com.penpal.domain.repository.SceneCharacterRepository
import com.penpal.domain.repository.SceneRepository
import com.penpal.domain.repository.StoryRepository
import com.penpal.presentation.ui.screens.GraphEdge
import com.penpal.presentation.ui.screens.GraphNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GraphViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val storyRepository: StoryRepository,
    private val sceneRepository: SceneRepository,
    private val characterRepository: CharacterRepository,
    private val sceneCharacterRepository: SceneCharacterRepository
) : ViewModel() {

    private val storyId: String = savedStateHandle.get<String>("storyId") ?: ""

    private val _uiState = MutableStateFlow(GraphScreenUiState())
    val uiState: StateFlow<GraphScreenUiState> = _uiState.asStateFlow()

    private val _sceneNodes = MutableStateFlow<List<SceneNodeData>>(emptyList())
    private val _characterNodes = MutableStateFlow<List<CharacterNodeData>>(emptyList())
    private val _edges = MutableStateFlow<List<GraphEdge>>(emptyList())

    val sceneNodes: StateFlow<List<SceneNodeData>> = _sceneNodes.asStateFlow()
    val characterNodes: StateFlow<List<CharacterNodeData>> = _characterNodes.asStateFlow()
    val edges: StateFlow<List<GraphEdge>> = _edges.asStateFlow()

    init {
        loadGraphData()
    }

    private fun loadGraphData() {
        viewModelScope.launch {
            val story = storyRepository.getStoryById(storyId)
            _uiState.update { it.copy(storyTitle = story?.title ?: "Graph") }

            sceneRepository.getScenesByStoryId(storyId).collect { scenes ->
                val nodes = scenes.mapIndexed { index, scene ->
                    SceneNodeData(
                        id = scene.id,
                        title = scene.title,
                        summary = scene.summary,
                        location = scene.location,
                        mood = scene.mood,
                        x = 100f + (index * 200f),
                        y = 200f,
                        characterIds = emptyList()
                    )
                }
                _sceneNodes.value = nodes

                val sceneEdges = mutableListOf<GraphEdge>()
                for (i in 0 until nodes.size - 1) {
                    sceneEdges.add(GraphEdge(nodes[i].id, nodes[i + 1].id, "sequence"))
                }
                _edges.value = sceneEdges
            }
        }

        viewModelScope.launch {
            characterRepository.getCharactersByStoryId(storyId).collect { characters ->
                val charNodes = characters.mapIndexed { index, char ->
                    CharacterNodeData(
                        id = char.id,
                        name = char.name,
                        description = char.description,
                        color = char.color,
                        x = 100f + (index * 150f),
                        y = 500f
                    )
                }
                _characterNodes.value = charNodes
            }
        }
    }

    fun updateNodePosition(nodeId: String, x: Float, y: Float) {
        _sceneNodes.update { nodes ->
            nodes.map { if (it.id == nodeId) it.copy(x = x, y = y) else it }
        }
    }

    fun selectNode(nodeId: String) {
        _uiState.update { it.copy(selectedNodeId = nodeId) }

        val sceneNode = _sceneNodes.value.find { it.id == nodeId }
        if (sceneNode != null) {
            _uiState.update { it.copy(detailTitle = sceneNode.title, detailSummary = sceneNode.summary) }
        } else {
            val charNode = _characterNodes.value.find { it.id == nodeId }
            if (charNode != null) {
                _uiState.update { it.copy(detailTitle = charNode.name, detailSummary = charNode.description) }
            }
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedNodeId = null, detailTitle = null, detailSummary = null) }
    }

    fun deleteScene(sceneId: String) {
        viewModelScope.launch {
            sceneRepository.deleteScene(sceneId)
            sceneCharacterRepository.removeSceneCharactersBySceneId(sceneId)
        }
    }
}

data class GraphScreenUiState(
    val storyTitle: String = "Graph",
    val selectedNodeId: String? = null,
    val detailTitle: String? = null,
    val detailSummary: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class SceneNodeData(
    val id: String,
    val title: String,
    val summary: String?,
    val location: String?,
    val mood: String?,
    val x: Float,
    val y: Float,
    val characterIds: List<String>
)

data class CharacterNodeData(
    val id: String,
    val name: String,
    val description: String?,
    val color: Int,
    val x: Float,
    val y: Float
)