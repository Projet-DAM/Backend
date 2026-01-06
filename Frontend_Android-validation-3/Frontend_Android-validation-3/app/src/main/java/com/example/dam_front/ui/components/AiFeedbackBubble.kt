package com.example.dam_front.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Composant pour afficher un message de feedback IA avec un design motivant
 */
@Composable
fun AiFeedbackMessageBubble(
    feedback: String,
    emoji: String,
    matchResult: String,
    teamName: String,
    score: String,
    phase: String,
    timestamp: String
) {
    // Animation pour l'icône IA
    val infiniteTransition = rememberInfiniteTransition(label = "ai_glow")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Couleurs selon le résultat
    val gradientColors = when (matchResult) {
        "victoire" -> listOf(
            Color(0xFFFFD700), // Or
            Color(0xFFFFA500)  // Orange
        )
        "defaite" -> listOf(
            Color(0xFF6B46C1), // Violet
            Color(0xFF805AD5)  // Violet clair
        )
        else -> listOf(
            Color(0xFF4299E1), // Bleu
            Color(0xFF63B3ED)  // Bleu clair
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        // Badge "Message IA"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                color = Color(0xFF7C3AED).copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "IA",
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier
                            .size(16.dp)
                            .scale(scale)
                    )
                    Text(
                        text = "Message IA Motivant",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3AED)
                    )
                }
            }
        }
        
        // Bulle de message principale
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 4.dp,
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = gradientColors.map { it.copy(alpha = 0.1f) }
                        )
                    )
                    .padding(16.dp)
            ) {
                // En-tête avec emoji et infos du match
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Emoji géant
                    Text(
                        text = emoji,
                        fontSize = 48.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    
                    // Infos du match
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = phase.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = gradientColors[0]
                        )
                        Text(
                            text = teamName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "Score: $score",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                // Divider
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = gradientColors[0].copy(alpha = 0.3f)
                )
                
                // Message de feedback
                Text(
                    text = feedback,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Color(0xFF1A202C),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Timestamp
                Text(
                    text = timestamp,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/**
 * Version compacte du feedback IA pour la liste de conversations
 */
@Composable
fun AiFeedbackPreview(
    emoji: String,
    previewText: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = Color(0xFF7C3AED).copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "IA",
                    tint = Color(0xFF7C3AED),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = emoji,
                    fontSize = 14.sp
                )
            }
        }
        Text(
            text = previewText,
            fontSize = 14.sp,
            color = Color.Gray,
            maxLines = 1
        )
    }
}
