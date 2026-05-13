package com.example.meetpoint.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResult>,
    val status: String
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    @Json(name = "formatted_address") val formattedAddress: String,
    val geometry: GeocodingGeometry
)

@JsonClass(generateAdapter = true)
data class GeocodingGeometry(
    val location: GeocodingLocation
)

@JsonClass(generateAdapter = true)
data class GeocodingLocation(
    val lat: Double,
    val lng: Double
)
