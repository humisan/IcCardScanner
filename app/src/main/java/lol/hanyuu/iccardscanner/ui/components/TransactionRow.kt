package lol.hanyuu.iccardscanner.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import lol.hanyuu.iccardscanner.domain.model.ProcessType
import lol.hanyuu.iccardscanner.domain.model.TransactionRecord
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

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
                text = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(Date(record.transactionDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            val amountStr = when (record.processType) {
                ProcessType.CHARGE -> "+¥${NumberFormat.getNumberInstance().format(record.amount)}"
                else -> "-¥${NumberFormat.getNumberInstance().format(record.amount)}"
            }
            Text(text = amountStr, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "残高 ¥${NumberFormat.getNumberInstance().format(record.balance)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
