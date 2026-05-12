package lol.hanyuu.iccardscanner.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { maxOf(cards.size, 1) })
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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

    LaunchedEffect(updateState) {
        if (updateState is UpdateState.ReadyToInstall) {
            val file = (updateState as UpdateState.ReadyToInstall).file
            if (canRequestPackageInstalls(context)) {
                context.startActivity(createInstallIntent(context, file))
            } else {
                viewModel.requireInstallPermission(file)
                context.startActivity(createInstallPermissionIntent(context))
            }
        }
    }

    DisposableEffect(lifecycleOwner, updateState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                updateState is UpdateState.InstallPermissionRequired &&
                canRequestPackageInstalls(context)
            ) {
                viewModel.retryInstallAfterPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            UpdateBanner(
                state = updateState,
                onDownload = viewModel::downloadUpdate,
                onOpenInstallPermission = {
                    context.startActivity(createInstallPermissionIntent(context))
                },
                onDismissError = viewModel::dismissUpdateError
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            item { Spacer(Modifier.height(16.dp)) }

            item {
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
            }

            item { Spacer(Modifier.height(24.dp)) }

            item {
                val currentCard = cards.getOrNull(selectedIndex)
                BalanceSection(card = currentCard, modifier = Modifier.padding(horizontal = 24.dp))
            }

            item { Spacer(Modifier.height(8.dp)) }

            item {
                NfcGuidance(
                    isLoading = scanState is ScanState.Loading,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            item { Spacer(Modifier.height(24.dp)) }

            item {
                val currentCard = cards.getOrNull(selectedIndex)
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
            }

            item { Spacer(Modifier.height(24.dp)) }

            if (recentTransactions.isNotEmpty()) {
                item {
                    Text(
                        "最近の利用",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(recentTransactions) { record ->
                    TransactionRow(record = record, modifier = Modifier.padding(horizontal = 24.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun UpdateBanner(
    state: UpdateState,
    onDownload: (String) -> Unit,
    onOpenInstallPermission: () -> Unit,
    onDismissError: () -> Unit
) {
    when (state) {
        UpdateState.Checking -> {
            Surface(tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("更新を確認中...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        is UpdateState.Available -> {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${state.versionName} に更新可能",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Button(onClick = { onDownload(state.downloadUrl) }) {
                        Text("更新")
                    }
                }
            }
        }
        is UpdateState.Downloading -> {
            Surface(tonalElevation = 4.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("ダウンロード中...", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        is UpdateState.InstallPermissionRequired -> {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "更新APKのインストール許可が必要です",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Button(onClick = onOpenInstallPermission) {
                        Text("許可")
                    }
                }
            }
        }
        is UpdateState.Error -> {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "更新に失敗しました",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    TextButton(onClick = onDismissError) {
                        Text("閉じる")
                    }
                }
            }
        }
        else -> {}
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

private fun canRequestPackageInstalls(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

private fun createInstallPermissionIntent(context: android.content.Context): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    } else {
        Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

private fun createInstallIntent(context: android.content.Context, file: java.io.File): Intent {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
