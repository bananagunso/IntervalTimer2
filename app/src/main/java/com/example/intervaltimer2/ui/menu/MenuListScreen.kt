package com.example.intervaltimer2.ui.menu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.intervaltimer2.data.TrainingMenu
import com.example.intervaltimer2.data.WorkoutRepository
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuListScreen(
    repository: WorkoutRepository,
    onStartWorkout: (TrainingMenu) -> Unit,
    onEditMenu: (TrainingMenu?) -> Unit
) {
    val context = LocalContext.current
    val menus by repository.menus.collectAsState()

    var exportJsonText by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var menuToDelete by remember { mutableStateOf<TrainingMenu?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "インターバルタイマー",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(
                        onClick = { showImportDialog = true },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("インポート", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEditMenu(null) }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新規メニュー追加"
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (menus.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "メニューがありません。\n右下の＋ボタンから追加してください。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 16.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 120.dp // 最下部ボタン(FAB)の裏に隠れないよう十分なスクロール余白を確保！
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(menus, key = { _, menu -> menu.id }) { index, menu ->
                        MenuCard(
                            menu = menu,
                            index = index,
                            totalCount = menus.size,
                            onPlay = { onStartWorkout(menu) },
                            onEdit = { onEditMenu(menu) },
                            onDuplicate = { repository.duplicateMenu(menu.id) },
                            onExport = { exportJsonText = repository.exportMenuToJson(menu) },
                            onDelete = { menuToDelete = menu },
                            onMoveUp = { repository.moveMenuUp(menu.id) },
                            onMoveDown = { repository.moveMenuDown(menu.id) }
                        )
                    }
                }
            }
        }
    }

    // メニュー削除確認ダイアログ
    menuToDelete?.let { targetMenu ->
        AlertDialog(
            onDismissRequest = { menuToDelete = null },
            title = { Text("メニューの削除") },
            text = { Text("「${targetMenu.title}」を削除しますか？\nこの操作は取り消せません。") },
            confirmButton = {
                TextButton(onClick = {
                    repository.deleteMenu(targetMenu.id)
                    menuToDelete = null
                }) {
                    Text("削除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { menuToDelete = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // エクスポートダイアログ
    exportJsonText?.let { json ->
        AlertDialog(
            onDismissRequest = { exportJsonText = null },
            title = { Text("メニューを共有 (JSON)") },
            text = {
                Column {
                    Text("以下のテキストをコピーして共有・保存できます:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = json,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("workout_menu", json)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "クリップボードにコピーしました", Toast.LENGTH_SHORT).show()
                    exportJsonText = null
                }) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("クリップボードにコピー")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportJsonText = null }) {
                    Text("閉じる")
                }
            }
        )
    }

    // インポートダイアログ
    if (showImportDialog) {
        var importInputText by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("メニューをインポート") },
            text = {
                Column {
                    Text("共有されたJSONテキストを下に貼り付けてください:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = {
                            importInputText = it
                            errorMessage = null
                        },
                        placeholder = { Text("JSONテキストを貼り付け...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                    errorMessage?.let { err ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = err, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (importInputText.isNotBlank()) {
                        val result = repository.importMenuFromJson(importInputText)
                        if (result.isSuccess) {
                            Toast.makeText(context, "メニューをインポートしました", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                        } else {
                            errorMessage = "フォーマットが正しくありません"
                        }
                    }
                }) {
                    Text("インポート実行")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun MenuCard(
    menu: TrainingMenu,
    index: Int,
    totalCount: Int,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = menu.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (menu.description.isNotBlank()) {
                        Text(
                            text = menu.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // タイマースタートボタン
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "スタート",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ステップ色のタイムラインプレビュー
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                menu.steps.forEach { step ->
                    Box(
                        modifier = Modifier
                            .weight(step.durationSeconds.toFloat().coerceAtLeast(1f))
                            .height(6.dp)
                            .background(
                                parseColorHex(step.colorHex),
                                RoundedCornerShape(3.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 時間表記のみ ＆ 全操作アイコン横1列配置
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ステップ数なし、「計」も無しで時間のみ (例: 10分00秒)
                Text(
                    text = formatMinutes(menu.totalDurationSeconds),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 操作アイコン列 (横1列配置)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "上へ移動",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "下へ移動",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onExport, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "共有", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "複製", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "編集", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "削除", tint = Color.Red, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

private fun formatMinutes(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.JAPAN, "%d分%02d秒", m, s)
}

private fun parseColorHex(hex: String): Color {
    return try {
        val colorInt = android.graphics.Color.parseColor(hex)
        Color(colorInt)
    } catch (e: Exception) {
        Color.Gray
    }
}
