# 📝 Code à ajouter dans ChatScreen.kt

## 🎯 Modifications à faire

### 1️⃣ Ajouter l'import en haut du fichier

Ajoute cette ligne avec les autres imports (vers la ligne 50) :

```kotlin
import com.example.dam_front.ui.components.AiFeedbackMessageBubble
import com.example.dam_front.models.AiFeedbackMetadata
import com.google.gson.Gson
```

---

### 2️⃣ Modifier la fonction MessageItem

**Trouve cette section** (vers la ligne 387) :

```kotlin
when (message.type) {
    MessageType.image -> {
        message.mediaUrl?.let { url ->
            // ... code existant pour les images
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
```

**Remplace par** :

```kotlin
when (message.type) {
    MessageType.AI_FEEDBACK -> {
        // Message de feedback IA - Affichage spécial
        try {
            // Parser les métadonnées depuis le message
            val metadata = if (message.metadata != null) {
                message.metadata
            } else {
                // Métadonnées par défaut si non disponibles
                AiFeedbackMetadata(
                    matchId = "",
                    matchResult = "victoire",
                    teamName = "Équipe",
                    score = "0-0",
                    phase = "match",
                    emoji = "⚽"
                )
            }
            
            // Afficher le composant de feedback IA
            AiFeedbackMessageBubble(
                feedback = message.content ?: "",
                emoji = metadata.emoji,
                matchResult = metadata.matchResult,
                teamName = metadata.teamName,
                score = metadata.score,
                phase = metadata.phase,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            // En cas d'erreur, afficher comme un message normal
            Text(
                text = message.content ?: "",
                color = if (isOwn) MessengerTextOwn else MessengerTextOther,
                fontSize = 16.sp
            )
        }
    }
    
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
```

---

### 3️⃣ Modifier le modèle Message (si nécessaire)

**Trouve le fichier** : `app/src/main/java/com/example/dam_front/models/Message.kt`

**Vérifie que le modèle Message a un champ metadata** :

```kotlin
data class Message(
    @SerializedName("_id") 
    val _id: String? = null,
    
    val sender: User,
    val receiver: User,
    val conversationId: String,
    val type: MessageType,
    val content: String? = null,
    val mediaUrl: String? = null,
    val read: Boolean = false,
    val edited: Boolean = false,
    
    @SerializedName("replyToMessageId")
    val replyToMessageId: String? = null,
    
    val reactions: Map<String, String>? = null,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
    
    // AJOUTE CETTE LIGNE si elle n'existe pas
    val metadata: AiFeedbackMetadata? = null
)
```

---

## 🧪 Test rapide

Pour tester sans backend, tu peux créer un message de test :

```kotlin
// Dans ChatScreen, ajoute temporairement :
val testMessage = Message(
    _id = "test123",
    sender = currentUser,
    receiver = otherUser,
    conversationId = "conv123",
    type = MessageType.AI_FEEDBACK,
    content = "🏆 Bravo Amélie ! Ton équipe Les Champions a gagné 3-2 en finale ! Continue comme ça !",
    read = false,
    edited = false,
    createdAt = "2026-01-02T15:30:00.000Z",
    metadata = AiFeedbackMetadata(
        matchId = "match123",
        matchResult = "victoire",
        teamName = "Les Champions",
        score = "3-2",
        phase = "finale",
        emoji = "🏆"
    )
)
```

---

## ✅ Checklist finale

- [ ] Import ajouté en haut de ChatScreen.kt
- [ ] Code du when(message.type) modifié
- [ ] Champ metadata ajouté dans le modèle Message
- [ ] Compilation réussie
- [ ] Test de l'affichage

---

## 🎨 Résultat attendu

Quand un message de type `AI_FEEDBACK` arrive, tu verras :

```
┌─────────────────────────────────────┐
│     🌟 Message IA Motivant          │
├─────────────────────────────────────┤
│  🏆    FINALE                       │
│        Les Champions                │
│        Score: 3-2                   │
│  ─────────────────────────────      │
│  Bravo Amélie ! Ton équipe Les      │
│  Champions a gagné 3-2 en finale !  │
│  Continue comme ça ! 🌟             │
│                        15:30        │
└─────────────────────────────────────┘
```

Au lieu d'une bulle de message normale !

---

C'est tout ! 🚀
