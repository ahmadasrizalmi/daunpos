package com.daunpos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import com.daunpos.app.ui.components.DaunPOSButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.ui.components.DaunPOSLogoMark
import com.daunpos.app.data.PinPolicy
import com.daunpos.app.data.UserRole
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Danger
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import com.daunpos.app.ui.theme.Mint
import com.daunpos.app.ui.theme.isTablet
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    vm: DaunPOSViewModel,
    onLogin: () -> Unit,
    onSetupNewStore: () -> Unit = {}
) {
    val config = LocalConfiguration.current
    val isTablet = config.isTablet

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var requiresPinChange by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var pinChangeError by remember { mutableStateOf<String?>(null) }
    var savingPin by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit(pinValue: String) {
        if (loading || pinValue.length !in 4..6) return
        loading = true
        error = null
        scope.launch {
            val user = vm.login(pinValue, UserRole.OWNER)
            loading = false
            if (user != null) {
                if (user.requiresPinChange) {
                    requiresPinChange = true
                    pinChangeError = "PIN bawaan tidak boleh digunakan untuk operasional."
                } else {
                    onLogin()
                }
            } else {
                error = "PIN salah. Coba lagi."
                pin = ""
            }
        }
    }

    fun pressDigit(d: String) {
        if (loading || pin.length >= 6) return
        pin += d
        error = null
    }

    fun saveNewPin() {
        if (savingPin) return
        pinChangeError = when {
            !PinPolicy.isAcceptableNewPin(newPin) -> "Gunakan PIN 4–6 digit dan hindari PIN bawaan."
            newPin != confirmNewPin -> "Konfirmasi PIN tidak sama."
            else -> null
        }
        if (pinChangeError != null) return

        savingPin = true
        scope.launch {
            val changed = vm.changeCurrentUserPin(newPin)
            savingPin = false
            if (changed) {
                requiresPinChange = false
                onLogin()
            } else {
                pinChangeError = "PIN gagal disimpan. Periksa kembali dan coba lagi."
            }
        }
    }

    if (requiresPinChange) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Ganti PIN bawaan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("PIN demonstrasi tidak boleh dipakai untuk operasional. Buat PIN baru untuk akun ini.")
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newPin = it },
                        label = { Text("PIN baru (4–6 digit)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmNewPin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmNewPin = it },
                        label = { Text("Ulangi PIN baru") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    pinChangeError?.let { Text(it, color = Danger, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                Button(
                    onClick = { saveNewPin() },
                    enabled = !savingPin,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDeep)
                ) {
                    if (savingPin) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    else Text("Simpan PIN")
                }
            }
        )
    }

    if (isTablet) {
        // Tablet Split Screen (1024px design matching DaunPOS.pen)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
        ) {
            // Left: Brand Panel (452px wide)
            Box(
                modifier = Modifier
                    .width(420.dp)
                    .fillMaxHeight()
                    .background(Color.White)
                    .border(1.dp, Line)
                    .padding(40.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DaunPOSLogoMark(size = 48.dp)
                            Spacer(Modifier.width(14.dp))
                            Text("DaunPOS", style = MaterialTheme.typography.headlineMedium, color = Ink)
                        }

                        Spacer(Modifier.height(28.dp))

                        Text(
                            "Kasir usaha yang ringan, rapi, dan siap digunakan setiap hari.",
                            style = MaterialTheme.typography.titleLarge,
                            color = InkSoft,
                            lineHeight = 28.sp
                        )

                        Spacer(Modifier.height(36.dp))

                        FeatureBenefitRow(
                            icon = Icons.Filled.Bolt,
                            title = "Transaksi kilat",
                            desc = "Keranjang dan checkout dalam satu alur"
                        )
                        Spacer(Modifier.height(20.dp))
                        FeatureBenefitRow(
                            icon = Icons.Filled.Insights,
                            title = "Laporan real-time",
                            desc = "Pendapatan harian langsung terhitung"
                        )
                        Spacer(Modifier.height(20.dp))
                        FeatureBenefitRow(
                            icon = Icons.Filled.Print,
                            title = "Cetak struk",
                            desc = "Printer thermal Bluetooth"
                        )
                    }

                    Text(
                        "DaunPOS · Offline-first POS",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft
                    )
                }
            }

            // Right: PIN Keypad Panel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 16.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                PinKeypadContent(
                    pin = pin,
                    error = error,
                    loading = loading,
                    isTablet = true,
                    onPressDigit = { pressDigit(it) },
                    onDelete = { pin = pin.dropLast(1); error = null },
                    onSubmit = { submit(pin) }
                )
            }
        }
    } else {
        // Phone Screen (390px design)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            PinKeypadContent(
                pin = pin,
                error = error,
                loading = loading,
                onPressDigit = { pressDigit(it) },
                onDelete = { pin = pin.dropLast(1); error = null },
                onSubmit = { submit(pin) }
            )
        }
    }
}

@Composable
private fun FeatureBenefitRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(GreenSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Ink, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = InkSoft)
        }
    }
}

@Composable
private fun PinKeypadContent(
    pin: String,
    error: String?,
    loading: Boolean,
    isTablet: Boolean = false,
    onPressDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit
) {
    val iconSize = if (isTablet) 52.dp else 76.dp
    val keySize = if (isTablet) 54.dp else 64.dp
    val spacingV = if (isTablet) 8.dp else 12.dp
    val spacerTop = if (isTablet) 10.dp else 16.dp
    val spacerPin = if (isTablet) 14.dp else 24.dp

    Column(
        modifier = Modifier
            .padding(horizontal = if (isTablet) 12.dp else 24.dp)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DaunPOSLogoMark(size = iconSize)

        Spacer(Modifier.height(spacerTop))
        Text("DaunPOS", style = if (isTablet) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium, color = Ink)
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Masuk sebagai Pemilik",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(spacerPin))

        // PIN dots
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(6) { i ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .then(
                            if (i < pin.length) Modifier.background(GreenDeep)
                            else Modifier.background(Color.White).border(1.5.dp, Line, CircleShape)
                        )
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Green)
        } else {
            Text(
                text = error ?: " ",
                style = MaterialTheme.typography.labelSmall,
                color = if (error != null) Danger else InkSoft
            )
        }

        Spacer(Modifier.height(if (isTablet) 12.dp else 24.dp))

        // Keypad
        Column(verticalArrangement = Arrangement.spacedBy(spacingV)) {
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "del")
            ).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        when {
                            key.isEmpty() -> Spacer(Modifier.size(keySize))
                            key == "del" -> {
                                Box(
                                    modifier = Modifier
                                        .size(keySize)
                                        .clip(CircleShape)
                                        .clickable { onDelete() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Hapus",
                                        tint = InkSoft,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(keySize)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(1.dp, Line, CircleShape)
                                        .clickable { onPressDigit(key) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(key, fontSize = if (isTablet) 20.sp else 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(if (isTablet) 12.dp else 22.dp))

        Text(
            text = "Masukkan PIN 4–6 digit",
            style = MaterialTheme.typography.labelSmall,
            color = InkSoft
        )

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = onSubmit,
            enabled = !loading && pin.length in 4..6,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 240.dp)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenDeep,
                contentColor = Color.White,
                disabledContainerColor = GreenSoft,
                disabledContentColor = InkSoft
            )
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
            } else {
                Text("Masuk", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(14.dp))
    }
}
