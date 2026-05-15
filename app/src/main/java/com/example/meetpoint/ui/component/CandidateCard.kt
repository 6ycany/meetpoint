package com.example.meetpoint.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.meetpoint.domain.model.FairnessGrade
import com.example.meetpoint.domain.model.MeetCandidate
import com.example.meetpoint.domain.model.PlaceType

@Composable
fun CandidateCard(
    rank: Int,
    candidate: MeetCandidate,
    personNames: List<String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (rank == 1) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rank == 1)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── ヘッダー（ランク・種別バッジ・名称・公平グレード） ──
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${rank}位",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        PlaceTypeBadge(candidate.placeType)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = candidate.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                GradeBadge(grade = candidate.fairnessGrade)
            }

            // ── 住所 ──
            val addressText = candidate.address.ifBlank {
                // 住所タグがない場合は座標を表示
                "${"%.4f".format(candidate.latitude)}, ${"%.4f".format(candidate.longitude)}"
            }
            Text(
                text = addressText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // ── 各参加者の移動距離 ──
            if (candidate.distancesKm.isNotEmpty()) {
                Text(
                    text = "各出発地からの距離",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                candidate.distancesKm.forEachIndexed { i, dist ->
                    val name = personNames.getOrElse(i) { "参加者${i + 1}" }
                    val distText = if (dist < 1.0) {
                        "${"%.0f".format(dist * 1000)} m"
                    } else {
                        "${"%.1f".format(dist)} km"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "  $name",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = distText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Googleマップで開くボタン ──
            Button(
                onClick = {
                    val uri = Uri.parse(
                        "google.navigation:q=${candidate.latitude},${candidate.longitude}&mode=d"
                    )
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        val webUri = Uri.parse(
                            "https://www.google.com/maps/search/?api=1&query=${candidate.latitude},${candidate.longitude}"
                        )
                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("🗺️  Googleマップで開く")
            }
        }
    }
}

@Composable
private fun PlaceTypeBadge(placeType: PlaceType) {
    val (text, color) = when (placeType) {
        PlaceType.STATION -> "🚆 駅" to Color(0xFF1565C0)
        PlaceType.SA_PA   -> "🛣️ SA/PA" to Color(0xFF2E7D32)
        PlaceType.POINT   -> "📍 中間地点" to Color(0xFF6A1B9A)
    }
    Badge(containerColor = color) {
        Text(text = text, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun GradeBadge(grade: FairnessGrade) {
    val (text, color) = when (grade) {
        FairnessGrade.A -> "公平 A" to Color(0xFF2E7D32)
        FairnessGrade.B -> "公平 B" to Color(0xFF558B2F)
        FairnessGrade.C -> "公平 C" to Color(0xFFF9A825)
        FairnessGrade.D -> "公平 D" to Color(0xFFC62828)
    }
    Badge(containerColor = color) {
        Text(text = text, color = Color.White)
    }
}
