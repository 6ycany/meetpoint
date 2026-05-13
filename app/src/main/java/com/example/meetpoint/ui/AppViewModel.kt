package com.example.meetpoint.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetpoint.data.local.entity.MemberEntity
import com.example.meetpoint.data.repository.GeoRepository
import com.example.meetpoint.data.repository.MemberRepository
import com.example.meetpoint.data.repository.PlaceRepository
import com.example.meetpoint.domain.model.MeetCandidate
import com.example.meetpoint.domain.model.Person
import com.example.meetpoint.domain.model.TravelMode
import com.example.meetpoint.domain.usecase.CalcMeetPointUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val geoRepository: GeoRepository,
    private val placeRepository: PlaceRepository,
    private val calcMeetPointUseCase: CalcMeetPointUseCase,
    memberRepository: MemberRepository
) : ViewModel() {

    // --- メンバー一覧（HomeScreenのメンバー選択に使用） ---
    val members: StateFlow<List<MemberEntity>> = memberRepository.getAllMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 入力状態 ---

    /**
     * @param latitude・longitude が非nullの場合はジオコーディング済み（メンバー選択時）
     * 住所を手動編集したら null にリセットして再ジオコーディングを行う
     */
    data class PersonInput(
        val name: String = "",
        val address: String = "",
        val latitude: Double? = null,
        val longitude: Double? = null
    ) {
        val hasCoordinates get() = latitude != null && longitude != null
    }

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
        data class Success(val candidates: List<MeetCandidate>, val persons: List<Person>) : UiState()
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

    /** 住所を手動入力したら座標キャッシュをリセット */
    fun updatePersonAddress(index: Int, address: String) {
        _personInputs.value = _personInputs.value.toMutableList().also {
            it[index] = it[index].copy(address = address, latitude = null, longitude = null)
        }
    }

    /** メンバー選択 — 座標はDBから取得済みなのでジオコーディング不要 */
    fun selectMember(index: Int, member: MemberEntity) {
        _personInputs.value = _personInputs.value.toMutableList().also {
            it[index] = PersonInput(
                name = member.name,
                address = member.address,
                latitude = member.latitude,
                longitude = member.longitude
            )
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

    fun setTravelMode(mode: TravelMode) { _travelMode.value = mode }

    fun resetResult() { _uiState.value = UiState.Idle }

    // --- 計算 ---

    fun calculate() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val inputs = _personInputs.value
            if (inputs.any { it.address.isBlank() }) {
                _uiState.value = UiState.Error("全員の出発地を入力してください")
                return@launch
            }

            // 座標キャッシュがあればそのまま使用、なければジオコーディング
            val persons = inputs.map { input ->
                if (input.hasCoordinates) {
                    Person(input.name, input.latitude!!, input.longitude!!, input.address)
                } else {
                    geoRepository.resolvePerson(input.name, input.address)
                }
            }
            if (persons.any { it == null }) {
                _uiState.value = UiState.Error("住所を認識できない入力があります。\n再度確認してください。")
                return@launch
            }
            val validPersons = persons.filterNotNull()

            val centroidResult = calcMeetPointUseCase(validPersons, emptyList())
            val center = centroidResult.first()

            val rawCandidates = when (_travelMode.value) {
                TravelMode.DRIVE   -> placeRepository.searchSaPa(center.latitude, center.longitude)
                TravelMode.TRANSIT -> placeRepository.searchStations(center.latitude, center.longitude)
            }

            val candidates = calcMeetPointUseCase(validPersons, rawCandidates, topN = 3)
            _uiState.value = UiState.Success(candidates, validPersons)
        }
    }
}
