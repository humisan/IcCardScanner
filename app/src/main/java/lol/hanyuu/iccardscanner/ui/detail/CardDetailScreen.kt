package lol.hanyuu.iccardscanner.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import lol.hanyuu.iccardscanner.ui.components.CardVisual

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: CardDetailViewModel = hiltViewModel()
) {
    val card by viewModel.card.collectAsStateWithLifecycle()
    var nicknameInput by remember(card?.nickname) { mutableStateOf(card?.nickname ?: "") }
    var editMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("カード詳細") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            card?.let { c ->
                CardVisual(
                    cardType = c.type,
                    nickname = c.nickname,
                    modifier = Modifier.fillMaxWidth()
                )
                DetailRow(label = "カード種別", value = c.type.displayName)
                if (editMode) {
                    OutlinedTextField(
                        value = nicknameInput,
                        onValueChange = { nicknameInput = it },
                        label = { Text("登録名") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            viewModel.updateNickname(nicknameInput)
                            editMode = false
                        }) { Text("保存") }
                        OutlinedButton(onClick = { editMode = false }) { Text("キャンセル") }
                    }
                } else {
                    DetailRow(label = "登録名", value = c.nickname)
                    OutlinedButton(onClick = { editMode = true }) { Text("登録名を変更") }
                }
                DetailRow(label = "カードID", value = viewModel.maskedIdm + "…")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}
