package lol.hanyuu.iccardscanner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lol.hanyuu.iccardscanner.domain.model.ProcessType
import lol.hanyuu.iccardscanner.domain.model.TransactionRecord
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TransactionRow(record: TransactionRecord, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.processType.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = dateFormatter.format(Instant.ofEpochMilli(record.transactionDate)) + "（時刻情報なし）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
            stationSummary(record)?.let { summary ->
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                )
            }
            record.details?.let { details ->
                Text(
                    text = details,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = formatAmount(record), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "残高 ${formatYen(record.balance)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private fun formatAmount(record: TransactionRecord): String {
    if (record.amount == 0) return "-"
    val sign = when (record.processType) {
        ProcessType.CHARGE -> "+"
        else -> "-"
    }
    return "$sign${formatYen(record.amount)}"
}

private fun stationSummary(record: TransactionRecord): String? {
    val from = record.entryStationCode?.let(StationNameResolver::resolve)
    val to = record.exitStationCode?.let(StationNameResolver::resolve)
    val fromCode = record.entryStationCode?.let(StationNameResolver::code)
    val toCode = record.exitStationCode?.let(StationNameResolver::code)
    return when {
        from != null && to != null -> "区間 $from → $to（$fromCode → $toCode）"
        from != null -> "駅 $from（$fromCode）"
        to != null -> "駅 $to（$toCode）"
        else -> null
    }
}

private fun formatYen(value: Int): String =
    "¥${NumberFormat.getNumberInstance(Locale.JAPAN).format(value)}"

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneId.of("Asia/Tokyo"))
