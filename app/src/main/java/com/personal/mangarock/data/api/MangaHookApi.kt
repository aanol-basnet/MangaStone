package com.personal.mangarock.data.api

import com.personal.mangarock.data.api.models.ChapterImagesResponse
import com.personal.mangarock.data.api.models.MangaDetailResponse
import com.personal.mangarock.data.api.models.MangaListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MangaHookApi {

    @GET("api/mangaList")
    suspend fun getMangaList(
        @Query("page") page: Int = 1,
        @Query("type") type: String? = null,
        @Query("state") state: String? = null,
        @Query("category") category: String? = null
    ): MangaListResponse

    @GET("api/search/{query}")
    suspend fun search(
        @Path("query") query: String,
        @Query("page") page: Int = 1
    ): MangaListResponse

    @GET("api/manga/{id}")
    suspend fun getManga(
        @Path("id") id: String
    ): MangaDetailResponse

    @GET("api/manga/{id}/{ch}")
    suspend fun getChapterImages(
        @Path("id") id: String,
        @Path("ch", encoded = true) chapterId: String
    ): ChapterImagesResponse

    @GET("api/genre/{genre}")
    suspend fun getGenreManga(
        @Path("genre") genre: String,
        @Query("page") page: Int = 1
    ): MangaListResponse
}
