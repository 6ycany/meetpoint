package com.example.meetpoint.data.remote.api

import com.example.meetpoint.data.remote.dto.GeocodingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleGeocodingApi {

    /**
     * 住所文字列から座標を取得する
     * @param address 住所（例: "神奈川県海老名市"）
     * @param key     Maps APIキー
     * @param language レスポンス言語（デフォルト: ja）
     */
    @GET("maps/api/geocode/json")
    suspend fun geocode(
        @Query("address") address: String,
        @Query("key") key: String,
        @Query("language") language: String = "ja"
    ): GeocodingResponse
}
