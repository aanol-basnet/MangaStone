package com.personal.mangastone.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class VersionInfo(
    val latestVersion: String = "",
    val apkUrl: String = "",
    val releaseNotes: String = ""
)

sealed class UpdateResult {
    data class Available(val info: VersionInfo) : UpdateResult()
    object UpToDate : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

@Singleton
class UpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient()
    private val gson = Gson()

    private val versionUrl =
        "https://raw.githubusercontent.com/aanol-basnet/MangaRock/main/version.json"

    suspend fun checkForUpdate(currentVersion: String): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(versionUrl).build()
            val body = client.newCall(request).execute().use { it.body?.string() }
                ?: return@withContext UpdateResult.Error("Empty response")
            val info = gson.fromJson(body, VersionInfo::class.java)
            if (isNewer(info.latestVersion, currentVersion)) {
                UpdateResult.Available(info)
            } else {
                UpdateResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun downloadApk(
        apkUrl: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(apkUrl).build()
            val response = client.newCall(request).execute()
            val body = response.body ?: return@withContext null
            val total = body.contentLength()
            val apkFile = File(context.cacheDir, "update.apk")
            apkFile.outputStream().use { out ->
                var downloaded = 0L
                val buffer = ByteArray(8192)
                val stream = body.byteStream()
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                    downloaded += read
                    if (total > 0) onProgress((downloaded * 100 / total).toInt())
                }
            }
            onProgress(100)
            apkFile
        } catch (e: Exception) {
            null
        }
    }

    fun triggerInstall(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv > cv) return true
            if (lv < cv) return false
        }
        return false
    }
}
