package lol.hanyuu.iccardscanner.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import lol.hanyuu.iccardscanner.data.repository.HistoryRepository
import lol.hanyuu.iccardscanner.domain.model.ProcessType
import lol.hanyuu.iccardscanner.domain.model.TransactionRecord
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

data class MonthSummary(
    val year: Int,
    val month: Int,
    val totalCharge: Int,
    val totalTransit: Int,
    val totalRetail: Int,
    val endingBalance: Int,
    val transactionCount: Int
) {
    val label: String get() = "${year}年${month}月"
}

@HiltViewModel
class MonthlySummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    historyRepository: HistoryRepository
) : ViewModel() {

    private val cardIdm: String = checkNotNull(savedStateHandle["cardIdm"])

    val summaries: StateFlow<List<MonthSummary>> =
        historyRepository.getTransactions(cardIdm)
            .map { buildSummaries(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun buildSummaries(transactions: List<TransactionRecord>): List<MonthSummary> {
        if (transactions.isEmpty()) return emptyList()
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"))
        return transactions
            .groupBy { tx ->
                cal.timeInMillis = tx.transactionDate
                // key: YYYYMM integer for stable grouping
                cal.get(Calendar.YEAR) * 100 + (cal.get(Calendar.MONTH) + 1)
            }
            .map { (key, txns) ->
                MonthSummary(
                    year = key / 100,
                    month = key % 100,
                    totalCharge = txns.filter { it.processType == ProcessType.CHARGE }.sumOf { it.amount },
                    // Only EXIT holds the actual fare; ENTRY amount is always 0
                    totalTransit = txns.filter { it.processType == ProcessType.EXIT }.sumOf { it.amount },
                    totalRetail = txns.filter { it.processType == ProcessType.PURCHASE }.sumOf { it.amount },
                    // transactions sorted DESC — first() is the latest in this month
                    endingBalance = txns.first().balance,
                    transactionCount = txns.size
                )
            }
            .sortedByDescending { it.year * 100 + it.month }
    }
}
