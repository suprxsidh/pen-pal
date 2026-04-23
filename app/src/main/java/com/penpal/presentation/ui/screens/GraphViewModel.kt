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
    private val _characterEdges = MutableStateFlow<List<GraphEdge>>(emptyList())

    val sceneNodes: StateFlow<List<SceneNodeData>> = _sceneNodes.asStateFlow()
    val characterNodes: StateFlow<List<CharacterNodeData>> = _characterNodes.asStateFlow()
    val edges: StateFlow<List<GraphEdge>> = _edges.asStateFlow()
    val characterEdges: StateFlow<List<GraphEdge>> = _characterEdges.asStateFlow()

    init {
        loadGraphData()
    }

    private fun loadGraphData() {
        viewModelScope.launch {
            val story = storyRepository.getStoryById(storyId)
            _uiState.update { it.copy(storyTitle = story?.title ?: "Graph") }
        }

        viewModelScope.launch {
            sceneRepository.getScenesByStoryId(storyId).collect { scenes ->
                if (scenes.isEmpty()) {
                    _sceneNodes.value = emptyList()
                    _edges.value = emptyList()
                    return@collect
                }

                val nodes = scenes.sortedBy { it.orderIndex }.mapIndexed { index, scene ->
                    SceneNodeData(
                        id = scene.id,
                        title = scene.title,
                        summary = scene.summary,
                        content = scene.content,
                        location = scene.location,
                        mood = scene.mood,
                        orderIndex = scene.orderIndex,
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
                        aliases = char.aliases,
                        description = char.description,
                        color = char.color,
                        x = 100f + (index * 150f),
                        y = 500f,
                        sceneIds = emptyList()
                    )
                }
                _characterNodes.value = charNodes
            }
        }

        viewModelScope.launch {
            loadCharacterConnections()
        }
    }

    private suspend fun loadCharacterConnections() {
        val charEdges = mutableListOf<GraphEdge>()

        for (charNode in _characterNodes.value) {
            val sceneIds = sceneCharacterRepository.getSceneIdsForCharacter(charNode.id)
            val sortedScenes = _sceneNodes.value.filter { it.id in sceneIds }.sortedBy { it.orderIndex }

            for (scene in sortedScenes) {
                charEdges.add(GraphEdge(charNode.id, scene.id, "appears_in"))
            }
        }

        _characterEdges.value = charEdges
    }

    fun showCharacterJourney(characterId: String) {
        _uiState.update { it.copy(viewMode = GraphViewMode.CHARACTER_JOURNEY) }

        viewModelScope.launch {
            val sceneIds = sceneCharacterRepository.getSceneIdsForCharacter(characterId)
            val character = characterRepository.getCharacterById(characterId)

            val journeyScenes = _sceneNodes.value
                .filter { it.id in sceneIds }
                .sortedBy { it.orderIndex }

            _uiState.update {
                it.copy(
                    selectedCharacterId = characterId,
                    characterName = character?.name,
                    journeyScenes = journeyScenes.map { s -> s.id }
                )
            }
        }
    }

    fun showSceneDetails(sceneId: String) {
        _uiState.update { it.copy(viewMode = GraphViewMode.SCENE_DETAIL) }

        val scene = _sceneNodes.value.find { it.id == sceneId }
        if (scene != null) {
            viewModelScope.launch {
                val characterIds = sceneCharacterRepository.getCharacterIdsForScene(sceneId)
                val characters = _characterNodes.value.filter { it.id in characterIds }

                _uiState.update {
                    it.copy(
                        selectedSceneId = sceneId,
                        sceneTitle = scene.title,
                        sceneSummary = scene.summary,
                        sceneContent = scene.content,
                        sceneLocation = scene.location,
                        sceneMood = scene.mood,
                        sceneCharacters = characters.map { c -> c.name }
                    )
                }
            }
        }
    }

    fun resetView() {
        _uiState.update {
            it.copy(
                viewMode = GraphViewMode.TIMELINE,
                selectedCharacterId = null,
                selectedSceneId = null,
                characterName = null,
                journeyScenes = emptyList(),
                sceneTitle = null,
                sceneSummary = null,
                sceneContent = null,
                sceneCharacters = emptyList()
            )
        }
    }

    fun updateNodePosition(nodeId: String, x: Float, y: Float) {
        _sceneNodes.update { nodes ->
            nodes.map { if (it.id == nodeId) it.copy(x = x, y = y) else it }
        }
        _characterNodes.update { nodes ->
            nodes.map { if (it.id == nodeId) it.copy(x = x, y = y) else it }
        }
    }

    fun selectNode(nodeId: String) {
        val sceneNode = _sceneNodes.value.find { it.id == nodeId }
        if (sceneNode != null) {
            selectNodeType = NodeType.SCENE
            _uiState.update { it.copy(selectedNodeId = nodeId, detailTitle = sceneNode.title, detailSummary = sceneNode.summary) }
            return
        }

        val charNode = _characterNodes.value.find { it.id == nodeId }
        if (charNode != null) {
            selectNodeType = NodeType.CHARACTER
            _uiState.update { it.copy(selectedNodeId = nodeId, detailTitle = charNode.name, detailSummary = charNode.description) }
        }
    }

    private var selectNodeType: NodeType = NodeType.SCENE

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedNodeId = null,
                detailTitle = null,
                detailSummary = null,
                viewMode = GraphViewMode.TIMELINE
            )
        }
        selectNodeType = NodeType.SCENE
    }

    fun deleteScene(sceneId: String) {
        viewModelScope.launch {
            sceneRepository.deleteScene(sceneId)
            sceneCharacterRepository.removeSceneCharactersBySceneId(sceneId)
            loadGraphData()
        }
    }

    fun mergeCharacters(primaryCharId: String, secondaryCharId: String) {
        viewModelScope.launch {
            val secondarySceneIds = sceneCharacterRepository.getSceneIdsForCharacter(secondaryCharId)
            for (sceneId in secondarySceneIds) {
                sceneCharacterRepository.removeSceneCharacter(sceneId, secondaryCharId)
                sceneCharacterRepository.addSceneCharacter(
                    com.penpal.domain.model.SceneCharacter(sceneId, primaryCharId)
                )
            }
            characterRepository.deleteCharacter(secondaryCharId)
            loadGraphData()
            _uiState.update { it.copy(error = "Characters merged successfully") }
        }
    }

    fun renameCharacter(characterId: String, newName: String) {
        viewModelScope.launch {
            val character = characterRepository.getCharacterById(characterId)
            if (character != null) {
                val updatedChar = character.copy(name = newName)
                characterRepository.updateCharacter(updatedChar)
                loadGraphData()
            }
        }
    }
}

