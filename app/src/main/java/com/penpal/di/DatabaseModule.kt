package com.penpal.di

import android.content.Context
import androidx.room.Room
import com.penpal.data.local.dao.*
import com.penpal.data.local.database.PenPalDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PenPalDatabase {
        return Room.databaseBuilder(
            context,
            PenPalDatabase::class.java,
            "penpal-database"
        ).build()
    }

    @Provides
    fun provideStoryDao(database: PenPalDatabase): StoryDao = database.storyDao()

    @Provides
    fun provideRecordingDao(database: PenPalDatabase): RecordingDao = database.recordingDao()

    @Provides
    fun provideSceneDao(database: PenPalDatabase): SceneDao = database.sceneDao()

    @Provides
    fun provideCharacterDao(database: PenPalDatabase): CharacterDao = database.characterDao()

    @Provides
    fun provideSceneCharacterDao(database: PenPalDatabase): SceneCharacterDao = database.sceneCharacterDao()

    @Provides
    fun provideGraphNodeDao(database: PenPalDatabase): GraphNodeDao = database.graphNodeDao()
}