package com.daunpos.app.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.OrderItem
import com.daunpos.app.data.PaymentMethod
import com.daunpos.app.data.Transaction
import com.daunpos.app.ui.receipt.ReceiptPreview
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line

@Composable
fun ReceiptCustomizationScreen(
    vm: DaunPOSViewModel,
    onBack: () -> Unit
) {
    val settings by vm.receiptSettings.collectAsState()

    val sampleTx = remember {
        Transaction(
            id = "#INV-0183",
            dateTime = System.currentTimeMillis(),
            items = listOf(
                OrderItem("P001", "Espresso", 18000, 1, "☕"),
                OrderItem("P003", "Cappuccino", 28000, 1, "🥛"),
                OrderItem("P009", "Croissant Butter", 22000, 1, "🥐"),
                OrderItem("P005", "Es Teh Manis", 12000, 1, "🧋")
            ),
            subtotal = 80000,
            tax = 0,
            total = 80000,
            paymentMethod = PaymentMethod.CASH,
            cashReceived = 100000,
            change = 20000
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Ink)
            }
            Text("Kustomisasi Struk", style = MaterialTheme.typography.titleLarge, color = Ink)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview
            Text("Live Preview Struk", style = MaterialTheme.typography.titleMedium, color = Ink)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFE9EDE6))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                ReceiptPreview(tx = sampleTx, settings = settings)
            }

            // Logo upload
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, Line, RoundedCornerShape(18.dp))
                    .clickable { /* mock upload */ }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GreenSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, tint = GreenDeep)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Upload Logo Kafe", style = MaterialTheme.typography.titleSmall, color = Ink, fontWeight = FontWeight.SemiBold)
                        Text("Monokrom (hitam-putih), PNG", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                    }
                    Text("Pilih", style = MaterialTheme.typography.labelMedium, color = GreenDeep, fontWeight = FontWeight.Bold)
                }
            }

            // Header inputs
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, Line, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Header Struk", style = MaterialTheme.typography.titleMedium, color = Ink)
                    settings.headerLines.forEachIndexed { index, value ->
                        OutlinedTextField(
                            value = value,
                            onValueChange = { new ->
                                val updated = settings.headerLines.toMutableList()
                                updated[index] = new
                                vm.updateReceiptSettings(settings.copy(headerLines = updated))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Baris ${index + 1}") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, unfocusedBorderColor = Line)
                        )
                    }
                }
            }

            // Footer input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, Line, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("Footer Struk", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = settings.footerText,
                        onValueChange = { vm.updateReceiptSettings(settings.copy(footerText = it)) },
                        modifier = Modifier.fillMaxWidth().height(96.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, unfocusedBorderColor = Line)
                    )
                }
            }

            // Toggles
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, Line, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    ToggleSetting("Tampilkan Logo", settings.showLogo) { vm.updateReceiptSettings(settings.copy(showLogo = it)) }
                    Spacer(Modifier.height(8.dp))
                    ToggleSetting("Tampilkan QR Struk Digital", settings.showQR) { vm.updateReceiptSettings(settings.copy(showQR = it)) }
                }
            }

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Simpan Pengaturan", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ToggleSetting(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Green,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Line
            )
        )
    }
}
