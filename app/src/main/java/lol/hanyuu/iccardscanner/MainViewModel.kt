package lol.hanyuu.iccardscanner

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import lol.hanyuu.iccardscanner.domain.usecase.ReadFeliCaUseCase
import javax.inject.Inject

sealed class ScanState {
    data object Idle : ScanState()
    data object Loading : ScanState()
    data class Success(val cardIdm: String) : ScanState()
    data class Error(val message: String) : ScanState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val readFeliCaUseCase: ReadFeliCaUseCase
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState

    fun handleNfcTag(tag: Tag) {
        if (_scanState.value is ScanState.Loading) return
        viewModelScope.launch {
            _scanState.value = ScanState.Loading
            try {
                val idm = readFeliCaUseCase(tag)
                _scanState.value = ScanState.Success(idm)
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "読み取りエラー")
            }
        }
    }

    fun resetScanState() { _scanState.value = ScanState.Idle }
}
