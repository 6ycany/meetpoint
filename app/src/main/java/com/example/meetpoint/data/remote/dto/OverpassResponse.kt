package com.example.meetpoint.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OverpassResponse(
    /** Overpass がエラー時は elements フィールド自体が存在しないので default = emptyList() が必須 */
    val elements: List<OverpassElement> = emptyList(),
    /** タイムアウト・メモリ不足時などに Overpass がセットするエラーメッセージ */
    val remark: String? = null
)

@JsonClass(generateAdapter = true)
data class OverpassCenter(
    val lat: Double,
    val lon: Double
)

@JsonClass(generateAdapter = true)
data class OverpassElement(
    val type: String = "",
    val id: Long = 0L,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String>? = null
) {
    /** node は lat/lon 直接、way は center.lat/center.lon を使う */
    val effectiveLat: Double? get() = lat ?: center?.lat
    val effectiveLon: Double? get() = lon ?: center?.lon
    val name: String get() = tags?.get("name") ?: tags?.get("name:ja") ?: ""
}
