package com.penpal.transcription

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTranscribing = false

    private val _transcriptionState = MutableStateFlow<TranscriptionState>(TranscriptionState.Idle)
    val transcriptionState: StateFlow<TranscriptionState> = _transcriptionState.asStateFlow()

    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText.asStateFlow()

    private val _interimText = MutableStateFlow("")
    val interimText: StateFlow<String> = _interimText.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    suspend fun transcribeAudio(audioFilePath: String): Result<String> = withContext(Dispatchers.Main) {
        try {
            if (!isAvailable()) {
                return@withContext Result.failure(Exception("Speech recognition not available"))
            }

            _transcriptionState.value = TranscriptionState.Processing
            _transcribedText.value = ""
            _interimText.value = ""

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createListener())

            val intent = createRecognitionIntent().apply {
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, audioFilePath)
            }

            speechRecognizer?.startListening(intent)

            isTranscribing = true

            Result.success("")
        } catch (e: Exception) {
            _transcriptionState.value = TranscriptionState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    suspend fun transcribeDirect(): Result<String> = withContext(Dispatchers.Main) {
        try {
            if (!isAvailable()) {
                return@withContext Result.failure(Exception("Speech recognition not available"))
            }

            _transcriptionState.value = TranscriptionState.Processing
            _transcribedText.value = ""
            _interimText.value = ""

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createListener())

            val intent = createRecognitionIntent()
            speechRecognizer?.startListening(intent)

            isTranscribing = true

            Result.success("")
        } catch (e: Exception) {
            _transcriptionState.value = TranscriptionState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    fun stopTranscription() {
        isTranscribing = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null

        if (_transcriptionState.value is TranscriptionState.Processing) {
            _transcriptionState.value = TranscriptionState.Completed(_transcribedText.value)
        }
    }

    private fun createRecognitionIntent() = android.content.Intent().apply {
        action = RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: android.os.Bundle?) {
            _transcriptionState.value = TranscriptionState.Listening
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _transcriptionState.value = TranscriptionState.Completed(_transcribedText.value)
            isTranscribing = false
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Unknown error"
            }

            if (error == SpeechRecognizer.ERROR_NO_MATCH && _transcribedText.value.isNotEmpty()) {
                _transcriptionState.value = TranscriptionState.Completed(_transcribedText.value)
            } else {
                _transcriptionState.value = TranscriptionState.Error(errorMessage)
            }
            isTranscribing = false
        }

        override fun onResults(results: android.os.Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _transcribedText.value = matches[0]
                _transcriptionState.value = TranscriptionState.Completed(_transcribedText.value)
            }
            isTranscribing = false
        }

        override fun onPartialResults(partialResults: android.os.Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _interimText.value = matches[0]
            }
        }

        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
    }

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isTranscribing = false
    }
}

sealed class TranscriptionState {
    data object Idle : TranscriptionState()
    data object Listening : TranscriptionState()
    data object Processing : TranscriptionState()
    data class Completed(val text: String) : TranscriptionState()
    data class Error(val message: String) : TranscriptionState()
}