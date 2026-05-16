package com.personal.mangastone.data.api.models

import com.google.gson.annotations.SerializedName

// ── Search / list ─────────────────────────────────────────────────────────────

data class ComicDto(
    val id: Int,
    val hid: String,
    val title: String,
    val slug: String,
    @SerializedName("content_rating") val contentRating: String?,
    @SerializedName("last_chapter") val lastChapter: Double?,
    @SerializedName("md_covers") val mdCovers: List<CoverDto>
)

data class CoverDto(
    val b2key: String?,
    val vol: String?,
    val gpurl: String?,
    val mdurl: String?
)

// ── Comic detail ──────────────────────────────────────────────────────────────

data class ComicDetailResponse(
    val comic: ComicDetailDto,
    val authors: List<AuthorDto>?
)

data class ComicDetailDto(
    val id: Int,
    val hid: String,
    val title: String,
    val slug: String,
    val desc: String?,
    val status: Int?,
    val country: String?,
    @SerializedName("last_chapter") val lastChapter: Double?,
    @SerializedName("md_covers") val mdCovers: List<CoverDto>,
    @SerializedName("md_comic_md_genres") val genres: List<GenreWrapper>?
)

data class GenreWrapper(
    @SerializedName("md_genres") val genre: GenreDto?
)

data class GenreDto(
    val name: String,
    val slug: String
)

data class AuthorDto(
    val name: String,
    val slug: String?
)

// ── Chapters ──────────────────────────────────────────────────────────────────

data class ChaptersResponse(
    val chapters: List<ChapterDto>,
    val total: Int
)

data class ChapterDto(
    val id: Int,
    val hid: String,
    val chap: String?,
    val vol: String?,
    val title: String?,
    val lang: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("group_name") val groupName: List<String>?
)

// ── Chapter images ────────────────────────────────────────────────────────────

data class ChapterDetailResponse(
    val chapter: ChapterImagesDto
)

data class ChapterImagesDto(
    val hid: String,
    val images: List<ChapterImageDto>
)

data class ChapterImageDto(
    val url: String?,
    @SerializedName("b2key") val b2key: String?
)
