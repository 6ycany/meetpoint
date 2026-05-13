package com.example.meetpoint.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.meetpoint.data.local.entity.MemberEntity
import com.example.meetpoint.data.repository.GeoRepository
import com.example.meetpoint.data.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemberViewModel @Inject constructor(
    private val memberRepository: MemberRepository,
    private val geoRepository: GeoRepository
) : ViewModel() {

    /** 全メンバー一覧（DBから自動更新） */
    val members: StateFlow<List<MemberEntity>> = memberRepository.getAllMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 保存処理の状態 */
    sealed class SaveState {
        object Idle : SaveState()
        object Saving : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    fun resetSaveState() { _saveState.value = SaveState.Idle }

    /**
     * メンバーを追加（住所をジオコーディングして座標を保存）
     */
    fun addMember(name: String, address: String) {
        if (name.isBlank() || address.isBlank()) {
            _saveState.value = SaveState.Error("名前と住所を入力してください")
            return
        }
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            val person = geoRepository.resolvePerson(name, address)
            if (person == null) {
                _saveState.value = SaveState.Error("住所を認識できませんでした")
                return@launch
            }
            memberRepository.insert(
                MemberEntity(
                    name = name,
                    address = person.address.ifBlank { address },
                    latitude = person.latitude,
                    longitude = person.longitude
                )
            )
            _saveState.value = SaveState.Success
        }
    }

    /**
     * メンバー情報を更新（住所が変わった場合は再ジオコーディング）
     */
    fun updateMember(member: MemberEntity, newName: String, newAddress: String) {
        if (newName.isBlank() || newAddress.isBlank()) {
            _saveState.value = SaveState.Error("名前と住所を入力してください")
            return
        }
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            if (newAddress == member.address) {
                // 住所変更なし → 名前だけ更新
                memberRepository.update(member.copy(name = newName))
                _saveState.value = SaveState.Success
            } else {
                // 住所変更あり → 再ジオコーディング
                val person = geoRepository.resolvePerson(newName, newAddress)
                if (person == null) {
                    _saveState.value = SaveState.Error("住所を認識できませんでした")
                    return@launch
                }
                memberRepository.update(
                    member.copy(
                        name = newName,
                        address = person.address.ifBlank { newAddress },
                        latitude = person.latitude,
                        longitude = person.longitude
                    )
                )
                _saveState.value = SaveState.Success
            }
        }
    }

    fun deleteMember(member: MemberEntity) {
        viewModelScope.launch { memberRepository.delete(member) }
    }
}
