package com.penpal.presentation.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penpal.audio.AudioPlayer
import com.penpal.audio.AudioRecorder
import com.penpal.domain.model.Recording
import com.penpal.domain.model.Story
import com.penpal.domain.repository.RecordingRepository
import com.penpal.domain.repository.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val storyRepository: StoryRepository,
    private val recordingRepository: RecordingRepository,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val storyId: String = savedStateHandle.get<String>("storyId") ?: ""

    private val _uiState = MutableStateFlow(RecordingScreenUiState())
    val uiState: StateFlow<RecordingScreenUiState> = _uiState.asStateFlow()

    val amplitude = audioRecorder.amplitude
    val isRecording = audioRecorder.isRecordingFlow
    val duration = audioRecorder.durationMs
    val isPlaying = audioPlayer.isPlaying
    val playbackPosition = audioPlayer.currentPosition
    val playbackDuration = audioPlayer.duration

    private var positionUpdateJob: Job? = null

    init {
        loadStory()
        loadRecordings()
    }

    private fun loadStory() {
        viewModelScope.launch {
            val story = storyRepository.getStoryById(storyId)
            _uiState.update { it.copy(storyTitle = story?.title ?: "Recording") }
        }
    }

    private fun loadRecordings() {
        viewModelScope.launch {
            recordingRepository.getRecordingsByStoryId(storyId).collect { recordings ->
                val recordingModels = recordings.map { recording ->
                    RecordingListItem(
                        id = recording.id,
                        filePath = recording.filePath,
                        durationMs = recording.durationMs,
                        createdAt = recording.createdAt
                    )
                }
                _uiState.update { it.copy(recordings = recordingModels) }
            }
        }
    }

    fun startRecording() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRecording = true, error = null) }
            val result = audioRecorder.startRecording(storyId)
            result.onFailure { e ->
                _uiState.update { it.copy(isRecording = false, error = e.message) }
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            val filePath = audioRecorder.stopRecording()
            if (filePath != null) {
                val recording = Recording(
                    id = UUID.randomUUID().toString(),
                    storyId = storyId,
                    filePath = filePath,
                    durationMs = duration.value,
                    createdAt = System.currentTimeMillis()
                )
                recordingRepository.saveRecording(recording)
                _uiState.update { it.copy(isRecording = false) }
            }
        }
    }

    fun cancelRecording() {
        audioRecorder.cancelRecording()
        _uiState.update { it.copy(isRecording = false) }
    }

    fun playRecording(filePath: String) {
        viewModelScope.launch {
            if (isPlaying.value && _uiState.value.currentPlayingFile == filePath) {
                audioPlayer.pause()
            } else {
                val result = audioPlayer.play(filePath)
                result.onSuccess {
                    _uiState.update { it.copy(currentPlayingFile = filePath) }
                    startPositionUpdates()
                }
                result.onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }
    }

    fun stopPlayback() {
        audioPlayer.stop()
        positionUpdateJob?.cancel()
        _uiState.update { it.copy(currentPlayingFile = null) }
    }

    fun deleteRecording(recordingId: String) {
        viewModelScope.launch {
            recordingRepository.deleteRecording(recordingId)
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (true) {
                audioPlayer.updatePosition()
                kotlinx.coroutines.delay(100)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        positionUpdateJob?.cancel()
        audioPlayer.release()
    }
}

data class RecordingScreenUiState(
    val storyTitle: String = "Recording",
    val recordings: List<RecordingListItem> = emptyList(),
    val isRecording: Boolean = false,
    val currentPlayingFile: String? = null,
    val error: String? = null
)

data class RecordingListItem(
    val id: String,
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long
)