package com.example.intervaltimer2.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class WorkoutRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("interval_timer_prefs", Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val _menus = MutableStateFlow<List<TrainingMenu>>(emptyList())
    val menus: StateFlow<List<TrainingMenu>> = _menus.asStateFlow()

    private val _stepPresets = MutableStateFlow<List<String>>(emptyList())
    val stepPresets: StateFlow<List<String>> = _stepPresets.asStateFlow()

    init {
        loadMenus()
        loadStepPresets()
    }

    fun loadMenus() {
        val jsonString = prefs.getString(KEY_MENUS, null)
        if (jsonString.isNullOrBlank()) {
            val defaultMenus = createDefaultMenus()
            saveMenus(defaultMenus)
            _menus.value = defaultMenus
        } else {
            try {
                val loaded = json.decodeFromString<List<TrainingMenu>>(jsonString)
                _menus.value = loaded
            } catch (e: Exception) {
                e.printStackTrace()
                val defaultMenus = createDefaultMenus()
                saveMenus(defaultMenus)
                _menus.value = defaultMenus
            }
        }
    }

    fun saveMenu(menu: TrainingMenu) {
        val currentList = _menus.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == menu.id }
        if (index >= 0) {
            currentList[index] = menu
        } else {
            currentList.add(menu)
        }
        saveMenus(currentList)
        _menus.value = currentList
    }

    fun deleteMenu(menuId: String) {
        val updatedList = _menus.value.filterNot { it.id == menuId }
        saveMenus(updatedList)
        _menus.value = updatedList
    }

    fun duplicateMenu(menuId: String) {
        val target = _menus.value.find { it.id == menuId } ?: return
        val newMenu = target.copy(
            id = UUID.randomUUID().toString(),
            title = "${target.title} (コピー)",
            steps = target.steps.map { it.copy(id = UUID.randomUUID().toString()) }
        )
        saveMenu(newMenu)
    }

    fun moveMenuUp(menuId: String) {
        val currentList = _menus.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == menuId }
        if (index > 0) {
            val item = currentList.removeAt(index)
            currentList.add(index - 1, item)
            saveMenus(currentList)
            _menus.value = currentList
        }
    }

    fun moveMenuDown(menuId: String) {
        val currentList = _menus.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == menuId }
        if (index >= 0 && index < currentList.size - 1) {
            val item = currentList.removeAt(index)
            currentList.add(index + 1, item)
            saveMenus(currentList)
            _menus.value = currentList
        }
    }

    fun exportMenuToJson(menu: TrainingMenu): String {
        return json.encodeToString(menu)
    }

    fun importMenuFromJson(jsonString: String): Result<TrainingMenu> {
        return try {
            val imported = json.decodeFromString<TrainingMenu>(jsonString)
            // 新規IDを割り振って保存衝突を回避
            val newMenu = imported.copy(
                id = UUID.randomUUID().toString(),
                steps = imported.steps.map { it.copy(id = UUID.randomUUID().toString()) }
            )
            saveMenu(newMenu)
            Result.success(newMenu)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveMenus(list: List<TrainingMenu>) {
        val jsonString = json.encodeToString(list)
        prefs.edit().putString(KEY_MENUS, jsonString).apply()
    }

    private fun createDefaultMenus(): List<TrainingMenu> {
        return listOf(
            TrainingMenu(
                id = UUID.randomUUID().toString(),
                title = "30s-30s インターバル (3セット)",
                description = "30秒高強度ペダリングと30秒レストを繰り返す定番メニュー",
                steps = listOf(
                    ExerciseStep(label = "ウォームアップ", durationSeconds = 300, bpm = 90, colorHex = "#00ACC1"),
                    // セット1
                    ExerciseStep(label = "高回転 (ON)", durationSeconds = 30, bpm = 110, colorHex = "#E53935"),
                    ExerciseStep(label = "レスト (OFF)", durationSeconds = 30, bpm = 80, colorHex = "#1E88E5"),
                    ExerciseStep(label = "高回転 (ON)", durationSeconds = 30, bpm = 110, colorHex = "#E53935"),
                    ExerciseStep(label = "レスト (OFF)", durationSeconds = 30, bpm = 80, colorHex = "#1E88E5"),
                    ExerciseStep(label = "高回転 (ON)", durationSeconds = 30, bpm = 110, colorHex = "#E53935"),
                    ExerciseStep(label = "レスト (OFF)", durationSeconds = 30, bpm = 80, colorHex = "#1E88E5"),
                    // クールダウン
                    ExerciseStep(label = "クールダウン", durationSeconds = 300, bpm = 85, colorHex = "#757575")
                )
            ),
            TrainingMenu(
                id = UUID.randomUUID().toString(),
                title = "SST 10分 ワークアウト",
                description = "SST(Sweet Spot Training)領域での10分維持メニュー",
                steps = listOf(
                    ExerciseStep(label = "ウォームアップ", durationSeconds = 300, bpm = 85, colorHex = "#00ACC1"),
                    ExerciseStep(label = "ランプアップ", durationSeconds = 180, bpm = 95, colorHex = "#FDD835"),
                    ExerciseStep(label = "SST メイン (90rpm)", durationSeconds = 600, bpm = 90, colorHex = "#FB8C00"),
                    ExerciseStep(label = "クールダウン", durationSeconds = 300, bpm = 80, colorHex = "#757575")
                )
            )
        )
    }

    companion object {
        private const val KEY_MENUS = "saved_training_menus"
        private const val KEY_STEP_PRESETS = "saved_step_presets"
    }

    fun loadStepPresets(): List<String> {
        val jsonString = prefs.getString(KEY_STEP_PRESETS, null)
        val list = if (jsonString.isNullOrBlank()) {
            listOf("ウォームアップ", "高回転", "高トルク", "レスト", "クールダウン")
        } else {
            try {
                json.decodeFromString<List<String>>(jsonString)
            } catch (e: Exception) {
                listOf("ウォームアップ", "高回転", "高トルク", "レスト", "クールダウン")
            }
        }
        _stepPresets.value = list
        return list
    }

    fun saveStepPresets(presets: List<String>) {
        val jsonString = json.encodeToString(presets)
        prefs.edit().putString(KEY_STEP_PRESETS, jsonString).apply()
        _stepPresets.value = presets
    }

    fun addStepPreset(preset: String) {
        if (preset.isBlank()) return
        val current = _stepPresets.value.toMutableList()
        if (!current.contains(preset)) {
            current.add(preset)
            saveStepPresets(current)
        }
    }

    fun removeStepPreset(preset: String) {
        val current = _stepPresets.value.filterNot { it == preset }
        saveStepPresets(current)
    }
}
