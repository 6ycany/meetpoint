package com.example.meetpoint.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetpoint.data.repository.GeoRepository
import com.example.meetpoint.data.repository.PlaceRepository
import com.example.meetpoint.domain.model.MeetCandidate
import com.example.meetpoint.domain.model.Person
import com.example.meetpoint.domain.model.TravelMode
import com.example.meetpoint.domain.usecase.CalcMeetPointUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val geoRepository: GeoRepository,
    private val placeRepository: PlaceRepository,
    private val calcMeetPointUseCase: CalcMeetPointUseCase
) : ViewModel() {

    // --- 入力状態 ---

    data class PersonInput(
        val name: String = "",
        val address: String = ""
    )

    private val _personInputs = MutableStateFlow(
        listOf(PersonInput("Aさん"), PersonInput("Bさん"))
    )
    val personInputs: StateFlow<List<PersonInput>> = _personInputs.asStateFlow()

    private val _travelMode = MutableStateFlow(TravelMode.DRIVE)
    val travelMode: StateFlow<TravelMode> = _travelMode.asStateFlow()

    // --- UI状態 ---

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(
            val candidates: List<MeetCandidate>,
            val persons: List<Person>
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // --- 入力操作 ---

    fun updatePersonName(index: Int, name: String) {
        _personInputs.value = _personInputs.value.toMutableList().also {
            it[index] = it[index].copy(name = name)
        }
    }

    fun updatePersonAddress(index: Int, address: String) {
        _personInputs.value = _personInputs.value.toMutableList().also {
            it[index] = it[index].copy(address = address)
        }
    }

    fun addPerson() {
        if (_personInputs.value.size >= 4) return
        val label = listOf("A", "B", "C", "D")[_personInputs.value.size]
        _personInputs.value = _personInputs.value + PersonInput("${label}さん")
    }

    fun removePerson(index: Int) {
        if (_personInputs.value.size <= 2) return
        _personInputs.value = _personInputs.value.toMutableList().also { it.removeAt(index) }
    }

    fun setTravelMode(mode: TravelMode) {
        _travelMode.value = mode
    }

    fun resetResult() {
        _uiState.value = UiState.Idle
    }

    // --- 計算 ---

    fun calculate() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            // 1. 入力バリデーション
            val inputs = _personInputs.value
            if (inputs.any { it.address.isBlank() }) {
                _uiState.value = UiState.Error("全員の出発地を入力してください")
                return@launch
            }

            // 2. 住所→座標（ジオコーディング）
            val persons = inputs.map { input ->
                geoRepository.resolvePerson(input.name, input.address)
            }
            if (persons.any { it == null }) {
                _uiState.value = UiState.Error("住所を認識できない入力があります。\n再度確認してください。")
                return@launch
            }
            val validPersons = persons.filterNotNull()

            // 3. ワイツェンベック重心を計算（候補検索の中心点として使用）
            val centroidResult = calcMeetPointUseCase(validPersons, emptyList())
            val center = centroidResult.first()

            // 4. 移動手段に応じた候補地点を取得
            val rawCandidates = when (_travelMode.value) {
                TravelMode.DRIVE ->
                    placeRepository.searchSaPa(center.latitude, center.longitude)
                TravelMode.TRANSIT ->
                    placeRepository.searchStations(center.latitude, center.longitude)
            }

            // 5. 候補スコアリング（候補なし→重心のみ）
            val candidates = calcMeetPointUseCase(validPersons, rawCandidates, topN = 3)

            _uiState.value = UiState.Success(candidates, validPersons)
        }
    }
}
