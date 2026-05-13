package lol.hanyuu.iccardscanner

import android.nfc.Tag
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import lol.hanyuu.iccardscanner.data.alert.BalanceAlertManager
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
    private val readFeliCaUseCase: ReadFeliCaUseCase,
    private val balanceAlertManager: BalanceAlertManager
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
                balanceAlertManager.checkAndNotify(idm)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read FeliCa tag", e)
                _scanState.value = ScanState.Error(e.message ?: "IC card read failed")
            }
        }
    }

    fun resetScanState() {
        _scanState.value = ScanState.Idle
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
