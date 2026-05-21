package com.personal.mangastone.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.personal.mangastone.data.local.dao.FavoriteDao
import com.personal.mangastone.data.local.dao.ReadingProgressDao
import com.personal.mangastone.data.local.entities.FavoriteEntity
import com.personal.mangastone.data.local.entities.ReadingProgressEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

data class LibraryBackup(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val favorites: List<FavoriteEntity> = emptyList(),
    val readingProgress: List<ReadingProgressEntity> = emptyList()
)

@Singleton
class LibraryBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val favoriteDao: FavoriteDao,
    private val progressDao: ReadingProgressDao
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /** Serializes the full library to JSON, writes to app files dir, returns a share URI. */
    suspend fun export(): Uri = withContext(Dispatchers.IO) {
        val backup = LibraryBackup(
            favorites       = favoriteDao.getAllFavoritesOnce(),
            readingProgress = progressDao.getAllProgress()
        )
        val json = gson.toJson(backup)
        val file = File(context.getExternalFilesDir(null), "mangastone_backup.json")
        file.writeText(json)
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    /** Reads JSON from the given input stream and restores favorites + reading progress. */
    suspend fun import(stream: InputStream): ImportResult = withContext(Dispatchers.IO) {
        try {
            val json    = stream.bufferedReader().readText()
            val backup  = gson.fromJson(json, LibraryBackup::class.java)
                ?: return@withContext ImportResult.Error("Invalid backup file")

            if (backup.version != 1)
                return@withContext ImportResult.Error("Unsupported backup version ${backup.version}")

            favoriteDao.insertAll(backup.favorites)
            backup.readingProgress.forEach { progressDao.saveProgress(it) }

            ImportResult.Success(
                favoritesRestored = backup.favorites.size,
                chaptersRestored  = backup.readingProgress.size
            )
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "Unknown error")
        }
    }

    fun shareIntent(uri: Uri): Intent = Intent(Intent.ACTION_SEND).apply {
        type  = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "MangaStone Library Backup")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

sealed class ImportResult {
    data class Success(val favoritesRestored: Int, val chaptersRestored: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}
