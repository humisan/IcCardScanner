package lol.hanyuu.iccardscanner.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lol.hanyuu.iccardscanner.BuildConfig
import lol.hanyuu.iccardscanner.data.alert.BalanceAlertManager
import lol.hanyuu.iccardscanner.data.repository.CardRepository
import lol.hanyuu.iccardscanner.data.updater.GithubUpdateChecker
import lol.hanyuu.iccardscanner.domain.model.Card
import java.io.File
import javax.inject.Inject

data class UpdateInfoUiState(
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val isCheckingLatest: Boolean = false,
    val latestVersionName: String? = null,
    val pendingVersionCode: Int? = null,
    val pendingFile: File? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cardRepository: CardRepository,
    private val updateChecker: GithubUpdateChecker,
    private val balanceAlertManager: BalanceAlertManager
) : ViewModel() {

    val cards: StateFlow<List<Card>> = cardRepository.getAllCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _alertThreshold = MutableStateFlow(balanceAlertManager.getThreshold())
    val alertThreshold: StateFlow<Int> = _alertThreshold

    fun setAlertThreshold(value: Int) {
        balanceAlertManager.setThreshold(value)
        _alertThreshold.value = value
    }

    private val _updateInfo = MutableStateFlow(UpdateInfoUiState())
    val updateInfo: StateFlow<UpdateInfoUiState> = _updateInfo

    init {
        loadPendingUpdate()
        checkLatestVersion()
    }

    fun deleteCard(idm: String) {
        viewModelScope.launch { cardRepository.deleteCard(idm) }
    }

    private fun loadPendingUpdate() {
        val prefs = context.getSharedPreferences(PREFS_UPDATE, Context.MODE_PRIVATE)
        val pendingVersionCode = prefs.getInt(KEY_PENDING_VERSION_CODE, -1)
        val fileName = prefs.getString(KEY_PENDING_FILE_NAME, UPDATE_APK_FILE_NAME) ?: UPDATE_APK_FILE_NAME
        val file = File(context.cacheDir, fileName)
        if (pendingVersionCode > BuildConfig.VERSION_CODE && file.isFile && file.length() > 0L) {
            _updateInfo.update { it.copy(pendingVersionCode = pendingVersionCode, pendingFile = file) }
        }
    }

    private fun checkLatestVersion() {
        viewModelScope.launch(Dispatchers.IO) {
            _updateInfo.update { it.copy(isCheckingLatest = true) }
            // Pass 0 so we always retrieve the latest release info regardless of current version
            val info = updateChecker.checkForUpdate(0)
            _updateInfo.update { it.copy(isCheckingLatest = false, latestVersionName = info?.versionName) }
        }
    }

    private companion object {
        const val PREFS_UPDATE = "github_update"
        const val KEY_PENDING_VERSION_CODE = "pending_version_code"
        const val KEY_PENDING_FILE_NAME = "pending_file_name"
        const val UPDATE_APK_FILE_NAME = "update.apk"
    }
}
