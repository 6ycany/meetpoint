package com.example.meetpoint.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OverpassResponse(
    val elements: List<OverpassElement>
)

@JsonClass(generateAdapter = true)
data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double?,
    val lon: Double?,
    val tags: Map<String, String>?
) {
    val name: String get() = tags?.get("name") ?: tags?.get("name:ja") ?: "名称不明"
}
