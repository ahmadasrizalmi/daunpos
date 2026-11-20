package com.daunpos.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep

/**
 * DaunPOS primary action button. Green primary actions use a restrained brand gradient;
 * neutral, disabled, and danger colors remain unchanged for clear action hierarchy.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun DaunPOSButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val backgroundColor = if (enabled) colors.containerColor else colors.disabledContainerColor
    val foregroundColor = if (enabled) colors.contentColor else colors.disabledContentColor
    val useBrandGradient = enabled && (
        backgroundColor == Green ||
            backgroundColor == GreenDeep ||
            backgroundColor == MaterialTheme.colorScheme.primary
        )
    val backgroundModifier = if (useBrandGradient) {
        Modifier.background(
            Brush.linearGradient(
                colors = listOf(Color(0xFF9BD449), Color(0xFF58AE3A), GreenDeep)
            ),
            shape
        )
    } else {
        Modifier.background(backgroundColor, shape)
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 64.dp, minHeight = 40.dp)
            .shadow(if (enabled && useBrandGradient) 2.dp else 0.dp, shape)
            .clip(shape)
            .then(backgroundModifier)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides foregroundColor) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}
