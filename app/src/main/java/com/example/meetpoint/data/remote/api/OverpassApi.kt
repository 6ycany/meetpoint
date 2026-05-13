package com.example.meetpoint.data.remote.api

import com.example.meetpoint.data.remote.dto.OverpassResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface OverpassApi {

    /**
     * Overpass QL クエリを実行する
     * @param data Overpass QLクエリ文字列
     */
    @FormUrlEncoded
    @POST("api/interpreter")
    suspend fun query(@Field("data") data: String): OverpassResponse
}

/** SA/PA検索クエリ（中心座標から半径radiusKm km以内） */
fun buildSaPaQuery(lat: Double, lon: Double, radiusKm: Int = 50): String {
    val radius = radiusKm * 1000
    return """
        [out:json][timeout:25];
        (
          node["highway"="services"](around:$radius,$lat,$lon);
          way["highway"="services"](around:$radius,$lat,$lon);
        );
        out center;
    """.trimIndent()
}

/** 主要駅検索クエリ（中心座標から半径radiusKm km以内） */
fun buildStationQuery(lat: Double, lon: Double, radiusKm: Int = 50): String {
    val radius = radiusKm * 1000
    return """
        [out:json][timeout:25];
        node["railway"="station"](around:$radius,$lat,$lon);
        out body;
    """.trimIndent()
}
