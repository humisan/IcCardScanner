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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
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
            stationSummary(context, record)?.let { summary ->
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                )
            }
            detailsSummary(record)?.let { details ->
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

private fun stationSummary(context: android.content.Context, record: TransactionRecord): String? {
    val area = record.details?.extractHex("area") ?: record.details?.extractHex("region")
    val from = record.entryStationCode?.let { StationNameResolver.resolve(context, area, it) }
    val to = record.exitStationCode?.let { StationNameResolver.resolve(context, area, it) }
    return when {
        from != null && to != null -> "入場 ${from.displayName} → 退場 ${to.displayName}（${from.code} → ${to.code}）"
        from != null -> "入場 ${from.displayName}（${from.code}）"
        to != null -> "退場 ${to.displayName}（${to.code}）"
        else -> null
    }
}

private fun detailsSummary(record: TransactionRecord): String? {
    val details = record.details ?: return null
    val terminal = details.extractValue("terminal") ?: return null
    val process = details.extractValue("process") ?: return null
    val sequence = details.extractValue("sequence") ?: return null
    return "端末 $terminal / 処理 $process / 連番 $sequence"
}

private fun String.extractValue(key: String): String? =
    split(';').firstOrNull { it.startsWith("$key=") }?.substringAfter('=')

private fun String.extractHex(key: String): Int? =
    extractValue(key)
        ?.removePrefix("0x")
        ?.removePrefix("0X")
        ?.toIntOrNull(16)

private fun formatYen(value: Int): String =
    "¥${NumberFormat.getNumberInstance(Locale.JAPAN).format(value)}"

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneId.of("Asia/Tokyo"))
