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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.UserRole
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Danger
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    vm: DaunPOSViewModel,
    onLogin: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.KASIR) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit(pinValue: String) {
        if (loading) return
        loading = true
        error = null
        scope.launch {
            val user = vm.login(pinValue, role)
            loading = false
            if (user != null) {
                onLogin()
            } else {
                error = "PIN salah. Coba lagi."
                pin = ""
            }
        }
    }

    fun pressDigit(d: String) {
        if (pin.length < 4) {
            pin += d
            if (pin.length == 4) {
                submit(pin)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Green),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Storefront,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("DaunPOS", style = MaterialTheme.typography.displayMedium, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (role == UserRole.OWNER) "Masuk sebagai pemilik" else "Masuk sebagai kasir",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(30.dp))

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .then(
                                if (i < pin.length) Modifier.background(GreenDeep)
                                else Modifier.background(Color.White).border(1.5.dp, Line, CircleShape)
                            )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Green)
            } else {
                Text(
                    text = error ?: " ",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (error != null) Danger else InkSoft
                )
            }

            Spacer(Modifier.height(30.dp))

            // Keypad
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "del")
                ).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            when {
                                key.isEmpty() -> Spacer(Modifier.size(64.dp))
                                key == "del" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .clickable { pin = pin.dropLast(1); error = null },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Hapus",
                                            tint = InkSoft,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(1.dp, Line, CircleShape)
                                            .clickable { pressDigit(key) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(key, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Role toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(GreenSoft)
                    .clickable {
                        role = if (role == UserRole.KASIR) UserRole.OWNER else UserRole.KASIR
                        pin = ""
                        error = null
                    }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    if (role == UserRole.KASIR) "Masuk sebagai Pemilik" else "Masuk sebagai Kasir",
                    style = MaterialTheme.typography.labelLarge,
                    color = GreenDeep,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (role == UserRole.KASIR) "PIN demo kasir: 1234" else "PIN demo pemilik: 0000",
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft
            )
        }
    }
}
