package com.personal.mangastone.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mangaId: String,
    val mangaTitle: String,
    val mangaCoverUrl: String,
    val chapterId: String,       // composite ID — used directly for navigation to reader
    val chapterNumber: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
