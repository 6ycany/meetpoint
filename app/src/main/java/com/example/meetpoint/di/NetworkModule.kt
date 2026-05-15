package com.example.meetpoint.di

import com.example.meetpoint.data.remote.api.GoogleGeocodingApi
import com.example.meetpoint.data.remote.api.OverpassApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /** Google API 用 OkHttpClient（標準タイムアウト） */
    @Provides
    @Singleton
    @Named("google")
    fun provideGoogleOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        )
        .build()

    /**
     * Overpass API 用 OkHttpClient。
     * Overpass クエリは最大 30〜60 秒かかるため、readTimeout を 90 秒に設定。
     * デフォルトの 10 秒だと必ずタイムアウトして 0 件になる。
     */
    @Provides
    @Singleton
    @Named("overpass")
    fun provideOverpassOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        )
        .build()

    @Provides
    @Singleton
    @Named("google")
    fun provideGoogleRetrofit(@Named("google") okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    @Named("overpass")
    fun provideOverpassRetrofit(@Named("overpass") okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://overpass-api.de/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideGoogleGeocodingApi(@Named("google") retrofit: Retrofit): GoogleGeocodingApi =
        retrofit.create(GoogleGeocodingApi::class.java)

    @Provides
    @Singleton
    fun provideOverpassApi(@Named("overpass") retrofit: Retrofit): OverpassApi =
        retrofit.create(OverpassApi::class.java)
}
