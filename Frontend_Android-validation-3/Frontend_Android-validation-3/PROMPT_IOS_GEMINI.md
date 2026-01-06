# 📱 Prompt pour Antigravity - Implémentation IA Coach SportyKids (iOS/Swift)

## 🎯 Objectif
Implémenter une fonctionnalité d'IA de coaching dans l'application iOS SportyKids qui génère des conseils personnalisés pour les enfants après les matchs, en utilisant l'API Gemini.

## 📋 Contexte
J'ai déjà implémenté cette fonctionnalité dans la version Android de l'application. Voici ce qui a été fait :

### Backend (NestJS) - Déjà implémenté ✅
- **Module Gemini** créé avec service et controller
- **Endpoint POST** `/gemini/generate-coaching-advice`
- **Données d'entrée** :
  ```json
  {
    "childName": "string",
    "performance": "number (0-10)",
    "presence": "boolean",
    "activityType": "string",
    "focusAreas": ["string"],
    "nextSessionGoals": ["string"],
    "effortLevel": "number (1-10)",
    "emotionalState": "string",
    "commentaire": "string"
  }
  ```
- **Réponse** : Un message de coaching personnalisé et motivant généré par Gemini AI

### Version Android - Déjà implémenté ✅
Dans `ChatScreen.kt`, j'ai ajouté :
1. Un bouton "⚡ Conseil IA" dans l'interface de chat
2. Une fonction qui envoie les données du dernier suivi de l'enfant à l'API
3. Affichage du conseil IA dans une bulle de message spéciale
4. Gestion du chargement et des erreurs

## 🎨 Ce que je veux dans la version iOS

### 1. **Localisation du code**
- Trouver l'écran de chat/messagerie dans l'application iOS
- Identifier le composant équivalent à `ChatScreen.kt` (probablement un `ChatView` ou `ConversationView` en SwiftUI)

### 2. **Ajout du bouton IA**
- Ajouter un bouton "⚡ Conseil IA" dans la barre d'outils du chat
- Design similaire à Android : icône éclair (⚡) + texte
- Couleur : Bleu/Vert pour correspondre au thème SportyKids

### 3. **Appel API Gemini**
Créer une fonction Swift qui :
```swift
func fetchAICoachingAdvice(for childId: String) async throws -> String {
    // 1. Récupérer le dernier suivi de l'enfant depuis l'API
    let latestSuivi = try await getSuiviEnfantsByEnfantId(childId)
    
    // 2. Préparer les données pour Gemini
    let requestBody = [
        "childName": latestSuivi.enfant.prenom,
        "performance": latestSuivi.performance,
        "presence": latestSuivi.presence,
        "activityType": latestSuivi.activityType,
        "focusAreas": latestSuivi.focusAreas,
        "nextSessionGoals": latestSuivi.nextSessionGoals,
        "effortLevel": latestSuivi.effortLevel,
        "emotionalState": latestSuivi.emotionalState,
        "commentaire": latestSuivi.commentaire
    ]
    
    // 3. Appeler l'endpoint Gemini
    let response = try await apiClient.post("/gemini/generate-coaching-advice", body: requestBody)
    
    return response.advice
}
```

### 4. **Affichage du message IA**
- Créer une bulle de message spéciale pour les conseils IA
- Design distinctif :
  - Fond dégradé (bleu clair → violet clair)
  - Icône ⚡ en haut à gauche
  - Badge "Conseil IA" ou "SportyKids Coach"
  - Animation d'apparition fluide

### 5. **Gestion des états**
- **Loading** : Afficher un indicateur de chargement pendant la génération
- **Success** : Afficher le conseil dans la bulle spéciale
- **Error** : Afficher un message d'erreur convivial
- **Empty** : Si pas de suivi disponible, afficher "Aucun suivi récent trouvé"

