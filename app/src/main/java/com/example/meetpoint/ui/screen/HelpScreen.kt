package com.example.meetpoint.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("使い方") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // アプリ概要
            SectionHeader(text = "MeetPoint とは？")
            Text(
                text = "複数人で待ち合わせをするとき、全員の移動距離が公平になる合流地点を自動で計算するアプリです。",
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider()

            // STEP 1
            StepCard(
                step = 1,
                title = "メンバーを登録する（任意）",
                description = "「メンバー」タブで自分や友人の自宅住所を事前登録しておくと、毎回入力する手間が省けます。",
                mockup = { MemberTabMockup() }
            )

            // STEP 2
            StepCard(
                step = 2,
                title = "参加者と出発地を入力する",
                description = "「実行」タブで参加者を設定します。\n登録済みメンバーは「選択」ボタンから選べます。\n手入力もできます（住所・駅名・ランドマーク名）。",
                mockup = { HomeInputMockup() }
            )

            // STEP 3
            StepCard(
                step = 3,
                title = "移動手段を選んで実行",
                description = "「🚗 車」→ 高速道路SA/PAを候補に含めます。\n「🚆 電車」→ 最寄り駅を候補に含めます。\n\n「合流地点を探す」ボタンを押すと計算が始まります。",
                mockup = { TravelModeMockup() }
            )

            // STEP 4
            StepCard(
                step = 4,
                title = "結果を確認する",
                description = "公平度の高い順に最大3件の候補地点が表示されます。\n公平グレード（A〜D）は移動距離のばらつきを表します。\nAに近いほど全員の移動距離が均等です。",
                mockup = { ResultMockup() }
            )

            // STEP 5
            StepCard(
                step = 5,
                title = "ナビを起動する",
                description = "各候補カードの「Googleマップで開く」ボタンを押すと、そのままナビゲーションを開始できます。",
                mockup = { NavButtonMockup() }
            )

            HorizontalDivider()

            // 住所の入力形式
            SectionHeader(text = "📍 住所の入力形式")
            AddressTipsCard()

            HorizontalDivider()

            // トラブルシューティング
            SectionHeader(text = "⚠️ うまくいかないときは")
            TroubleShootCard()

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---- ヘルパーコンポーザブル ----

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun StepCard(
    step: Int,
    title: String,
    description: String,
    mockup: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$step",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(text = description, style = MaterialTheme.typography.bodySmall)
            mockup()
        }
    }
}

// ---- 模式図コンポーザブル ----

@Composable
private fun PhoneMockup(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, Color.Gray, RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(8.dp)
    ) {
        Column {
            // ステータスバー模式
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("MeetPoint", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun MockupTab(label: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.LightGray,
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = if (selected) Color.White else Color.DarkGray, fontSize = 11.sp)
    }
}

@Composable
private fun MockupField(label: String, value: String = "", highlight: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 9.sp, color = Color.Gray)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (highlight) 2.dp else 1.dp,
                    color = if (highlight) MaterialTheme.colorScheme.primary else Color.Gray,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(6.dp)
        ) {
            Text(value.ifEmpty { "　" }, fontSize = 11.sp, color = if (value.isEmpty()) Color.LightGray else Color.Black)
        }
    }
}

