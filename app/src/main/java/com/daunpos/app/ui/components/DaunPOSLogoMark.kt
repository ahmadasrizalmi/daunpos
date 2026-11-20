package com.daunpos.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.daunpos.app.R

/** Uses the approved DaunPOS logo artwork from design/Logo daunPOS small.jpg. */
@Composable
fun DaunPOSLogoMark(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.logo_daunpos),
        contentDescription = "DaunPOS",
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f)),
        contentScale = ContentScale.Crop
    )
}
