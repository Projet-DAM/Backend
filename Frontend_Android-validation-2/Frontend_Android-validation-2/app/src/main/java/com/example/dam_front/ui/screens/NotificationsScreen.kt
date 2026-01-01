package com.example.dam_front.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.models.Notification
import com.example.dam_front.ui.theme.HeaderBlue
import com.example.dam_front.viewmodels.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationViewModel,
    onBackClick: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.markAllAsRead() }) {
                        Text("Tout marquer comme lu", color = HeaderBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = HeaderBlue,
                    navigationIconContentColor = HeaderBlue
                )
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Aucune notification",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = { viewModel.markAsRead(notification.id) }
                    )
                }

                // Debug section
                item {
                    DebugFcmSection()
                }
            }
        }
    }
}

@Composable
fun DebugFcmSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var fcmToken by remember { mutableStateOf("Chargement...") }

    LaunchedEffect(Unit) {
        val manager = com.example.dam_front.utils.FirebaseTokenManager(context)
        fcmToken = manager.getFCMToken() ?: "Indisponible"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Debug Notifications", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text("FCM Token (pour test console Firebase):", fontSize = 10.sp, color = Color.Gray)
            Text(
                text = fcmToken,
                fontSize = 10.sp,
                color = HeaderBlue,
                modifier = Modifier
                    .clickable {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("FCM Token", fcmToken)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Token copié !", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(notification.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Icon / Status indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (notification.read) Color(0xFFF0F0F0) else HeaderBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (notification.read) Color.Gray else HeaderBlue,
                    modifier = Modifier.size(24.dp)
                )
                
                if (!notification.read) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = if (notification.read) FontWeight.SemiBold else FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (notification.read) Color.DarkGray else Color.Black
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = notification.body,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}
