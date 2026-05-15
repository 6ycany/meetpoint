package com.example.meetpoint.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OverpassResponse(
    val elements: List<OverpassElement>
)

@JsonClass(generateAdapter = true)
data class OverpassCenter(
    val lat: Double,
    val lon: Double
)

@JsonClass(generateAdapter = true)
data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double?,
    val lon: Double?,
    val center: OverpassCenter?,
    val tags: Map<String, String>?
) {
    /** node は lat/lon 直接、way は center.lat/center.lon を使う */
    val effectiveLat: Double? get() = lat ?: center?.lat
    val effectiveLon: Double? get() = lon ?: center?.lon
    val name: String get() = tags?.get("name") ?: tags?.get("name:ja") ?: ""
}