### 6. **Intégration dans le chat**
- Le message IA doit apparaître comme un message système dans la conversation
- Il doit être sauvegardé localement (optionnel) ou juste affiché temporairement
- L'utilisateur doit pouvoir le copier

## 🔧 Détails techniques iOS

### Structure de données (Swift)
```swift
struct SuiviEnfant: Codable {
    let id: String
    let dateSuivi: Date
    let presence: Bool
    let performance: Int
    let commentaire: String?
    let enfant: EnfantRef?
    let activityType: String?
    let focusAreas: [String]?
    let nextSessionGoals: [String]?
    let effortLevel: Int?
    let emotionalState: String?
}

struct GeminiRequest: Codable {
    let childName: String
    let performance: Int
    let presence: Bool
    let activityType: String?
    let focusAreas: [String]?
    let nextSessionGoals: [String]?
    let effortLevel: Int?
    let emotionalState: String?
    let commentaire: String?
}

struct GeminiResponse: Codable {
    let advice: String
}
```

### Exemple d'UI (SwiftUI)
```swift
// Bouton dans la toolbar
.toolbar {
    ToolbarItem(placement: .navigationBarTrailing) {
        Button(action: { fetchAIAdvice() }) {
            Label("Conseil IA", systemImage: "bolt.fill")
                .foregroundColor(.blue)
        }
    }
}

// Bulle de message IA
struct AIMessageBubble: View {
    let advice: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "bolt.fill")
                    .foregroundColor(.yellow)
                Text("Conseil IA SportyKids")
                    .font(.caption)
                    .fontWeight(.bold)
            }
            
            Text(advice)
                .font(.body)
                .foregroundColor(.primary)
        }
        .padding()
        .background(
            LinearGradient(
                colors: [Color.blue.opacity(0.1), Color.purple.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .cornerRadius(16)
    }
}
```

## 📝 Checklist d'implémentation

- [ ] Localiser l'écran de chat dans le projet iOS
- [ ] Créer le service API pour appeler `/gemini/generate-coaching-advice`
- [ ] Créer les modèles de données Swift (SuiviEnfant, GeminiRequest, GeminiResponse)
- [ ] Ajouter le bouton "⚡ Conseil IA" dans la toolbar du chat
- [ ] Implémenter la fonction `fetchAICoachingAdvice`
- [ ] Créer le composant `AIMessageBubble` pour afficher le conseil
- [ ] Gérer les états de chargement, succès et erreur
- [ ] Tester avec différents profils d'enfants
- [ ] Vérifier que l'API backend fonctionne correctement
- [ ] Ajouter des animations pour une meilleure UX

## 🎨 Design Guidelines
- **Couleurs** : Utiliser le thème SportyKids (bleu, vert, orange)
- **Icône** : Éclair (⚡) pour représenter l'IA
- **Animation** : Transition douce lors de l'apparition du message
- **Accessibilité** : S'assurer que le texte est lisible et le bouton accessible

## 🔗 Endpoints API à utiliser
- `GET /suivi-enfant/enfant/{enfantId}` - Récupérer les suivis de l'enfant
- `POST /gemini/generate-coaching-advice` - Générer le conseil IA

## ⚠️ Points d'attention
1. Gérer le cas où l'enfant n'a pas encore de suivi
2. Limiter les appels API (éviter les appels multiples rapides)
3. Afficher un message d'erreur clair si l'API Gemini est indisponible
4. S'assurer que seuls les coaches peuvent utiliser cette fonctionnalité (vérifier le rôle)

---

## 🚀 Résultat attendu
Une fois implémenté, le coach pourra :
1. Ouvrir le chat avec un parent
2. Cliquer sur "⚡ Conseil IA"
3. Recevoir instantanément un conseil personnalisé basé sur les dernières performances de l'enfant
4. Le message apparaît dans le chat avec un design distinctif

---

**Note** : Cette fonctionnalité est déjà opérationnelle sur Android et le backend est prêt. Il suffit de créer l'interface iOS et de connecter les appels API.
