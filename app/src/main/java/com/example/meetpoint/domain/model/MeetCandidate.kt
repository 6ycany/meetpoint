package com.example.meetpoint.domain.model

/**
 * 合流候補地点
 * @param name          場所名（例: "海老名SA"）
 * @param latitude      緯度
 * @param longitude     経度
 * @param placeType     場所の種別
 * @param distancesKm   各参加者からの距離(km)リスト（Person順に対応）
 * @param fairnessScore 公平スコア（0.0〜1.0、高いほど公平）
 * @param fairnessGrade 公平ランク（A〜D）
 */
data class MeetCandidate(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val placeType: PlaceType,
    val address: String = "",          // 住所文字列（Overpassタグから生成）
    val distancesKm: List<Double> = emptyList(),
    val fairnessScore: Double = 0.0,
    val fairnessGrade: FairnessGrade = FairnessGrade.C
)

enum class FairnessGrade { A, B, C, D }
