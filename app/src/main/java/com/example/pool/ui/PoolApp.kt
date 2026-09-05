package com.example.pool.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pool.data.AffairType
import com.example.pool.data.PlannerRepository
import com.example.pool.data.schedule.ScheduleRepository
import com.example.pool.ui.affair.AffairEditScreen
import com.example.pool.ui.affair.AffairHubScreen
import com.example.pool.ui.affair.AffairTypeListScreen
import com.example.pool.ui.course.CardColorSettingsScreen
import com.example.pool.ui.course.CourseEditScreen
import com.example.pool.ui.course.CourseManagementScreen
import com.example.pool.ui.course.CourseViewModel
import com.example.pool.ui.course.CourseViewModelFactory
import com.example.pool.ui.course.ScheduleGridScreen
import com.example.pool.ui.course.ScheduleSettingsScreen
import com.example.pool.ui.course.SemesterSettingsScreen
import com.example.pool.ui.course.TimeSlotSettingsScreen
import com.example.pool.ui.course.importing.ScheduleImportScreen
import com.example.pool.ui.home.AppPreferencesScreen
import com.example.pool.ui.home.HomeScreen
import com.example.pool.ui.task.TaskEditScreen
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PoolApp(
    repository: PlannerRepository,
    scheduleRepository: ScheduleRepository,
    widgetRoute: String? = null,
    widgetLaunchKey: Int = 0,
    onThemeChanged: (String) -> Unit = {},
) {
    val navController = rememberNavController()

    LaunchedEffect(widgetLaunchKey) {
        if (widgetLaunchKey == 0) return@LaunchedEffect
        val route = NavRoutes.resolveWidgetRoute(widgetRoute)
        if (route == NavRoutes.HOME) {
            navController.popBackStack(NavRoutes.HOME, inclusive = false)
        } else {
            try {
                navController.navigate(route) {
                    launchSingleTop = true
                }
            } catch (_: IllegalArgumentException) {
                navController.popBackStack(NavRoutes.HOME, inclusive = false)
            }
        }
    }

    NavHost(navController = navController, startDestination = NavRoutes.HOME) {
        composable(NavRoutes.HOME) {
            HomeScreen(
                plannerRepository = repository,
                scheduleRepository = scheduleRepository,
                onOpenAffairs = { navController.navigate(NavRoutes.AFFAIRS) },
                onOpenCourses = { navController.navigate(NavRoutes.COURSES) },
                onOpenPreferences = { navController.navigate(NavRoutes.APP_PREFERENCES) },
                onEditCourse = { courseId ->
                    navController.navigate(NavRoutes.courseEdit(courseId))
                },
                onEditEvent = { affairId ->
                    navController.navigate(
                        NavRoutes.affairEdit(AffairType.EVENT, affairId),
                    )
                },
                onEditTask = { taskId ->
                    navController.navigate(NavRoutes.taskEdit(taskId))
                },
                onEditOpportunity = { affairId ->
                    navController.navigate(NavRoutes.affairEdit(AffairType.OPPORTUNITY, affairId))
                },
                onEditReminder = { affairId ->
                    navController.navigate(NavRoutes.affairEdit(AffairType.REMINDER, affairId))
                },
            )
        }
        composable(NavRoutes.AFFAIRS) {
            AffairHubScreen(
                onBack = { navController.popBackStack() },
                onOpenType = { type ->
                    navController.navigate(NavRoutes.affairList(type))
                },
            )
        }
        composable(
            route = NavRoutes.AFFAIR_LIST,
            arguments = listOf(navArgument("affairType") { type = NavType.StringType }),
        ) { entry ->
            val typeName = entry.arguments?.getString("affairType").orEmpty()
            val affairType = NavRoutes.parseAffairTypeOrNull(typeName)
            LaunchedEffect(typeName) {
                if (affairType == null) {
                    navController.popBackStack(NavRoutes.HOME, inclusive = false)
                }
            }
            if (affairType == null) return@composable
            AffairTypeListScreen(
                affairType = affairType,
                repository = repository,
                onBack = { navController.popBackStack() },
                onAdd = {
                    when (affairType) {
                        AffairType.TASK -> navController.navigate(NavRoutes.taskEdit(0))
                        else -> navController.navigate(NavRoutes.affairEdit(affairType, 0))
                    }
                },
                onEdit = { id ->
                    when (affairType) {
                        AffairType.TASK -> navController.navigate(NavRoutes.taskEdit(id))
                        else -> navController.navigate(NavRoutes.affairEdit(affairType, id))
                    }
                },
            )
        }
        composable(
            route = NavRoutes.AFFAIR_EDIT,
            arguments = listOf(
                navArgument("affairType") { type = NavType.StringType },
                navArgument("affairId") { type = NavType.LongType },
            ),
        ) { entry ->
            val typeName = entry.arguments?.getString("affairType").orEmpty()
            val affairId = entry.arguments?.getLong("affairId") ?: 0L
            val affairType = NavRoutes.parseAffairTypeOrNull(typeName)
            LaunchedEffect(typeName) {
                if (affairType == null) {
                    navController.popBackStack(NavRoutes.HOME, inclusive = false)
                }
            }
            if (affairType == null) return@composable
            AffairEditScreen(
                affairType = affairType,
                affairId = affairId,
                repository = repository,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = NavRoutes.TASK_EDIT,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
        ) { entry ->
            val taskId = entry.arguments?.getLong("taskId") ?: 0L
            TaskEditScreen(
                taskId = taskId,
                repository = repository,
                onBack = { navController.popBackStack() },
            )
        }
        composable(NavRoutes.APP_PREFERENCES) {
            AppPreferencesScreen(
                onBack = { navController.popBackStack() },
                onThemeChanged = onThemeChanged,
            )
        }
        composable(NavRoutes.COURSES) {
            ScheduleGridScreen(
                scheduleRepository = scheduleRepository,
                onBack = { navController.popBackStack() },
                onAddCourse = { day, section ->
                    navController.navigate(
                        NavRoutes.courseEdit(
                            courseId = 0,
                            day = day ?: -1,
                            section = section ?: -1,
                        ),
                    )
                },
                onEditCourse = { courseId ->
                    navController.navigate(NavRoutes.courseEdit(courseId))
                },
                onOpenSettings = { navController.navigate(NavRoutes.SCHEDULE_SETTINGS) },
                onOpenImport = { navController.navigate(NavRoutes.SCHEDULE_IMPORT) },
            )
        }
        composable(NavRoutes.SCHEDULE_SETTINGS) {
            ScheduleSettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenSemesters = { navController.navigate(NavRoutes.SCHEDULE_SEMESTERS) },
                onOpenCourseManagement = { navController.navigate(NavRoutes.SCHEDULE_COURSE_MANAGEMENT) },
                onOpenTimeSlots = { navController.navigate(NavRoutes.SCHEDULE_TIME_SLOTS) },
                onOpenCardColors = { navController.navigate(NavRoutes.SCHEDULE_CARD_COLORS) },
                onOpenImport = { navController.navigate(NavRoutes.SCHEDULE_IMPORT) },
            )
        }
        composable(NavRoutes.SCHEDULE_IMPORT) {
            ScheduleImportScreen(
                scheduleRepository = scheduleRepository,
                onBack = { navController.popBackStack() },
                onImported = { navController.popBackStack() },
            )
        }
        composable(NavRoutes.SCHEDULE_CARD_COLORS) {
            val viewModel: CourseViewModel = viewModel(
                factory = CourseViewModelFactory(scheduleRepository),
            )
            CardColorSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(NavRoutes.SCHEDULE_SEMESTERS) {
            val viewModel: CourseViewModel = viewModel(
                factory = CourseViewModelFactory(scheduleRepository),
            )
            SemesterSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(NavRoutes.SCHEDULE_COURSE_MANAGEMENT) {
            val viewModel: CourseViewModel = viewModel(
                factory = CourseViewModelFactory(scheduleRepository),
            )
            CourseManagementScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEditCourse = { courseId ->
                    navController.navigate(NavRoutes.courseEdit(courseId))
                },
            )
        }
        composable(NavRoutes.SCHEDULE_TIME_SLOTS) {
            val viewModel: CourseViewModel = viewModel(
                factory = CourseViewModelFactory(scheduleRepository),
            )
            TimeSlotSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = NavRoutes.COURSE_EDIT,
            arguments = listOf(
                navArgument("courseId") { type = NavType.LongType },
                navArgument("day") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("section") {
                    type = NavType.IntType
                    defaultValue = -1
                },
            ),
        ) { entry ->
            val courseId = entry.arguments?.getLong("courseId") ?: 0L
            val day = entry.arguments?.getInt("day") ?: -1
            val section = entry.arguments?.getInt("section") ?: -1
            CourseEditScreen(
                courseId = courseId,
                prefilledDay = day,
                prefilledSection = section,
                scheduleRepository = scheduleRepository,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
