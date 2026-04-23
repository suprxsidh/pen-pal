package com.penpal.presentation.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penpal.domain.model.Recording
import com.penpal.domain.model.Scene
import com.penpal.domain.repository.RecordingRepository
import com.penpal.domain.repository.SceneRepository
import com.penpal.transcription.PipelineState
import com.penpal.transcription.TranscriptionPipeline
import com.penpal.transcription.TranscriptionService
import com.penpal.transcription.TranscriptionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TranscribeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transcriptionService: TranscriptionService,
    private val transcriptionPipeline: TranscriptionPipeline,
    private val recordingRepository: RecordingRepository,
    private val sceneRepository: SceneRepository
) : ViewModel() {

    private val recordingId: String = savedStateHandle.get<String>("recordingId") ?: ""

    private val _uiState = MutableStateFlow(TranscribeUiState())
    val uiState: StateFlow<TranscribeUiState> = _uiState.asStateFlow()

    val transcriptionState = transcriptionService.transcriptionState
    val interimText = transcriptionService.interimText
    val pipelineState = transcriptionPipeline.pipelineState

    init {
        loadRecording()
        observePipeline()
    }

    private fun loadRecording() {
        viewModelScope.launch {
            val recording = recordingRepository.getRecordingById(recordingId)
            _uiState.update {
                it.copy(
                    recording = recording,
                    storyId = recording?.storyId ?: ""
                )
            }
        }
    }

    private fun observePipeline() {
        viewModelScope.launch {
            transcriptionPipeline.pipelineState.collect { state ->
                when (state) {
                    is PipelineState.Completed -> {
                        _uiState.update { it.copy(isProcessing = false, isComplete = true) }
                    }
                    is PipelineState.Error -> {
                        _uiState.update { it.copy(isProcessing = false, error = state.message) }
                    }
                    else -> {}
                }
            }
        }
    }

    fun startTranscription() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            transcriptionPipeline.startDirectTranscription()

            val result = transcriptionService.transcribeDirect()
            result.onFailure { e ->
                _uiState.update { it.copy(isProcessing = false, error = e.message) }
            }
        }
    }

    fun stopTranscription() {
        transcriptionService.stopTranscription()
    }

    fun processTranscribedText(text: String) {
        viewModelScope.launch {
            val storyId = _uiState.value.storyId
            if (storyId.isEmpty()) return@launch

            val result = transcriptionPipeline.processTranscribedText(
                storyId = storyId,
                recordingId = recordingId,
                rawText = text
            )

            result.onSuccess { scenes ->
                _uiState.update { it.copy(generatedScenes = scenes.size) }
            }
            result.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun transcribeFile(filePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }

            val result = transcriptionService.transcribeAudio(filePath)
            result.onFailure { e ->
                _uiState.update { it.copy(isProcessing = false, error = e.message) }
            }
        }
    }

    fun saveTranscription() {
        viewModelScope.launch {
            val text = transcriptionService.transcribedText.value
            if (text.isNotEmpty()) {
                processTranscribedText(text)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        transcriptionService.release()
    }
}

data class TranscribeUiState(
    val recording: Recording? = null,
    val storyId: String = "",
    val isProcessing: Boolean = false,
    val isComplete: Boolean = false,
    val generatedScenes: Int = 0,
    val error: String? = null
)