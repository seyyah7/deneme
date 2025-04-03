package com.example.deneme.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.deneme.ui.screens.MessagesScreen
import com.example.deneme.ui.screens.NotificationsScreen
import com.example.deneme.ui.screens.ProblemDetailScreen
import com.example.deneme.ui.screens.ProfileScreen
import com.example.deneme.ui.screens.SearchScreen
import com.example.deneme.ui.screens.SignUpScreen
import com.example.deneme.ui.screens.UserProfileScreen
import com.example.deneme.ui.screens.ChatScreen
import com.example.deneme.viewmodel.AuthViewModel
import com.example.deneme.viewmodel.ChatViewModel
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun Navigation() {
    AppNavigation()
}

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object AddProblem : Screen("add_problem")
    object Search : Screen("search")
    object Messages : Screen("messages")
    object Notifications : Screen("notifications")
    data class ProblemDetail(val id: String = "{problemId}") : Screen("problem_detail/$id") {
        fun createRoute(id: String) = "problem_detail/$id"
    }
    data class UserProfile(val id: String = "{userId}") : Screen("user_profile/$id") {
        fun createRoute(id: String) = "user_profile/$id"
    }
    data class Chat(val userId: String = "{userId}") : Screen("chat/$userId") {
        fun createRoute(userId: String) = "chat/$userId"
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
    
    object Search : BottomNavItem(
        route = Screen.Search.route,
        title = "Arama",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    )
    
    object Messages : BottomNavItem(
        route = Screen.Messages.route,
        title = "Mesajlar",
        selectedIcon = Icons.Filled.Email,
        unselectedIcon = Icons.Outlined.Email
    )
    
    object Notifications : BottomNavItem(
        route = Screen.Notifications.route,
        title = "Bildirimler",
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications
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
        
        // Arama ekranı
        composable(Screen.Search.route) {
            MainScreenContainer(
                navController = navController,
                currentRoute = Screen.Search.route,
                showFab = false
            ) {
                SearchScreen(
                    onNavigateToProblemDetail = { problemId ->
                        Log.d("Navigation", "Navigating to ProblemDetail screen from Search: $problemId")
                        navController.navigate(Screen.ProblemDetail().createRoute(problemId))
                    }
                )
            }
        }
        
        // Mesajlar ekranı
        composable(Screen.Messages.route) {
            MainScreenContainer(
                navController = navController,
                currentRoute = Screen.Messages.route,
                showFab = false
            ) {
                MessagesScreen(
                    onNavigateToChat = { userId ->
                        Log.d("Navigation", "Navigating to Chat with user: $userId")
                        navController.navigate(Screen.Chat().createRoute(userId))
                    }
                )
            }
        }
        
        // Bildirimler ekranı
        composable(Screen.Notifications.route) {
            MainScreenContainer(
                navController = navController,
                currentRoute = Screen.Notifications.route,
                showFab = false
            ) {
                NotificationsScreen()
            }
        }
        
        composable(Screen.AddProblem.route) {
            AddProblemScreen(
                onNavigateBack = {
                    Log.d("Navigation", "Navigating back from AddProblem")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.ProblemDetail().route) { backStackEntry ->
            val problemId = backStackEntry.arguments?.getString("problemId") ?: ""
            MainScreenContainer(
                navController = navController,
                currentRoute = "",  // Boş string vermemiz alt navigasyon çubuğunda hiçbir item'ın seçili olmamasını sağlar
                showFab = false
            ) {
                ProblemDetailScreen(
                    problemId = problemId,
                    onNavigateBack = {
                        Log.d("Navigation", "Navigating back from ProblemDetail")
                        navController.popBackStack()
                    },
                    onNavigateToUserProfile = { userId ->
                        Log.d("Navigation", "Navigating to UserProfile: $userId")
                        navController.navigate(Screen.UserProfile().createRoute(userId))
                    }
                )
            }
        }
        
        // Kullanıcı profili ekranı
        composable(Screen.UserProfile().route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            MainScreenContainer(
                navController = navController,
                currentRoute = "",  // Boş string vermemiz alt navigasyon çubuğunda hiçbir item'ın seçili olmamasını sağlar
                showFab = false
            ) {
                UserProfileScreen(
                    userId = userId,
                    onNavigateBack = {
                        Log.d("Navigation", "Navigating back from UserProfile")
                        navController.popBackStack()
                    },
                    onNavigateToProblemDetail = { problemId ->
                        Log.d("Navigation", "Navigating to ProblemDetail screen from UserProfile: $problemId")
                        navController.navigate(Screen.ProblemDetail().createRoute(problemId))
                    },
                    onNavigateToChat = { chatUserId ->
                        Log.d("Navigation", "Navigating to Chat screen with user: $chatUserId")
                        navController.navigate(Screen.Chat().createRoute(chatUserId))
                    }
                )
            }
        }

        // ChatScreen ekranı
        composable(Screen.Chat().route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            MainScreenContainer(
                navController = navController,
                currentRoute = Screen.Messages.route,  // Mesajlar sekmesi seçili olsun
                showFab = false
            ) {
                ChatScreen(
                    userId = userId,
                    onNavigateBack = {
                        Log.d("Navigation", "Navigating back from Chat")
                        navController.popBackStack()
                    }
                )
            }
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
        BottomNavItem.Search,
        BottomNavItem.Messages,
        BottomNavItem.Notifications,
        BottomNavItem.Profile
    )
    
    // ChatViewModel'i ekleyelim
    val chatViewModel: ChatViewModel = hiltViewModel()
    val unreadCount by chatViewModel.totalUnreadCount.collectAsState()
    
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
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title
                        )
                        
                        // Mesajlar için badge ekle
                        if (item == BottomNavItem.Messages && unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .offset(x = 10.dp, y = (-10).dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                    color = MaterialTheme.colorScheme.onError,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                },
                label = { Text(text = item.title) }
            )
        }
    }
} 