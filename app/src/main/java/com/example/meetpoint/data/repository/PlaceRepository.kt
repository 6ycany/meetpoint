package com.example.meetpoint.data.repository

import android.util.Log
import com.example.meetpoint.data.remote.api.OverpassApi
import com.example.meetpoint.data.remote.api.buildSaPaQuery
import com.example.meetpoint.data.remote.api.buildSaPaQueryMulti
import com.example.meetpoint.data.remote.api.buildStationQuery
import com.example.meetpoint.data.remote.api.buildStationQueryMulti
import com.example.meetpoint.data.remote.dto.OverpassElement
import com.example.meetpoint.domain.model.MeetCandidate
import com.example.meetpoint.domain.model.Person
import com.example.meetpoint.domain.model.PlaceType
import com.example.meetpoint.domain.usecase.haversine
import javax.inject.Inject

private const val TAG = "PlaceRepository"

/** 近接重複とみなす距離（km） */
private const val DEDUP_KM = 0.3

class PlaceRepository @Inject constructor(
    private val overpassApi: OverpassApi
) {

    // ──────────────────────────────────────────
    //  SA / PA 検索
    // ──────────────────────────────────────────

    /**
     * SA/PA を検索する。
     * - 参加者がいる場合 → 各人の出発地周辺を1リクエストでまとめて検索（80km）
     * - 0件なら重心から150km でリトライ
     */
    suspend fun searchSaPa(
        centerLat: Double,
        centerLon: Double,
        persons: List<Person> = emptyList(),
        radiusKm: Int = 80,
        maxResults: Int = 10
    ): List<MeetCandidate> {
        val result = if (persons.size >= 2) {
            val centers = persons.map { Pair(it.latitude, it.longitude) }
            queryOverpass(
                query = buildSaPaQueryMulti(centers, radiusKm),
                centerLat = centerLat,
                centerLon = centerLon,
                placeType = PlaceType.SA_PA,
                defaultName = "サービスエリア",
                maxResults = maxResults,
                tag = "SA/PA(multi)"
            )
        } else {
            queryOverpass(
                query = buildSaPaQuery(centerLat, centerLon, radiusKm),
                centerLat = centerLat,
                centerLon = centerLon,
                placeType = PlaceType.SA_PA,
                defaultName = "サービスエリア",
                maxResults = maxResults,
                tag = "SA/PA"
            )
        }

        if (result.isNotEmpty()) return result

        // 0件 → 重心から大半径でリトライ
        val retryRadius = 150
        Log.w(TAG, "SA/PA 0件 → 重心から半径${retryRadius}kmでリトライ")
        return queryOverpass(
            query = buildSaPaQuery(centerLat, centerLon, retryRadius),
            centerLat = centerLat,
            centerLon = centerLon,
            placeType = PlaceType.SA_PA,
            defaultName = "サービスエリア",
            maxResults = maxResults,
            tag = "SA/PA(retry)"
        )
    }

    // ──────────────────────────────────────────
    //  駅 検索
    // ──────────────────────────────────────────

    /**
     * 駅を検索する。
     * - 参加者がいる場合 → 各人の出発地周辺を1リクエストでまとめて検索（30km）
     * - 0件なら重心から150km でリトライ
     */
    suspend fun searchStations(
        centerLat: Double,
        centerLon: Double,
        persons: List<Person> = emptyList(),
        radiusKm: Int = 80,
        maxResults: Int = 30
    ): List<MeetCandidate> {
        val result = if (persons.size >= 2) {
            val centers = persons.map { Pair(it.latitude, it.longitude) }
            queryOverpass(
                query = buildStationQueryMulti(centers, perPersonRadiusKm = 30),
                centerLat = centerLat,
                centerLon = centerLon,
                placeType = PlaceType.STATION,
                defaultName = "駅",
                maxResults = maxResults,
                tag = "駅(multi)"
            )
        } else {
            queryOverpass(
                query = buildStationQuery(centerLat, centerLon, radiusKm),
                centerLat = centerLat,
                centerLon = centerLon,
                placeType = PlaceType.STATION,
                defaultName = "駅",
                maxResults = maxResults,
                tag = "駅"
            )
        }

        if (result.isNotEmpty()) return result

        // 0件 → 重心から大半径でリトライ
        val retryRadius = 150
        Log.w(TAG, "駅 0件 → 重心から半径${retryRadius}kmでリトライ")
        return queryOverpass(
            query = buildStationQuery(centerLat, centerLon, retryRadius),
            centerLat = centerLat,
            centerLon = centerLon,
            placeType = PlaceType.STATION,
            defaultName = "駅",
            maxResults = maxResults,
            tag = "駅(retry)"
        )
    }

    // ──────────────────────────────────────────
    //  共通クエリ実行
    // ──────────────────────────────────────────

    private suspend fun queryOverpass(
        query: String,
        centerLat: Double,
        centerLon: Double,
        placeType: PlaceType,
        defaultName: String,
        maxResults: Int,
        tag: String
    ): List<MeetCandidate> = runCatching {
        Log.d(TAG, "[$tag] Overpassリクエスト送信\nquery=\n$query")
        val response = overpassApi.query(query)

        // Overpass がエラーを返した場合は remark にメッセージが入る
        if (response.remark != null) {
            Log.w(TAG, "[$tag] Overpass remark: ${response.remark}")
        }
        Log.d(TAG, "[$tag] レスポンス: ${response.elements.size}件")

        response.elements
            .mapNotNull { element ->
                val lat = element.effectiveLat ?: return@mapNotNull null
                val lon = element.effectiveLon ?: return@mapNotNull null
                MeetCandidate(
                    name = element.name.ifBlank { defaultName },
                    latitude = lat,
                    longitude = lon,
                    placeType = placeType,
                    address = buildAddress(element)
                )
            }
            .deduplicateByProximity(DEDUP_KM)
            .sortedBy { haversine(centerLat, centerLon, it.latitude, it.longitude) }
            .also { Log.d(TAG, "[$tag] 重複除去後: ${it.size}件") }
            .take(maxResults)
    }.getOrElse { e ->
        Log.e(TAG, "[$tag] 例外: ${e.javaClass.simpleName} — ${e.message}", e)
        emptyList()
    }

    // ──────────────────────────────────────────
    //  ユーティリティ
    // ──────────────────────────────────────────

    private fun List<MeetCandidate>.deduplicateByProximity(thresholdKm: Double): List<MeetCandidate> {
        val result = mutableListOf<MeetCandidate>()
        for (c in this) {
            if (result.none { haversine(it.latitude, it.longitude, c.latitude, c.longitude) < thresholdKm }) {
                result.add(c)
            }
        }
        return result
    }

    private fun buildAddress(element: OverpassElement): String {
        val tags = element.tags ?: return ""
        tags["addr:full"]?.let { return it }
        val parts = listOf(
            tags["addr:province"] ?: tags["addr:prefecture"] ?: "",
            tags["addr:city"] ?: tags["addr:county"] ?: "",
            tags["addr:suburb"] ?: tags["addr:quarter"] ?: "",
            tags["addr:street"] ?: "",
            tags["addr:housenumber"] ?: ""
        ).filter { it.isNotBlank() }
        return parts.joinToString("")
    }
}
