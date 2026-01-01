package com.example.dam_front.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.dam_front.models.Notification
import com.example.dam_front.repository.NotificationRepository
import com.example.dam_front.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationBadge(
    modifier: Modifier = Modifier,
    isCoach: Boolean = false
) {
    val context = LocalContext.current
    val notificationRepository = remember { NotificationRepository(context) }
    var unreadCount by remember { mutableStateOf(0) }
    var showDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Charger le nombre de notifications non lues
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                unreadCount = notificationRepository.getUnreadCount()
            }
        }
    }
    
    // Observer les changements (toutes les 2 secondes pour les notifications en temps réel)
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            withContext(Dispatchers.IO) {
                val newCount = notificationRepository.getUnreadCount()
                if (newCount != unreadCount) {
                    android.util.Log.d("NotificationBadge", "📊 Nombre de notifications non lues: $newCount")
                }
                unreadCount = newCount
            }
        }
    }
    
    Box(modifier = modifier) {
        IconButton(
            onClick = { 
                if (isCoach) {
                    showDialog = true
                }
            },
            modifier = Modifier.size(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(IconOrange, SportyKidsGreen)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            // Badge avec le nombre de notifications non lues
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(20.dp)
                        .background(
                            Color(0xFFFF5722),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    
    // Dialog pour afficher les notifications
    if (showDialog && isCoach) {
        NotificationDialog(
            onDismiss = { showDialog = false },
            notificationRepository = notificationRepository,
            onNotificationRead = {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        unreadCount = notificationRepository.getUnreadCount()
                    }
                }
            }
        )
    }
}

@Composable
fun NotificationDialog(
    onDismiss: () -> Unit,
    notificationRepository: NotificationRepository,
    onNotificationRead: () -> Unit
) {
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                notifications = notificationRepository.getAllNotifications()
            }
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header amélioré sans titre
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(IconOrange.copy(alpha = 0.1f), SportyKidsGreen.copy(alpha = 0.1f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    notificationRepository.markAllAsRead()
                                    notifications = notificationRepository.getAllNotifications()
                                }
                                onNotificationRead()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = SportyKidsGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tout marquer comme lu", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                
                HorizontalDivider(
                    color = TextDarkGray.copy(alpha = 0.2f),
                    thickness = 1.dp
                )
                
                // Liste des notifications
                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = TextDarkGray.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Aucune notification",
                                color = TextDarkGray.copy(alpha = 0.7f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notifications.size) { index ->
                            val notification = notifications[index]
                            NotificationItem(
                                notification = notification,
                                onClick = {
                                    coroutineScope.launch {
                                        withContext(Dispatchers.IO) {
                                            notificationRepository.markAsRead(notification.id)
                                            notifications = notificationRepository.getAllNotifications()
                                        }
                                        onNotificationRead()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.FRENCH)
    val dateStr = dateFormat.format(Date(notification.timestamp))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) {
                Color.White
            } else {
                IconOrange.copy(alpha = 0.08f)
            }
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.read) 0.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icône de notification avec fond coloré
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(IconOrange, SportyKidsGreen)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SportyKidsGreen
                    )
                    if (!notification.read) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    Color(0xFFFF5722),
                                    CircleShape
                                )
                        )
                    }
                }
                
                Text(
                    text = notification.body,
                    fontSize = 14.sp,
                    color = TextDarkGray,
                    lineHeight = 20.sp
                )
                
                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = TextDarkGray.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

