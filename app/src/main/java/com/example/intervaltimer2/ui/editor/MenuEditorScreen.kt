package com.example.intervaltimer2.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.intervaltimer2.data.ExerciseStep
import com.example.intervaltimer2.data.StepColorPalette
import com.example.intervaltimer2.data.TrainingMenu
import com.example.intervaltimer2.data.WorkoutRepository
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuEditorScreen(
    repository: WorkoutRepository,
    initialMenu: TrainingMenu?,
    onSave: (TrainingMenu) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(initialMenu?.title ?: "新規トレーニングメニュー") }
    var description by remember { mutableStateOf(initialMenu?.description ?: "") }
    var steps by remember { mutableStateOf(initialMenu?.steps ?: emptyList()) }

    var editingStepIndex by remember { mutableIntStateOf(-1) }
    var editingStep by remember { mutableStateOf<ExerciseStep?>(null) }
    var showStepDialog by remember { mutableStateOf(false) }

    val stepPresets by repository.stepPresets.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialMenu == null) "メニュー作成" else "メニュー編集") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val finalMenu = TrainingMenu(
                            id = initialMenu?.id ?: UUID.randomUUID().toString(),
                            title = title.ifBlank { "無題のメニュー" },
                            description = description,
                            steps = steps
                        )
                        onSave(finalMenu)
                    }) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "保存")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingStepIndex = -1
                editingStep = ExerciseStep(
                    label = "新しいステップ",
                    durationSeconds = 60,
                    bpm = 90,
                    colorHex = StepColorPalette.colors.first().first
                )
                showStepDialog = true
            }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "ステップ追加")
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // メニュー基本情報
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("メニュータイトル") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("説明 (任意)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ステップ構成 (${steps.size}個)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "合計: ${formatTotalTime(steps.sumOf { it.durationSeconds })}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (steps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("ステップが追加されていません。\n右下の＋ボタンから追加してください。", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(steps) { index, step ->
                            StepEditCard(
                                index = index,
                                step = step,
                                canMoveUp = index > 0,
                                canMoveDown = index < steps.size - 1,
                                onEdit = {
                                    editingStepIndex = index
                                    editingStep = step
                                    showStepDialog = true
                                },
                                onDuplicate = {
                                    val newSteps = steps.toMutableList()
                                    newSteps.add(index + 1, step.copy(id = UUID.randomUUID().toString()))
                                    steps = newSteps
                                },
                                onDelete = {
                                    val newSteps = steps.toMutableList()
                                    newSteps.removeAt(index)
                                    steps = newSteps
                                },
                                onMoveUp = {
                                    if (index > 0) {
                                        val newSteps = steps.toMutableList()
                                        val item = newSteps.removeAt(index)
                                        newSteps.add(index - 1, item)
                                        steps = newSteps
                                    }
                                },
                                onMoveDown = {
                                    if (index < steps.size - 1) {
                                        val newSteps = steps.toMutableList()
                                        val item = newSteps.removeAt(index)
                                        newSteps.add(index + 1, item)
                                        steps = newSteps
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ステップ編集ダイアログ
    if (showStepDialog && editingStep != null) {
        StepEditorDialog(
            initialStep = editingStep!!,
            presets = stepPresets,
            onAddPreset = { repository.addStepPreset(it) },
            onRemovePreset = { repository.removeStepPreset(it) },
            onDismiss = { showStepDialog = false },
            onConfirm = { updatedStep ->
                val newSteps = steps.toMutableList()
                if (editingStepIndex >= 0) {
                    newSteps[editingStepIndex] = updatedStep
                } else {
                    newSteps.add(updatedStep)
                }
                steps = newSteps
                showStepDialog = false
            }
        )
    }
}

@Composable
private fun StepEditCard(
    index: Int,
    step: ExerciseStep,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val stepColor = parseColorHex(step.colorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(stepColor, CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${index + 1}. ${step.label}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 「時間:」ラベルは無しで時間のみ (例: 5分0秒)
                    Text(
                        text = formatStepTime(step.durationSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (step.bpm > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${step.bpm}BPM",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 操作アイコンボタン
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(28.dp)) {
                Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "上へ", modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(28.dp)) {
                Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "下へ", modifier = Modifier.size(16.dp))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepEditorDialog(
    initialStep: ExerciseStep,
    presets: List<String>,
    onAddPreset: (String) -> Unit,
    onRemovePreset: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (ExerciseStep) -> Unit
) {
    var label by remember { mutableStateOf(initialStep.label) }
    var minutesText by remember { mutableStateOf((initialStep.durationSeconds / 60).toString()) }
    var secondsText by remember { mutableStateOf((initialStep.durationSeconds % 60).toString()) }
    var bpm by remember { mutableIntStateOf(initialStep.bpm) }
    var selectedColorHex by remember { mutableStateOf(initialStep.colorHex) }

    var presetToDelete by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ステップ設定") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // ラベル設定 & テンプレート追加ボタン付き
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("ステップ名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (label.isNotBlank() && !presets.contains(label)) {
                            IconButton(onClick = { onAddPreset(label) }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "テンプレートに登録",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 登録済みテンプレートチップ一覧 (タップで適用、赤いゴミ箱アイコンで削除確認)
                Text("テンプレート (タップで適用):", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        val isSelected = label == preset
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = borderForSelected(isSelected)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { label = preset }
                                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                            ) {
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                // 赤いゴミ箱アイコンボタン (タップで削除確認ダイアログ)
                                Box(
                                    modifier = Modifier
                                        .clickable { presetToDelete = preset }
                                        .padding(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "削除",
                                        tint = Color.Red, // 他の削除ボタンと同様の赤色に統一！
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 時間（分・秒）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter { c -> c.isDigit() } },
                        label = { Text("分") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = { secondsText = it.filter { c -> c.isDigit() } },
                        label = { Text("秒") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // BPM設定スライダー
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("BPM (テンポ):", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = if (bpm > 0) "$bpm BPM" else "オフ (0)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = bpm.toFloat(),
                    onValueChange = { bpm = it.toInt() },
                    valueRange = 0f..180f,
                    steps = 180
                )

                Spacer(modifier = Modifier.height(12.dp))

                // カラーパレット選択 (UD 10色)
                Text("表示カラー:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))

                val row1 = StepColorPalette.colors.take(5)
                val row2 = StepColorPalette.colors.drop(5)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, Color(0xFF333333))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1行目 (5色)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row1.forEach { (hex, _) ->
                                val color = parseColorHex(hex)
                                val isSelected = selectedColorHex.equals(hex, ignoreCase = true)

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(color, RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedColorHex = hex }
                                )
                            }
                        }

                        // 2行目 (5色)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row2.forEach { (hex, _) ->
                                val color = parseColorHex(hex)
                                val isSelected = selectedColorHex.equals(hex, ignoreCase = true)

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(color, RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedColorHex = hex }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val m = minutesText.toIntOrNull() ?: 0
                val s = secondsText.toIntOrNull() ?: 0
                val totalSec = (m * 60 + s).coerceAtLeast(1)

                val updatedStep = initialStep.copy(
                    label = label.ifBlank { "ステップ" },
                    durationSeconds = totalSec,
                    bpm = bpm,
                    colorHex = selectedColorHex
                )
                onConfirm(updatedStep)
            }) {
                Text("確定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )

    // テンプレート削除の確認ダイアログ
    presetToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            title = { Text("テンプレートの削除") },
            text = { Text("「$target」をテンプレート一覧から削除しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    onRemovePreset(target)
                    presetToDelete = null
                }) {
                    Text("削除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { presetToDelete = null }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun borderForSelected(isSelected: Boolean): BorderStroke? {
    return if (isSelected) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }
}

private fun formatStepTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}分${s}秒" else "${s}秒"
}

private fun formatTotalTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m}分${s}秒"
}

private fun parseColorHex(hex: String): Color {
    return try {
        val colorInt = android.graphics.Color.parseColor(hex)
        Color(colorInt)
    } catch (e: Exception) {
        Color.Gray
    }
}
