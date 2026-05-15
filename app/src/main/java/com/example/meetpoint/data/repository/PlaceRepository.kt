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

/** 出発地から近すぎる候補を除外する最小距離（km） */
private const val MIN_DIST_FROM_PERSON_KM = 0.3

class PlaceRepository @Inject constructor(
    private val overpassApi: OverpassApi
) {
    /**
     * SA/PA候補を取得する。0件なら1.5倍半径でリトライ（SPEC準拠）。
     * 出発地から近すぎる候補は除外。
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
            .filter { candidate -> isFarEnoughFromPersons(candidate, persons) }
            .sortedBy { haversine(centerLat, centerLon, it.latitude, it.longitude) }
            .take(maxResults)
    }.getOrElse { e ->
        Log.e(TAG, "SA/PA検索エラー: ${e.message}", e)
        emptyList()
    }

    /**
     * 駅候補を取得する。0件なら1.5倍半径でリトライ（SPEC準拠）。
     * 出発地から近すぎる候補は除外。
     */
    suspend fun searchStations(
        centerLat: Double,
        centerLon: Double,
        persons: List<Person> = emptyList(),
        radiusKm: Int = 80,
        maxResults: Int = 30
    ): List<MeetCandidate> {
        val result = queryStations(centerLat, centerLon, persons, radiusKm, maxResults)
        if (result.isNotEmpty()) return result

        val retryRadius = (radiusKm * 1.5).toInt()
        Log.w(TAG, "駅 0件 → 半径を ${radiusKm}km から ${retryRadius}km に拡大してリトライ")
        return queryStations(centerLat, centerLon, persons, retryRadius, maxResults)
    }

    private suspend fun queryStations(
        centerLat: Double,
        centerLon: Double,
        persons: List<Person>,
        radiusKm: Int,
        maxResults: Int
    ): List<MeetCandidate> = runCatching {
        val query = buildStationQuery(centerLat, centerLon, radiusKm)
        Log.d(TAG, "駅検索: 半径=${radiusKm}km, center=($centerLat, $centerLon)")
        val response = overpassApi.query(query)
        Log.d(TAG, "駅 レスポンス件数: ${response.elements.size}")

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
            .filter { candidate -> isFarEnoughFromPersons(candidate, persons) }
            .also { Log.d(TAG, "フィルタ後の駅件数: ${it.size}") }
            .sortedBy { haversine(centerLat, centerLon, it.latitude, it.longitude) }
            .take(maxResults)
    }.getOrElse { e ->
        Log.e(TAG, "駅検索エラー: ${e.message}", e)
        emptyList()
    }

    /** OSM タグから住所文字列を組み立てる */
    private fun buildAddress(element: OverpassElement): String {
        val tags = element.tags ?: return ""
        // addr:full が最優先
        tags["addr:full"]?.let { return it }
        // 都道府県 + 市区町村 + 番地で組み立て
        val prefecture = tags["addr:province"] ?: tags["addr:prefecture"] ?: ""
        val city = tags["addr:city"] ?: tags["addr:county"] ?: ""
        val suburb = tags["addr:suburb"] ?: tags["addr:quarter"] ?: ""
        val street = tags["addr:street"] ?: ""
        val housenumber = tags["addr:housenumber"] ?: ""
        val parts = listOf(prefecture, city, suburb, street, housenumber).filter { it.isNotBlank() }
        return parts.joinToString("")
    }

    /** 候補地点がいずれかの出発地から十分離れているかチェック */
    private fun isFarEnoughFromPersons(candidate: MeetCandidate, persons: List<Person>): Boolean {
        if (persons.isEmpty()) return true
        return persons.all { person ->
            haversine(person.latitude, person.longitude, candidate.latitude, candidate.longitude) >= MIN_DIST_FROM_PERSON_KM
        }
    }
}
