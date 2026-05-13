package com.example.meetpoint.data.repository

import com.example.meetpoint.BuildConfig
import com.example.meetpoint.data.remote.api.GoogleGeocodingApi
import com.example.meetpoint.domain.model.Person
import javax.inject.Inject

/**
 * 住所→座標変換リポジトリ
 */
class GeoRepository @Inject constructor(
    private val geocodingApi: GoogleGeocodingApi
) {
    /**
     * 住所文字列から Person を生成する
     * @param name    参加者名
     * @param address 住所文字列
     * @return 座標付き Person。ジオコーディング失敗時は null
     */
    suspend fun resolvePerson(name: String, address: String): Person? {
        return runCatching {
            val response = geocodingApi.geocode(
                address = address,
                key = BuildConfig.MAPS_API_KEY
            )
            if (response.status == "OK" && response.results.isNotEmpty()) {
                val location = response.results[0].geometry.location
                Person(
                    name = name,
                    latitude = location.lat,
                    longitude = location.lng,
                    address = response.results[0].formattedAddress
                )
            } else null
        }.getOrNull()
    }
}
