package com.penpal.domain.model

data class Story(
    val id: String,
    val title: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Recording(
    val id: String,
    val storyId: String,
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long = System.currentTimeMillis()
)

data class Scene(
    val id: String,
    val storyId: String,
    val recordingId: String,
    val title: String,
    val content: String,
    val summary: String? = null,
    val location: String? = null,
    val mood: String? = null,
    val orderIndex: Int,
    val timestampStartMs: Long = 0,
    val timestampEndMs: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class Character(
    val id: String,
    val storyId: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val description: String? = null,
    val color: Int = 0xFF6650a4.toInt(),
    val createdAt: Long = System.currentTimeMillis()
)

data class SceneCharacter(
    val sceneId: String,
    val characterId: String
)

data class GraphNode(
    val id: String,
    val type: String,
    val x: Float,
    val y: Float,
    val updatedAt: Long = System.currentTimeMillis()
)