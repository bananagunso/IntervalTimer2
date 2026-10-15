package com.example.intervaltimer2.ui.timer

import android.app.Activity
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.intervaltimer2.data.ExerciseStep
import com.example.intervaltimer2.timer.TimerUiState
import com.example.intervaltimer2.timer.WorkoutTimerEngine

@Composable
fun WorkoutTimerScreen(
    timerEngine: WorkoutTimerEngine,
    onBackToList: () -> Unit
) {
    val context = LocalContext.current
    val uiState by timerEngine.uiState.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // タイマー実行画面が開いている間は、画面の自動スリープ（消灯）を防止！
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val stepColor = parseColorHex(uiState.currentStep?.colorHex ?: "#121212")

    // BPM点滅用の背景/外枠フラッシュアニメーション
    val flashColor by animateColorAsState(
        targetValue = if (uiState.isFlashActive) Color.White.copy(alpha = 0.85f) else Color.Transparent,
        animationSpec = tween(durationMillis = 60)
    )

    // 全体進捗割合 (0.0f ~ 1.0f)
    val totalProgress = if (uiState.totalDurationSeconds > 0) {
        if (uiState.isFinished) 1f else (1f - uiState.totalRemainingSeconds.toFloat() / uiState.totalDurationSeconds).coerceIn(0f, 1f)
    } else 0f

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                // 1. 一番上: 全体の進捗バー
                LinearProgressIndicator(
                    progress = { totalProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Color(0xFF00E676),
                    trackColor = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 2. 戻るボタン & 中央揃え「残り時間 / 全体の時間」
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onBackToList()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "${formatTime(uiState.totalRemainingSeconds)} / ${formatTime(uiState.totalDurationSeconds)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.size(40.dp))
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = Color(0xFF121212)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // 背景フラッシュ演出
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(flashColor)
                )

                if (isLandscape) {
                    // 横画面レイアウト
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MainTimerPanel(
                            uiState = uiState,
                            stepColor = stepColor,
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceAround,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            StepsPreviewList(
                                uiState = uiState,
                                modifier = Modifier.fillMaxWidth()
                            )

                            ControlButtons(
                                timerEngine = timerEngine,
                                uiState = uiState,
                                onBackToList = onBackToList,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // 縦画面レイアウト
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MainTimerPanel(
                            uiState = uiState,
                            stepColor = stepColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        StepsPreviewList(
                            uiState = uiState,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        ControlButtons(
                            timerEngine = timerEngine,
                            uiState = uiState,
                            onBackToList = onBackToList,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainTimerPanel(
    uiState: TimerUiState,
    stepColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                width = if (uiState.isFlashActive) 8.dp else 2.dp,
                color = if (uiState.isFlashActive) Color.White else if (uiState.isFinished) Color(0xFF00E676) else stepColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isFinished) {
                // 完了時表示
                Text(
                    text = "お疲れ様でした！",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF00E676),
                    fontWeight = FontWeight.Bold
                )

                SevenSegmentDisplay(
                    text = "00:00",
                    activeColor = Color(0xFF00E676),
                    inactiveColor = Color(0xFF282828),
                    digitWidth = 40.dp,
                    digitHeight = 76.dp
                )

                LinearProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Color(0xFF00E676),
                    trackColor = Color.DarkGray
                )

                Text(
                    text = "WORKOUT COMPLETE!",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            } else {
                // 通常タイマー時表示
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(stepColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.currentStep?.label ?: "準備中",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val timerColor = if (uiState.stepRemainingSeconds <= 3 && uiState.stepRemainingSeconds > 0) {
                    Color(0xFFFF5252)
                } else {
                    Color(0xFF00E676)
                }

                SevenSegmentDisplay(
                    text = formatTime(uiState.stepRemainingSeconds),
                    activeColor = timerColor,
                    inactiveColor = Color(0xFF282828),
                    digitWidth = 40.dp,
                    digitHeight = 76.dp
                )

                LinearProgressIndicator(
                    progress = { uiState.stepProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = stepColor,
                    trackColor = Color.DarkGray
                )

                Text(
                    text = if (uiState.currentBpm > 0) "${uiState.currentBpm} BPM" else "BPM オフ",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (uiState.isFlashActive) Color.Yellow else Color.Cyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StepsPreviewList(
    uiState: TimerUiState,
    modifier: Modifier = Modifier
) {
    val nextStep = uiState.nextStep
    val nextNextStep = uiState.nextNextStep

    val isHeaderVisible = !uiState.isFinished && (nextStep != null || nextNextStep != null)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = if (isHeaderVisible) "予告ステップ (${uiState.currentStepIndex + 1}/${uiState.totalStepCount})" else "",
            style = MaterialTheme.typography.labelLarge,
            color = Color.Gray,
            modifier = Modifier.alpha(if (isHeaderVisible) 1f else 0f)
        )

        // 【次】のカード
        StepPreviewCard(
            title = if (!uiState.isFinished && nextStep != null) "【次】${nextStep.label}" else "",
            step = if (!uiState.isFinished) nextStep else null,
            modifier = Modifier.alpha(if (!uiState.isFinished && nextStep != null) 1f else 0f)
        )

        // 【その次】のカード
        StepPreviewCard(
            title = if (!uiState.isFinished && nextNextStep != null) "【その次】${nextNextStep.label}" else "",
            step = if (!uiState.isFinished) nextNextStep else null,
            modifier = Modifier.alpha(if (!uiState.isFinished && nextNextStep != null) 1f else 0f)
        )
    }
}

@Composable
private fun StepPreviewCard(
    title: String,
    step: ExerciseStep?,
    modifier: Modifier = Modifier
) {
    val color = if (step != null) parseColorHex(step.colorHex) else Color.Transparent

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (step != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (step.bpm > 0) {
                        Text(
                            text = "${step.bpm}BPM",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Cyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = formatTime(step.durationSeconds),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = "00:00",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Transparent
                )
            }
        }
    }
}

@Composable
private fun ControlButtons(
    timerEngine: WorkoutTimerEngine,
    uiState: TimerUiState,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 前のステップへスキップ (完了時は非表示・位置保持)
        IconButton(
            onClick = { if (!uiState.isFinished) timerEngine.skipPrevious() },
            modifier = Modifier
                .size(56.dp)
                .alpha(if (uiState.isFinished) 0f else 1f)
        ) {
            Icon(
                imageVector = Icons.Default.FastRewind,
                contentDescription = "前へ",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // 中央ボタン (通常時: 再生/一時停止, 完了時: トップ画面へ戻る)
        IconButton(
            onClick = {
                if (uiState.isFinished) {
                    onBackToList()
                } else if (uiState.isRunning && !uiState.isPaused) {
                    timerEngine.pause()
                } else {
                    timerEngine.start()
                }
            },
            modifier = Modifier
                .size(72.dp)
                .background(
                    if (uiState.isFinished) {
                        Color(0xFF00E676)
                    } else if (uiState.isRunning && !uiState.isPaused) {
                        Color(0xFFFF9800)
                    } else {
                        Color(0xFF4CAF50)
                    },
                    CircleShape
                )
        ) {
            Icon(
                imageVector = if (uiState.isFinished) {
                    Icons.Default.Check
                } else if (uiState.isRunning && !uiState.isPaused) {
                    Icons.Default.Pause
                } else {
                    Icons.Default.PlayArrow
                },
                contentDescription = if (uiState.isFinished) "トップへ戻る" else if (uiState.isRunning && !uiState.isPaused) "一時停止" else "再生",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        // 次のステップへスキップ (完了時は非表示・位置保持)
        IconButton(
            onClick = { if (!uiState.isFinished) timerEngine.skipNext() },
            modifier = Modifier
                .size(56.dp)
                .alpha(if (uiState.isFinished) 0f else 1f)
        ) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "次へ",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}

private fun parseColorHex(hex: String): Color {
    return try {
        val colorInt = android.graphics.Color.parseColor(hex)
        Color(colorInt)
    } catch (e: Exception) {
        Color.Green
    }
}
