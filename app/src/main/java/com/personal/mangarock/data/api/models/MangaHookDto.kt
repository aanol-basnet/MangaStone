package com.personal.mangarock.data.api.models

import com.google.gson.annotations.SerializedName

data class MangaListResponse(
    val mangaList: List<MangaListItem>,
    val metaData: MetaData?
)

data class MangaListItem(
    val id: String,
    val image: String,
    val title: String,
    val chapter: String?,
    val view: String?,
    val description: String?
)

data class MetaData(
    val totalStories: String?,
    val totalPages: String?,
    val type: List<MetaOption>?,
    val state: List<MetaOption>?,
    val category: List<MetaOption>?
)

data class MetaOption(
    val id: String,
    @SerializedName("type") val name: String
)

data class MangaDetailResponse(
    val imageUrl: String,
    val name: String,
    val author: String?,
    val status: String?,
    val updated: String?,
    val view: String?,
    val genres: List<String>?,
    val chapterList: List<ChapterListItem>
)

data class ChapterListItem(
    val id: String,
    val path: String?,
    val name: String,
    val view: String?,
    val createdAt: String?
)

data class ChapterImagesResponse(
    val title: String?,
    val currentChapter: String?,
    val images: List<ImageItem>
)

data class ImageItem(
    val title: String?,
    val image: String
)
