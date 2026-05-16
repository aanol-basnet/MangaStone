package com.personal.mangastone.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mangaId: String,
    val title: String,
    val coverUrl: String,
    val status: String,
    val addedAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long = 0L,
    val lastReadChapterId: String? = null,
    val lastReadChapterNumber: String? = null,
    // Tracks the latest chapter seen by the background checker (not the one being read).
    // null = not yet checked; first check sets the baseline without notifying.
    val latestKnownChapterNumber: String? = null
)
