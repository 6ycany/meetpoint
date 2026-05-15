package com.example.meetpoint.domain.usecase

import com.example.meetpoint.domain.model.FairnessGrade
import com.example.meetpoint.domain.model.MeetCandidate
import com.example.meetpoint.domain.model.Person
import com.example.meetpoint.domain.model.PlaceType
import javax.inject.Inject
import kotlin.math.pow

/**
 * モード2（合流→目的地）の合流地点計算ユースケース。
 *
 * totalScore = α × progress + β × fairness
 *   progress = 候補地点が「重心→目的地」方向にどれだけ進んでいるか（内積射影、0〜1）
 *   fairness = calcFairnessScore() — 変動係数ベースで [0,1] に正規化済み
 *   β = 1 - α
 *
 * ※ progress と fairness を同じ [0,1] スケールに揃えることで
 *   α/β スライダーが実際にランクに影響する。
 */
class CalcWaypointUseCase @Inject constructor() {

    operator fun invoke(
        persons: List<Person>,
        destination: Person,
        candidates: List<MeetCandidate> = emptyList(),
        alpha: Double = 0.5,
        topN: Int = 3
    ): List<MeetCandidate> {
        require(persons.size >= 2) { "参加者は2人以上必要です" }

        val beta = 1.0 - alpha
        val points = persons.map { Pair(it.latitude, it.longitude) }
        val (centerLat, centerLon) = weiszfeld(points)

        return if (candidates.isEmpty()) {
            // 候補なし → 重心をそのまま返す
            val distances = persons.map { haversine(it.latitude, it.longitude, centerLat, centerLon) }
            val fairness = calcFairnessScore(distances)
            listOf(
                MeetCandidate(
                    name = "最適合流地点",
                    latitude = centerLat,
                    longitude = centerLon,
                    placeType = PlaceType.POINT,
                    distancesKm = distances,
                    fairnessScore = fairness,
                    fairnessGrade = scoreToGrade(fairness)
                )
            )
        } else {
            candidates
                .map { candidate ->
                    scoreCandidate(candidate, persons, destination, centerLat, centerLon, alpha, beta)
                }
                .sortedByDescending { it.fairnessScore }
                .take(topN)
        }
    }

    private fun scoreCandidate(
        candidate: MeetCandidate,
        persons: List<Person>,
        destination: Person,
        centerLat: Double,
        centerLon: Double,
        alpha: Double,
        beta: Double
    ): MeetCandidate {
        val distances = persons.map { p ->
            haversine(p.latitude, p.longitude, candidate.latitude, candidate.longitude)
        }

        // --- 進捗スコア [0, 1] ---
        // 「重心 → 目的地」ベクトルへの、「重心 → 候補」ベクトルの射影率
        val vCandLat = candidate.latitude - centerLat
        val vCandLon = candidate.longitude - centerLon
        val vDestLat = destination.latitude - centerLat
        val vDestLon = destination.longitude - centerLon
        val dot = vCandLat * vDestLat + vCandLon * vDestLon
        val destLen2 = vDestLat.pow(2) + vDestLon.pow(2)
        val progress = if (destLen2 < 1e-10) 0.0 else (dot / destLen2).coerceIn(0.0, 1.0)

        // --- 公平スコア [0, 1] ---
        // calcFairnessScore は変動係数ベースで 0〜1 を返す（progress と同スケール）
        val fairness = calcFairnessScore(distances)

        // --- 合計スコア [0, 1] ---
        val totalScore = (alpha * progress + beta * fairness).coerceIn(0.0, 1.0)

        return candidate.copy(
            distancesKm = distances,
            fairnessScore = totalScore,
            fairnessGrade = scoreToGrade(fairness) // グレードは公平性のみで判定
        )
    }

    private fun scoreToGrade(score: Double): FairnessGrade = when {
        score >= 0.85 -> FairnessGrade.A
        score >= 0.70 -> FairnessGrade.B
        score >= 0.50 -> FairnessGrade.C
        else          -> FairnessGrade.D
    }
}
