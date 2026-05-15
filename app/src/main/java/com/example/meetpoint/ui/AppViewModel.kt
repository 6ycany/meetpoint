package com.example.meetpoint.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetpoint.data.local.entity.MemberEntity
import com.example.meetpoint.data.repository.GeoRepository
import com.example.meetpoint.data.repository.MemberRepository
import com.example.meetpoint.data.repository.PlaceRepository
import com.example.meetpoint.domain.model.AppMode
import com.example.meetpoint.domain.model.MeetCandidate
import com.example.meetpoint.domain.model.Person
import com.example.meetpoint.domain.model.TravelMode
import com.example.meetpoint.domain.usecase.CalcMeetPointUseCase
import com.example.meetpoint.domain.usecase.CalcWaypointUseCase
import android.util.Log
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
    private val calcWaypointUseCase: CalcWaypointUseCase,
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

    private val _appMode = MutableStateFlow(AppMode.MEET_ONLY)
    val appMode: StateFlow<AppMode> = _appMode.asStateFlow()

    /** モード2の目的地入力 */
    private val _destination = MutableStateFlow(PersonInput("目的地"))
    val destination: StateFlow<PersonInput> = _destination.asStateFlow()

    /** モード2のαウェイト（0〜1）。β = 1 - α */
    private val _alpha = MutableStateFlow(0.5f)
    val alpha: StateFlow<Float> = _alpha.asStateFlow()

    // --- UI状態 ---

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(
            val candidates: List<MeetCandidate>,
            val persons: List<Person>,
            val appMode: AppMode = AppMode.MEET_ONLY,
            val travelMode: TravelMode = TravelMode.DRIVE,
            /** true = Overpassが0件で重心フォールバック使用 */
            val usedFallback: Boolean = false
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

    fun setAppMode(mode: AppMode) { _appMode.value = mode }

    fun updateDestinationAddress(address: String) {
        _destination.value = _destination.value.copy(address = address, latitude = null, longitude = null)
    }

    fun setAlpha(value: Float) { _alpha.value = value.coerceIn(0f, 1f) }

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

            // モード2で目的地未入力チェック
            if (_appMode.value == AppMode.WAYPOINT && _destination.value.address.isBlank()) {
                _uiState.value = UiState.Error("目的地を入力してください")
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
                TravelMode.DRIVE   -> placeRepository.searchSaPa(center.latitude, center.longitude, validPersons)
                TravelMode.TRANSIT -> placeRepository.searchStations(center.latitude, center.longitude, validPersons)
            }

            val usedFallback = rawCandidates.isEmpty()
            if (usedFallback) {
                Log.w("AppViewModel", "Overpass 候補 0件 → 重心フォールバック使用")
            }

            val candidates = when (_appMode.value) {
                AppMode.MEET_ONLY -> {
                    calcMeetPointUseCase(validPersons, rawCandidates, topN = 3)
                }
                AppMode.WAYPOINT -> {
                    // 目的地をジオコーディング（キャッシュがあればスキップ）
                    val destInput = _destination.value
                    val destPerson = if (destInput.hasCoordinates) {
                        Person("目的地", destInput.latitude!!, destInput.longitude!!, destInput.address)
                    } else {
                        geoRepository.resolvePerson("目的地", destInput.address)
                    }
                    if (destPerson == null) {
                        _uiState.value = UiState.Error("目的地の住所を認識できませんでした。\n再度確認してください。")
                        return@launch
                    }
                    // ジオコーディング結果をキャッシュ
                    _destination.value = _destination.value.copy(
                        latitude = destPerson.latitude,
                        longitude = destPerson.longitude
                    )
                    calcWaypointUseCase(
                        persons = validPersons,
                        destination = destPerson,
                        candidates = rawCandidates,
                        alpha = _alpha.value.toDouble(),
                        topN = 3
                    )
                }
            }

            _uiState.value = UiState.Success(
                candidates = candidates,
                persons = validPersons,
                appMode = _appMode.value,
                travelMode = _travelMode.value,
                usedFallback = usedFallback
            )
        }
    }
}
