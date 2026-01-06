# 🧪 Exemples d'Utilisation de l'API Gemini

## 📡 Endpoints Disponibles

### 1. Générer un Feedback
`POST /api/gemini/feedback/generate`

### 2. Envoyer aux Conversations
`POST /api/gemini/feedback/send-to-conversations`

---

## 🎯 Exemples de Requêtes

### Exemple 1 : Victoire en Finale 🏆

```bash
curl -X POST http://localhost:3001/api/gemini/feedback/generate \
  -H "Content-Type: application/json" \
  -d '{
    "childId": "child_123",
    "childName": "Amélie",
    "childAge": 8,
    "matchId": "match_456",
    "matchResult": "victoire",
    "teamName": "Les Champions",
    "score": "3-2",
    "phase": "finale",
    "performance": "excellent esprit d'\''équipe",
    "tournamentName": "Tournoi d'\''Été 2026"
  }'
```

**Réponse attendue :**
```json
{
  "feedback": "🏆 Félicitations Amélie ! Ton équipe Les Champions a gagné 3-2 en finale ! Tu as montré un excellent esprit d'équipe ! Continue comme ça championne !",
  "generatedAt": "2026-01-02T15:30:00.000Z",
  "childId": "child_123",
  "matchId": "match_456",
  "metadata": {
    "matchResult": "victoire",
    "teamName": "Les Champions",
    "score": "3-2",
    "phase": "finale",
    "emoji": "🏆"
  }
}
```

---

### Exemple 2 : Défaite en Demi-Finale 💪

```bash
curl -X POST http://localhost:3001/api/gemini/feedback/generate \
  -H "Content-Type: application/json" \
  -d '{
    "childId": "child_789",
    "childName": "Lucas",
    "childAge": 10,
    "matchId": "match_101",
    "matchResult": "defaite",
    "teamName": "Les Tigres",
    "score": "1-3",
    "phase": "demi-finale",
    "performance": "beaucoup de courage",
    "tournamentName": "Coupe des Jeunes"
  }'
```

**Réponse attendue :**
```json
{
  "feedback": "💪 Bravo Lucas pour ton effort ! Même si Les Tigres ont perdu 1-3, tu as montré beaucoup de courage. La prochaine fois sera encore meilleure !",
  "generatedAt": "2026-01-02T15:35:00.000Z",
  "childId": "child_789",
  "matchId": "match_101",
  "metadata": {
    "matchResult": "defaite",
    "teamName": "Les Tigres",
    "score": "1-3",
    "phase": "demi-finale",
    "emoji": "💪"
  }
}
```

---

### Exemple 3 : Match Nul ⚡

```bash
curl -X POST http://localhost:3001/api/gemini/feedback/generate \
  -H "Content-Type: application/json" \
  -d '{
    "childId": "child_202",
    "childName": "Emma",
    "childAge": 9,
    "matchId": "match_303",
    "matchResult": "nul",
    "teamName": "Les Étoiles",
    "score": "2-2",
    "phase": "quart-finale",
    "tournamentName": "Championnat Régional"
  }'
```

**Réponse attendue :**
```json
{
  "feedback": "⚡ Beau match Emma ! Les Étoiles ont fait match nul 2-2 en quart-finale ! Tu as bien joué et montré de la détermination !",
  "generatedAt": "2026-01-02T15:40:00.000Z",
  "childId": "child_202",
  "matchId": "match_303",
  "metadata": {
    "matchResult": "nul",
    "teamName": "Les Étoiles",
    "score": "2-2",
    "phase": "quart-finale",
    "emoji": "⚡"
  }
}
```

---

## 🚀 Envoyer aux Conversations

### Exemple : Envoyer automatiquement au parent et coach

```bash
curl -X POST http://localhost:3001/api/gemini/feedback/send-to-conversations \
  -H "Content-Type: application/json" \
  -d '{
    "childId": "child_123",
    "childName": "Amélie",
    "childAge": 8,
    "matchId": "match_456",
    "matchResult": "victoire",
    "teamName": "Les Champions",
    "score": "3-2",
    "phase": "finale",
    "tournamentName": "Tournoi d'\''Été 2026"
  }'
```

