package lol.hanyuu.iccardscanner.ui.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import lol.hanyuu.iccardscanner.BuildConfig
import lol.hanyuu.iccardscanner.data.repository.CardRepository
import lol.hanyuu.iccardscanner.data.repository.HistoryRepository
import lol.hanyuu.iccardscanner.data.updater.GithubUpdateChecker
import lol.hanyuu.iccardscanner.domain.model.Card
import lol.hanyuu.iccardscanner.domain.model.TransactionRecord
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class Available(val versionCode: Int, val versionName: String, val downloadUrl: String) : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data class ReadyToInstall(val file: File) : UpdateState()
    data class InstallPermissionRequired(val file: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cardRepository: CardRepository,
    private val historyRepository: HistoryRepository,
    private val updateChecker: GithubUpdateChecker
) : ViewModel() {

    val cards: StateFlow<List<Card>> = cardRepository.getAllCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex

    @OptIn(ExperimentalCoroutinesApi::class)
    val recentTransactions: StateFlow<List<TransactionRecord>> =
        combine(cards, _selectedIndex) { list, idx -> list.getOrNull(idx)?.idm }
            .flatMapLatest { idm ->
                if (idm == null) flowOf(emptyList())
                else historyRepository.getRecentTransactions(idm, 3)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    init {
        checkForUpdate()
    }

    fun selectCard(index: Int) { _selectedIndex.value = index }

    fun dismissUpdateError() { _updateState.value = UpdateState.Idle }

    fun reportInstallLaunchFailed(message: String) {
        _updateState.value = UpdateState.Error(message)
    }

    fun requireInstallPermission(file: File) {
        _updateState.value = UpdateState.InstallPermissionRequired(file)
    }

    fun retryInstallAfterPermission() {
        val state = _updateState.value
        if (state is UpdateState.InstallPermissionRequired) {
            _updateState.value = UpdateState.ReadyToInstall(state.file)
        }
    }

    private fun checkForUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            _updateState.value = UpdateState.Checking
            val info = updateChecker.checkForUpdate(BuildConfig.VERSION_CODE)
            if (info != null) {
                Log.d(TAG, "Auto update found: version=${info.versionName} asset=${info.assetName} size=${info.assetSize}")
                _updateState.value = UpdateState.Available(info.versionCode, info.versionName, info.downloadUrl)
                downloadUpdate(info.downloadUrl)
            } else {
                _updateState.value = UpdateState.Idle
            }
        }
    }

    fun downloadUpdate(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _updateState.value = UpdateState.Downloading(0f)
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 15_000
                conn.readTimeout = 120_000
                val httpCode = conn.responseCode
                if (httpCode !in 200..299) {
                    conn.disconnect()
                    throw IllegalStateException("APKダウンロードに失敗しました: HTTP $httpCode")
                }
                val total = conn.contentLengthLong
                val tmpFile = File(context.cacheDir, "update.apk.download")
                val file = File(context.cacheDir, "update.apk")
                tmpFile.delete()
                file.delete()
                var downloaded = 0L
                conn.inputStream.use { input ->
                    tmpFile.outputStream().use { output ->
                        val buf = ByteArray(8192)
                        var n: Int
                        while (input.read(buf).also { n = it } != -1) {
                            output.write(buf, 0, n)
                            downloaded += n
                            if (total > 0) {
                                _updateState.value = UpdateState.Downloading(downloaded.toFloat() / total)
                            }
                        }
                    }
                }
                conn.disconnect()
                if (downloaded <= 0L) {
                    tmpFile.delete()
                    throw IllegalStateException("APKファイルが空です")
                }
                if (total > 0 && downloaded != total) {
                    tmpFile.delete()
                    throw IllegalStateException("APKダウンロードが不完全です: $downloaded/$total bytes")
                }
                if (!tmpFile.renameTo(file)) {
                    tmpFile.delete()
                    throw IllegalStateException("APKファイルの保存に失敗しました")
                }
                Log.d(TAG, "APK downloaded: ${file.absolutePath} bytes=$downloaded")
                _updateState.value = UpdateState.ReadyToInstall(file)
            } catch (e: Exception) {
                Log.e(TAG, "APK download failed", e)
                _updateState.value = UpdateState.Error(e.message ?: "ダウンロードに失敗しました")
            }
        }
    }

    private companion object {
        const val TAG = "HomeViewModel"
    }
}
