package com.example.intervaltimer2.timer

import com.example.intervaltimer2.audio.SoundManager
import com.example.intervaltimer2.data.ExerciseStep
import com.example.intervaltimer2.data.TrainingMenu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimerUiState(
    val menuTitle: String = "",
    val currentStep: ExerciseStep? = null,
    val nextStep: ExerciseStep? = null,
    val nextNextStep: ExerciseStep? = null,
    val stepRemainingSeconds: Int = 0,
    val stepTotalSeconds: Int = 0,
    val stepProgress: Float = 0f,
    val totalRemainingSeconds: Int = 0,
    val totalDurationSeconds: Int = 0,
    val currentBpm: Int = 0,
    val isFlashActive: Boolean = false,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
    val currentStepIndex: Int = 0,
    val totalStepCount: Int = 0,
    val isPrepPhase: Boolean = false
)

class WorkoutTimerEngine(
    private val soundManager: SoundManager
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null
    private var flashResetJob: Job? = null

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var currentMenu: TrainingMenu? = null
    private var currentStepIndex = -1 // -1はスタート準備（3秒カウントダウン）

    private val prepStep = ExerciseStep(
        id = "prep_step",
        label = "スタート準備",
        durationSeconds = 3,
        bpm = 0,
        colorHex = "#00ACC1"
    )

    // 各ステップ内での状態管理
    private var stepStartNanos = 0L
    private var pausedElapsedNanos = 0L
    private var lastBeatIndex = -1
    private var lastCountdownSecond = -1

    fun loadMenuAndStart(menu: TrainingMenu) {
        stop()
        currentMenu = menu
        currentStepIndex = -1 // スタート準備から開始
        updateUiStateForCurrentStep(resetElapsed = true)
        start()
    }

    fun start() {
        val menu = currentMenu ?: return
        if (menu.steps.isEmpty()) return

        if (_uiState.value.isFinished) {
            currentStepIndex = -1
            updateUiStateForCurrentStep(resetElapsed = true)
        }

        if (_uiState.value.isPaused) {
            stepStartNanos = System.nanoTime() - pausedElapsedNanos
        } else {
            stepStartNanos = System.nanoTime()
            pausedElapsedNanos = 0L
            lastBeatIndex = -1
            lastCountdownSecond = -1
        }

        _uiState.update { it.copy(isRunning = true, isPaused = false, isFinished = false) }

        timerJob?.cancel()
        timerJob = scope.launch {
            runTimerLoop()
        }
    }

    fun pause() {
        if (!_uiState.value.isRunning || _uiState.value.isPaused) return
        pausedElapsedNanos = System.nanoTime() - stepStartNanos
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
    }

    fun stop() {
        timerJob?.cancel()
        flashResetJob?.cancel()
        currentStepIndex = -1
        pausedElapsedNanos = 0L
        lastBeatIndex = -1
        lastCountdownSecond = -1
        _uiState.update { it.copy(isRunning = false, isPaused = false, isFinished = false) }
        updateUiStateForCurrentStep(resetElapsed = true)
    }

    fun skipNext() {
        val menu = currentMenu ?: return
        if (currentStepIndex < menu.steps.size - 1) {
            currentStepIndex++
            stepStartNanos = System.nanoTime()
            pausedElapsedNanos = 0L
            lastBeatIndex = -1
            lastCountdownSecond = -1
            updateUiStateForCurrentStep(resetElapsed = true)
        } else {
            finishWorkout()
        }
    }

    fun skipPrevious() {
        val menu = currentMenu ?: return
        if (currentStepIndex > 0) {
            currentStepIndex--
            stepStartNanos = System.nanoTime()
            pausedElapsedNanos = 0L
            lastBeatIndex = -1
            lastCountdownSecond = -1
            updateUiStateForCurrentStep(resetElapsed = true)
        } else if (currentStepIndex == 0) {
            currentStepIndex = -1
            stepStartNanos = System.nanoTime()
            pausedElapsedNanos = 0L
            lastBeatIndex = -1
            lastCountdownSecond = -1
            updateUiStateForCurrentStep(resetElapsed = true)
        }
    }

    private suspend fun runTimerLoop() {
        val tickIntervalMs = 20L

        while (_uiState.value.isRunning && !_uiState.value.isPaused) {
            val nowNanos = System.nanoTime()
            val stepElapsedNanos = nowNanos - stepStartNanos
            val stepElapsedSeconds = (stepElapsedNanos / 1_000_000_000L).toInt()

            val menu = currentMenu ?: break
            val step = if (currentStepIndex == -1) prepStep else menu.steps.getOrNull(currentStepIndex) ?: break

            val remainingSeconds = (step.durationSeconds - stepElapsedSeconds).coerceAtLeast(0)

            // 1. BPMメトロノーム判定
            if (step.bpm > 0) {
                val nanosPerBeat = (60.0 * 1_000_000_000.0 / step.bpm).toLong()
                val currentBeatIndex = (stepElapsedNanos / nanosPerBeat).toInt()

                if (currentBeatIndex > lastBeatIndex) {
                    lastBeatIndex = currentBeatIndex
                    soundManager.playBeat()
                    triggerFlash()
                }
            }

            // 2. カウントダウン音判定 (残り 3秒, 2秒, 1秒)
            if (remainingSeconds in 1..3 && remainingSeconds != lastCountdownSecond) {
                lastCountdownSecond = remainingSeconds
                soundManager.playCountdown()
            }

            // 3. ステップ終了判定
            if (stepElapsedNanos >= step.durationSeconds * 1_000_000_000L) {
                soundManager.playStepChange()
                if (currentStepIndex < menu.steps.size - 1) {
                    currentStepIndex++
                    stepStartNanos = System.nanoTime()
                    lastBeatIndex = -1
                    lastCountdownSecond = -1
                    updateUiStateForCurrentStep(resetElapsed = true)
                    continue
                } else {
                    finishWorkout()
                    break
                }
            }

            // UI State更新
            val progress = (stepElapsedNanos.toFloat() / (step.durationSeconds * 1_000_000_000L)).coerceIn(0f, 1f)
            val totalRemaining = calculateTotalRemainingSeconds(currentStepIndex, remainingSeconds)

            _uiState.update {
                it.copy(
                    stepRemainingSeconds = remainingSeconds,
                    stepProgress = progress,
                    totalRemainingSeconds = totalRemaining
                )
            }

            delay(tickIntervalMs)
        }
    }

    private fun triggerFlash() {
        flashResetJob?.cancel()
        _uiState.update { it.copy(isFlashActive = true) }
        flashResetJob = scope.launch {
            delay(90)
            _uiState.update { it.copy(isFlashActive = false) }
        }
    }

    private fun finishWorkout() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                isRunning = false,
                isPaused = false,
                isFinished = true,
                stepRemainingSeconds = 0,
                stepProgress = 1f,
                totalRemainingSeconds = 0
            )
        }
    }

    private fun updateUiStateForCurrentStep(resetElapsed: Boolean) {
        val menu = currentMenu ?: return

        val isPrep = currentStepIndex == -1
        val currentStep = if (isPrep) prepStep else menu.steps.getOrNull(currentStepIndex)
        val nextStep = if (isPrep) menu.steps.getOrNull(0) else menu.steps.getOrNull(currentStepIndex + 1)
        val nextNextStep = if (isPrep) menu.steps.getOrNull(1) else menu.steps.getOrNull(currentStepIndex + 2)

        val totalDuration = menu.totalDurationSeconds
        val totalRemaining = calculateTotalRemainingSeconds(currentStepIndex, currentStep?.durationSeconds ?: 0)

        _uiState.update {
            it.copy(
                menuTitle = menu.title,
                currentStep = currentStep,
                nextStep = nextStep,
                nextNextStep = nextNextStep,
                stepRemainingSeconds = currentStep?.durationSeconds ?: 0,
                stepTotalSeconds = currentStep?.durationSeconds ?: 0,
                stepProgress = if (resetElapsed) 0f else it.stepProgress,
                totalRemainingSeconds = totalRemaining,
                totalDurationSeconds = totalDuration,
                currentBpm = currentStep?.bpm ?: 0,
                currentStepIndex = if (isPrep) 0 else currentStepIndex,
                totalStepCount = menu.steps.size,
                isPrepPhase = isPrep
            )
        }
    }

    private fun calculateTotalRemainingSeconds(fromIndex: Int, currentStepRemaining: Int): Int {
        val menu = currentMenu ?: return 0
        var remaining = if (fromIndex == -1) 0 else currentStepRemaining
        val startIdx = if (fromIndex == -1) 0 else (fromIndex + 1)
        for (i in startIdx until menu.steps.size) {
            remaining += menu.steps[i].durationSeconds
        }
        return remaining
    }
}
