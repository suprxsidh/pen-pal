package com.penpal.domain.repository

import com.penpal.domain.model.*
import kotlinx.coroutines.flow.Flow

interface StoryRepository {
    fun getAllStories(): Flow<List<Story>>
    suspend fun getStoryById(id: String): Story?
    suspend fun createStory(story: Story)
    suspend fun updateStory(story: Story)
    suspend fun deleteStory(id: String)
}

interface RecordingRepository {
    fun getRecordingsByStoryId(storyId: String): Flow<List<Recording>>
    suspend fun getRecordingById(id: String): Recording?
    suspend fun saveRecording(recording: Recording)
    suspend fun deleteRecording(id: String)
    suspend fun getRecordingCount(storyId: String): Int
}

interface SceneRepository {
    fun getScenesByStoryId(storyId: String): Flow<List<Scene>>
    suspend fun getSceneById(id: String): Scene?
    suspend fun saveScene(scene: Scene)
    suspend fun updateScene(scene: Scene)
    suspend fun deleteScene(id: String)
    suspend fun getSceneCount(storyId: String): Int
    suspend fun getMaxOrderIndex(storyId: String): Int?
}

interface CharacterRepository {
    fun getCharactersByStoryId(storyId: String): Flow<List<Character>>
    suspend fun getCharacterById(id: String): Character?
    suspend fun findCharacterByName(storyId: String, name: String): Character?
    suspend fun saveCharacter(character: Character)
    suspend fun updateCharacter(character: Character)
    suspend fun deleteCharacter(id: String)
    suspend fun getCharacterCount(storyId: String): Int
}

interface SceneCharacterRepository {
    fun getSceneCharactersBySceneId(sceneId: String): Flow<List<SceneCharacter>>
    fun getSceneCharactersByCharacterId(characterId: String): Flow<List<SceneCharacter>>
    suspend fun getSceneIdsForCharacter(characterId: String): List<String>
    suspend fun getCharacterIdsForScene(sceneId: String): List<String>
    suspend fun addSceneCharacter(sceneCharacter: SceneCharacter)
    suspend fun removeSceneCharacter(sceneId: String, characterId: String)
    suspend fun removeSceneCharactersBySceneId(sceneId: String)
    suspend fun removeSceneCharactersByCharacterId(characterId: String)
}

interface GraphNodeRepository {
    suspend fun getGraphNodeById(id: String): GraphNode?
    fun getGraphNodesByType(type: String): Flow<List<GraphNode>>
    suspend fun saveGraphNode(graphNode: GraphNode)
    suspend fun updateGraphNode(graphNode: GraphNode)
    suspend fun deleteGraphNode(id: String)
}