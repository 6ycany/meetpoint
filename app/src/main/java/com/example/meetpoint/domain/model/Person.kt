package com.example.meetpoint.domain.model

/**
 * 出発地点を持つ参加者
 * @param name      表示名（例: "Aさん"）
 * @param latitude  出発地の緯度
 * @param longitude 出発地の経度
 * @param address   表示用住所文字列
 */
data class Person(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String = ""
)
