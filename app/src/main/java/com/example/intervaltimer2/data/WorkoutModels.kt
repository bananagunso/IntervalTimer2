package com.example.intervaltimer2.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ExerciseStep(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val durationSeconds: Int,
    val bpm: Int = 0, // 0 means no BPM metronome
    val colorHex: String = "#4CAF50" // Default Green
)

@Serializable
data class TrainingMenu(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val steps: List<ExerciseStep> = emptyList()
) {
    val totalDurationSeconds: Int
        get() = steps.sumOf { it.durationSeconds }
}

object StepColorPalette {
    val colors = listOf(
        "#FF4B00" to "赤橙 (高強度/VO2Max)",
        "#F6AA00" to "橙 (SST/閾値)",
        "#FFF100" to "黄 (テンポ)",
        "#03AF7A" to "緑 (エンデュランス)",
        "#4DC4FF" to "明るい青 (ウォームアップ)",
        "#005AFF" to "青 (レスト/リカバリー)",
        "#990099" to "紫 (高回転/スプリント)",
        "#FF8082" to "ピンク (高トルク)",
        "#804000" to "茶 (ランプアップ)",
        "#84919E" to "グレー (クールダウン)"
    )

    fun getColorHex(index: Int): String {
        return colors[index % colors.size].first
    }
}