@Composable
private fun MockupButton(label: String, filled: Boolean = true) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (filled) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (filled) Color.White else MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MemberTabMockup() {
    PhoneMockup {
        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MockupTab("実行", false)
                MockupTab("メンバー", true)
                MockupTab("ヘルプ", false)
            }
            Spacer(Modifier.height(4.dp))
            // メンバーカード
            listOf("太郎　　東京都渋谷区…", "花子　　神奈川県横浜市…").forEach { name ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(6.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, fontSize = 11.sp)
                        Row {
                            Text("✏️", fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                            Text("🗑️", fontSize = 14.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun HomeInputMockup() {
    PhoneMockup {
        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                MockupTab("実行", true)
                MockupTab("メンバー", false)
                MockupTab("ヘルプ", false)
            }
            // 参加者カード1
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF81C784), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("参加者 1", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) { Text("👤 選択", color = Color.White, fontSize = 9.sp) }
                    }
                    MockupField("名前", "太郎")
                    MockupField("出発地", "東京都渋谷区渋谷1-1-1", highlight = true)
                    Text("✓ メンバー登録済み（ジオコーディング不要）", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            // 参加者カード2
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(6.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("参加者 2", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    MockupField("名前", "花子")
                    MockupField("出発地", "新宿駅")
                }
            }
        }
    }
}

@Composable
private fun TravelModeMockup() {
    PhoneMockup {
        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("移動手段", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) { Text("🚗 車", color = Color.White, fontSize = 12.sp) }
                Box(
                    modifier = Modifier
                        .border(1.dp, Color.Gray, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) { Text("🚆 電車", fontSize = 12.sp) }
            }
            MockupButton("合流地点を探す")
        }
    }
}

@Composable
private fun ResultMockup() {
    PhoneMockup {
        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // 地図エリア
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color(0xFFDCEDC8), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🗺️", fontSize = 24.sp)
                    Text("地図", fontSize = 10.sp, color = Color.DarkGray)
                }
            }
            // 候補カード
            listOf(
                Triple("1位", "海老名SA", "公平 A"),
                Triple("2位", "厚木PA", "公平 B"),
                Triple("3位", "綾瀬スマートIC", "公平 B")
            ).forEachIndexed { index, (rank, name, grade) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (index == 0) Color(0xFFE3F2FD) else Color.White,
                            RoundedCornerShape(6.dp)
                        )
                        .border(1.dp, if (index == 0) Color(0xFF1976D2) else Color.LightGray, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(rank, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (index == 0) Color(0xFF2E7D32) else Color(0xFF558B2F),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) { Text(grade, color = Color.White, fontSize = 9.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavButtonMockup() {
    PhoneMockup {
        Column(modifier = Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE3F2FD), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF1976D2), RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("1位　海老名SA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF2E7D32), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) { Text("公平 A", color = Color.White, fontSize = 9.sp) }
                    }
                    Text("  太郎: 45.2 km　花子: 43.8 km", fontSize = 10.sp, color = Color.Gray)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF546E7A), RoundedCornerShape(6.dp))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Googleマップで開く", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Text(
                "↑ このボタンでGoogleマップのナビが起動します",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AddressTipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("✅ 使える入力形式", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

            val okExamples = listOf(
                "東京都渋谷区渋谷1-1-1" to "都道府県から始まる完全な住所",
                "神奈川県横浜市港北区日吉1-1" to "番地まで含めた住所",
                "新宿駅" to "駅名",
                "東京タワー" to "有名なランドマーク名",
                "渋谷スクランブル交差点" to "有名な場所の名称"
            )
            okExamples.forEach { (example, desc) ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("　• ", fontSize = 12.sp)
                    Column {
                        Text(example, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(desc, fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("❌ 認識されにくい形式", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)

            val ngExamples = listOf(
                "砂子2-6-42" to "都道府県・市区町村が省略されている",
                "実家" to "固有名詞でない通称",
                "会社" to "固有名詞でない通称"
            )
            ngExamples.forEach { (example, desc) ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("　• ", fontSize = 12.sp)
                    Column {
                        Text(example, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                        Text(desc, fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun TroubleShootCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TroubleItem(
                problem = "「住所を認識できない」と表示される",
                solutions = listOf(
                    "都道府県から始まる完全な住所を入力してください",
                    "Google Cloud ConsoleでGeocodingAPIが有効になっているか確認",
                    "Google Cloudの課金設定が完了しているか確認（無料枠あり）",
                    "APIキーの制限が「なし」になっているか確認"
                )
            )
            HorizontalDivider(color = Color(0xFFEF9A9A))
            TroubleItem(
                problem = "地図が表示されない（グレーの画面）",
                solutions = listOf(
                    "Maps SDK for Android がGoogle Cloudで有効になっているか確認",
                    "GitHub SecretsにMAPS_API_KEYが正しく設定されているか確認"
                )
            )
            HorizontalDivider(color = Color(0xFFEF9A9A))
            TroubleItem(
                problem = "SA/PAや駅が見つからない",
                solutions = listOf(
                    "出発地が非常に近い場合は候補が少なくなることがあります",
                    "「最適合流地点」（純粋な計算上の中間地点）が表示される場合があります"
                )
            )
        }
    }
}

@Composable
private fun TroubleItem(problem: String, solutions: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("❓ $problem", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Color(0xFFC62828))
        solutions.forEach { solution ->
            Row(verticalAlignment = Alignment.Top) {
                Text("　→ ", fontSize = 11.sp, color = Color.DarkGray)
                Text(solution, fontSize = 11.sp, color = Color.DarkGray)
            }
        }
    }
}
