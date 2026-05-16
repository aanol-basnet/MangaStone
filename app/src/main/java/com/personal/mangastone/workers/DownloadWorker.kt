package com.personal.mangastone.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.personal.mangastone.R
import com.personal.mangastone.data.local.dao.DownloadDao
import com.personal.mangastone.data.local.entities.DownloadStatus
import com.personal.mangastone.data.source.MangaSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Named

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val mangaSource: MangaSource,
    private val downloadDao: DownloadDao,
    @Named("image") private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo = buildForegroundInfo(0, 0)

    override suspend fun doWork(): Result {
        val chapterId = inputData.getString(KEY_CHAPTER_ID) ?: return Result.failure()
        Log.d("DW", "doWork() started for chapterId=$chapterId")

        return withContext(Dispatchers.IO) {
            try {
                Log.d("DW", "[$chapterId] Setting status → DOWNLOADING")
                downloadDao.updateDownloadProgress(chapterId, DownloadStatus.DOWNLOADING, 0)

                val pages = mangaSource.getPageList(chapterId)
                Log.d("DW", "[$chapterId] Got ${pages.size} pages")
                downloadDao.setTotalPages(chapterId, pages.size)

                val chapterDir = chapterDir(context, chapterId)
                chapterDir.mkdirs()

                val completed = AtomicInteger(0)
                val semaphore = Semaphore(4)

                coroutineScope {
                    pages.mapIndexed { index, url ->
                        async(Dispatchers.IO) {
                            semaphore.withPermit {
                                val destFile = File(chapterDir, "$index.jpg")
                                if (!destFile.exists()) {
                                    val request = Request.Builder().url(url).build()
                                    okHttpClient.newCall(request).execute().use { response ->
                                        response.body?.byteStream()?.use { input ->
                                            destFile.outputStream().use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                    }
                                }
                                val done = completed.incrementAndGet()
                                downloadDao.updateDownloadProgress(
                                    chapterId, DownloadStatus.DOWNLOADING, done
                                )
                                // Update foreground notification progress
                                setForeground(buildForegroundInfo(done, pages.size))
                            }
                        }
                    }.awaitAll()
                }

                downloadDao.updateDownloadProgress(chapterId, DownloadStatus.COMPLETED, pages.size)
                Result.success()
            } catch (e: Exception) {
                Log.e("DW", "[$chapterId] Download FAILED: ${e::class.simpleName}: ${e.message}", e)
                downloadDao.updateDownloadProgress(chapterId, DownloadStatus.FAILED, 0)
                Result.failure()
            }
        }
    }

    private fun buildForegroundInfo(progress: Int, total: Int): ForegroundInfo {
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Downloading chapter")
            .setContentText(if (total > 0) "$progress / $total pages" else "Starting…")
            .setProgress(total, progress, total == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel() {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Chapter Downloads",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Shows download progress for manga chapters" }
            )
        }
    }

    companion object {
        const val KEY_CHAPTER_ID = "chapter_id"
        private const val CHANNEL_ID = "chapter_downloads"
        private const val NOTIFICATION_ID = 1001

        fun chapterDir(context: Context, chapterId: String): File =
            File(context.getExternalFilesDir(null), "chapters/${chapterId.toSafeDirName()}")

        fun enqueue(context: Context, chapterId: String) {
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workDataOf(KEY_CHAPTER_ID to chapterId))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context)
                // REPLACE clears any stuck/failed job with the same ID before queuing a fresh one
                .enqueueUniqueWork(chapterId, ExistingWorkPolicy.REPLACE, request)
        }
    }
}

/** Safe filesystem name: "berserk::v40/c363" → "berserk::v40_c363" */
internal fun String.toSafeDirName() = replace("/", "_")
