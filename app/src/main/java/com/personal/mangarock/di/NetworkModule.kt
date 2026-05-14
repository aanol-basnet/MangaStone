package com.personal.mangarock.di

import com.personal.mangarock.BuildConfig
import com.personal.mangarock.data.api.MangaHookApi
import com.personal.mangarock.ui.screens.settings.AppPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideServerUrlHolder(prefs: AppPreferences): ServerUrlHolder {
        val holder = ServerUrlHolder()
        // Read the saved URL synchronously once at startup
        holder.url = runBlocking { prefs.serverUrl.first() }
        return holder
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(serverUrlHolder: ServerUrlHolder): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            // Rewrite the host/port on every request from the live holder value
            .addInterceptor { chain ->
                val savedBase = serverUrlHolder.url.trimEnd('/')
                val newBase = savedBase.toHttpUrlOrNull()
                if (newBase == null) {
                    chain.proceed(chain.request())
                } else {
                    val newUrl = chain.request().url.newBuilder()
                        .scheme(newBase.scheme)
                        .host(newBase.host)
                        .port(newBase.port)
                        .build()
                    chain.proceed(chain.request().newBuilder().url(newUrl).build())
                }
            }
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
        }
        return builder.build()
    }

    // Used by Coil and DownloadWorker — sets Referer for MangaHere's image CDN
    @Provides
    @Singleton
    @Named("image")
    fun provideImageOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Referer", "https://www.mangahere.cc/")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                chain.proceed(request)
            }
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("http://localhost/") // placeholder — interceptor rewrites the host on every call
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideMangaHookApi(retrofit: Retrofit): MangaHookApi =
        retrofit.create(MangaHookApi::class.java)
}
