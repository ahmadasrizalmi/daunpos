package com.daunpos.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.ui.screens.MenuManagementScreen
import com.daunpos.app.ui.screens.POSScreen
import com.daunpos.app.ui.screens.PrinterSettingsScreen
import com.daunpos.app.ui.screens.ReportsScreen
import com.daunpos.app.ui.theme.Danger
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import com.daunpos.app.ui.theme.Mint

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
    val config = LocalConfiguration.current
    val isTablet = config.smallestScreenWidthDp >= 600

    var selected by remember { mutableStateOf(0) }
    val bluetooth by vm.bluetoothState.collectAsState()
    val cashier by vm.cashier.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (isTablet) {
                // Top Navigation Bar matching DaunPOS Tablet 1024px design
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .background(Color.White)
                        .border(1.dp, Line)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo mark & brand
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selected = 0 }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(GreenDeep),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Spa, contentDescription = null, tint = Mint, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("DaunPOS", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                            Text(cashier, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                        }
                    }

                    Spacer(Modifier.width(36.dp))

                    // Center nav tabs
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tabs.forEachIndexed { index, tab ->
                            val active = selected == index
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) GreenSoft else Color.Transparent)
                                    .clickable { selected = index }
                                    .padding(horizontal = 16.dp, vertical = 9.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        tab.icon,
                                        contentDescription = tab.label,
                                        tint = if (active) GreenDeep else InkSoft,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        tab.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (active) GreenDeep else InkSoft,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Printer status chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (bluetooth.connected) GreenSoft else Color(0xFFFBEAE7))
                            .border(1.dp, if (bluetooth.connected) Green else Danger, RoundedCornerShape(50))
                            .clickable { selected = 3 }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (bluetooth.connected) GreenDeep else Danger)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (bluetooth.connected) (bluetooth.deviceName.ifBlank { "RPP02N" } + " · Siap") else "Printer Terputus",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (bluetooth.connected) GreenDeep else Danger,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Mint),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            cashier.firstOrNull()?.toString() ?: "A",
                            style = MaterialTheme.typography.titleSmall,
                            color = GreenDeep,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (!isTablet) {
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
