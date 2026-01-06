package com.example.dam_front.ui.call

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dam_front.config.ApiConfig

class CallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val receiverId = intent?.getStringExtra("receiverId") ?: ""
        val callType = intent?.getStringExtra("callType") ?: "audio"
        val userName = intent?.getStringExtra("userName") ?: "Utilisateur"
        val userPhoto = intent?.getStringExtra("userPhoto")

        setContent {
            MaterialTheme {
                CallScreen(
                    userName = userName,
                    userPhoto = userPhoto,
                    callType = callType,
                    onHangUp = { finish() }
                )
            }
        }
    }
}

@Composable
fun CallScreen(
    userName: String,
    userPhoto: String?,
    callType: String,
    onHangUp: () -> Unit
) {
    val isVideo = callType == "video"
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isVideoEnabled by remember { mutableStateOf(isVideo) }

    // Pulsing animation for avatar border
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isVideoEnabled) 
                        listOf(Color(0xFF2C3E50), Color(0xFF000000)) 
                    else 
                        listOf(Color(0xFF202020), Color(0xFF101010))
                )
            )
    ) {
        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = userName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isVideo) "Appel vidéo en cours..." else "Appel audio en cours...",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Central Avatar
            Box(contentAlignment = Alignment.Center) {
                // Pulsing rings
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                )
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    val model = if (!userPhoto.isNullOrBlank()) {
                         if (userPhoto.startsWith("http")) userPhoto else "${ApiConfig.BASE_URL.trimEnd('/')}/${userPhoto.trimStart('/')}"
                    } else "https://via.placeholder.com/150"

                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.graphics.painter.ColorPainter(Color.Gray)
                    )
                }
            }

            // Bottom Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallControlButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        isActive = isMuted,
                        onClick = { isMuted = !isMuted }
                    )
                    CallControlButton(
                        icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        isActive = isSpeakerOn,
                        onClick = { isSpeakerOn = !isSpeakerOn }
                    )
                    if (isVideo) {
                        CallControlButton(
                            icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            isActive = !isVideoEnabled,
                            onClick = { isVideoEnabled = !isVideoEnabled }
                        )
                    }
                }

                // Hangup Button
                IconButton(
                    onClick = onHangUp,
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFFFF3B30), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CallControlButton(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .background(
                color = if (isActive) Color.White else Color.White.copy(alpha = 0.2f),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) Color.Black else Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}
