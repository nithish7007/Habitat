package com.example.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.LifeOsViewModel
import com.example.ui.auth.AnimatedSplashScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.components.AppleAmbientBackground
import com.example.ui.components.FloatingNavTab
import com.example.ui.components.LifeOSFloatingNavigation
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.screens.AchievementsScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.BackupExportScreen
import com.example.ui.screens.ClassTimetableScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.GlobalSearchScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.JobInterviewTrackerScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.LearnHubScreen
import com.example.ui.screens.LifeHubScreen
import com.example.ui.screens.MemoriesScreen
import com.example.ui.screens.MoneyScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.MotivationGoalScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.NutritionTrackerScreen
import com.example.ui.screens.PlanScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.RoutinesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmartNotificationCenterScreen
import com.example.ui.screens.StepDashboardScreen
import com.example.ui.screens.StreakDashboardScreen
import com.example.ui.screens.TrackScreen
import com.example.ui.screens.VideoDetailScreen
import com.example.ui.screens.WaterTrackerScreen
import com.example.ui.theme.LocalLifeOsGlass
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

private val primaryTabs = listOf(
    FloatingNavTab("home", "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    FloatingNavTab("life", "Life", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard),
    FloatingNavTab("learn", "Learn", Icons.Rounded.School, Icons.Outlined.School),
    FloatingNavTab("money", "Money", Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    FloatingNavTab("profile", "Profile", Icons.Rounded.Person, Icons.Outlined.Person)
)

private val primaryRouteSet = primaryTabs.map { it.route }.toSet()

@Composable
fun LifeOsApp(
    deepLink: Pair<String, String>? = null,
    onConsumeDeepLink: () -> Unit = {}
) {
    var splashDone by remember { mutableStateOf(false) }
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    if (!splashDone) {
        AnimatedSplashScreen(onSplashFinished = { splashDone = true })
        return
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        key(user.uid) {
            AuthenticatedLifeOsShell(
                userId = user.uid,
                displayName = user.displayName ?: "Builder",
                email = user.email ?: "",
                deepLink = deepLink,
                onConsumeDeepLink = onConsumeDeepLink
            )
        }
    }
}

@Composable
private fun AuthenticatedLifeOsShell(
    userId: String,
    displayName: String,
    email: String,
    deepLink: Pair<String, String>?,
    onConsumeDeepLink: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val viewModel: LifeOsViewModel = viewModel(
        key = userId,
        factory = LifeOsViewModel.factory(context, userId, displayName, email)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    val snackbarHostState = remember { SnackbarHostState() }
    val glass = LocalLifeOsGlass.current

    val navigateTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navigateSmart: (String) -> Unit = { route ->
        if (route in primaryRouteSet) {
            navigateTab(route)
        } else {
            navController.navigate(route) { launchSingleTop = true }
        }
    }

    // Handle notification tap deep-links directly into the exact LIFE OS screen
    LaunchedEffect(deepLink) {
        val dl = deepLink ?: return@LaunchedEffect
        val (route, arg) = dl
        when {
            route == "video_detail" && arg.isNotBlank() -> {
                navController.navigate("video_detail/$arg") { launchSingleTop = true }
            }
            route == "learn" && arg == "news" -> {
                navController.navigate("learn_news") { launchSingleTop = true }
            }
            route in primaryRouteSet -> navigateTab(route)
            else -> runCatching {
                navController.navigate(route) { launchSingleTop = true }
            }
        }
        onConsumeDeepLink()
    }

    AppleAmbientBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp
            val showFloatingBar = !useRail && currentRoute in primaryRouteSet

            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.padding(bottom = if (showFloatingBar) 84.dp else 8.dp)
                        )
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useRail) {
                            NavigationRail(
                                containerColor = glass.dockFillTop,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                primaryTabs.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateTab(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label
                                            )
                                        },
                                        label = { Text(dest.label) },
                                        modifier = Modifier.testTag("rail_${dest.route}")
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                enterTransition = {
                                    fadeIn(animationSpec = tween(220)) +
                                        slideInHorizontally(animationSpec = tween(220)) { it / 18 }
                                },
                                exitTransition = {
                                    fadeOut(animationSpec = tween(160))
                                },
                                popEnterTransition = {
                                    fadeIn(animationSpec = tween(200))
                                },
                                popExitTransition = {
                                    fadeOut(animationSpec = tween(180)) +
                                        slideOutHorizontally(animationSpec = tween(180)) { it / 18 }
                                }
                            ) {
                                // -------- 5 PRIMARY APPLE LIQUID GLASS TABS --------
                                composable("home") {
                                    DashboardScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        snackbarHostState = snackbarHostState,
                                        onNavigateToPlan = { navController.navigate("plan") },
                                        onNavigateToTrack = { navController.navigate("track") },
                                        onNavigateToFocus = { navController.navigate("focus") },
                                        onNavigateToGoals = { navController.navigate("goals") },
                                        onNavigateToMotivationGoal = { navController.navigate("motivation_goal") },
                                        onNavigateToReminders = { navController.navigate("notifications") },
                                        onNavigateToSearch = { navController.navigate("search") },
                                        onNavigateToMore = { navController.navigate("more") },
                                        onNavigateRoute = { navigateSmart(it) }
                                    )
                                }
                                composable("life") {
                                    LifeHubScreen(
                                        state = state,
                                        onNavigate = { navigateSmart(it) }
                                    )
                                }
                                composable("learn") {
                                    LearnHubScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        onNavigate = { navigateSmart(it) }
                                    )
                                }
                                composable("learn_news") {
                                    LearnHubScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        onNavigate = { navigateSmart(it) },
                                        initialSection = 1
                                    )
                                }
                                composable("money") {
                                    MoneyScreen(
                                        state = state,
                                        viewModel = viewModel
                                    )
                                }
                                composable("profile") {
                                    ProfileScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        snackbarHostState = snackbarHostState,
                                        onNavigate = { navigateSmart(it) },
                                        onSignOut = {
                                            signOutUser(
                                                credentialManager = credentialManager,
                                                scope = scope,
                                                onBeforeSignOut = { viewModel.stopSyncForSignOut() }
                                            )
                                        }
                                    )
                                }

                                // -------- PRODUCTIVITY & HEALTH MODULES --------
                                composable("plan") {
                                    PlanScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        snackbarHostState = snackbarHostState,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("track") {
                                    TrackScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("focus") {
                                    FocusScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("insights") {
                                    InsightsScreen(
                                        state = state,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("streak") {
                                    StreakDashboardScreen(
                                        state = state,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("water") {
                                    WaterTrackerScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("steps") {
                                    StepDashboardScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("nutrition") {
                                    NutritionTrackerScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                // -------- CAREER, LEARNING & AI --------
                                composable("timetable") {
                                    ClassTimetableScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("jobs") {
                                    JobInterviewTrackerScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("ai_assistant") {
                                    AiAssistantScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        snackbarHostState = snackbarHostState,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable(
                                    route = "video_detail/{payload}",
                                    arguments = listOf(navArgument("payload") { type = NavType.StringType })
                                ) { entry ->
                                    val encoded = entry.arguments?.getString("payload").orEmpty()
                                    VideoDetailScreen(
                                        encodedPayload = encoded,
                                        state = state,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                // -------- MEMORIES, NOTES, GOALS, MOTIVATION & NOTIFICATIONS --------
                                composable("memories") {
                                    MemoriesScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("notes") {
                                    NotesScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("routines") {
                                    RoutinesScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("goals") {
                                    GoalsScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("motivation_goal") {
                                    MotivationGoalScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        snackbarHostState = snackbarHostState,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("journal") {
                                    JournalScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        snackbarHostState = snackbarHostState,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("notifications") {
                                    SmartNotificationCenterScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        onNavigate = { navigateSmart(it) },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("reminders") {
                                    RemindersScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        snackbarHostState = snackbarHostState,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                // -------- SYSTEM & UTILITIES --------
                                composable("more") {
                                    MoreHubScreen(
                                        state = state,
                                        onNavigate = { navigateSmart(it) },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("achievements") {
                                    AchievementsScreen(
                                        state = state,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("search") {
                                    GlobalSearchScreen(
                                        state = state,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("backup") {
                                    BackupExportScreen(
                                        viewModel = viewModel,
                                        snackbarHostState = snackbarHostState,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("settings") {
                                    SettingsScreen(
                                        state = state,
                                        viewModel = viewModel,
                                        snackbarHostState = snackbarHostState,
                                        onSignOut = {
                                            signOutUser(
                                                credentialManager = credentialManager,
                                                scope = scope,
                                                onBeforeSignOut = { viewModel.stopSyncForSignOut() }
                                            )
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Apple iOS-style Liquid Glass Bottom Capsule Dock
                if (showFloatingBar) {
                    LifeOSFloatingNavigation(
                        tabs = primaryTabs,
                        currentRoute = currentRoute,
                        onSelect = { navigateTab(it) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
