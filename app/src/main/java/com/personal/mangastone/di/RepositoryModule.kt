package com.personal.mangastone.di

import com.personal.mangastone.data.repository.ChapterRepository
import com.personal.mangastone.data.repository.ChapterRepositoryImpl
import com.personal.mangastone.data.repository.FavoritesRepository
import com.personal.mangastone.data.repository.FavoritesRepositoryImpl
import com.personal.mangastone.data.repository.MangaRepository
import com.personal.mangastone.data.repository.MangaRepositoryImpl
import com.personal.mangastone.data.scraper.MangaHereScraper
import com.personal.mangastone.data.source.MangaSource
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
    abstract fun bindMangaSource(impl: MangaHereScraper): MangaSource

    @Binds
    @Singleton
    abstract fun bindMangaRepository(impl: MangaRepositoryImpl): MangaRepository

    @Binds
    @Singleton
    abstract fun bindChapterRepository(impl: ChapterRepositoryImpl): ChapterRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository
}
