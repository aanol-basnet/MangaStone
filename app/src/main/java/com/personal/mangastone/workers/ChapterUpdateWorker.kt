package com.personal.mangastone.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.personal.mangastone.MainActivity
import com.personal.mangastone.R
import com.personal.mangastone.data.local.dao.FavoriteDao
import com.personal.mangastone.data.local.dao.NotificationDao
import com.personal.mangastone.data.local.entities.NotificationEntity
import com.personal.mangastone.data.source.MangaSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class ChapterUpdateWorker @AssistedInject constructor(
    @Assisted private val context: android.content.Context,
    @Assisted workerParams: WorkerParameters,
    private val mangaHookSource: MangaSource,
    private val favoriteDao: FavoriteDao,
    private val notificationDao: NotificationDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        ensureNotificationChannel()

        val favorites = favoriteDao.getAllFavorites().first()
        favorites.forEach { favorite ->
            try {
                val chapters = mangaHookSource.getChapterList(favorite.mangaId)
                val latest = chapters.firstOrNull() ?: return@forEach
                val latestNum = latest.chapterNumber ?: return@forEach

                when {
                    // First time checking — save baseline silently
                    favorite.latestKnownChapterNumber == null -> {
                        favoriteDao.updateLatestKnownChapter(favorite.mangaId, latestNum)
                    }
                    // New chapter appeared since last check
                    latestNum != favorite.latestKnownChapterNumber -> {
                        // Save to in-app notification feed
                        notificationDao.insert(
                            NotificationEntity(
                                mangaId = favorite.mangaId,
                                mangaTitle = favorite.title,
                                mangaCoverUrl = favorite.coverUrl,
                                chapterId = latest.id,
                                chapterNumber = latestNum
                            )
                        )
                        // Also fire a system notification
                        sendNotification(favorite.mangaId, favorite.title, latestNum)
                        favoriteDao.updateLatestKnownChapter(favorite.mangaId, latestNum)
                    }
                    // No change — do nothing
                }
            } catch (_: Exception) {}
        }
        return Result.success()
    }

    private fun ensureNotificationChannel() {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "New Chapters", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Alerts when a new chapter drops for your favorites" }
            )
        }
    }

    private fun sendNotification(mangaId: String, mangaTitle: String, chapterNumber: String) {
        val nm = context.getSystemService(NotificationManager::class.java)

        // Tap notification → open the app
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, mangaId.hashCode(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(mangaTitle)
            .setContentText("Chapter $chapterNumber is out!")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(mangaId.hashCode(), notification)
    }

    companion object {
        const val WORK_NAME = "chapter_update_check"
        private const val CHANNEL_ID = "chapter_updates"

        fun schedule(context: android.content.Context, intervalHours: Long = 6) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<ChapterUpdateWorker>(intervalHours, TimeUnit.HOURS)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
            )
        }

        fun cancel(context: android.content.Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
