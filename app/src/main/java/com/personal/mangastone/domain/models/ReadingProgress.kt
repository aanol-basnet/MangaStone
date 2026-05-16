package com.personal.mangastone.domain.models

data class ReadingProgress(
    val chapterId: String,
    val mangaId: String,
    val lastPage: Int,
    val totalPages: Int,
    val chapterNumber: String?,
    val readAt: Long
)