enum class GraphViewMode {
    TIMELINE,
    SCENE_DETAIL,
    CHARACTER_JOURNEY
}

enum class NodeType {
    SCENE,
    CHARACTER
}

data class GraphScreenUiState(
    val storyTitle: String = "Graph",
    val viewMode: GraphViewMode = GraphViewMode.TIMELINE,
    val selectedNodeId: String? = null,
    val detailTitle: String? = null,
    val detailSummary: String? = null,
    val selectedCharacterId: String? = null,
    val characterName: String? = null,
    val journeyScenes: List<String> = emptyList(),
    val selectedSceneId: String? = null,
    val sceneTitle: String? = null,
    val sceneSummary: String? = null,
    val sceneContent: String? = null,
    val sceneLocation: String? = null,
    val sceneMood: String? = null,
    val sceneCharacters: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class SceneNodeData(
    val id: String,
    val title: String,
    val summary: String?,
    val content: String?,
    val location: String?,
    val mood: String?,
    val orderIndex: Int,
    val x: Float,
    val y: Float,
    val characterIds: List<String>
)

data class CharacterNodeData(
    val id: String,
    val name: String,
    val aliases: List<String>,
    val description: String?,
    val color: Int,
    val x: Float,
    val y: Float,
    val sceneIds: List<String>
)