package com.example.meetpoint.data.repository

import android.util.Log
import com.example.meetpoint.data.remote.api.OverpassApi
import com.example.meetpoint.data.remote.api.buildSaPaQuery
import com.example.meetpoint.data.remote.api.buildStationQuery
import com.example.meetpoint.domain.model.MeetCandidate
import com.example.meetpoint.domain.model.PlaceType
import com.example.meetpoint.domain.usecase.haversine
import javax.inject.Inject

private const val TAG = "PlaceRepository"

/**
 * SA/PA・駅候補を取得するリポジトリ
 */
class PlaceRepository @Inject constructor(
    private val overpassApi: OverpassApi
) {
    /**
     * 重心座標の周囲のSA/PAを取得する。
     * 0件の場合は1.5倍の半径でリトライする（1回のみ）。
     */
    suspend fun searchSaPa(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Int = 50,
        maxResults: Int = 10
    ): List<MeetCandidate> {
        val result = querySaPa(centerLat, centerLon, radiusKm, maxResults)
        if (result.isNotEmpty()) return result

        // 0件 → 1.5倍の半径でリトライ
        val retryRadius = (radiusKm * 1.5).toInt()
        Log.w(TAG, "SA/PA 0件 → 半径を ${radiusKm}km から ${retryRadius}km に拡大してリトライ")
        return querySaPa(centerLat, centerLon, retryRadius, maxResults)
    }

    private suspend fun querySaPa(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Int,
        maxResults: Int
    ): List<MeetCandidate> = runCatching {
        val query = buildSaPaQuery(centerLat, centerLon, radiusKm)
        Log.d(TAG, "SA/PA検索: 半径=${radiusKm}km, center=(${centerLat}, ${centerLon})")
        val response = overpassApi.query(query)
        Log.d(TAG, "SA/PA レスポンス件数: ${response.elements.size}")

        response.elements
            .mapNotNull { element ->
                val lat = element.effectiveLat ?: return@mapNotNull null
                val lon = element.effectiveLon ?: return@mapNotNull null
                MeetCandidate(
                    name = element.name.ifBlank { "サービスエリア" },
                    latitude = lat,
                    longitude = lon,
                    placeType = PlaceType.SA_PA
                )
            }
            .sortedBy { haversine(centerLat, centerLon, it.latitude, it.longitude) }
            .take(maxResults)
    }.getOrElse { e ->
        Log.e(TAG, "SA/PA検索エラー: ${e.message}", e)
        emptyList()
    }

    /**
     * 重心座標の周囲の駅を取得する。
     * 0件の場合は1.5倍の半径でリトライする（1回のみ）。
     */
    suspend fun searchStations(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Int = 80,
        maxResults: Int = 30
    ): List<MeetCandidate> {
        val result = queryStations(centerLat, centerLon, radiusKm, maxResults)
        if (result.isNotEmpty()) return result

        // 0件 → 1.5倍の半径でリトライ
        val retryRadius = (radiusKm * 1.5).toInt()
        Log.w(TAG, "駅 0件 → 半径を ${radiusKm}km から ${retryRadius}km に拡大してリトライ")
        return queryStations(centerLat, centerLon, retryRadius, maxResults)
    }

    private suspend fun queryStations(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Int,
        maxResults: Int
    ): List<MeetCandidate> = runCatching {
        val query = buildStationQuery(centerLat, centerLon, radiusKm)
        Log.d(TAG, "駅検索: 半径=${radiusKm}km, center=(${centerLat}, ${centerLon})")
        val response = overpassApi.query(query)
        Log.d(TAG, "駅 レスポンス件数: ${response.elements.size}")

        response.elements
            .filter { it.effectiveLat != null && it.effectiveLon != null }
            .also { Log.d(TAG, "有効な駅件数: ${it.size}") }
            .map { element ->
                MeetCandidate(
                    name = element.name.ifBlank { "駅" },
                    latitude = element.effectiveLat!!,
                    longitude = element.effectiveLon!!,
                    placeType = PlaceType.STATION
                )
            }
            .sortedBy { haversine(centerLat, centerLon, it.latitude, it.longitude) }
            .take(maxResults)
    }.getOrElse { e ->
        Log.e(TAG, "駅検索エラー: ${e.message}", e)
        emptyList()
    }
}
