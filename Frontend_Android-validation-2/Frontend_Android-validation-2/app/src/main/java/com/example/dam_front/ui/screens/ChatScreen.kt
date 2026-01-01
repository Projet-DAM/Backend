package com.example.dam_front.ui.screens

import android.R
import android.app.Application
import androidx.activity.ComponentActivity
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.dam_front.config.ApiConfig
import com.example.dam_front.models.Message
import com.example.dam_front.utils.ImageUtils
import com.example.dam_front.models.MessageType
import com.example.dam_front.repository.AuthRepository
import com.example.dam_front.ui.theme.*
import com.example.dam_front.viewmodels.ConversationViewModel
import com.example.dam_front.viewmodels.ConversationViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.collections.eachCount
import kotlin.collections.forEach
import kotlin.collections.groupingBy
import kotlin.collections.isNotEmpty
import kotlin.collections.isNullOrEmpty
import kotlin.collections.lastIndex
import kotlin.collections.take
import kotlin.let
import kotlin.text.ifBlank
import kotlin.text.isEmpty
import kotlin.text.isNotBlank
import kotlin.text.isNotEmpty
import kotlin.text.startsWith
import kotlin.text.trim
import kotlin.text.trimEnd

// Messenger Colors
val MessengerBlue = Color(0xFF0084FF)
val MessengerGray = Color(0xFFF0F0F0) // For received messages
val MessengerTextOwn = Color.White
val MessengerTextOther = Color.Black
val MessengerBackground = Color.White

@Composable
fun ChatScreen(
    userId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val application = context.applicationContext as Application

    val vm: ConversationViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = ConversationViewModelFactory(application)
    )

    val uiState by vm.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    var currentUserId by remember { mutableStateOf("") }
    var otherUserName by remember { mutableStateOf("") }
    var otherUserPhoto by remember { mutableStateOf<String?>(null) }
    var replyingTo by remember { mutableStateOf<Message?>(null) }
    var imagePreviewUri by remember { mutableStateOf<Uri?>(null) }
    var menuMessage by remember { mutableStateOf<Message?>(null) }
    var editDraft by remember { mutableStateOf("") }
    var isEditingDialogOpen by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imagePreviewUri = it }
    }

    LaunchedEffect(userId) {
        try {
            val authRepo = AuthRepository(context)
            currentUserId = authRepo.getUserId() ?: ""
            if (uiState.availableContacts.isEmpty()) {
                vm.loadAvailableContacts()
            }
            vm.startConversationWith(userId)
        } catch (_: Exception) { }
    }

    LaunchedEffect(uiState.currentOtherUser) {
        uiState.currentOtherUser?.let { user ->
            otherUserName = "${user.prenom} ${user.nom}".trim().ifBlank { user.email ?: "Utilisateur" }
            otherUserPhoto = ImageUtils.getPhotoUrl(user.photoProfil)
        }
    }

    // Scroll to bottom when messages change and mark visible messages as read
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
            // Marquer automatiquement les messages re\u00e7us comme lus
            vm.markAllVisibleMessagesAsRead()
        }
    }

    Scaffold(
        topBar = {
            MessagerTopBar(
                navController = navController,
                name = otherUserName,
                photoUrl = otherUserPhoto,
                onAudioCall = { vm.startAudioCall(userId) },
                onVideoCall = { vm.startVideoCall(userId) }
            )
        },
        containerColor = MessengerBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoadingMessages) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MessengerBlue)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(uiState.messages) { message ->
                        val isOwn = message.sender._id == currentUserId
                        MessageItem(
                            message = message,
                            isOwn = isOwn,
                            senderPhoto = otherUserPhoto, // Only needed if !isOwn
                            onLongPress = { menuMessage = message },
                            onReply = {
                                replyingTo = message
                                vm.setReplyingTo(message)
                            }
                        )

                        // Context Menu (Action Sheet substitute)
                        if (menuMessage?._id == message._id) {
                            MessageContextMenu(
                                isOwn = isOwn,
                                onDismiss = { menuMessage = null },
                                onEdit = {
                                    editDraft = message.content ?: ""
                                    isEditingDialogOpen = true
                                },
                                onDeleteForMe = {
                                    vm.deleteMessageForMe(message._id ?: "")
                                    menuMessage = null
                                },
                                onDeleteForEveryone = {
                                    vm.deleteMessageForEveryone(message._id ?: "")
                                    menuMessage = null
                                },
                                onReact = { emoji ->
                                    vm.reactToMessage(message._id ?: "", emoji)
                                    menuMessage = null
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.isTypingOtherUser) {
                TypingIndicator(otherUserPhoto)
            }

            MessengerInputBar(
                text = messageText,
                onTextChange = {
                    messageText = it
                    vm.onTyping(uiState.currentConversationId)
                },
                onSend = {
                    if (imagePreviewUri != null && vm.uiState.value.currentConversationId != null) {
                        vm.sendImageMessage(userId, imagePreviewUri!!)
                        imagePreviewUri = null
                    }
                    if (messageText.isNotBlank() && vm.uiState.value.currentConversationId != null) {
                        vm.sendMessage(userId, messageText)
                        messageText = ""
                        replyingTo = null
                        vm.setReplyingTo(null)
                    }
                },
                onImagePick = { imagePickerLauncher.launch("image/*") },
                isSending = uiState.isSendingMessage,
                replyingTo = replyingTo,
                onCancelReply = { replyingTo = null; vm.setReplyingTo(null) },
                imagePreview = imagePreviewUri,
                onClearImage = { imagePreviewUri = null }
            )
        }
    }

    // Edit Dialog
    if (isEditingDialogOpen && menuMessage != null) {
        AlertDialog(
            onDismissRequest = { isEditingDialogOpen = false; menuMessage = null },
            confirmButton = {
                TextButton(onClick = {
                    vm.editMessage(menuMessage!!._id ?: "", editDraft)
                    isEditingDialogOpen = false
                    menuMessage = null
                }, colors = ButtonDefaults.textButtonColors(contentColor = MessengerBlue)) { 
                    Text("Terminer") 
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingDialogOpen = false; menuMessage = null }, 
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)) { 
                    Text("Annuler") 
                }
            },
            title = { Text("Modifier le message") },
            text = {
                OutlinedTextField(
                    value = editDraft,
                    onValueChange = { editDraft = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MessengerBlue,
                        cursorColor = MessengerBlue
                    )
                )
            }
        )
    }
}

