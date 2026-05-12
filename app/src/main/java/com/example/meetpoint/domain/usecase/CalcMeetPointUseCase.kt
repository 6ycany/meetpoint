package com.example.meetpoint.domain.usecase

import com.example.meetpoint.domain.model.FairnessGrade
import com.example.meetpoint.domain.model.MeetCandidate
import com.example.meetpoint.domain.model.Person
import com.example.meetpoint.domain.model.PlaceType
import javax.inject.Inject

/**
 * モード1（集合のみ）の合流地点計算ユースケース
 *
 * 候補地点リストが与えられた場合 → 各候補のスコアを算出して上位3件返す
 * 候補地点リストが空の場合      → ワイツェンベック重心を純粋地点として返す
 */
class CalcMeetPointUseCase @Inject constructor() {

    /**
     * @param persons    参加者リスト（2人以上）
     * @param candidates 候補地点リスト（SA/PA・駅など。空なら重心のみ返す）
     * @param topN       返す候補数
     * @return スコア順の MeetCandidate リスト（最大 topN 件）
     */
    operator fun invoke(
        persons: List<Person>,
        candidates: List<MeetCandidate> = emptyList(),
        topN: Int = 3
    ): List<MeetCandidate> {
        require(persons.size >= 2) { "参加者は2人以上必要です" }

        return if (candidates.isEmpty()) {
            // 候補なし → ワイツェンベック重心を返す
            listOf(calcCentroid(persons))
        } else {
            // 候補あり → 各候補の公平スコアを計算してソート
            candidates
                .map { candidate -> scoredCandidate(candidate, persons) }
                .sortedByDescending { it.fairnessScore }
                .take(topN)
        }
    }

    /** ワイツェンベック重心を計算して MeetCandidate として返す */
    private fun calcCentroid(persons: List<Person>): MeetCandidate {
        val points = persons.map { Pair(it.latitude, it.longitude) }
        val (lat, lon) = weiszfeld(points)
        val distances = persons.map { haversine(it.latitude, it.longitude, lat, lon) }
        val score = calcFairnessScore(distances)
        return MeetCandidate(
            name = "最適合流地点",
            latitude = lat,
            longitude = lon,
            placeType = PlaceType.POINT,
            distancesKm = distances,
            fairnessScore = score,
            fairnessGrade = scoreToGrade(score)
        )
    }

    /** 候補地点に対して各参加者の距離・公平スコアを付与する */
    private fun scoredCandidate(candidate: MeetCandidate, persons: List<Person>): MeetCandidate {
        val distances = persons.map {
            haversine(it.latitude, it.longitude, candidate.latitude, candidate.longitude)
        }
        val score = calcFairnessScore(distances)
        return candidate.copy(
            distancesKm = distances,
            fairnessScore = score,
            fairnessGrade = scoreToGrade(score)
        )
    }

    private fun scoreToGrade(score: Double): FairnessGrade = when {
        score >= 0.85 -> FairnessGrade.A
        score >= 0.70 -> FairnessGrade.B
        score >= 0.50 -> FairnessGrade.C
        else          -> FairnessGrade.D
    }
}
