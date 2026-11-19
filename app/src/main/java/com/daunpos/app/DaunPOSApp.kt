package com.daunpos.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.daunpos.app.data.BusinessCategory
import com.daunpos.app.ui.MainScreen
import com.daunpos.app.ui.screens.BusinessCategoryScreen
import com.daunpos.app.ui.screens.LoginScreen
import com.daunpos.app.ui.screens.OnboardingSplashScreen
import com.daunpos.app.ui.screens.PaymentScreen
import com.daunpos.app.ui.screens.ReceiptCustomizationScreen
import com.daunpos.app.ui.screens.StoreSetupScreen
import com.daunpos.app.ui.screens.SuccessScreen
import com.daunpos.app.ui.screens.TransactionDetailScreen

@Composable
fun DaunPOSApp(vm: DaunPOSViewModel) {
    val navController = rememberNavController()
    val isOnboarded by vm.isOnboardingCompleted.collectAsState()
    val startDest = if (isOnboarded) "login" else "onboarding_splash"

    NavHost(navController = navController, startDestination = startDest) {
        composable("onboarding_splash") {
            OnboardingSplashScreen(
                onStartSetup = {
                    navController.navigate("onboarding_category")
                },
                onLoginClick = {
                    navController.navigate("login")
                }
            )
        }

        composable("onboarding_category") {
            BusinessCategoryScreen(
                initialCategory = vm.storeProfile.value.businessCategory,
                onBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate("login")
                    }
                },
                onCategorySelected = { category ->
                    navController.navigate("onboarding_setup/${category.id}")
                }
            )
        }

        composable(
            route = "onboarding_setup/{categoryId}",
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { entry ->
            val catId = entry.arguments?.getString("categoryId") ?: "FNB"
            val category = BusinessCategory.fromId(catId)

            StoreSetupScreen(
                vm = vm,
                category = category,
                onBack = { navController.popBackStack() },
                onSetupCompleted = {
                    navController.navigate("main") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                vm = vm,
                onLogin = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onSetupNewStore = {
                    navController.navigate("onboarding_category")
                }
            )
        }

        composable("main") {
            MainScreen(
                vm = vm,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable("payment") {
            PaymentScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onSuccess = { id ->
                    navController.navigate("success/$id") {
                        popUpTo("main") { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = "success/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            SuccessScreen(
                vm = vm,
                transactionId = id,
                onNewTransaction = {
                    navController.navigate("main") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            TransactionDetailScreen(
                vm = vm,
                transactionId = id,
                onBack = { navController.popBackStack() }
            )
        }

        composable("receipt") {
            ReceiptCustomizationScreen(
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
