package com.example.meetpoint.data.repository

import android.util.Log
import com.example.meetpoint.data.remote.api.OverpassApi
import com.example.meetpoint.data.remote.api.buildSaPaQuery
import com.example.meetpoint.data.remote.api.buildStationQuery
import com.example.meetpoint.data.remote.dto.OverpassElement
import com.example.meetpoint.domain.model.MeetCandidate
import com.example.meetpoint.domain.model.Person
import com.example.meetpoint.domain.model.PlaceType
import com.example.meetpoint.domain.usecase.haversine
import javax.inject.Inject

private const val TAG = "PlaceRepository"

/**
 * 出発地から近すぎる候補を除外する最小距離（km）。
 * 50m 未満 = GPS 誤差レベル。完全に同地点の場合のみ除外。
 */
private const val MIN_DIST_FROM_PERSON_KM = 0.05

/** 重複駅をまとめる近接閾値（km）。この距離以内は同一駅とみなす */
private const val STATION_DEDUP_KM = 0.3

class PlaceRepository @Inject constructor(
    private val overpassApi: OverpassApi
) {

    // ─────────────────────────────────────────────
    // SA/PA 検索
    // ─────────────────────────────────────────────

    /**
     * 重心周辺の SA/PA を取得する。
     * 0件なら 1.5 倍半径でリトライ（SPEC準拠）。
     */
    suspend fun searchSaPa(
        centerLat: Double,
        centerLon: Double,
        persons: List<Person> = emptyList(),
        radiusKm: Int = 50,
        maxResults: Int = 10
    ): List<MeetCandidate> {
        val result = querySaPa(centerLat, centerLon, persons, radiusKm, maxResults)
        if (result.isNotEmpty()) return result

        val retryRadius = (radiusKm * 1.5).toInt()
        Log.w(TAG, "SA/PA 0件 → 半径を ${radiusKm}km から ${retryRadius}km に拡大してリトライ")
        return querySaPa(centerLat, centerLon, persons, retryRadius, maxResults)
    }

    private suspend fun querySaPa(
        centerLat: Double,
        centerLon: Double,
        persons: List<Person>,
        radiusKm: Int,
        maxResults: Int
    ): List<MeetCandidate> = runCatching {
        val query = buildSaPaQuery(centerLat, centerLon, radiusKm)
        Log.d(TAG, "SA/PA検索: 半径=${radiusKm}km, center=($centerLat, $centerLon)")
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
                    placeType = PlaceType.SA_PA,
                    address = buildAddress(element)
                )
            }
            .filter { isFarEnoughFromPersons(it, persons) }
            .sortedBy { haversine(centerLat, centerLon, it.latitude, it.longitude) }
            .take(maxResults)
    }.getOrElse { e ->
        Log.e(TAG, "SA/PA検索エラー: ${e.message}", e)
        emptyList()
    }

    // ─────────────────────────────────────────────
    // 駅検索
    // ─────────────────────────────────────────────

    /**
     * 各参加者の周辺駅を個別に検索してマージする。
     *
     * 重心1点から検索する方式だと Overpass が0件を返しやすいため、
     * 各人の出発地周辺（半径30km）を個別に検索して重複除去後に集約する。
     * 0件なら半径を50kmに広げてリトライ。
     */
    suspend fun searchStations(
        centerLat: Double,
        centerLon: Double,
        persons: List<Person> = emptyList(),
        radiusKm: Int = 80,          // 後方互換のため残す（centroid検索用）
        maxResults: Int = 30
    ): List<MeetCandidate> {
        return if (persons.isNotEmpty()) {
            searchStationsNearPersons(persons, centerLat, centerLon, maxResults)
        } else {
            // persons が空なら従来通り centroid から検索
            val result = queryStations(centerLat, centerLon, persons, radiusKm, maxResults)
            if (result.isNotEmpty()) result
            else {
                val retryRadius = (radiusKm * 1.5).toInt()
                Log.w(TAG, "駅 0件 → 半径を ${radiusKm}km から ${retryRadius}km に拡大してリトライ")
                queryStations(centerLat, centerLon, persons, retryRadius, maxResults)
            }
        }
    }

    /**
     * 各参加者の周辺駅を個別に検索して重複除去し、重心に近い順でソートする。
     *
     * 1. 各人の周辺 perPersonRadiusKm 以内の駅を取得
     * 2. 全結果を合算して近接重複（0.3km以内）を除去
     * 3. 出発地に近すぎる（0.05km未満）候補を除外
     * 4. 重心からの距離でソートして返す
     */
    private suspend fun searchStationsNearPersons(
        persons: List<Person>,
        centerLat: Double,
        centerLon: Double,
        maxResults: Int,
        perPersonRadiusKm: Int = 30
    ): List<MeetCandidate> {
        val allStations = mutableListOf<MeetCandidate>()

        for (person in persons) {
            val result = queryStations(
                centerLat = person.latitude,
                centerLon = person.longitude,
                persons = emptyList(), // 個別検索では近接フィルタを後でまとめてかける
                radiusKm = perPersonRadiusKm,
                maxResults = 30
            )
            Log.d(TAG, "${person.name}周辺の駅: ${result.size}件 (半径${perPersonRadiusKm}km)")
            allStations.addAll(result)
        }

        if (allStations.isEmpty()) {
            // 全員の周辺でも0件 → 重心から広域検索
            Log.w(TAG, "全参加者周辺で駅0件 → 重心から半径100kmで再検索")
            allStations.addAll(
                queryStations(centerLat, centerLon, emptyList(), 100, 50)
            )
        }

        return allStations
            .deduplicateByProximity(STATION_DEDUP_KM)
            .filter { isFarEnoughFromPersons(it, persons) }
            .sortedBy { haversine(centerLat, centerLon, it.latitude, it.longitude) }
            .also { Log.d(TAG, "最終的な駅候補: ${it.size}件") }
            .take(maxResults)
    }

    private suspend fun queryStations(
        centerLat: Double,
        centerLon: Double,
        persons: List<Person>,
        radiusKm: Int,
        maxResults: Int
    ): List<MeetCandidate> = runCatching {
        val query = buildStationQuery(centerLat, centerLon, radiusKm)
        Log.d(TAG, "駅クエリ実行: 半径=${radiusKm}km, center=($centerLat, $centerLon)")
        val response = overpassApi.query(query)
        Log.d(TAG, "駅レスポンス: ${response.elements.size}件")

        response.elements
            .filter { it.effectiveLat != null && it.effectiveLon != null }
            .map { element ->
                MeetCandidate(
                    name = element.name.ifBlank { "駅" },
                    latitude = element.effectiveLat!!,
                    longitude = element.effectiveLon!!,
                    placeType = PlaceType.STATION,
                    address = buildAddress(element)
                )
            }
            .filter { isFarEnoughFromPersons(it, persons) }
            .take(maxResults)
    }.getOrElse { e ->
        Log.e(TAG, "駅クエリエラー: ${e.message}", e)
        emptyList()
    }

    // ─────────────────────────────────────────────
    // ユーティリティ
    // ─────────────────────────────────────────────

    /** 候補リストから近接重複を除去する（greedy approach） */
    private fun List<MeetCandidate>.deduplicateByProximity(thresholdKm: Double): List<MeetCandidate> {
        val result = mutableListOf<MeetCandidate>()
        for (candidate in this) {
            val isDuplicate = result.any { existing ->
                haversine(existing.latitude, existing.longitude, candidate.latitude, candidate.longitude) < thresholdKm
            }
            if (!isDuplicate) result.add(candidate)
        }
        return result
    }

    /** 候補がいずれかの出発地から十分に離れているかチェック */
    private fun isFarEnoughFromPersons(candidate: MeetCandidate, persons: List<Person>): Boolean {
        if (persons.isEmpty()) return true
        return persons.all { person ->
            haversine(person.latitude, person.longitude, candidate.latitude, candidate.longitude) >= MIN_DIST_FROM_PERSON_KM
        }
    }

    /** OSM タグから住所文字列を組み立てる */
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
