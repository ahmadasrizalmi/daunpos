package com.daunpos.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.BusinessCategory
import com.daunpos.app.data.qris.QrisConverter
import com.daunpos.app.ui.components.QrisTutorialDialog
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Danger
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import com.daunpos.app.ui.theme.Mint

@Composable
fun StoreSetupScreen(
    vm: DaunPOSViewModel,
    category: BusinessCategory,
    onBack: () -> Unit,
    onSetupCompleted: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Default values per industry
    val defaultName = when (category) {
        BusinessCategory.FNB -> "DaunPOS Coffee & Eatery"
        BusinessCategory.RETAIL -> "Toko Daun Berkah"
        BusinessCategory.SERVICE -> "DaunPOS Barbershop & Grooming"
    }

    var storeName by remember { mutableStateOf(defaultName) }
    var address by remember { mutableStateOf("Jl. Daun Hijau No. 8, Bandung") }
    var phone by remember { mutableStateOf("0812-3456-7890") }

    var staticQris by remember { mutableStateOf("") }
    var ownerPin by remember { mutableStateOf("1234") }

    var isMultiUser by remember { mutableStateOf(true) }
    var cashierName by remember { mutableStateOf("Ayu · Kasir Pagi") }
    var cashierPin by remember { mutableStateOf("0000") }

    var isSubmitting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var showTutorialDialog by remember { mutableStateOf(false) }
    var scanFeedback by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val decoded = QrisConverter.decodeQrFromUri(context, uri)
            if (!decoded.isNullOrBlank()) {
                val validation = QrisConverter.validateQris(decoded)
                staticQris = decoded.trim()
                if (validation.isValid) {
                    scanFeedback = "✅ Valid: ${validation.merchantName} (${validation.city})"
                } else {
                    val err = validation.errors.firstOrNull() ?: "Format QRIS perlu diverifikasi"
                    scanFeedback = "⚠️ QRIS Terbaca: $err"
                }
            } else {
                scanFeedback = "⚠️ QR Code tidak terdeteksi pada gambar. Pastikan foto QRIS jelas."
            }
        }
    }

    // Parse QRIS info
    val merchantInfo = remember(staticQris) {
        if (staticQris.isBlank()) null
        else QrisConverter.parseMerchantInfo(staticQris)
    }

    if (showTutorialDialog) {
        QrisTutorialDialog(onDismiss = { showTutorialDialog = false })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Line, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Ink
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(GreenSoft)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Langkah 2 dari 2 · ${category.title}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenDeep
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Setup Profil Toko & Tim",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Lengkapi identitas usaha, QRIS statis, dan hak akses staf kasir.",
                    fontSize = 14.sp,
                    color = InkSoft,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Section 1: Profil Toko
                SectionCard(title = "Identitas Toko", icon = Icons.Filled.Store) {
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Nama Toko / Brand") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = outlinedColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Alamat Singkat") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Place, null, tint = InkSoft) },
                        colors = outlinedColors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("No. WhatsApp / Telepon") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Filled.Phone, null, tint = InkSoft) },
                        colors = outlinedColors()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: QRIS Mandiri
                SectionCard(title = "QRIS Dinamis Mandiri", icon = Icons.Filled.QrCode) {
                    Text(
                        text = "Gunakan stiker QRIS statis merchant Anda (BCA, Mandiri, BRI, Dana, GoPay, OVO). Sistem akan mengonversinya menjadi dinamis dengan nominal transaksi otomatis.",
                        fontSize = 12.sp,
                        color = InkSoft,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Shortcut: Scan Foto & Panduan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSoft, contentColor = GreenDeep),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Scan Foto QRIS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showTutorialDialog = true },
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Line)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = InkSoft, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Panduan", color = InkSoft, fontSize = 11.sp)
                        }
                    }

                    scanFeedback?.let { msg ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(msg, fontSize = 11.sp, color = GreenDeep, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = staticQris,
                        onValueChange = {
                            staticQris = it
                            scanFeedback = null
                        },
                        label = { Text("String QRIS Statis Merchant (EMVCo)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = outlinedColors()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (merchantInfo != null && merchantInfo.isValidCrc) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(GreenSoft)
                                .border(1.dp, Green.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, null, tint = GreenDeep, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Valid: ${merchantInfo.merchantName.ifBlank { "Toko" }} (${merchantInfo.city.ifBlank { "ID" }})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenDeep
                                )
                            }
                        }
                    } else if (staticQris.isNotBlank()) {
                        Text(
                            text = "Format QRIS tidak valid atau checksum salah.",
                            fontSize = 11.sp,
                            color = Danger
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { staticQris = QrisConverter.DEFAULT_SAMPLE_STATIC_QRIS },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Gunakan QRIS Contoh (Sample BCA)", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Akun Pemilik & Tim
                SectionCard(title = "Akun Pemilik & Kasir", icon = Icons.Filled.SupervisorAccount) {
                    OutlinedTextField(
                        value = ownerPin,
                        onValueChange = { if (it.length <= 6) ownerPin = it },
                        label = { Text("PIN Pemilik / Owner (4-6 Digit)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = InkSoft) },
                        colors = outlinedColors()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gunakan Akun Tim Kasir",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                            Text(
                                text = "Kasir login dengan PIN tersendiri tanpa akses laporan keuangan sensitif.",
                                fontSize = 11.sp,
                                color = InkSoft,
                                lineHeight = 15.sp
                            )
                        }
                        Switch(
                            checked = isMultiUser,
                            onCheckedChange = { isMultiUser = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GreenDeep, checkedTrackColor = GreenSoft)
                        )
                    }

                    if (isMultiUser) {
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = cashierName,
                            onValueChange = { cashierName = it },
                            label = { Text("Nama Kasir") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = outlinedColors()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = cashierPin,
                            onValueChange = { if (it.length <= 6) cashierPin = it },
                            label = { Text("PIN Kasir (4-6 Digit)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Filled.Lock, null, tint = InkSoft) },
                            colors = outlinedColors()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Button
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    isSubmitting = true
                    vm.completeOnboarding(
                        name = storeName.ifBlank { defaultName },
                        category = category,
                        address = address,
                        phone = phone,
                        staticQris = staticQris,
                        isMultiUser = isMultiUser,
                        ownerPin = ownerPin.ifBlank { "1234" },
                        cashierPin = if (isMultiUser) cashierPin.ifBlank { "0000" } else null,
                        onDone = {
                            isSubmitting = false
                            onSetupCompleted()
                        }
                    )
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenDeep,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RocketLaunch,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Mint
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Selesaikan & Buka Mesin Kasir",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GreenSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = GreenDeep,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}

@Composable
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GreenDeep,
    unfocusedBorderColor = Line,
    focusedLabelColor = GreenDeep,
    unfocusedLabelColor = InkSoft
)
