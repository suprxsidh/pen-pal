package com.penpal.presentation.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penpal.domain.model.Story
import com.penpal.domain.repository.*
import com.penpal.presentation.ui.screens.HomeUiState
import com.penpal.presentation.ui.screens.StoryUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val sceneRepository: SceneRepository,
    private val recordingRepository: RecordingRepository,
    private val characterRepository: CharacterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadStories()
    }

    private fun loadStories() {
        viewModelScope.launch {
            storyRepository.getAllStories()
                .collect { stories ->
                    val storyUiModels = stories.map { story ->
                        val sceneCount = sceneRepository.getSceneCount(story.id)
                        val recordingCount = recordingRepository.getRecordingCount(story.id)
                        val characterCount = characterRepository.getCharacterCount(story.id)
                        StoryUiModel(
                            id = story.id,
                            title = story.title,
                            description = story.description,
                            sceneCount = sceneCount,
                            recordingCount = recordingCount,
                            characterCount = characterCount,
                            createdAt = story.createdAt,
                            updatedAt = story.updatedAt
                        )
                    }
                    _uiState.update { it.copy(stories = storyUiModels, isLoading = false) }
                }
        }
    }

    fun createStory() {
        viewModelScope.launch {
            val story = Story(
                id = UUID.randomUUID().toString(),
                title = "New Story",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            storyRepository.createStory(story)
        }
    }

    fun deleteStory(storyId: String) {
        viewModelScope.launch {
            storyRepository.deleteStory(storyId)
        }
    }
}