package com.example.meetpoint.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meetpoint.ui.AppViewModel
import com.example.meetpoint.ui.component.CandidateCard
import com.example.meetpoint.ui.component.MeetPointMapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val personInputs by viewModel.personInputs.collectAsState()

    val success = uiState as? AppViewModel.UiState.Success ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("合流地点の候補") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetResult()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
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
        ) {
            // --- 地図（上半分） ---
            MeetPointMapView(
                persons = success.persons,
                candidates = success.candidates,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )

            // --- 候補カードリスト（下半分） ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "上位 ${success.candidates.size} 件",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                success.candidates.forEachIndexed { index, candidate ->
                    CandidateCard(
                        rank = index + 1,
                        candidate = candidate,
                        personNames = personInputs.map { it.name }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
