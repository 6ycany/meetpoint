package com.example.meetpoint.ui.screen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meetpoint.domain.model.TravelMode
import com.example.meetpoint.ui.AppViewModel
import com.example.meetpoint.ui.component.PersonInputCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToResult: () -> Unit
) {
    val personInputs by viewModel.personInputs.collectAsState()
    val travelMode by viewModel.travelMode.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 計算成功 → 結果画面へ遷移
    LaunchedEffect(uiState) {
        if (uiState is AppViewModel.UiState.Success) {
            onNavigateToResult()
        }
        if (uiState is AppViewModel.UiState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as AppViewModel.UiState.Error).message
            )
            viewModel.resetResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MeetPoint") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- 参加者入力カード ---
            personInputs.forEachIndexed { index, input ->
                PersonInputCard(
                    index = index,
                    name = input.name,
                    address = input.address,
                    canRemove = personInputs.size > 2,
                    onNameChange = { viewModel.updatePersonName(index, it) },
                    onAddressChange = { viewModel.updatePersonAddress(index, it) },
                    onRemove = { viewModel.removePerson(index) }
                )
            }

            // --- 参加者追加ボタン ---
            if (personInputs.size < 4) {
                OutlinedButton(
                    onClick = { viewModel.addPerson() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(text = "　参加者を追加")
                }
            }

            // --- 移動手段選択 ---
            Text(
                text = "移動手段",
                style = MaterialTheme.typography.titleSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = travelMode == TravelMode.DRIVE,
                    onClick = { viewModel.setTravelMode(TravelMode.DRIVE) },
                    label = { Text("🚗 車") }
                )
                FilterChip(
                    selected = travelMode == TravelMode.TRANSIT,
                    onClick = { viewModel.setTravelMode(TravelMode.TRANSIT) },
                    label = { Text("🚆 電車") }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // --- 計算ボタン ---
            val isLoading = uiState is AppViewModel.UiState.Loading
            Button(
                onClick = { viewModel.calculate() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = "合流地点を探す")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
