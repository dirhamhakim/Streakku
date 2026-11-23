package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.domain.model.ThemeMode
import com.example.domain.time.IndonesianDateFormatter
import com.example.ui.StreakkuViewModel
import com.example.ui.components.ScreenHeader
import com.example.ui.navigation.StreakkuRoutes
import com.example.ui.navigation.TopLevelTab
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.HabitFormScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.StreakkuTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as StreakkuApplication).container

        setContent {
            val viewModel: StreakkuViewModel = viewModel(
                factory = StreakkuViewModel.provideFactory(
                    habitRepository = container.habitRepository,
                    settingsRepository = container.settingsRepository,
                    dateTicker = container.dateTicker,
                    appContext = applicationContext
                )
            )

            // Refresh device local date whenever Activity resumes (e.g., after midnight sleep)
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.refreshTodayDate()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val settings by viewModel.settingsState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SideEffect {
                val barStyle = if (useDarkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(
                    statusBarStyle = barStyle,
                    navigationBarStyle = barStyle
                )
            }

            StreakkuTheme(themeMode = settings.themeMode) {
                StreakkuApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun StreakkuApp(viewModel: StreakkuViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val todayState by viewModel.todayUiState.collectAsStateWithLifecycle()
    val calendarState by viewModel.calendarUiState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyUiState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val backupState by viewModel.backupDialogState.collectAsStateWithLifecycle()

    val isTopLevel = TopLevelTab.entries.any { it.route == currentRoute }

    val navigateToTab: (TopLevelTab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navBarItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val navRailItemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (isTopLevel && useNavRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    TopLevelTab.entries.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(tab) },
                            modifier = Modifier.testTag(tab.testTag),
                            colors = navRailItemColors,
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = stringResource(tab.labelRes)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    if (isTopLevel) {
                        val (headerTitle, headerSubtitle) = when (currentRoute) {
                            StreakkuRoutes.ROUTE_CALENDAR -> {
                                stringResource(R.string.calendar_title) to null
                            }
                            StreakkuRoutes.ROUTE_HISTORY -> {
                                val selectedHabitName = historyState.selectedHabitFilterId?.let { id ->
                                    historyState.habits.firstOrNull { it.id == id }?.name
                                }
                                stringResource(R.string.history_title) to (
                                    selectedHabitName ?: stringResource(R.string.history_subtitle_all)
                                )
                            }
                            else -> {
                                stringResource(R.string.nav_today) to
                                    IndonesianDateFormatter.formatFullDayFullMonth(todayState.today)
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ScreenHeader(
                                title = headerTitle,
                                subtitle = headerSubtitle,
                                onOpenSettings = {
                                    navController.navigate(StreakkuRoutes.ROUTE_SETTINGS)
                                },
                                modifier = Modifier.statusBarsPadding()
                            )
                        }
                    }
                },
                bottomBar = {
                    if (isTopLevel && !useNavRail) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp
                            ) {
                                TopLevelTab.entries.forEach { tab ->
                                    val selected = currentRoute == tab.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(tab) },
                                        modifier = Modifier.testTag(tab.testTag),
                                        colors = navBarItemColors,
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = stringResource(tab.labelRes)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(tab.labelRes),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 11.sp,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (isTopLevel) {
                        FloatingActionButton(
                            onClick = {
                                navController.navigate(StreakkuRoutes.habitFormRoute(null))
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 6.dp
                            ),
                            modifier = Modifier
                                .size(60.dp)
                                .testTag("add_habit_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.cd_add_habit),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = StreakkuRoutes.ROUTE_TODAY,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isTopLevel) innerPadding else PaddingValues(0.dp))
                ) {
                    composable(StreakkuRoutes.ROUTE_TODAY) {
                        TodayScreen(
                            uiState = todayState,
                            onCycleStatus = { card -> viewModel.cycleTodayStatus(card) },
                            onEditHabit = { habitId ->
                                navController.navigate(StreakkuRoutes.habitFormRoute(habitId))
                            },
                            onAddHabit = {
                                navController.navigate(StreakkuRoutes.habitFormRoute(null))
                            }
                        )
                    }

                    composable(StreakkuRoutes.ROUTE_CALENDAR) {
                        CalendarScreen(
                            uiState = calendarState,
                            onSelectHabit = { habitId -> viewModel.selectCalendarHabit(habitId) },
                            onPreviousMonth = { viewModel.previousCalendarMonth() },
                            onNextMonth = { viewModel.nextCalendarMonth() },
                            onJumpToday = { viewModel.jumpCalendarToToday() },
                            onSetEntryStatus = { habit, date, status ->
                                viewModel.setEntryStatus(habit, date, status)
                            },
                            onAddHabit = {
                                navController.navigate(StreakkuRoutes.habitFormRoute(null))
                            }
                        )
                    }

                    composable(StreakkuRoutes.ROUTE_HISTORY) {
                        HistoryScreen(
                            uiState = historyState,
                            onSelectHabitFilter = { habitId -> viewModel.selectHistoryFilter(habitId) }
                        )
                    }

                    composable(
                        route = StreakkuRoutes.ROUTE_HABIT_FORM,
                        arguments = listOf(
                            navArgument(StreakkuRoutes.ARG_HABIT_ID) {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { entry ->
                        val habitId = entry.arguments?.getString(StreakkuRoutes.ARG_HABIT_ID)
                        HabitFormScreen(
                            habitId = habitId,
                            loadHabit = { id -> viewModel.loadHabitById(id) },
                            onSave = { existing, name, colorHex, autoFail, backfill, weekdays ->
                                viewModel.saveHabit(
                                    existingHabit = existing,
                                    name = name,
                                    colorHex = colorHex,
                                    autoFail = autoFail,
                                    backfillLimitDays = backfill,
                                    activeWeekdays = weekdays,
                                    onSaved = { navController.popBackStack() }
                                )
                            },
                            onToggleArchive = { id, archived ->
                                viewModel.setHabitArchived(id, archived) {
                                    navController.popBackStack()
                                }
                            },
                            onDelete = { id ->
                                viewModel.deleteHabit(id) {
                                    navController.popBackStack()
                                }
                            },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(StreakkuRoutes.ROUTE_SETTINGS) {
                        SettingsScreen(
                            settings = settings,
                            today = todayState.today,
                            backupState = backupState,
                            onSelectTheme = { viewModel.setThemeMode(it) },
                            onSetReminderEnabled = { viewModel.setReminderEnabled(it) },
                            onSetReminderTime = { h, m -> viewModel.setReminderTime(h, m) },
                            onCreateBackupJson = { viewModel.createBackupJson() },
                            onExportSuccess = { h, e -> viewModel.notifyExportSuccess(h, e) },
                            onExportError = { viewModel.notifyExportError(it) },
                            onPrepareImportJson = { viewModel.prepareImportFromJson(it) },
                            onImportReadError = { viewModel.notifyImportReadError(it) },
                            onConfirmImport = { viewModel.confirmPendingImport() },
                            onDismissImportDialog = { viewModel.dismissPendingImport() },
                            onDismissBackupError = { viewModel.dismissBackupError() },
                            onConsumeFeedback = { viewModel.consumeFeedbackMessage() },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
