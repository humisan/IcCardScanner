package lol.hanyuu.iccardscanner.ui.home

import android.content.Context
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
    data class Available(val versionCode: Int, val downloadUrl: String) : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data class ReadyToInstall(val file: File) : UpdateState()
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

    private fun checkForUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = updateChecker.checkForUpdate(BuildConfig.VERSION_CODE)
            if (info != null) {
                _updateState.value = UpdateState.Available(info.versionCode, info.downloadUrl)
            }
        }
    }

    fun downloadUpdate(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _updateState.value = UpdateState.Downloading(0f)
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 120_000
                val total = conn.contentLengthLong
                val file = File(context.cacheDir, "update.apk")
                var downloaded = 0L
                conn.inputStream.use { input ->
                    file.outputStream().use { output ->
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
                _updateState.value = UpdateState.ReadyToInstall(file)
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "ダウンロードに失敗しました")
            }
        }
    }
}
