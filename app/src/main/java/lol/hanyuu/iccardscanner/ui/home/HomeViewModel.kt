package lol.hanyuu.iccardscanner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import lol.hanyuu.iccardscanner.data.repository.CardRepository
import lol.hanyuu.iccardscanner.data.repository.HistoryRepository
import lol.hanyuu.iccardscanner.domain.model.Card
import lol.hanyuu.iccardscanner.domain.model.TransactionRecord
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val historyRepository: HistoryRepository
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

    fun selectCard(index: Int) { _selectedIndex.value = index }
}
