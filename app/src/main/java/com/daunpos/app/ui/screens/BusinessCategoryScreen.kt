package com.daunpos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Storefront
import com.daunpos.app.ui.components.DaunPOSButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.data.BusinessCategory
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import com.daunpos.app.ui.theme.Mint

@Composable
fun BusinessCategoryScreen(
    initialCategory: BusinessCategory = BusinessCategory.FNB,
    onBack: () -> Unit,
    onCategorySelected: (BusinessCategory) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    val scrollState = rememberScrollState()
    val compactLayout = LocalConfiguration.current.screenWidthDp >= 600

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 88.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Navigation Bar
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
                            text = "Langkah 1 dari 2",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenDeep
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (compactLayout) 12.dp else 20.dp))

                Text(
                    text = "Pilih Kategori Bisnis",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Antarmuka kasir dan alur transaksi akan otomatis disesuaikan secara cerdas dengan jenis usaha Anda.",
                    fontSize = 14.sp,
                    color = InkSoft,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(if (compactLayout) 12.dp else 24.dp))

                // 3 Hero Category Cards
                BusinessCategoryCard(
                    category = BusinessCategory.FNB,
                    compactLayout = compactLayout,
                    icon = Icons.Filled.Restaurant,
                    isSelected = selectedCategory == BusinessCategory.FNB,
                    onClick = { selectedCategory = BusinessCategory.FNB }
                )

                Spacer(modifier = Modifier.height(if (compactLayout) 6.dp else 10.dp))

                BusinessCategoryCard(
                    category = BusinessCategory.RETAIL,
                    compactLayout = compactLayout,
                    icon = Icons.Filled.Storefront,
                    isSelected = selectedCategory == BusinessCategory.RETAIL,
                    onClick = { selectedCategory = BusinessCategory.RETAIL }
                )

                Spacer(modifier = Modifier.height(if (compactLayout) 6.dp else 10.dp))

                BusinessCategoryCard(
                    category = BusinessCategory.SERVICE,
                    compactLayout = compactLayout,
                    icon = Icons.Filled.ContentCut,
                    isSelected = selectedCategory == BusinessCategory.SERVICE,
                    onClick = { selectedCategory = BusinessCategory.SERVICE }
                )
            }

        }

        // Keep the primary action reachable while the category list scrolls.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Bg)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = { onCategorySelected(selectedCategory) },
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Lanjutkan ke Setup Profil",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Lanjut",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BusinessCategoryCard(
    category: BusinessCategory,
    icon: ImageVector,
    compactLayout: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) GreenDeep else Line
    val bgColor = if (isSelected) Color.White else Color.White
    val iconBgColor = if (isSelected) GreenDeep else GreenSoft
    val iconTint = if (isSelected) Mint else GreenDeep

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(if (compactLayout) 10.dp else 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (compactLayout) 40.dp else 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = category.title,
                            tint = iconTint,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(if (compactLayout) 10.dp else 14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.title,
                            fontSize = if (compactLayout) 16.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = category.subtitle,
                            fontSize = if (compactLayout) 11.sp else 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = InkSoft,
                            maxLines = 2,
                            lineHeight = if (compactLayout) 14.sp else 16.sp,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Clip
                        )
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(GreenDeep)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Terpilih",
                                tint = Mint,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Terpilih",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (compactLayout) 4.dp else 8.dp))

            Text(
                text = category.description,
                fontSize = if (compactLayout) 12.sp else 13.sp,
                color = InkSoft,
                lineHeight = if (compactLayout) 16.sp else 18.sp
            )

            Spacer(modifier = Modifier.height(if (compactLayout) 6.dp else 10.dp))

            // Feature tags wrap to the next line on narrow phones.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                category.features.take(3).forEach { feature ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) GreenSoft else Bg)
                            .border(1.dp, if (isSelected) GreenDeep.copy(alpha = 0.2f) else Line, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = if (compactLayout) 3.dp else 4.dp)
                    ) {
                        Text(
                            text = feature,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) GreenDeep else InkSoft
                        )
                    }
                }
            }
        }
    }
}
