# Documentation & Guide d'Implémentation du Chatbot Vocal IA

## 1. Architecture Globale

Ce projet implémente un chatbot vocal intelligent pour assister les parents dans la gestion de leurs abonnements, répondre aux FAQ et fournir des informations sur les coachs et l'académie.

### Flux de Données
1.  **Utilisateur (Parent)** : Parle à l'application mobile.
2.  **Mobile (Frontend)** : Capture la voix, la convertit en texte (Speech-to-Text), et envoie ce texte au backend via API.
3.  **Backend (NestJS)** :
    *   Reçoit le message texte.
    *   Construit un "contexte" enrichi (infos académie, liste des coachs, offres actives, abonnements du parent).
    *   Interroge l'IA (OpenAI) avec ce contexte et la question du parent.
    *   Retourne la réponse textuelle générée.
4.  **Mobile (Frontend)** : Reçoit le texte, et le lit à haute voix (Text-to-Speech).

---

## 2. Backend (NestJS)

Le module `ChatbotModule` a été créé dans le backend.

### Configuration Requise
Pour que l'IA fonctionne réellement, vous devez ajouter votre clé API OpenAI dans le fichier `.env` :

```env
OPENAI_API_KEY=sk-votre-cle-api-ici
```

*Si aucune clé n'est fournie, le chatbot utilisera un mode de secours basique (règles simples).*

### Endpoint API
- **URL** : `POST /chatbot/message`
- **Body JSON** :
  ```json
  {
    "message": "Quels sont les tarifs pour le football ?",
    "userId": "ID_DU_PARENT_OPTIONNEL" 
  }
  ```
- **Réponse JSON** :
  ```json
  {
    "response": "Nos tarifs pour le football commencent à 50 TND par mois..."
  }
  ```

---

## 3. Guide d'Implémentation Frontend (Android / Kotlin)

Voici comment intégrer le chatbot vocal dans votre application mobile Android.

### Étape A : Permissions AndroidManifest.xml
Assurez-vous d'avoir la permission Internet et Microphone.
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

### Étape B : Interface Utilisateur (Jetpack Compose)
Une interface simple avec un gros bouton microphone.

```kotlin
@Composable
fun VoiceChatScreen(viewModel: ChatbotViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zone de chat (Historique)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.messages) { msg ->
                ChatBubble(msg)
            }
        }
        
        // Indicateur d'état (Écoute, Réflexion, Parle)
        Text(text = state.statusText)
        
        // Bouton Micro
        IconButton(
            onClick = { viewModel.toggleListening() },
            modifier = Modifier.size(80.dp).background(Color.Blue, CircleShape)
        ) {
            Icon(
                imageVector = if (state.isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = "Parler",
                tint = Color.White
            )
        }
    }
}
```

### Étape C : Speech-to-Text (Reconnaissance Vocale)
Utilisez `SpeechRecognizer` d'Android.

```kotlin
val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
}

speechRecognizer.setRecognitionListener(object : RecognitionListener {
    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        // Envoyer 'text' au ViewModel pour appel API
        viewModel.sendMessage(text)
    }
    // ... implémenter les autres méthodes (onError, etc.)
})

// Démarrer l'écoute
speechRecognizer.startListening(intent)
```

### Étape D : Text-to-Speech (Synthèse Vocale)
Pour lire la réponse de l'IA.

```kotlin
val tts = TextToSpeech(context) { status ->
    if (status == TextToSpeech.SUCCESS) {
        tts.language = Locale.getDefault()
    }
}

// Lire la réponse
fun speak(text: String) {
    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
}
```

### Étape E : Appel API (Retrofit)
Connectez votre `ChatbotViewModel` à votre API NestJS.

```kotlin
interface ChatbotApi {
    @POST("chatbot/message")
    suspend fun sendMessage(@Body req: ChatMessageRequest): ChatMessageResponse
}

// Dans le ViewModel
fun sendMessage(text: String) {
    viewModelScope.launch {
        status = "Réflexion..."
        try {
            val response = api.sendMessage(ChatMessageRequest(message = text, userId = currentUserId))
            // Ajouter la réponse à la liste
            messages.add(Message(role = "assistant", content = response.response))
            // Lire à voix haute
            speak(response.response)
            status = "Prêt"
        } catch (e: Exception) {
            status = "Erreur"
        }
    }
}
```

---

## 4. Bénéfices pour l'Académie

1.  **Disponibilité 24/7** : Les parents peuvent obtenir des réponses tard le soir ou le week-end sans attendre.
2.  **Gain de Temps** : Réduit les appels téléphoniques répétitifs pour des questions simples (horaires, prix).
3.  **Modernité** : Offre une image innovante et technologique de l'académie.
4.  **Personnalisation** : L'IA peut dire "Bonjour Mme X, votre fils Y a un abonnement qui expire bientôt", ce qui crée un lien fort.
5.  **Accessibilité** : La voix est plus simple pour certains utilisateurs que de naviguer dans des menus complexes.
