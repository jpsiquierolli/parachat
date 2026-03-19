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
object GroupsRoute

@Serializable
data class GroupManagementRoute(val groupId: String)

@Serializable
data class ChatRoute(val userId: String)

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
                        popUpTo(LoginRoute) { inclusive = true }
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
                    navController.navigate(ChatRoute(userId)) {
                        launchSingleTop = true
                    }
                },
                onCreateGroupClick = {
                    navController.navigate(CreateGroupRoute)
                },
                onProfileClick = {
                    navController.navigate(ProfileRoute) {
                        launchSingleTop = true
                    }
                },
                onGroupsClick = {
                    navController.navigate(GroupsRoute) {
                        launchSingleTop = true
                    }
                },
                onSignOut = {
                    navController.navigate(LoginRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<CreateGroupRoute> {
            com.example.parachat.ui.feature.chat.CreateGroupScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable<GroupsRoute> {
            com.example.parachat.ui.feature.chat.GroupsScreen(
                onBackClick = { navController.popBackStack() },
                onCreateGroupClick = { navController.navigate(CreateGroupRoute) },
                onManageGroupClick = { groupId ->
                    navController.navigate(GroupManagementRoute(groupId))
                }
            )
        }

        composable<GroupManagementRoute> { backStackEntry ->
            val route: GroupManagementRoute = backStackEntry.toRoute()
            com.example.parachat.ui.feature.chat.GroupManagementScreen(
                groupId = route.groupId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<ProfileRoute> {
            ProfileScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable<ChatRoute> { backStackEntry ->
            val route: ChatRoute = backStackEntry.toRoute()
            ChatScreen(
                userId = route.userId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}