package lol.hanyuu.iccardscanner.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import lol.hanyuu.iccardscanner.data.repository.HistoryRepository
import lol.hanyuu.iccardscanner.domain.model.TransactionRecord
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    historyRepository: HistoryRepository
) : ViewModel() {

    private val cardIdm: String = checkNotNull(savedStateHandle["cardIdm"])

    val transactions: StateFlow<List<TransactionRecord>> =
        historyRepository.getTransactions(cardIdm)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
