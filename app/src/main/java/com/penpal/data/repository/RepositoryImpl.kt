package com.penpal.data.repository

import com.penpal.data.local.dao.*
import com.penpal.data.local.entity.*
import com.penpal.domain.model.*
import com.penpal.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StoryRepositoryImpl @Inject constructor(
    private val storyDao: StoryDao
) : StoryRepository {
    override fun getAllStories(): Flow<List<Story>> =
        storyDao.getAllStories().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getStoryById(id: String): Story? =
        storyDao.getStoryById(id)?.toDomain()

    override suspend fun createStory(story: Story) =
        storyDao.insertStory(story.toEntity())

    override suspend fun updateStory(story: Story) =
        storyDao.updateStory(story.toEntity())

    override suspend fun deleteStory(id: String) =
        storyDao.deleteStoryById(id)
}

class RecordingRepositoryImpl @Inject constructor(
    private val recordingDao: RecordingDao
) : RecordingRepository {
    override fun getRecordingsByStoryId(storyId: String): Flow<List<Recording>> =
        recordingDao.getRecordingsByStoryId(storyId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getRecordingById(id: String): Recording? =
        recordingDao.getRecordingById(id)?.toDomain()

    override suspend fun saveRecording(recording: Recording) =
        recordingDao.insertRecording(recording.toEntity())

    override suspend fun deleteRecording(id: String) =
        recordingDao.deleteRecordingById(id)

    override suspend fun getRecordingCount(storyId: String): Int =
        recordingDao.getRecordingCount(storyId)
}

class SceneRepositoryImpl @Inject constructor(
    private val sceneDao: SceneDao
) : SceneRepository {
    override fun getScenesByStoryId(storyId: String): Flow<List<Scene>> =
        sceneDao.getScenesByStoryId(storyId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getSceneById(id: String): Scene? =
        sceneDao.getSceneById(id)?.toDomain()

    override suspend fun saveScene(scene: Scene) =
        sceneDao.insertScene(scene.toEntity())

    override suspend fun updateScene(scene: Scene) =
        sceneDao.updateScene(scene.toEntity())

    override suspend fun deleteScene(id: String) =
        sceneDao.deleteSceneById(id)

    override suspend fun getSceneCount(storyId: String): Int =
        sceneDao.getSceneCount(storyId)

    override suspend fun getMaxOrderIndex(storyId: String): Int? =
        sceneDao.getMaxOrderIndex(storyId)
}

class CharacterRepositoryImpl @Inject constructor(
    private val characterDao: CharacterDao
) : CharacterRepository {
    override fun getCharactersByStoryId(storyId: String): Flow<List<Character>> =
        characterDao.getCharactersByStoryId(storyId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getCharacterById(id: String): Character? =
        characterDao.getCharacterById(id)?.toDomain()

    override suspend fun findCharacterByName(storyId: String, name: String): Character? =
        characterDao.findCharacterByName(storyId, name)?.toDomain()

    override suspend fun saveCharacter(character: Character) =
        characterDao.insertCharacter(character.toEntity())

    override suspend fun updateCharacter(character: Character) =
        characterDao.updateCharacter(character.toEntity())

    override suspend fun deleteCharacter(id: String) =
        characterDao.deleteCharacterById(id)

    override suspend fun getCharacterCount(storyId: String): Int =
        characterDao.getCharacterCount(storyId)
}

class SceneCharacterRepositoryImpl @Inject constructor(
    private val sceneCharacterDao: SceneCharacterDao
) : SceneCharacterRepository {
    override fun getSceneCharactersBySceneId(sceneId: String): Flow<List<SceneCharacter>> =
        sceneCharacterDao.getSceneCharactersBySceneId(sceneId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getSceneCharactersByCharacterId(characterId: String): Flow<List<SceneCharacter>> =
        sceneCharacterDao.getSceneCharactersByCharacterId(characterId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getSceneIdsForCharacter(characterId: String): List<String> =
        sceneCharacterDao.getSceneIdsForCharacter(characterId)

    override suspend fun getCharacterIdsForScene(sceneId: String): List<String> =
        sceneCharacterDao.getCharacterIdsForScene(sceneId)

    override suspend fun addSceneCharacter(sceneCharacter: SceneCharacter) =
        sceneCharacterDao.insertSceneCharacter(sceneCharacter.toEntity())

    override suspend fun removeSceneCharacter(sceneId: String, characterId: String) {
        val entity = sceneCharacterDao.getSceneCharactersBySceneId(sceneId).let {
            SceneCharacterEntity(id = "${sceneId}_$characterId", sceneId = sceneId, characterId = characterId)
        }
        sceneCharacterDao.deleteSceneCharacter(entity)
    }

    override suspend fun removeSceneCharactersBySceneId(sceneId: String) =
        sceneCharacterDao.deleteSceneCharactersBySceneId(sceneId)

    override suspend fun removeSceneCharactersByCharacterId(characterId: String) =
        sceneCharacterDao.deleteSceneCharactersByCharacterId(characterId)
}

class GraphNodeRepositoryImpl @Inject constructor(
    private val graphNodeDao: GraphNodeDao
) : GraphNodeRepository {
    override suspend fun getGraphNodeById(id: String): GraphNode? =
        graphNodeDao.getGraphNodeById(id)?.toDomain()

    override fun getGraphNodesByType(type: String): Flow<List<GraphNode>> =
        graphNodeDao.getGraphNodesByType(type).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun saveGraphNode(graphNode: GraphNode) =
        graphNodeDao.insertGraphNode(graphNode.toEntity())

    override suspend fun updateGraphNode(graphNode: GraphNode) =
        graphNodeDao.updateGraphNode(graphNode.toEntity())

    override suspend fun deleteGraphNode(id: String) =
        graphNodeDao.deleteGraphNodeById(id)
}

private fun StoryEntity.toDomain() = Story(
    id = id,
    title = title,
    description = description,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun Story.toEntity() = StoryEntity(
    id = id,
    title = title,
    description = description,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun RecordingEntity.toDomain() = Recording(
    id = id,
    storyId = storyId,
    filePath = filePath,
    durationMs = durationMs,
    createdAt = createdAt
)

private fun Recording.toEntity() = RecordingEntity(
    id = id,
    storyId = storyId,
    filePath = filePath,
    durationMs = durationMs,
    createdAt = createdAt
)

private fun SceneEntity.toDomain() = Scene(
    id = id,
    storyId = storyId,
    recordingId = recordingId,
    title = title,
    content = content,
    summary = summary,
    location = location,
    mood = mood,
    orderIndex = orderIndex,
    timestampStartMs = timestampStartMs,
    timestampEndMs = timestampEndMs,
    createdAt = createdAt
)

private fun Scene.toEntity() = SceneEntity(
    id = id,
    storyId = storyId,
    recordingId = recordingId,
    title = title,
    content = content,
    summary = summary,
    location = location,
    mood = mood,
    orderIndex = orderIndex,
    timestampStartMs = timestampStartMs,
    timestampEndMs = timestampEndMs,
    createdAt = createdAt
)

private fun CharacterEntity.toDomain() = Character(
    id = id,
    storyId = storyId,
    name = name,
    aliases = aliases.split(",").filter { it.isNotBlank() },
    description = description,
    color = color,
    createdAt = createdAt
)

private fun Character.toEntity() = CharacterEntity(
    id = id,
    storyId = storyId,
    name = name,
    aliases = aliases.joinToString(","),
    description = description,
    color = color,
    createdAt = createdAt
)

private fun SceneCharacterEntity.toDomain() = SceneCharacter(
    sceneId = sceneId,
    characterId = characterId
)

private fun SceneCharacter.toEntity() = SceneCharacterEntity(
    id = "${sceneId}_$characterId",
    sceneId = sceneId,
    characterId = characterId
)

private fun GraphNodeEntity.toDomain() = GraphNode(
    id = id,
    type = type,
    x = x,
    y = y,
    updatedAt = updatedAt
)

private fun GraphNode.toEntity() = GraphNodeEntity(
    id = id,
    type = type,
    x = x,
    y = y,
    updatedAt = updatedAt
)