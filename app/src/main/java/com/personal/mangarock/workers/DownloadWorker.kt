package com.personal.mangarock.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.personal.mangarock.data.local.dao.DownloadDao
import com.personal.mangarock.data.local.entities.DownloadStatus
import com.personal.mangarock.data.source.MangaSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Named

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val mangaHookSource: MangaSource,
    private val downloadDao: DownloadDao,
    @Named("image") private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val chapterId = inputData.getString(KEY_CHAPTER_ID) ?: return Result.failure()
        val mangaId = inputData.getString(KEY_MANGA_ID) ?: return Result.failure()

        return withContext(Dispatchers.IO) {
            try {
                downloadDao.updateDownloadProgress(chapterId, DownloadStatus.DOWNLOADING, 0)

                // chapterId is stored as "mangaId::chapId" — pass directly to getPageList
                val compositeId = "$mangaId::$chapterId"
                val pages = mangaHookSource.getPageList(compositeId)
                val chapterDir = File(context.getExternalFilesDir(null), "chapters/$chapterId")
                chapterDir.mkdirs()

                pages.forEachIndexed { index, url ->
                    val destFile = File(chapterDir, "$index.jpg")
                    if (!destFile.exists()) {
                        val request = Request.Builder()
                            .url(url)
                            .header("Referer", "https://mangakakalot.com")
                            .build()
                        okHttpClient.newCall(request).execute().use { response ->
                            response.body?.byteStream()?.use { input ->
                                destFile.outputStream().use { output -> input.copyTo(output) }
                            }
                        }
                    }
                    downloadDao.updateDownloadProgress(chapterId, DownloadStatus.DOWNLOADING, index + 1)
                }

                downloadDao.updateDownloadProgress(chapterId, DownloadStatus.COMPLETED, pages.size)
                Result.success()
            } catch (e: Exception) {
                downloadDao.updateDownloadProgress(chapterId, DownloadStatus.FAILED, 0)
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_CHAPTER_ID = "chapter_id"
        const val KEY_MANGA_ID = "manga_id"

        fun enqueue(context: Context, chapterId: String, mangaId: String) {
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workDataOf(KEY_CHAPTER_ID to chapterId, KEY_MANGA_ID to mangaId))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(chapterId, ExistingWorkPolicy.KEEP, request)
        }
    }
}
