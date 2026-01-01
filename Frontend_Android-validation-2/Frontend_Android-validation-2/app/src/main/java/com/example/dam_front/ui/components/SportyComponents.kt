package com.example.dam_front.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.ui.theme.SportyChipBlue
import com.example.dam_front.ui.theme.SportyChipOrange
import com.example.dam_front.ui.theme.SportyDarkBlue
import com.example.dam_front.ui.theme.SportyLime
import com.example.dam_front.ui.theme.SportyMutedText
import com.example.dam_front.ui.theme.SportyOrange
import com.example.dam_front.ui.theme.SportyTeal

@Composable
fun SportyGradientButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val gradient = remember {
        Brush.horizontalGradient(listOf(SportyOrange, SportyLime, SportyTeal))
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(28.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(gradient)
                .graphicsLayer(alpha = if (enabled) 1f else 0.5f)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingIcon?.invoke()
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun SportyInfoChip(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SportyChipBlue,
    contentColor: Color = SportyDarkBlue,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(backgroundColor.copy(alpha = 0.9f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingIcon?.invoke()
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
fun SportyMutedChip(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    SportyInfoChip(
        text = text,
        modifier = modifier,
        backgroundColor = SportyChipOrange,
        contentColor = SportyMutedText,
        leadingIcon = leadingIcon
    )
}

@Composable
fun SportySectionDivider(modifier: Modifier = Modifier) {
    androidx.compose.material3.HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = com.example.dam_front.ui.theme.SportyDivider
    )
}
