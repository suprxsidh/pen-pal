package com.penpal.transcription

import com.penpal.domain.model.Character
import com.penpal.domain.model.Scene
import com.penpal.domain.repository.CharacterRepository
import com.penpal.domain.repository.SceneRepository
import com.penpal.domain.repository.SceneCharacterRepository
import com.penpal.domain.repository.StoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionPipeline @Inject constructor(
    private val transcriptionService: TranscriptionService,
    private val storyRepository: StoryRepository,
    private val sceneRepository: SceneRepository,
    private val characterRepository: CharacterRepository,
    private val sceneCharacterRepository: SceneCharacterRepository
) {
    private val _pipelineState = MutableStateFlow<PipelineState>(PipelineState.Idle)
    val pipelineState: StateFlow<PipelineState> = _pipelineState.asStateFlow()

    private val _currentText = MutableStateFlow("")
    val currentText: StateFlow<String> = _currentText.asStateFlow()

    private val _extractedScenes = MutableStateFlow<List<ExtractedScene>>(emptyList())
    val extractedScenes: StateFlow<List<ExtractedScene>> = _extractedScenes.asStateFlow()

    private val _extractedCharacters = MutableStateFlow<List<ExtractedCharacter>>(emptyList())
    val extractedCharacters: StateFlow<List<ExtractedCharacter>> = _extractedCharacters.asStateFlow()

    fun startDirectTranscription() {
        _pipelineState.value = PipelineState.Transcribing
    }

    fun startFromFile(recordingId: String, filePath: String) {
        _pipelineState.value = PipelineState.ProcessingFile(recordingId)
    }

    suspend fun processTranscribedText(
        storyId: String,
        recordingId: String,
        rawText: String
    ): Result<List<Scene>> {
        return try {
            _pipelineState.value = PipelineState.ExtractingEntities

            val summary = generateSummary(rawText)
            val title = generateTitle(rawText)
            val extractedScenes = extractScenes(rawText, recordingId, storyId)
            val extractedChars = extractCharacters(rawText, storyId)

            _extractedScenes.value = extractedScenes
            _extractedCharacters.value = extractedChars

            _pipelineState.value = PipelineState.Saving

            val savedScenes = mutableListOf<Scene>()
            for (sceneData in extractedScenes) {
                val scene = Scene(
                    id = UUID.randomUUID().toString(),
                    storyId = storyId,
                    recordingId = recordingId,
                    title = sceneData.title,
                    content = sceneData.content,
                    summary = sceneData.summary,
                    location = sceneData.location,
                    mood = sceneData.mood,
                    orderIndex = sceneData.orderIndex,
                    createdAt = System.currentTimeMillis()
                )
                sceneRepository.saveScene(scene)
                savedScenes.add(scene)

                for (charId in sceneData.characterIds) {
                    sceneCharacterRepository.addSceneCharacter(
                        com.penpal.domain.model.SceneCharacter(scene.id, charId)
                    )
                }
            }

            for (charData in extractedChars) {
                if (characterRepository.findCharacterByName(storyId, charData.name) == null) {
                    characterRepository.saveCharacter(
                        Character(
                            id = UUID.randomUUID().toString(),
                            storyId = storyId,
                            name = charData.name,
                            aliases = charData.aliases,
                            description = null,
                            color = charData.color,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            _pipelineState.value = PipelineState.Completed(savedScenes.size)
            Result.success(savedScenes)
        } catch (e: Exception) {
            _pipelineState.value = PipelineState.Error(e.message ?: "Pipeline failed")
            Result.failure(e)
        }
    }

    private fun generateSummary(text: String): String {
        if (text.length < 100) return text

        val sentences = text.split(Regex("[.!?]+")).filter { it.trim().length > 20 }
        return when {
            sentences.size <= 2 -> text.take(200)
            else -> sentences.take(3).joinToString(". ").take(200)
        }
    }

    private fun generateTitle(text: String): String {
        val firstSentence = text.split(Regex("[.!?]")).firstOrNull() ?: "Untitled"
        val words = firstSentence.split(" ").take(6)
        return words.joinToString(" ").take(50).replaceFirstChar { it.uppercase() }
    }

    private fun extractScenes(
        text: String,
        recordingId: String,
        storyId: String
    ): List<ExtractedScene> {
        val paragraphs = text.split(Regex("\n\n+|\r\n\r\n+")).filter { it.trim().length > 30 }

        if (paragraphs.isEmpty()) {
            return listOf(
                ExtractedScene(
                    title = generateTitle(text),
                    content = text.take(1000),
                    summary = generateSummary(text),
                    location = null,
                    mood = detectMood(text),
                    orderIndex = 0,
                    characterIds = emptyList()
                )
            )
        }

        return paragraphs.mapIndexed { index, paragraph ->
            ExtractedScene(
                title = generateTitle(paragraph),
                content = paragraph,
                summary = generateSummary(paragraph),
                location = extractLocation(paragraph),
                mood = detectMood(paragraph),
                orderIndex = index,
                characterIds = extractCharacterIds(paragraph, storyId)
            )
        }
    }

    private fun extractCharacters(text: String, storyId: String): List<ExtractedCharacter> {
        val namePatterns = listOf(
            Regex("\\b([A-Z][a-z]+)\\s+([A-Z][a-z]+)\\b"),
            Regex("\\b([A-Z][a-z]+)\\b"),
        )

        val foundNames = mutableSetOf<String>()
        for (pattern in namePatterns) {
            val matches = pattern.findAll(text)
            for (match in matches) {
                val name = match.value.trim()
                if (name.length >= 2 && name.lowercase() !in commonWords) {
                    foundNames.add(name)
                }
            }
        }

        val colors = listOf(0xFF6650a4.toInt(), 0xFF2196F3.toInt(), 0xFF4CAF50.toInt(), 0xFFFF9800.toInt(), 0xFFE91E63.toInt())
        return foundNames.take(10).mapIndexed { index, name ->
            ExtractedCharacter(
                name = name,
                aliases = listOf(name),
                description = null,
                color = colors[index % colors.size]
            )
        }
    }

    private fun extractCharacterIds(text: String, storyId: String): List<String> {
        val namePatterns = listOf(
            Regex("\\b([A-Z][a-z]+)\\s+([A-Z][a-z]+)\\b"),
            Regex("\\b([A-Z][a-z]+)\\b"),
        )

        val foundNames = mutableListOf<String>()
        for (pattern in namePatterns) {
            val matches = pattern.findAll(text)
            for (match in matches) {
                val name = match.value.trim()
                if (name.length >= 2 && name.lowercase() !in commonWords) {
                    foundNames.add(name)
                }
            }
        }
        return foundNames.take(5).distinct()
    }

    private fun extractLocation(text: String): String? {
        val locationPatterns = listOf(
            Regex("\\b(in|at|to)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?)\\b"),
            Regex("\\b(near|by|inside|outside)\\s+([A-Z][a-z]+)\\b"),
        )

        for (pattern in locationPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues.lastOrNull()
            }
        }
        return null
    }

    private fun detectMood(text: String): String? {
        val moodIndicators = mapOf(
            "tense" to listOf("fear", "worry", "nervous", "anxious", "danger"),
            "happy" to listOf("joy", "laugh", "happy", "smile", "excited"),
            "sad" to listOf("cry", "tear", "sad", "grief", "sorrow"),
            "romantic" to listOf("love", "kiss", "heart", "romance", "affection"),
            "action" to listOf("run", "fight", "attack", "chase", "escape"),
            "mysterious" to listOf("strange", "mystery", "secret", "hidden", "shadow")
        )

        val lowerText = text.lowercase()
        for ((mood, indicators) in moodIndicators) {
            if (indicators.any { it in lowerText }) {
                return mood
            }
        }
        return null
    }

    fun reset() {
        _pipelineState.value = PipelineState.Idle
        _currentText.value = ""
        _extractedScenes.value = emptyList()
        _extractedCharacters.value = emptyList()
    }

    companion object {
        private val commonWords = setOf(
            "the", "and", "for", "are", "but", "not", "you", "all", "can", "had", "her", "was", "one", "our", "out", "day", "get", "has", "him", "his", "how", "its", "may", "new", "now", "old", "see", "two", "way", "who", "boy", "did", "own", "say", "she", "too", "use", "mom", "dad"
        )
    }
}

data class ExtractedScene(
    val title: String,
    val content: String,
    val summary: String?,
    val location: String?,
    val mood: String?,
    val orderIndex: Int,
    val characterIds: List<String>
)

data class ExtractedCharacter(
    val name: String,
    val aliases: List<String>,
    val description: String?,
    val color: Int
)

sealed class PipelineState {
    data object Idle : PipelineState()
    data object Transcribing : PipelineState()
    data class ProcessingFile(val recordingId: String) : PipelineState()
    data object ExtractingEntities : PipelineState()
    data object Saving : PipelineState()
    data class Completed(val sceneCount: Int) : PipelineState()
    data class Error(val message: String) : PipelineState()
}