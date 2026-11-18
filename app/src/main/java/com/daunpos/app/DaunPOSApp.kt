package com.daunpos.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.daunpos.app.ui.MainScreen
import com.daunpos.app.ui.screens.LoginScreen
import com.daunpos.app.ui.screens.PaymentScreen
import com.daunpos.app.ui.screens.ReceiptCustomizationScreen
import com.daunpos.app.ui.screens.SuccessScreen
import com.daunpos.app.ui.screens.TransactionDetailScreen

@Composable
fun DaunPOSApp(vm: DaunPOSViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                vm = vm,
                onLogin = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
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
