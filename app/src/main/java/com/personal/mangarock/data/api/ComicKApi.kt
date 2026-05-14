package com.personal.mangarock.data.api

import com.personal.mangarock.data.api.models.ChapterDetailResponse
import com.personal.mangarock.data.api.models.ChaptersResponse
import com.personal.mangarock.data.api.models.ComicDetailResponse
import com.personal.mangarock.data.api.models.ComicDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ComicKApi {

    @GET("v1.0/search")
    suspend fun search(
        @Query("q") query: String? = null,
        @Query("sort") sort: String? = null,
        @Query("status") status: Int? = null,
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int = 1,
        @Query("tachiyomi") tachiyomi: Boolean = true
    ): List<ComicDto>

    @GET("comic/{slug}")
    suspend fun getComic(
        @Path("slug") slug: String,
        @Query("tachiyomi") tachiyomi: Boolean = true
    ): ComicDetailResponse

    @GET("comic/{hid}/chapters")
    suspend fun getChapters(
        @Path("hid") hid: String,
        @Query("lang") lang: String = "en",
        @Query("limit") limit: Int = 500,
        @Query("page") page: Int = 1,
        @Query("tachiyomi") tachiyomi: Boolean = true
    ): ChaptersResponse

    @GET("chapter/{hid}")
    suspend fun getChapter(
        @Path("hid") hid: String,
        @Query("tachiyomi") tachiyomi: Boolean = true
    ): ChapterDetailResponse
}
