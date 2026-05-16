package com.personal.mangarock.data.local.dao

import androidx.room.*
import com.personal.mangarock.data.local.entities.DownloadEntity
import com.personal.mangarock.data.local.entities.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE mangaId = :mangaId")
    fun getDownloadsForManga(mangaId: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE chapterId = :chapterId")
    suspend fun getDownload(chapterId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Query("UPDATE downloads SET status = :status, downloadedPages = :downloadedPages WHERE chapterId = :chapterId")
    suspend fun updateDownloadProgress(chapterId: String, status: DownloadStatus, downloadedPages: Int)

    @Query("UPDATE downloads SET totalPages = :totalPages WHERE chapterId = :chapterId")
    suspend fun setTotalPages(chapterId: String, totalPages: Int)

    @Query("DELETE FROM downloads WHERE chapterId = :chapterId")
    suspend fun deleteDownload(chapterId: String)

    @Query("SELECT * FROM downloads WHERE status = :status")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadEntity>>
}
