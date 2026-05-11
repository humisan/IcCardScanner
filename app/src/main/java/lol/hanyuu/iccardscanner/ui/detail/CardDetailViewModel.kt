package lol.hanyuu.iccardscanner.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import lol.hanyuu.iccardscanner.data.repository.CardRepository
import lol.hanyuu.iccardscanner.domain.model.Card
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardRepository: CardRepository
) : ViewModel() {

    private val cardIdm: String = checkNotNull(savedStateHandle["cardIdm"])

    val card: StateFlow<Card?> = cardRepository.getAllCards()
        .map { list -> list.find { it.idm == cardIdm } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val maskedIdm: String by lazy { sha256Prefix8(cardIdm) }

    fun updateNickname(nickname: String) {
        viewModelScope.launch { cardRepository.updateNickname(cardIdm, nickname) }
    }

    private fun sha256Prefix8(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02X".format(it) }.take(8)
    }
}
