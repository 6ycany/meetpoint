package com.example.meetpoint.data.repository

import com.example.meetpoint.data.remote.api.OverpassApi
import com.example.meetpoint.data.remote.api.buildSaPaQuery
import com.example.meetpoint.data.remote.api.buildStationQuery
import com.example.meetpoint.domain.model.MeetCandidate
import com.example.meetpoint.domain.model.PlaceType
import com.example.meetpoint.domain.usecase.haversine
import javax.inject.Inject

/**
 * SA/PA・駅候補を取得するリポジトリ
 */
class PlaceRepository @Inject constructor(
    private val overpassApi: OverpassApi
) {
    /**
     * 重心座標の周囲のSA/PAを取得する
     * @param centerLat  検索中心緯度
     * @param centerLon  検索中心経度
     * @param radiusKm   検索半径(km)
     * @param maxResults 最大取得件数
     * @return 重心から近い順の MeetCandidate リスト
     */
    suspend fun searchSaPa(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Int = 50,
        maxResults: Int = 10
    ): List<MeetCandidate> = runCatching {
        val query = buildSaPaQuery(centerLat, centerLon, radiusKm)
        val response = overpassApi.query(query)

        response.elements
            .filter { it.lat != null && it.lon != null }
            .map { element ->
                MeetCandidate(
                    name = element.name,
                    latitude = element.lat!!,
                    longitude = element.lon!!,
                    placeType = PlaceType.SA_PA
                )
            }
            .sortedBy { haversine(centerLat, centerLon, it.latitude, it.longitude) }
            .take(maxResults)
    }.getOrElse { emptyList() }

    /**
     * 重心座標の周囲の駅を取得する
     * @param centerLat  検索中心緯度
     * @param centerLon  検索中心経度
     * @param radiusKm   検索半径(km)
     * @param maxResults 最大取得件数
     * @return 重心から近い順の MeetCandidate リスト
     */
    suspend fun searchStations(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Int = 50,
        maxResults: Int = 10
    ): List<MeetCandidate> = runCatching {
        val query = buildStationQuery(centerLat, centerLon, radiusKm)
        val response = overpassApi.query(query)

        response.elements
            .filter { it.lat != null && it.lon != null }
            .map { element ->
                MeetCandidate(
                    name = element.name,
                    latitude = element.lat!!,
                    longitude = element.lon!!,
                    placeType = PlaceType.STATION
                )
            }
            .sortedBy { haversine(centerLat, centerLon, it.latitude, it.longitude) }
            .take(maxResults)
    }.getOrElse { emptyList() }
}
