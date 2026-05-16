package com.personal.mangastone.domain.models

data class Chapter(
    val id: String,
    val mangaId: String,
    val title: String?,
    val chapterNumber: String?,
    val volume: String?,
    val pages: Int,
    val publishAt: String,
    val scanlationGroup: String,
    val externalUrl: String?
)
