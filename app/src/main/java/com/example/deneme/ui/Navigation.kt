package com.example.deneme.ui

import android.util.Log
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.deneme.ui.screens.AddProblemScreen
import com.example.deneme.ui.screens.HomeScreen
import com.example.deneme.ui.screens.LoginScreen
import com.example.deneme.ui.screens.ProblemDetailScreen
import com.example.deneme.ui.screens.ProfileScreen
import com.example.deneme.ui.screens.SignUpScreen
import com.example.deneme.viewmodel.AuthViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object AddProblem : Screen("add_problem")
    data class ProblemDetail(val id: String = "{problemId}") : Screen("problem_detail/$id") {
        fun createRoute(id: String) = "problem_detail/$id"
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        route = Screen.Home.route,
        title = "Ana Sayfa",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    object Profile : BottomNavItem(
        route = Screen.Profile.route,
        title = "Profil",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    
    // Observe auth state
    val authState by authViewModel.authState.collectAsState()
    
    // Initialize with login screen
    var startDestination by remember { mutableStateOf(Screen.Login.route) }
    
    // Set start destination based on auth state
    LaunchedEffect(authState) {
        Log.d("Navigation", "Auth state changed: $authState")
        if (authState is AuthViewModel.AuthState.Authenticated) {
            Log.d("Navigation", "User is authenticated, navigating to Home")
            startDestination = Screen.Home.route
        } else if (authState is AuthViewModel.AuthState.Unauthenticated) {
            Log.d("Navigation", "User is not authenticated, navigating to Login")
            startDestination = Screen.Login.route
        }
    }

    // Only create NavHost once the start destination is determined
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth screens
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignUp = { 
                    Log.d("Navigation", "Navigating to SignUp screen")
                    navController.navigate(Screen.SignUp.route) 
                },
                onNavigateToHome = {
                    Log.d("Navigation", "Navigating to Home screen from Login")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateToLogin = { 
                    Log.d("Navigation", "Navigating to Login screen")
                    navController.navigate(Screen.Login.route) 
                },
                onNavigateToHome = {
                    Log.d("Navigation", "Navigating to Home screen from SignUp")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Main app screens
        composable(Screen.Home.route) {
            MainScreenContainer(
                navController = navController,
                currentRoute = Screen.Home.route,
                showFab = true,
                onFabClick = {
                    Log.d("Navigation", "Navigating to AddProblem screen")
                    navController.navigate(Screen.AddProblem.route)
                }
            ) {
                HomeScreen(
                    onNavigateToProblemDetail = { problemId ->
                        Log.d("Navigation", "Navigating to ProblemDetail screen: $problemId")
                        navController.navigate(Screen.ProblemDetail().createRoute(problemId))
                    }
                )
            }
        }
        
        composable(Screen.Profile.route) {
            MainScreenContainer(
                navController = navController,
                currentRoute = Screen.Profile.route,
                showFab = false
            ) {
                ProfileScreen(
                    onNavigateToProblemDetail = { problemId ->
                        Log.d("Navigation", "Navigating to ProblemDetail screen: $problemId")
                        navController.navigate(Screen.ProblemDetail().createRoute(problemId))
                    },
                    onNavigateToLogin = {
                        Log.d("Navigation", "Signing out, navigating to Login screen")
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
        }
        
        composable(Screen.AddProblem.route) {
            AddProblemScreen(
                onNavigateBack = {
                    Log.d("Navigation", "Navigating back from AddProblem")
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.ProblemDetail().route) { backStackEntry ->
            val problemId = backStackEntry.arguments?.getString("problemId") ?: ""
            ProblemDetailScreen(
                problemId = problemId,
                onNavigateBack = {
                    Log.d("Navigation", "Navigating back from ProblemDetail")
                    navController.popBackStack()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContainer(
    navController: NavController,
    currentRoute: String,
    showFab: Boolean = false,
    onFabClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            BottomNavBar(navController = navController, currentRoute = currentRoute)
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(onClick = onFabClick) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
            content()
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController, currentRoute: String) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Profile
    )
    
    NavigationBar {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        Log.d("Navigation", "Navigating to ${item.route} from bottom nav")
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = { Text(text = item.title) }
            )
        }
    }
} 