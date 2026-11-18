package com.daunpos.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.ui.screens.MenuManagementScreen
import com.daunpos.app.ui.screens.POSScreen
import com.daunpos.app.ui.screens.PrinterSettingsScreen
import com.daunpos.app.ui.screens.ReportsScreen
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft

private data class Tab(
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    Tab("Kasir", Icons.Filled.PointOfSale),
    Tab("Laporan", Icons.Filled.BarChart),
    Tab("Menu", Icons.Filled.RestaurantMenu),
    Tab("Printer", Icons.Filled.Print)
)

@Composable
fun MainScreen(
    vm: DaunPOSViewModel,
    onNavigate: (String) -> Unit
) {
    var selected by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 0.dp,
                modifier = Modifier.height(72.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    val active = index == selected
                    NavigationBarItem(
                        selected = active,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = GreenDeep,
                            indicatorColor = Green,
                            unselectedIconColor = InkSoft,
                            unselectedTextColor = InkSoft
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selected) {
                0 -> POSScreen(
                    vm = vm,
                    onCheckout = { onNavigate("payment") },
                    onPrinterSettings = { selected = 3 }
                )
                1 -> ReportsScreen(
                    vm = vm,
                    onOpenDetail = { id -> onNavigate("detail/$id") }
                )
                2 -> MenuManagementScreen(vm = vm)
                3 -> PrinterSettingsScreen(
                    vm = vm,
                    onOpenReceiptCustomization = { onNavigate("receipt") }
                )
            }
        }
    }
}
