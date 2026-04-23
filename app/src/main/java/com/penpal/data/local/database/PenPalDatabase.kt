package com.penpal.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.penpal.data.local.dao.*
import com.penpal.data.local.entity.*

@Database(
    entities = [
        StoryEntity::class,
        RecordingEntity::class,
        SceneEntity::class,
        CharacterEntity::class,
        SceneCharacterEntity::class,
        GraphNodeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PenPalDatabase : RoomDatabase() {
    abstract fun storyDao(): StoryDao
    abstract fun recordingDao(): RecordingDao
    abstract fun sceneDao(): SceneDao
    abstract fun characterDao(): CharacterDao
    abstract fun sceneCharacterDao(): SceneCharacterDao
    abstract fun graphNodeDao(): GraphNodeDao
}