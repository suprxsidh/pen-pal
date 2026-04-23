package com.penpal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey val id: String,
    val storyId: String,
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long
)

@Entity(tableName = "scenes")
data class SceneEntity(
    @PrimaryKey val id: String,
    val storyId: String,
    val recordingId: String,
    val title: String,
    val content: String,
    val summary: String?,
    val location: String?,
    val mood: String?,
    val orderIndex: Int,
    val timestampStartMs: Long,
    val timestampEndMs: Long,
    val createdAt: Long
)

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: String,
    val storyId: String,
    val name: String,
    val aliases: String,
    val description: String?,
    val color: Int,
    val createdAt: Long
)

@Entity(tableName = "scene_characters")
data class SceneCharacterEntity(
    @PrimaryKey val id: String,
    val sceneId: String,
    val characterId: String
)

@Entity(tableName = "graph_nodes")
data class GraphNodeEntity(
    @PrimaryKey val id: String,
    val type: String,
    val x: Float,
    val y: Float,
    val updatedAt: Long
)