**Réponse :**
```json
{
  "feedback": "🏆 Félicitations Amélie ! ...",
  "generatedAt": "2026-01-02T15:30:00.000Z",
  "childId": "child_123",
  "matchId": "match_456",
  "metadata": { ... },
  "sentToParent": true,
  "sentToCoach": true
}
```

---

## 📱 Depuis Android (Kotlin)

### Utilisation du Repository

```kotlin
// Dans votre ViewModel ou Activity
val feedbackRepo = GeminiFeedbackRepository(context)

// Après la fin d'un match
viewModelScope.launch {
    val result = feedbackRepo.sendFeedbackToConversations(
        childId = "child_123",
        childName = "Amélie",
        childAge = 8,
        match = match,
        teamName = "Les Champions",
        matchResult = "victoire",
        performance = "excellent esprit d'équipe",
        tournamentName = "Tournoi d'Été 2026"
    )
    
    result.onSuccess { feedback ->
        Log.d("Gemini", "Feedback envoyé : ${feedback.feedback}")
    }.onFailure { error ->
        Log.e("Gemini", "Erreur : ${error.message}")
    }
}
```

---

## 🎨 Variations de Feedbacks

Gemini génère des messages **différents à chaque fois** ! Voici des exemples :

### Victoire 🏆
- "Bravo [nom] ! Quelle belle victoire pour [équipe] ! Continue comme ça !"
- "Félicitations [nom] ! [équipe] a gagné grâce à toi ! Tu es formidable !"
- "Super match [nom] ! [équipe] est fier de toi ! Encore bravo !"

### Défaite 💪
- "Bravo [nom] pour ton effort ! La défaite fait partie du jeu. Continue à t'améliorer !"
- "Bien joué [nom] ! Même si [équipe] a perdu, tu as montré du courage !"
- "Ne baisse pas les bras [nom] ! [équipe] compte sur toi pour la prochaine fois !"

### Match Nul ⚡
- "Beau match [nom] ! [équipe] a bien résisté ! Continue !"
- "Bravo [nom] ! Match serré mais tu as bien joué !"
- "Félicitations [nom] ! [équipe] a montré de la détermination !"

---

## 🔧 Personnalisation

### Modifier le Prompt

Dans `gemini.service.ts`, méthode `buildPrompt()`, vous pouvez :

1. **Changer le ton** : Plus enthousiaste, plus calme, etc.
2. **Ajouter des détails** : Statistiques du joueur, historique, etc.
3. **Changer la longueur** : Plus court ou plus long
4. **Ajouter des emojis** : Plus ou moins d'emojis

### Exemple de modification :

```typescript
return `
Tu es un coach sportif TRÈS enthousiaste pour enfants.

Écris un message COURT (1-2 phrases) et TRÈS TRÈS motivant pour ${childName}.

${tone}

Utilise BEAUCOUP d'emojis et d'exclamations !
`;
```

---

## 📊 Monitoring

### Logs Backend

```
[GeminiService] Generating feedback for child: Amélie
[GeminiService] Feedback generated successfully for Amélie
[GeminiController] Feedback sent to conversations successfully
```

### Logs Android

```
D/GeminiFeedbackRepository: Sending AI feedback to conversations for child: Amélie
D/GeminiFeedbackRepository: Feedback sent to conversations successfully
```

---

## ⚠️ Gestion d'Erreurs

### Si la clé API est invalide :
```json
{
  "statusCode": 500,
  "message": "Error generating feedback: Invalid API key"
}
```

### Si les données sont invalides :
```json
{
  "statusCode": 400,
  "message": ["childName should not be empty", "matchResult should not be empty"]
}
```

---

## 💡 Conseils

1. **Testez d'abord avec `/generate`** avant d'utiliser `/send-to-conversations`
2. **Vérifiez les logs** pour déboguer
3. **Personnalisez les prompts** selon vos besoins
4. **Limitez les appels** pour économiser l'API Gemini (gratuite mais limitée)

---

Bon développement ! 🚀
