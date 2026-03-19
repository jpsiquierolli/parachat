package com.example.parachat.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.parachat.ui.feature.login.LoginScreen
import com.example.parachat.ui.feature.signup.SignupScreen
import com.example.parachat.ui.feature.home.HomeScreen
import com.example.parachat.ui.feature.chat.ChatScreen
import com.example.parachat.ui.feature.profile.ProfileScreen
import com.example.parachat.ui.feature.group.CreateGroupScreen
import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object SignupRoute

@Serializable
object HomeRoute

@Serializable
object ProfileRoute

@Serializable
object CreateGroupRoute

@Serializable
data class ChatRoute(val userId: String? = null, val groupId: String? = null)

@Composable
fun ParachatNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = LoginRoute
    ) {
        composable<LoginRoute> {
            LoginScreen (
                navigateToListScreen = {
                    navController.navigate(HomeRoute) {
                        popUpTo(LoginRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                navigateToSignupScreen = {
                    navController.navigate(SignupRoute)
                }
            )
        }

        composable<SignupRoute> {
            SignupScreen (
                navigateToListScreen = {
                    navController.navigate(HomeRoute) {
                        popUpTo(SignupRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                navigateToLoginScreen = {
                    navController.navigate(LoginRoute)
                }
            )
        }

        composable<HomeRoute> {
            HomeScreen(
                onUserClick = { userId ->
                    navController.navigate(ChatRoute(userId = userId)) {
                        launchSingleTop = true
                    }
                },
                onGroupClick = { groupId ->
                    navController.navigate(ChatRoute(groupId = groupId)) {
                        launchSingleTop = true
                    }
                },
                onProfileClick = {
                    navController.navigate(ProfileRoute) {
                        launchSingleTop = true
                    }
                },
                onAddGroupClick = {
                    navController.navigate(CreateGroupRoute)
                },
                onSignOut = {
                    navController.navigate(LoginRoute) {
                        popUpTo(HomeRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<ProfileRoute> {
            ProfileScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable<CreateGroupRoute> {
            CreateGroupScreen(
                onBackClick = { navController.popBackStack() },
                onGroupCreated = { groupId ->
                    navController.navigate(ChatRoute(groupId = groupId)) {
                        popUpTo(CreateGroupRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<ChatRoute> { backStackEntry ->
            val route: ChatRoute = backStackEntry.toRoute()
            ChatScreen(
                userId = route.userId,
                groupId = route.groupId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
