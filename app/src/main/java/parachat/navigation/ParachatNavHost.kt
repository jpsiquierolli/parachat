package parachat.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import parachat.ui.feature.login.LoginScreen
import parachat.ui.feature.signup.SignupScreen
import parachat.ui.feature.home.HomeScreen
import parachat.ui.feature.chat.ChatScreen
import kotlinx.serialization.Serializable
import androidx.compose.material3.Text

@Serializable
object LoginRoute

@Serializable
object SignupRoute

@Serializable
object HomeRoute

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
                onSignOut = {
                    navController.navigate(LoginRoute) {
                        popUpTo(LoginRoute) { inclusive = true }
                    }
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