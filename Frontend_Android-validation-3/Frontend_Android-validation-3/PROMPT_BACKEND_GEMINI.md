# 🤖 PROMPT POUR IA - Intégration Gemini AI dans Backend NestJS

Copie-colle ce prompt dans ton autre Antigravity où le backend est ouvert :

---

## PROMPT À UTILISER :

```
Je veux intégrer Gemini AI dans mon backend NestJS pour générer des feedbacks motivants pour les enfants après chaque match de tournoi.

OBJECTIF :
Quand un match se termine, je veux que Gemini génère automatiquement un message personnalisé et motivant pour chaque enfant, puis envoie ce message dans la conversation (chat) du parent ET du coach de cet enfant.

EXEMPLE DE FEEDBACK :
- Victoire : "🏆 Bravo Amélie ! Ton équipe Les Champions a gagné 3-2 en finale ! Continue comme ça !"
- Défaite : "💪 Bravo Lucas pour ton effort ! Même si ton équipe a perdu, tu as montré du courage !"
- Nul : "⚡ Beau match Emma ! Match serré mais tu as bien joué !"

ÉTAPES À IMPLÉMENTER :

1. **Créer un module Gemini** avec :
   - Service pour générer les feedbacks avec l'API Gemini
   - Controller avec 2 endpoints :
     * POST /api/gemini/feedback/generate - Générer un feedback
     * POST /api/gemini/feedback/send-to-conversations - Générer ET envoyer aux conversations
   - DTO pour valider les données :
     * childId, childName, childAge (optionnel)
     * matchId, matchResult (victoire/defaite/nul)
     * teamName, score, phase
     * performance (optionnel), tournamentName (optionnel)

2. **Installer la dépendance** :
   ```bash
   npm install @google/generative-ai
   ```

3. **Configurer Gemini** :
   - Ajouter GEMINI_API_KEY dans .env
   - Utiliser le modèle 'gemini-pro'

4. **Prompt pour Gemini** (très important) :
   Le prompt doit :
   - Être adapté aux enfants (langage simple)
   - Être TRÈS motivant et positif
   - Mentionner l'équipe, le résultat, la phase
   - Commencer par un emoji (🏆 victoire, 💪 défaite, ⚡ nul)
   - Faire 2-3 phrases maximum
   - Parler directement à l'enfant (tutoiement)
   - Adapter le ton selon victoire/défaite/nul

5. **Logique d'envoi aux conversations** :
   Dans l'endpoint /send-to-conversations :
   - Générer le feedback avec Gemini
   - Trouver le parent de l'enfant (via childId)
   - Trouver le coach de l'enfant
   - Créer un message de type "ai_feedback" dans la conversation du parent
   - Créer un message de type "ai_feedback" dans la conversation du coach
   - Inclure les métadonnées : matchId, matchResult, teamName, score, phase, emoji

6. **Déclencher automatiquement après un match** :
   Quand un match passe au statut "termine" :
   - Récupérer tous les enfants des 2 équipes
   - Pour chaque enfant :
     * Déterminer le résultat (victoire/defaite/nul) selon son équipe
     * Appeler /api/gemini/feedback/send-to-conversations
     * Le feedback sera automatiquement envoyé au parent et coach

STRUCTURE DES FICHIERS À CRÉER :
```
src/
├── gemini/
│   ├── dto/
│   │   └── generate-feedback.dto.ts
│   ├── gemini.controller.ts
│   ├── gemini.service.ts
│   └── gemini.module.ts
```

EXEMPLE DE PROMPT POUR GEMINI (à adapter) :
```
Tu es un coach sportif bienveillant pour enfants.

CONTEXTE :
- Lucas joue dans l'équipe Les Tigres
- L'équipe a GAGNÉ 3-2 en demi-finale
- Lucas a montré un excellent esprit d'équipe

MISSION :
Écris un message court (2-3 phrases) et TRÈS motivant pour Lucas.

RÈGLES :
1. Félicite chaleureusement pour la victoire
2. Langage simple adapté aux enfants
3. Sois TRÈS positif et encourageant
4. Mentionne l'équipe et le résultat
5. Ajoute 🏆 au début
6. 2-3 phrases maximum
7. Tutoie l'enfant
8. Termine par un encouragement

Génère le message :
```

INTÉGRATION AVEC LE SYSTÈME DE MESSAGES :
- Type de message : "ai_feedback"
- Le message doit apparaître dans le chat comme un message spécial
- Inclure les métadonnées pour l'affichage côté Android

SÉCURITÉ :
- Valider toutes les données avec class-validator
- Logger les générations de feedback
- Gérer les erreurs (clé API invalide, etc.)

Crée tous les fichiers nécessaires et implémente cette fonctionnalité complète.
```

---

## 📋 CHECKLIST POUR TOI :

Après avoir utilisé ce prompt dans ton autre Antigravity :

- [ ] Vérifier que le module Gemini est créé
- [ ] Installer `@google/generative-ai`
- [ ] Ajouter `GEMINI_API_KEY` dans .env
- [ ] Tester l'endpoint `/api/gemini/feedback/generate`
- [ ] Implémenter l'envoi aux conversations
- [ ] Déclencher automatiquement après les matchs
- [ ] Tester avec un vrai match

---

## 🔑 OBTENIR LA CLÉ API GEMINI :

1. Va sur : https://makersuite.google.com/app/apikey
2. Clique sur "Create API Key"
3. Copie la clé
4. Ajoute dans ton .env :
   ```
   GEMINI_API_KEY=ta_clé_ici
   ```

---

## 🧪 TESTER APRÈS IMPLÉMENTATION :

```bash
curl -X POST http://localhost:3001/api/gemini/feedback/generate \
  -H "Content-Type: application/json" \
  -d '{
    "childId": "123",
    "childName": "Amélie",
    "childAge": 8,
    "matchId": "match123",
    "matchResult": "victoire",
    "teamName": "Les Champions",
    "score": "3-2",
    "phase": "finale"
  }'
```

---

Voilà ! Utilise ce prompt dans ton autre Antigravity et tout sera créé automatiquement ! 🚀
