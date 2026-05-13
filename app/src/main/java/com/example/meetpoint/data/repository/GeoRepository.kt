package com.example.meetpoint.data.repository

import android.util.Log
import com.example.meetpoint.BuildConfig
import com.example.meetpoint.data.remote.api.GoogleGeocodingApi
import com.example.meetpoint.domain.model.Person
import javax.inject.Inject

class GeoRepository @Inject constructor(
    private val geocodingApi: GoogleGeocodingApi
) {
    companion object {
        private const val TAG = "GeoRepository"
    }

    /**
     * 住所文字列から Person を生成する
     * @return 成功時は Person、失敗時は null（ログにエラー詳細を出力）
     */
    suspend fun resolvePerson(name: String, address: String): Person? {
        if (BuildConfig.MAPS_API_KEY.isBlank()) {
            Log.e(TAG, "MAPS_API_KEY が未設定です")
            return null
        }
        return try {
            val response = geocodingApi.geocode(
                address = address,
                key = BuildConfig.MAPS_API_KEY
            )
            when {
                response.status == "OK" && response.results.isNotEmpty() -> {
                    val location = response.results[0].geometry.location
                    Log.d(TAG, "Geocoding成功: $address → (${location.lat}, ${location.lng})")
                    Person(
                        name = name,
                        latitude = location.lat,
                        longitude = location.lng,
                        address = response.results[0].formattedAddress
                    )
                }
                response.status == "ZERO_RESULTS" -> {
                    Log.w(TAG, "住所が見つかりません: $address")
                    null
                }
                response.status == "REQUEST_DENIED" -> {
                    Log.e(TAG, "APIキーが拒否されました。課金設定・制限を確認してください。status=${response.status}")
                    null
                }
                else -> {
                    Log.e(TAG, "Geocodingエラー: status=${response.status}, address=$address")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoding例外: address=$address, error=${e.message}", e)
            null
        }
    }
}
