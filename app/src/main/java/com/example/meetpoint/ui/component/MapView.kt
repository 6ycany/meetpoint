package com.example.meetpoint.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.meetpoint.domain.model.MeetCandidate
import com.example.meetpoint.domain.model.Person
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MeetPointMapView(
    persons: List<Person>,
    candidates: List<MeetCandidate>,
    modifier: Modifier = Modifier
) {
    // 最初の候補地点（または重心）をカメラ初期位置に
    val initialTarget = candidates.firstOrNull()
        ?.let { LatLng(it.latitude, it.longitude) }
        ?: persons.firstOrNull()
            ?.let { LatLng(it.latitude, it.longitude) }
        ?: LatLng(35.6762, 139.6503) // デフォルト: 東京

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialTarget, 9f)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState
    ) {
        // 出発地マーカー
        persons.forEach { person ->
            val state = remember(person) {
                MarkerState(position = LatLng(person.latitude, person.longitude))
            }
            Marker(
                state = state,
                title = person.name,
                snippet = person.address
            )
        }

        // 候補地点マーカー（ランク付き）
        candidates.forEachIndexed { index, candidate ->
            val state = remember(candidate) {
                MarkerState(position = LatLng(candidate.latitude, candidate.longitude))
            }
            Marker(
                state = state,
                title = "${index + 1}位: ${candidate.name}",
                snippet = "公平度: ${candidate.fairnessGrade}"
            )
        }
    }
}
