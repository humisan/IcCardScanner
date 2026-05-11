package lol.hanyuu.iccardscanner.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lol.hanyuu.iccardscanner.ScanState
import lol.hanyuu.iccardscanner.domain.model.Card
import lol.hanyuu.iccardscanner.domain.model.CardType
import lol.hanyuu.iccardscanner.domain.model.TransactionRecord
import lol.hanyuu.iccardscanner.ui.components.CardVisual
import lol.hanyuu.iccardscanner.ui.components.TransactionRow
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun HomeScreen(
    scanState: ScanState,
    onScanStateReset: () -> Unit,
    onNavigateToHistory: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val selectedIndex by viewModel.selectedIndex.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { maxOf(cards.size, 1) })

    LaunchedEffect(pagerState.currentPage) { viewModel.selectCard(pagerState.currentPage) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(scanState) {
        when (scanState) {
            is ScanState.Success -> {
                snackbarHostState.showSnackbar("読み取り完了")
                onScanStateReset()
            }
            is ScanState.Error -> {
                snackbarHostState.showSnackbar(scanState.message)
                onScanStateReset()
            }
            else -> {}
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            if (cards.isEmpty()) {
                EmptyCardPlaceholder(modifier = Modifier.padding(horizontal = 24.dp))
            } else {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val card = cards[page]
                    CardVisual(
                        cardType = card.type,
                        nickname = card.nickname,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            val currentCard = cards.getOrNull(selectedIndex)
            BalanceSection(card = currentCard, modifier = Modifier.padding(horizontal = 24.dp))

            Spacer(Modifier.height(8.dp))

            NfcGuidance(
                isLoading = scanState is ScanState.Loading,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { currentCard?.let { onNavigateToHistory(it.idm) } },
                    modifier = Modifier.weight(1f),
                    enabled = currentCard != null
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("利用履歴")
                }
                OutlinedButton(
                    onClick = { currentCard?.let { onNavigateToDetail(it.idm) } },
                    modifier = Modifier.weight(1f),
                    enabled = currentCard != null
                ) {
                    Icon(Icons.Default.AccountBox, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("カード詳細")
                }
                OutlinedButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("設定")
                }
            }

            Spacer(Modifier.height(24.dp))

            if (recentTransactions.isNotEmpty()) {
                Text(
                    "最近の利用",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(8.dp))
                recentTransactions.forEach { record ->
                    TransactionRow(record = record, modifier = Modifier.padding(horizontal = 24.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BalanceSection(card: Card?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            "残高",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        if (card != null && card.lastBalance >= 0) {
            Text(
                text = "¥${NumberFormat.getNumberInstance(Locale.JAPAN).format(card.lastBalance)}",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "最終読み取り: ${formatDateTime(card.lastScannedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        } else {
            Text("---", fontSize = 48.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NfcGuidance(isLoading: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("読み取り中...", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(
                "ICカードを端末の背面に当ててください",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmptyCardPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        CardVisual(
            cardType = CardType.UNKNOWN,
            nickname = "カードを読み取ってください",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private val dateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy/MM/dd HH:mm")
    .withZone(ZoneId.of("Asia/Tokyo"))

private fun formatDateTime(epochMillis: Long): String =
    dateTimeFormatter.format(Instant.ofEpochMilli(epochMillis))
