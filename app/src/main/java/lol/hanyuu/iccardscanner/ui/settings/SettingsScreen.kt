package lol.hanyuu.iccardscanner.ui.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lol.hanyuu.iccardscanner.domain.model.Card
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val alertThreshold by viewModel.alertThreshold.collectAsStateWithLifecycle()
    var cardToDelete by remember { mutableStateOf<Card?>(null) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showThresholdDialog) {
        ThresholdPickerDialog(
            current = alertThreshold,
            onSelect = { viewModel.setAlertThreshold(it); showThresholdDialog = false },
            onDismiss = { showThresholdDialog = false }
        )
    }

    cardToDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
            title = { Text("カードを削除") },
            text = { Text("「${card.nickname}」を削除しますか？利用履歴も削除されます。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCard(card.idm)
                    cardToDelete = null
                }) { Text("削除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { cardToDelete = null }) { Text("キャンセル") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Text(
                    "アップデート",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("現在のバージョン") },
                    trailingContent = {
                        Text(
                            "v${updateInfo.currentVersionName} (${updateInfo.currentVersionCode})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("最新バージョン") },
                    trailingContent = {
                        if (updateInfo.isCheckingLatest) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                updateInfo.latestVersionName ?: "取得失敗",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (updateInfo.latestVersionName == null)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }

            val pendingFile = updateInfo.pendingFile
            val pendingVersionCode = updateInfo.pendingVersionCode
            if (pendingFile != null) {
                item {
                    ListItem(
                        headlineContent = { Text("インストール待ち") },
                        supportingContent = pendingVersionCode?.let { { Text("v$it") } },
                        trailingContent = {
                            Button(
                                onClick = {
                                    runCatching {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            pendingFile
                                        )
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "application/vnd.android.package-archive")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    }
                                }
                            ) {
                                Text("インストール")
                            }
                        }
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            item {
                Text(
                    "通知",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    modifier = Modifier.clickable { showThresholdDialog = true },
                    headlineContent = { Text("残高アラート") },
                    supportingContent = { Text("スキャン後に残高がしきい値を下回ると通知") },
                    trailingContent = {
                        Text(
                            formatThreshold(alertThreshold),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            item {
                Text(
                    "カード",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (cards.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "登録されたカードがありません",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                items(cards) { card ->
                    ListItem(
                        headlineContent = { Text(card.nickname) },
                        supportingContent = { Text(card.type.displayName) },
                        trailingContent = {
                            IconButton(onClick = { cardToDelete = card }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "削除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ThresholdPickerDialog(current: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(-1 to "オフ", 500 to "¥500", 1_000 to "¥1,000", 2_000 to "¥2,000", 3_000 to "¥3,000")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("残高アラートのしきい値") },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = current == value, onClick = { onSelect(value) })
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}

private fun formatThreshold(value: Int): String =
    if (value < 0) "オフ"
    else "¥${NumberFormat.getNumberInstance(Locale.JAPAN).format(value)}"
