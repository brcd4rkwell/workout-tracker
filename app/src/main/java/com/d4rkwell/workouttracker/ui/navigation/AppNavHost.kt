package com.d4rkwell.workouttracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.d4rkwell.workouttracker.WorkoutApplication
import com.d4rkwell.workouttracker.timer.TimerController
import com.d4rkwell.workouttracker.ui.screens.dayeditor.DayEditorScreen
import com.d4rkwell.workouttracker.ui.screens.dayeditor.DayEditorViewModel
import com.d4rkwell.workouttracker.ui.screens.exerciseeditor.ExerciseEditorScreen
import com.d4rkwell.workouttracker.ui.screens.exerciseeditor.ExerciseEditorViewModel
import com.d4rkwell.workouttracker.ui.screens.history.HistoryScreen
import com.d4rkwell.workouttracker.ui.screens.history.HistoryViewModel
import com.d4rkwell.workouttracker.ui.screens.historydetail.HistoryDetailScreen
import com.d4rkwell.workouttracker.ui.screens.historydetail.HistoryDetailViewModel
import com.d4rkwell.workouttracker.ui.screens.weekdetail.WeekDetailScreen
import com.d4rkwell.workouttracker.ui.screens.weekdetail.WeekDetailViewModel
import com.d4rkwell.workouttracker.ui.screens.weeklist.WeekListScreen
import com.d4rkwell.workouttracker.ui.screens.weeklist.WeekListViewModel
import com.d4rkwell.workouttracker.ui.screens.weightprogress.WeightProgressScreen
import com.d4rkwell.workouttracker.ui.screens.weightprogress.WeightProgressViewModel
import com.d4rkwell.workouttracker.ui.screens.workoutsession.WorkoutSessionScreen
import com.d4rkwell.workouttracker.ui.screens.workoutsession.WorkoutSessionViewModel
import com.d4rkwell.workouttracker.util.GenericViewModelFactory

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val app = context.applicationContext as WorkoutApplication
    val repository = app.repository
    val timerController = TimerController.getInstance(context)

    NavHost(navController = navController, startDestination = Destinations.WEEK_LIST) {

        composable(Destinations.WEEK_LIST) {
            val vm: WeekListViewModel = viewModel(factory = GenericViewModelFactory { WeekListViewModel(repository) })
            WeekListScreen(
                viewModel = vm,
                onOpenWeek = { weekId -> navController.navigate(Destinations.weekDetail(weekId)) },
                onOpenHistory = { navController.navigate(Destinations.HISTORY) },
                onOpenWeightProgress = { navController.navigate(Destinations.WEIGHT_PROGRESS) }
            )
        }

        composable(
            route = Destinations.WEEK_DETAIL,
            arguments = listOf(navArgument("weekId") { type = NavType.LongType })
        ) { backStackEntry ->
            val weekId = backStackEntry.arguments?.getLong("weekId") ?: return@composable
            val vm: WeekDetailViewModel = viewModel(
                factory = GenericViewModelFactory { WeekDetailViewModel(repository, weekId) }
            )
            WeekDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenDay = { dayId -> navController.navigate(Destinations.dayEditor(dayId)) }
            )
        }

        composable(
            route = Destinations.DAY_EDITOR,
            arguments = listOf(navArgument("dayId") { type = NavType.LongType })
        ) { backStackEntry ->
            val dayId = backStackEntry.arguments?.getLong("dayId") ?: return@composable
            val vm: DayEditorViewModel = viewModel(
                factory = GenericViewModelFactory { DayEditorViewModel(repository, dayId) }
            )
            DayEditorScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onAddExercise = { navController.navigate(Destinations.exerciseEditor(dayId)) },
                onOpenExercise = { exerciseId -> navController.navigate(Destinations.exerciseEditor(dayId, exerciseId)) },
                onStartWorkout = { navController.navigate(Destinations.workoutSession(dayId)) }
            )
        }

        composable(
            route = Destinations.EXERCISE_EDITOR,
            arguments = listOf(
                navArgument("dayId") { type = NavType.LongType },
                navArgument("exerciseId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val dayId = backStackEntry.arguments?.getLong("dayId") ?: return@composable
            val exerciseId = backStackEntry.arguments?.getLong("exerciseId") ?: -1L
            val vm: ExerciseEditorViewModel = viewModel(
                factory = GenericViewModelFactory { ExerciseEditorViewModel(repository, dayId, exerciseId) }
            )
            ExerciseEditorScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Destinations.WORKOUT_SESSION,
            arguments = listOf(navArgument("dayId") { type = NavType.LongType })
        ) { backStackEntry ->
            val dayId = backStackEntry.arguments?.getLong("dayId") ?: return@composable
            val vm: WorkoutSessionViewModel = viewModel(
                factory = GenericViewModelFactory { WorkoutSessionViewModel(repository, timerController, dayId) }
            )
            WorkoutSessionScreen(
                viewModel = vm,
                onExit = { navController.popBackStack() },
                onFinished = {
                    navController.popBackStack(Destinations.dayEditor(dayId), inclusive = false)
                }
            )
        }

        composable(Destinations.HISTORY) {
            val vm: HistoryViewModel = viewModel(
                factory = GenericViewModelFactory { HistoryViewModel(repository) }
            )
            HistoryScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenSession = { sessionId -> navController.navigate(Destinations.historyDetail(sessionId)) }
            )
        }

        composable(
            route = Destinations.HISTORY_DETAIL,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
            val vm: HistoryDetailViewModel = viewModel(
                factory = GenericViewModelFactory { HistoryDetailViewModel(repository, sessionId) }
            )
            HistoryDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.WEIGHT_PROGRESS) {
            val vm: WeightProgressViewModel = viewModel(
                factory = GenericViewModelFactory { WeightProgressViewModel(repository) }
            )
            WeightProgressScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
