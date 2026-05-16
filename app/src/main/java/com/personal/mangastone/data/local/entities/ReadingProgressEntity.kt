package com.personal.mangastone.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val chapterId: String,
    val mangaId: String,
    val lastPage: Int,
    val totalPages: Int,
    val chapterNumber: String?,
    val readAt: Long = System.currentTimeMillis()
)
