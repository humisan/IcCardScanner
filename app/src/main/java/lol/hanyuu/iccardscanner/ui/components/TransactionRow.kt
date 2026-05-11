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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.processType.displayName,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = dateTimeFormatter.format(Instant.ofEpochMilli(record.transactionDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
            stationSummary(record)?.let { summary ->
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
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
    val from = record.entryStationCode?.toStationCode()
    val to = record.exitStationCode?.toStationCode()
    return when {
        from != null && to != null -> "区間 $from → $to"
        from != null -> "駅 $from"
        to != null -> "駅 $to"
        else -> null
    }
}

private fun Int.toStationCode(): String {
    val line = (this ushr 8) and 0xFF
    val station = this and 0xFF
    return "${line.toCode()}-${station.toCode()}"
}

private fun Int.toCode(): String = toString(16).padStart(2, '0').uppercase()

private fun formatYen(value: Int): String =
    "¥${NumberFormat.getNumberInstance(Locale.JAPAN).format(value)}"

private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(ZoneId.of("Asia/Tokyo"))
