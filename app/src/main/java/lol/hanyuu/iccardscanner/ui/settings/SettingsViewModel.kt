package lol.hanyuu.iccardscanner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lol.hanyuu.iccardscanner.data.repository.CardRepository
import lol.hanyuu.iccardscanner.domain.model.Card
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {

    val cards: StateFlow<List<Card>> = cardRepository.getAllCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteCard(idm: String) {
        viewModelScope.launch { cardRepository.deleteCard(idm) }
    }
}
