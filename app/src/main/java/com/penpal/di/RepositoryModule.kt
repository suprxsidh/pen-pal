package com.penpal.di

import com.penpal.data.repository.*
import com.penpal.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindStoryRepository(impl: StoryRepositoryImpl): StoryRepository

    @Binds
    @Singleton
    abstract fun bindRecordingRepository(impl: RecordingRepositoryImpl): RecordingRepository

    @Binds
    @Singleton
    abstract fun bindSceneRepository(impl: SceneRepositoryImpl): SceneRepository

    @Binds
    @Singleton
    abstract fun bindCharacterRepository(impl: CharacterRepositoryImpl): CharacterRepository

    @Binds
    @Singleton
    abstract fun bindSceneCharacterRepository(impl: SceneCharacterRepositoryImpl): SceneCharacterRepository

    @Binds
    @Singleton
    abstract fun bindGraphNodeRepository(impl: GraphNodeRepositoryImpl): GraphNodeRepository
}