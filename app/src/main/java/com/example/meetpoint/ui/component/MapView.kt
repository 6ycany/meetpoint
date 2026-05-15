package com.example.meetpoint.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.meetpoint.domain.model.MeetCandidate
import com.example.meetpoint.domain.model.Person
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/** 候補ランクに対応するマーカー色（1位:緑、2位:黄、3位:橙） */
private val CANDIDATE_HUES = listOf(
    BitmapDescriptorFactory.HUE_GREEN,
    BitmapDescriptorFactory.HUE_YELLOW,
    BitmapDescriptorFactory.HUE_ORANGE
)

/** 出発地マーカー色（青系） */
private const val PERSON_HUE = BitmapDescriptorFactory.HUE_AZURE

@Composable
fun MeetPointMapView(
    persons: List<Person>,
    candidates: List<MeetCandidate>,
    modifier: Modifier = Modifier
) {
    // 全ピン（出発地＋候補）を包含するカメラ位置を計算
    val allLatLngs = buildList {
        persons.forEach { add(LatLng(it.latitude, it.longitude)) }
        candidates.forEach { add(LatLng(it.latitude, it.longitude)) }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = calcInitialCamera(allLatLngs)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState
    ) {
        // ── 出発地マーカー（青）──
        persons.forEachIndexed { idx, person ->
            val markerState = remember(person.latitude, person.longitude) {
                MarkerState(position = LatLng(person.latitude, person.longitude))
            }
            Marker(
                state = markerState,
                title = person.name,
                snippet = person.address.ifBlank { null },
                icon = BitmapDescriptorFactory.defaultMarker(PERSON_HUE),
                zIndex = 1f
            )
        }

        // ── 候補地点マーカー（緑/黄/橙、ランク番号付きラベル）──
        candidates.forEachIndexed { idx, candidate ->
            val hue = CANDIDATE_HUES.getOrElse(idx) { BitmapDescriptorFactory.HUE_RED }
            val markerState = remember(candidate.latitude, candidate.longitude) {
                MarkerState(position = LatLng(candidate.latitude, candidate.longitude))
            }
            val rankLabel = when (idx) { 0 -> "🥇" ; 1 -> "🥈" ; else -> "🥉" }
            Marker(
                state = markerState,
                title = "$rankLabel ${candidate.name}",
                snippet = candidate.address.ifBlank {
                    "公平度: ${candidate.fairnessGrade} | ${"%.1f".format(candidate.fairnessScore * 100)}点"
                },
                icon = BitmapDescriptorFactory.defaultMarker(hue),
                zIndex = 2f
            )
        }
    }
}

/** 全ピンを包含するカメラ位置を計算する */
private fun calcInitialCamera(points: List<LatLng>): CameraPosition {
    if (points.isEmpty()) {
        return CameraPosition.fromLatLngZoom(LatLng(35.6762, 139.6503), 9f) // 東京デフォルト
    }
    if (points.size == 1) {
        return CameraPosition.fromLatLngZoom(points[0], 12f)
    }
    val bounds = points.fold(
        LatLngBounds.Builder()
    ) { builder, latLng -> builder.include(latLng) }.build()
    val center = bounds.center
    // zoom は後で CameraUpdateFactory.newLatLngBounds で更新する想定だが、
    // Compose Maps では初期値として center + zoom=9 を設定する
    return CameraPosition.fromLatLngZoom(center, 9f)
}