@Composable
fun MessagerTopBar(
    navController: NavController,
    name: String,
    photoUrl: String?,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    Surface(
        shadowElevation = 1.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding() // Fixes the overlap with camera/time
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MessengerBlue)
            }
            
            Avatar(url = photoUrl, size = 40.dp)
            
            Spacer(Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                    maxLines = 1
                )
            }

            IconButton(onClick = onAudioCall) {
                Icon(Icons.Default.Call, "Call", tint = MessengerBlue)
            }
            IconButton(onClick = onVideoCall) {
                Icon(Icons.Default.Videocam, "Video", tint = MessengerBlue)
            }
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    isOwn: Boolean,
    senderPhoto: String?,
    onLongPress: () -> Unit,
    onReply: () -> Unit
) {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timestamp = try {
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        iso.parse(message.createdAt)?.let { sdf.format(it) } ?: ""
    } catch (e: Exception) { "" }

    // Logic to aggregate reactions
    val reactionCounts = message.reactions?.values?.groupingBy { it }?.eachCount()
    val hasReactions = !reactionCounts.isNullOrEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        // Avatar for other user
        if (!isOwn) {
            Avatar(url = senderPhoto, size = 28.dp)
            Spacer(Modifier.width(8.dp))
        }

        // Message Bubble Box acting as anchor for reactions
        Box {
            Column(
                horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start,
                modifier = Modifier.padding(bottom = if (hasReactions) 16.dp else 0.dp)
            ) {
                // Reply Bubble Header
                if (message.replyToMessageId != null) {
                    Surface(
                        color = Color(0xFFE8E8E8),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        Text(
                            text = "↪ A répondu à un message",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Main Message Bubble
                Surface(
                    color = if (isOwn) MessengerBlue else MessengerGray,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { onLongPress() },
                                onDoubleTap = { onReply() }
                            )
                        }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        when (message.type) {
                            MessageType.image -> {
                                message.mediaUrl?.let { url ->
                                    val fixedUrl = when {
                                        url.startsWith("http://") || url.startsWith("https://") -> url
                                        url.startsWith("/") -> "${ApiConfig.BASE_URL.trimEnd('/')}$url"
                                        else -> "${ApiConfig.BASE_URL.trimEnd('/')}/$url"
                                    }
                                    
                                    Log.d("ChatScreen", "Loading image from: $fixedUrl")

                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(fixedUrl)
                                            .crossfade(true)
                                            .error(R.drawable.ic_menu_report_image)
                                            .build(),
                                        contentDescription = "Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 100.dp, max = 300.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFEEEEEE)),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = message.content ?: "",
                                    color = if (isOwn) MessengerTextOwn else MessengerTextOther,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
                
                // Timestamp & Status (Lu/Modifié)
                if (timestamp.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                    ) {
                        Text(
                            text = timestamp + if (message.edited) " (modifié)" else "",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        // Afficher "Lu" pour les messages envoyés (isOwn) qui ont été lus
                        if (isOwn && message.read) {
                            Text(
                                text = "• Lu",
                                fontSize = 10.sp,
                                color = MessengerBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Reactions Bubbles (Overlapping)
            if (hasReactions) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(y = 8.dp, x = (-4).dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            reactionCounts?.entries?.take(3)?.forEach { (emoji, count) ->
                                Text(text = "$emoji${if (count > 1) count.toString() else ""}", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageContextMenu(
    isOwn: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onReact: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column {
                // Reactions Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("👍", "❤️", "😂", "😮", "😢", "😡").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 24.sp,
                            modifier = Modifier.clickable { onReact(emoji) }
                        )
                    }
                }
                HorizontalDivider()
                // Actions
                if (isOwn) {
                    TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Modifier", color = Color.Black) }
                    TextButton(onClick = onDeleteForEveryone, modifier = Modifier.fillMaxWidth()) { Text("Supprimer pour tout le monde", color = Color.Red) }
                }
                TextButton(onClick = onDeleteForMe, modifier = Modifier.fillMaxWidth()) { Text("Supprimer pour moi", color = Color.Red) }
            }
        },
        confirmButton = {},
        containerColor = Color.White
    )
}

@Composable
fun MessengerInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onImagePick: () -> Unit,
    isSending: Boolean,
    replyingTo: Message?,
    onCancelReply: () -> Unit,
    imagePreview: Uri?,
    onClearImage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(8.dp)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Reply Preview
        if (replyingTo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Réponse à...", color = Color.Gray, fontSize = 12.sp)
                    Text(replyingTo.content ?: "Image/Audio", maxLines = 1, fontSize = 14.sp)
                }
                IconButton(onClick = onCancelReply) {
                    Icon(Icons.Default.Close, "Cancel", tint = Color.Gray)
                }
            }
        }

        // Image Preview
        if (imagePreview != null) {
            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                AsyncImage(
                    model = imagePreview,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = onClearImage,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(24.dp)
                ) {
                    Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.padding(4.dp))
                }
            }
        }

        // Input Row
        Row(verticalAlignment = Alignment.Bottom) {
            IconButton(onClick = onImagePick) {
                Icon(Icons.Default.Image, "Image", tint = MessengerBlue)
            }
            
            // Pill shaped text field
            Surface(
                color = Color(0xFFF0F0F0),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                    maxLines = 5,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text("Aa", color = Color.Gray)
                        }
                        innerTextField()
                    }
                )
            }

            IconButton(
                onClick = onSend, 
                enabled = (text.isNotBlank() || imagePreview != null) && !isSending
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MessengerBlue)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = MessengerBlue)
                }
            }
        }
    }
}

@Composable
fun TypingIndicator(photoUrl: String?) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    // Animation pour faire clignoter les dots
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )
    
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(8.dp)) {
        Avatar(url = photoUrl, size = 28.dp)
        Spacer(Modifier.width(8.dp))
        Surface(
            color = MessengerGray,
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = dot1Alpha))
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = dot2Alpha))
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = dot3Alpha))
                )
            }
        }
    }
}

@Composable
fun Avatar(url: String?, size: Dp) {
    val model = ImageUtils.getPhotoUrl(url) ?: "https://placehold.co/150"
    
    AsyncImage(
        model = model,
        contentDescription = "Avatar",
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(0.5.dp, Color.LightGray, CircleShape),
        contentScale = ContentScale.Crop,
        error = ColorPainter(Color.Gray)
    )
}
