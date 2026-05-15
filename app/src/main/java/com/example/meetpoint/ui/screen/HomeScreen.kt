package com.example.meetpoint.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meetpoint.domain.model.AppMode
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
    val appMode by viewModel.appMode.collectAsState()
    val destination by viewModel.destination.collectAsState()
    val alpha by viewModel.alpha.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val members by viewModel.members.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAdvanced by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (uiState) {
            is AppViewModel.UiState.Success -> onNavigateToResult()
            is AppViewModel.UiState.Error -> {
                snackbarHostState.showSnackbar((uiState as AppViewModel.UiState.Error).message)
                viewModel.resetResult()
            }
            else -> {}
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

            // ─── モード切替 ───
            Text("モード", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = appMode == AppMode.MEET_ONLY,
                    onClick = { viewModel.setAppMode(AppMode.MEET_ONLY) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("集合のみ") }
                )
                SegmentedButton(
                    selected = appMode == AppMode.WAYPOINT,
                    onClick = { viewModel.setAppMode(AppMode.WAYPOINT) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("合流→目的地") }
                )
            }

            // ─── 参加者入力 ───
            personInputs.forEachIndexed { index, input ->
                PersonInputCard(
                    index = index,
                    name = input.name,
                    address = input.address,
                    hasCoordinates = input.hasCoordinates,
                    members = members,
                    canRemove = personInputs.size > 2,
                    onNameChange = { viewModel.updatePersonName(index, it) },
                    onAddressChange = { viewModel.updatePersonAddress(index, it) },
                    onMemberSelect = { viewModel.selectMember(index, it) },
                    onRemove = { viewModel.removePerson(index) }
                )
            }

            if (personInputs.size < 4) {
                OutlinedButton(
                    onClick = { viewModel.addPerson() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("　参加者を追加")
                }
            }

            // ─── 目的地入力（モード2のみ） ───
            AnimatedVisibility(visible = appMode == AppMode.WAYPOINT) {
                OutlinedTextField(
                    value = destination.address,
                    onValueChange = { viewModel.updateDestinationAddress(it) },
                    label = { Text("目的地") },
                    placeholder = { Text("例：東京ディズニーランド") },
                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // ─── 移動手段 ───
            Text("移動手段", style = MaterialTheme.typography.titleSmall)
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

            // ─── 詳細設定（モード2のみ） ───
            AnimatedVisibility(visible = appMode == AppMode.WAYPOINT) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = { showAdvanced = !showAdvanced },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (showAdvanced) "▲ 詳細設定を閉じる" else "▼ 詳細設定（α/β調整）")
                    }
                    AnimatedVisibility(visible = showAdvanced) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "進捗重み α = ${"%.2f".format(alpha)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "公平性重み β = ${"%.2f".format(1f - alpha)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Slider(
                                value = alpha,
                                onValueChange = { viewModel.setAlpha(it) },
                                valueRange = 0f..1f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "α大→目的地に近い地点を優先 / α小→移動距離の公平性を優先",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

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
                    Text(
                        when (appMode) {
                            AppMode.MEET_ONLY -> "合流地点を探す"
                            AppMode.WAYPOINT  -> "合流地点を探す（目的地方向優先）"
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
