package com.example.intervaltimer2

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.intervaltimer2.audio.SoundManager
import com.example.intervaltimer2.data.TrainingMenu
import com.example.intervaltimer2.data.WorkoutRepository
import com.example.intervaltimer2.timer.WorkoutTimerEngine
import com.example.intervaltimer2.ui.editor.MenuEditorScreen
import com.example.intervaltimer2.ui.menu.MenuListScreen
import com.example.intervaltimer2.ui.theme.IntervalTimer2Theme
import com.example.intervaltimer2.ui.timer.WorkoutTimerScreen

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val soundManager = SoundManager()
    val timerEngine = WorkoutTimerEngine(soundManager)
    val repository = WorkoutRepository(application.applicationContext)

    override fun onCleared() {
        super.onCleared()
        timerEngine.stop()
        soundManager.release()
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            IntervalTimer2Theme {
                MainAppNavigation(
                    repository = viewModel.repository,
                    timerEngine = viewModel.timerEngine
                )
            }
        }
    }
}

@Composable
fun MainAppNavigation(
    repository: WorkoutRepository,
    timerEngine: WorkoutTimerEngine
) {
    val navController = rememberNavController()
    var editingMenu by remember { mutableStateOf<TrainingMenu?>(null) }

    NavHost(
        navController = navController,
        startDestination = "menu_list"
    ) {
        // 1. メニュー一覧画面
        composable("menu_list") {
            MenuListScreen(
                repository = repository,
                onStartWorkout = { menu ->
                    timerEngine.loadMenuAndStart(menu)
                    navController.navigate("workout_timer")
                },
                onEditMenu = { menu ->
                    editingMenu = menu
                    navController.navigate("menu_editor")
                }
            )
        }

        // 2. タイマー実行画面
        composable("workout_timer") {
            WorkoutTimerScreen(
                timerEngine = timerEngine,
                onBackToList = {
                    navController.popBackStack()
                }
            )
        }

        // 3. メニュー編集画面
        composable("menu_editor") {
            MenuEditorScreen(
                repository = repository,
                initialMenu = editingMenu,
                onSave = { savedMenu ->
                    repository.saveMenu(savedMenu)
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
