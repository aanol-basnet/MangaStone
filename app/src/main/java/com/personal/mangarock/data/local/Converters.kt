package com.personal.mangarock.data.local

import androidx.room.TypeConverter
import com.personal.mangarock.data.local.entities.DownloadStatus

class Converters {
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)
}
