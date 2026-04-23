package com.penpal.data.local.dao

import androidx.room.*
import com.penpal.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY updatedAt DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun getStoryById(id: String): StoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Update
    suspend fun updateStory(story: StoryEntity)

    @Delete
    suspend fun deleteStory(story: StoryEntity)

    @Query("DELETE FROM stories WHERE id = :id")
    suspend fun deleteStoryById(id: String)
}

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings WHERE storyId = :storyId ORDER BY createdAt DESC")
    fun getRecordingsByStoryId(storyId: String): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: String): RecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity)

    @Delete
    suspend fun deleteRecording(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: String)

    @Query("SELECT COUNT(*) FROM recordings WHERE storyId = :storyId")
    suspend fun getRecordingCount(storyId: String): Int
}

@Dao
interface SceneDao {
    @Query("SELECT * FROM scenes WHERE storyId = :storyId ORDER BY orderIndex ASC")
    fun getScenesByStoryId(storyId: String): Flow<List<SceneEntity>>

    @Query("SELECT * FROM scenes WHERE id = :id")
    suspend fun getSceneById(id: String): SceneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: SceneEntity)

    @Update
    suspend fun updateScene(scene: SceneEntity)

    @Delete
    suspend fun deleteScene(scene: SceneEntity)

    @Query("DELETE FROM scenes WHERE id = :id")
    suspend fun deleteSceneById(id: String)

    @Query("SELECT COUNT(*) FROM scenes WHERE storyId = :storyId")
    suspend fun getSceneCount(storyId: String): Int

    @Query("SELECT MAX(orderIndex) FROM scenes WHERE storyId = :storyId")
    suspend fun getMaxOrderIndex(storyId: String): Int?
}

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters WHERE storyId = :storyId")
    fun getCharactersByStoryId(storyId: String): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getCharacterById(id: String): CharacterEntity?

    @Query("SELECT * FROM characters WHERE storyId = :storyId AND (name LIKE '%' || :name || '%' OR aliases LIKE '%' || :name || '%')")
    suspend fun findCharacterByName(storyId: String, name: String): CharacterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity)

    @Update
    suspend fun updateCharacter(character: CharacterEntity)

    @Delete
    suspend fun deleteCharacter(character: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun deleteCharacterById(id: String)

    @Query("SELECT COUNT(*) FROM characters WHERE storyId = :storyId")
    suspend fun getCharacterCount(storyId: String): Int
}

@Dao
interface SceneCharacterDao {
    @Query("SELECT * FROM scene_characters WHERE sceneId = :sceneId")
    fun getSceneCharactersBySceneId(sceneId: String): Flow<List<SceneCharacterEntity>>

    @Query("SELECT * FROM scene_characters WHERE characterId = :characterId")
    fun getSceneCharactersByCharacterId(characterId: String): Flow<List<SceneCharacterEntity>>

    @Query("SELECT sceneId FROM scene_characters WHERE characterId = :characterId")
    suspend fun getSceneIdsForCharacter(characterId: String): List<String>

    @Query("SELECT characterId FROM scene_characters WHERE sceneId = :sceneId")
    suspend fun getCharacterIdsForScene(sceneId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSceneCharacter(sceneCharacter: SceneCharacterEntity)

    @Delete
    suspend fun deleteSceneCharacter(sceneCharacter: SceneCharacterEntity)

    @Query("DELETE FROM scene_characters WHERE sceneId = :sceneId")
    suspend fun deleteSceneCharactersBySceneId(sceneId: String)

    @Query("DELETE FROM scene_characters WHERE characterId = :characterId")
    suspend fun deleteSceneCharactersByCharacterId(characterId: String)
}

@Dao
interface GraphNodeDao {
    @Query("SELECT * FROM graph_nodes WHERE id = :id")
    suspend fun getGraphNodeById(id: String): GraphNodeEntity?

    @Query("SELECT * FROM graph_nodes WHERE type = :type")
    fun getGraphNodesByType(type: String): Flow<List<GraphNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGraphNode(graphNode: GraphNodeEntity)

    @Update
    suspend fun updateGraphNode(graphNode: GraphNodeEntity)

    @Query("DELETE FROM graph_nodes WHERE id = :id")
    suspend fun deleteGraphNodeById(id: String)
}