package com.example.dam_front.ui.calls

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VoiceCallScreen(callerName: String, onEnd: () -> Unit) {
    CallScaffold(title = "Voice call with $callerName", onEnd = onEnd) {
        Text("Calling...", style = MaterialTheme.typography.titleMedium)
    }
}

///////////////////////////////:
@Composable
fun VideoCallScreen(callerName: String, onEnd: () -> Unit) {
    CallScaffold(title = "Video call with $callerName", onEnd = onEnd) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Video stream placeholder")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { /* toggle camera */ }) { Text("Camera on/off") }
            Button(onClick = { /* switch camera */ }) { Text("Switch camera") }
            Button(onClick = { /* mute */ }) { Text("Mute") }
        }
    }
}

@Composable
private fun CallScaffold(title: String, onEnd: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        content()
        Spacer(Modifier.weight(1f))
        Button(onClick = onEnd) { Text("End call") }
    }
}

// Minimal WebRTC helper stubs
class WebRtcClient {
    fun createOffer() { /* TODO: implement with WebRTC */ }
    fun createAnswer() { /* TODO: implement with WebRTC */ }
    fun addIceCandidate() { /* TODO: implement with WebRTC */ }
    fun endCall() { /* TODO: implement with WebRTC */ }
}
