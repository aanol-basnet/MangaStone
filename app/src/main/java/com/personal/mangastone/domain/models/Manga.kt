package com.personal.mangastone.domain.models

data class Manga(
    val id: String,
    val title: String,
    val titleRomanji: String?,
    val coverUrl: String,
    val description: String,
    val status: String,
    val tags: List<Tag>,
    val author: String,
    val year: Int?,
    val lastChapter: String?,
    val latestChapterId: String?,
    val createdAt: String,
    val updatedAt: String
)

data class Tag(
    val id: String,
    val name: String,
    val group: String
)
