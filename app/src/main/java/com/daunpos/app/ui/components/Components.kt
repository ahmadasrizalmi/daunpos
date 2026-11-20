package com.daunpos.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.daunpos.app.data.Product
import com.daunpos.app.data.formatRupiah
import com.daunpos.app.data.formatDurationMinutes
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Danger
import com.daunpos.app.ui.theme.DangerSoft
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import com.daunpos.app.ui.theme.Mint

@Composable
fun StatusBadge(
    text: String,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (positive) GreenSoft else DangerSoft
    val fg = if (positive) GreenDeep else Danger
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = fg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProductThumbnail(
    product: Product,
    height: Int = 104,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(product.colorHex)),
        contentAlignment = Alignment.Center
    ) {
        if (!product.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(product.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(height.dp)
            )
        } else {
            Text(text = product.emoji, fontSize = (height * 0.42).sp)
        }
    }
}

@Composable
fun ProductEmoji(
    emoji: String,
    colorHex: Long,
    size: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 4).dp))
            .background(Color(colorHex)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = (size * 0.45).sp)
    }
}

@Composable
fun ProductMenuCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trackStock: Boolean = true
) {
    val outOfStock = !product.available || (trackStock && product.stock <= 0)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .clickable(enabled = !outOfStock, onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Box {
                ProductThumbnail(product = product, height = 104)
                if (outOfStock) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xB3FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DangerSoft)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Stok Habis",
                                color = Danger,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (trackStock && product.stock <= 10) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Sisa ${product.stock}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (product.durationMinutes > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xD9102A1E))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("⏱ ${formatDurationMinutes(product.durationMinutes)}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (product.barcodeSku.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xD92D3748))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(product.barcodeSku, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                product.name,
                style = MaterialTheme.typography.titleSmall,
                color = Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )
            if (product.description.isNotBlank()) {
                Text(
                    product.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${formatRupiah(product.price)} / ${product.unitLabel}",
                    style = MaterialTheme.typography.titleSmall,
                    color = GreenDeep,
                    fontWeight = FontWeight.Bold
                )

                // Plus button matching DaunPOS.pen (26x26 with green-soft background and green-deep plus)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (outOfStock) Line else GreenSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Tambah",
                        tint = if (outOfStock) InkSoft else GreenDeep,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = emoji, fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun QrisTutorialDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GreenSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Panduan QRIS Toko",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = InkSoft)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Cara 1 (Paling Mudah)
                TutorialStepItem(
                    number = "1",
                    title = "Foto / Screenshot QRIS (Paling Mudah)",
                    desc = "Cukup foto stiker QRIS fisik toko Anda atau ambil screenshot QRIS dari m-Banking (BCA, Mandiri, BRI, DANA, GoPay, OVO, ShopeePay). Lalu klik tombol 'Scan / Pilih Foto QRIS'. DaunPOS membaca kodenya langsung secara offline!",
                    isRecommended = true
                )

                Spacer(Modifier.height(10.dp))

                // Cara 2
                TutorialStepItem(
                    number = "2",
                    title = "Via Google Lens / Scanner HP",
                    desc = "Buka aplikasi Google Lens atau pemindai QR bawaan HP Anda. Arahkan ke stiker QRIS. Salin teks kode yang muncul (diawali '00020101...'), lalu tempel (paste) ke kolom QRIS di DaunPOS.",
                    isRecommended = false
                )

                Spacer(Modifier.height(10.dp))

                // Cara 3
                TutorialStepItem(
                    number = "3",
                    title = "Belum memiliki QRIS?",
                    desc = "Jika belum memiliki QRIS merchant, kosongkan kolom ini. Pembayaran tunai dan transfer tetap dapat digunakan; metode QRIS baru aktif setelah kode merchant yang valid disimpan.",
                    isRecommended = false
                )

                Spacer(Modifier.height(18.dp))

                DaunPOSButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDeep, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Mengerti & Kembali", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TutorialStepItem(
    number: String,
    title: String,
    desc: String,
    isRecommended: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isRecommended) GreenSoft.copy(alpha = 0.5f) else Bg)
            .border(1.dp, if (isRecommended) Green.copy(alpha = 0.5f) else Line, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isRecommended) GreenDeep else InkSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(number, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
                if (isRecommended) {
                    Spacer(Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GreenDeep)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "Rekomendasi",
                            fontSize = 9.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun InfoAction(
    contentDescription: String = "Informasi",
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
            contentDescription = contentDescription,
            tint = InkSoft
        )
    }
}

@Composable
fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Mengerti", color = GreenDeep, fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = Color.White
    )
}
