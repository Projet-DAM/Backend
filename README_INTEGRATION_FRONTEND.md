# 📚 Documentation Complète - Intégration Frontend avec Backend NestJS

Bienvenue dans la documentation complète pour l'intégration de votre application frontend avec le backend NestJS de SportyConnect Kids.

---

## 📖 Table des Matières

### 🎯 Guides Principaux

1. **[Guide d'Intégration Frontend](./GUIDE_INTEGRATION_FRONTEND.md)** ⭐
   - Configuration de base
   - Architecture de l'API
   - Authentification JWT complète
   - Tous les endpoints détaillés
   - Exemples de code Flutter/Dart complets
   - Service API complet
   - Gestion des erreurs
   - Bonnes pratiques

2. **[Guide d'Intégration Android/Kotlin](./GUIDE_INTEGRATION_ANDROID_KOTLIN.md)** 🤖
   - Configuration Retrofit
   - Modèles de données Kotlin
   - Repository Pattern
   - ViewModel examples
   - Exemples d'Activity/Fragment
   - Adapters RecyclerView
   - Gestion des erreurs Android

3. **[Guide des Flux d'Appels API](./GUIDE_FLUX_APPELS_API.md)** 🔄
   - Diagrammes de séquence
   - Flux d'authentification
   - Flux de création d'abonnement
   - Flux de paiement Stripe
   - Flux de gestion des offres
   - Scénarios complets (Parent, Académie)

4. **[Référence Rapide API](./REFERENCE_RAPIDE_API.md)** ⚡
   - Tableau de tous les endpoints
   - Types de données
   - Headers requis
   - Exemples de body
   - Query parameters
   - Codes d'erreur
   - Exemples cURL
   - Configuration IP mobile
   - Checklist d'intégration

---

## 🚀 Démarrage Rapide

### 1. Pour les Développeurs Flutter/Dart

```bash
# Lire dans l'ordre :
1. GUIDE_INTEGRATION_FRONTEND.md (Section "Configuration de Base")
2. GUIDE_INTEGRATION_FRONTEND.md (Section "Authentification JWT")
3. GUIDE_FLUX_APPELS_API.md (Scénario A: Parcours Parent)
4. REFERENCE_RAPIDE_API.md (Pour référence rapide)
```

**Code à copier** :
- Service API complet : `GUIDE_INTEGRATION_FRONTEND.md` (Section "Exemples de Code Complets")
- Modèles de données : `GUIDE_INTEGRATION_FRONTEND.md` (Section "Modèles de Données")

### 2. Pour les Développeurs Android/Kotlin

```bash
# Lire dans l'ordre :
1. GUIDE_INTEGRATION_ANDROID_KOTLIN.md (Section "Configuration Retrofit")
2. GUIDE_INTEGRATION_ANDROID_KOTLIN.md (Section "Gestion de l'Authentification")
3. GUIDE_FLUX_APPELS_API.md (Scénario A: Parcours Parent)
4. REFERENCE_RAPIDE_API.md (Pour référence rapide)
```

**Code à copier** :
- RetrofitClient : `GUIDE_INTEGRATION_ANDROID_KOTLIN.md` (Section "Configuration Retrofit")
- Modèles : `GUIDE_INTEGRATION_ANDROID_KOTLIN.md` (Section "Modèles de Données")
- Repository : `GUIDE_INTEGRATION_ANDROID_KOTLIN.md` (Section "Repository Pattern")

### 3. Pour les Développeurs iOS/Swift

Utilisez le **Guide d'Intégration Frontend** comme référence et adaptez les exemples Flutter/Dart en Swift :
- Utilisez `URLSession` ou `Alamofire` au lieu de `http`
- Utilisez `Codable` pour les modèles
- Utilisez `UserDefaults` ou `Keychain` pour le token

---

## 🎯 Par Cas d'Usage

### Je veux implémenter l'authentification

1. **Lire** : `GUIDE_INTEGRATION_FRONTEND.md` → Section "Authentification JWT"
2. **Voir le flux** : `GUIDE_FLUX_APPELS_API.md` → "Flux d'Authentification"
3. **Référence** : `REFERENCE_RAPIDE_API.md` → Section "AUTHENTIFICATION"

**Endpoints à utiliser** :
- `POST /auth/register`
- `POST /auth/verify-email`
- `POST /auth/login`

### Je veux afficher les offres d'abonnement

1. **Lire** : `GUIDE_INTEGRATION_FRONTEND.md` → Section "OFFERS"
2. **Voir le flux** : `GUIDE_FLUX_APPELS_API.md` → "Scénario A: Parcours Parent" (étape 4)
3. **Référence** : `REFERENCE_RAPIDE_API.md` → Section "OFFRES"

**Endpoint à utiliser** :
- `GET /offers?isActive=true`

### Je veux créer un abonnement

1. **Lire** : `GUIDE_INTEGRATION_FRONTEND.md` → Section "SUBSCRIPTIONS"
2. **Voir le flux** : `GUIDE_FLUX_APPELS_API.md` → "Flux de Création d'Abonnement"
3. **Référence** : `REFERENCE_RAPIDE_API.md` → Section "ABONNEMENTS"

**Endpoints à utiliser** :
- `POST /subscriptions`
- `POST /payments/create-payment-intent`
- `POST /payments/complete`

### Je veux gérer les paiements Stripe

1. **Lire** : `GUIDE_INTEGRATION_FRONTEND.md` → Section "PAYMENTS"
2. **Voir le flux** : `GUIDE_FLUX_APPELS_API.md` → "Flux de Paiement Stripe"
3. **Référence** : `REFERENCE_RAPIDE_API.md` → Section "PAIEMENTS"

**Endpoints à utiliser** :
- `POST /payments/create-payment-intent`
- Stripe SDK (frontend)
- `POST /payments/complete`

### Je suis une académie et je veux gérer mes offres

1. **Lire** : `GUIDE_INTEGRATION_FRONTEND.md` → Section "OFFERS"
2. **Voir le flux** : `GUIDE_FLUX_APPELS_API.md` → "Scénario B: Parcours Académie"
3. **Référence** : `REFERENCE_RAPIDE_API.md` → Section "OFFRES"

**Endpoints à utiliser** :
- `POST /offers` (créer)
- `GET /offers` (lister mes offres)
- `PATCH /offers/:id` (modifier)
- `DELETE /offers/:id` (supprimer)
- `GET /offers/all-subscribers` (voir les inscrits)

---

## 📊 Architecture Globale

```
┌─────────────────────────────────────────────────────────┐
│                    FRONTEND                             │
│  (Flutter, Android, iOS, React, etc.)                   │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │   UI Layer   │  │ State Mgmt   │  │  Navigation  │ │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘ │
│         │                  │                            │
│  ┌──────▼──────────────────▼───────┐                   │
│  │      API Service / Repository    │                   │
│  │  - Authentification              │                   │
│  │  - Gestion du Token JWT          │                   │
│  │  - Appels HTTP                   │                   │
│  └──────────────┬───────────────────┘                   │
└─────────────────┼───────────────────────────────────────┘
                  │
                  │ HTTP/HTTPS
                  │ Authorization: Bearer {token}
                  │
┌─────────────────▼───────────────────────────────────────┐
│                    BACKEND                              │
│              (NestJS - Node.js)                         │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │ Controllers  │  │   Services   │  │  Middleware  │ │
│  │ (Routes)     │  │  (Business   │  │  - JWT Auth  │ │
│  │              │  │   Logic)     │  │  - CORS      │ │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘ │
│         │                  │                            │
│  ┌──────▼──────────────────▼───────┐                   │
│  │         MongoDB                  │                   │
│  │  - Users                         │                   │
│  │  - Offers                        │                   │
│  │  - Subscriptions                 │                   │
│  │  - Payments                      │                   │
│  └──────────────────────────────────┘                   │
└──────────────────┬──────────────────────────────────────┘
                   │
                   │ External APIs
                   │
          ┌────────▼────────┐
          │  Stripe API     │
          │  (Paiements)    │
          └─────────────────┘
```

---

## 🔐 Flux d'Authentification Simplifié

```
1. User s'inscrit → POST /auth/register
   ↓
2. Reçoit email avec code
   ↓
3. Vérifie email → POST /auth/verify-email
   ↓
4. Reçoit JWT token
   ↓
5. Sauvegarde token localement
   ↓
6. Utilise token dans toutes les requêtes
   Header: Authorization: Bearer {token}
```

---

## 💡 Concepts Clés

### 1. JWT (JSON Web Token)
- **Qu'est-ce que c'est ?** Un token sécurisé contenant les infos de l'utilisateur
- **Contenu** : `{userId, email, role, iat, exp}`
- **Utilisation** : Ajouté dans le header `Authorization: Bearer {token}`
- **Durée de vie** : Configurable (généralement 24h-7j)

### 2. Rôles et Permissions
- **PARENT** : Gère ses enfants et abonnements
- **COACH** : Accès limité aux données
- **ACADEMIE** : Gère ses offres et voit ses inscrits
- **ADMIN** : Accès complet

### 3. Filtrage Automatique
Le backend filtre automatiquement les données selon le rôle :
- Une **académie** ne voit que ses propres offres et abonnements
- Un **parent** ne voit que ses propres enfants et abonnements

### 4. Status des Abonnements
- **PENDING** → En attente de paiement
- **ACTIVE** → Actif et payé
- **EXPIRED** → Expiré
- **CANCELLED** → Annulé par l'utilisateur
- **SUSPENDED** → Suspendu par l'académie

---

## 🛠️ Outils de Développement

### 1. Swagger UI (Documentation Interactive)
```
http://localhost:3000/api
```
- Tester tous les endpoints
- Voir les schémas de données
- Générer des exemples de requêtes

### 2. Postman
- Importer la collection depuis Swagger
- Créer des environnements (dev, prod)
- Sauvegarder les tokens

### 3. JWT Decoder
```
https://jwt.io
```
- Décoder et vérifier les tokens
- Voir le contenu du payload
- Vérifier l'expiration

---

## ⚠️ Erreurs Courantes et Solutions

### Erreur 401 (Unauthorized)
**Cause** : Token invalide ou expiré  
**Solution** : Redemander à l'utilisateur de se connecter

### Erreur 403 (Forbidden)
**Cause** : Permissions insuffisantes  
**Solution** : Vérifier le rôle de l'utilisateur

### Erreur 404 (Not Found)
**Cause** : Ressource introuvable  
**Solution** : Vérifier l'ID de la ressource

### Erreur CORS
**Cause** : Frontend et backend sur des domaines différents  
**Solution** : Le backend accepte déjà `origin: '*'` en dev

### Connexion refusée (ECONNREFUSED)
**Cause** : Backend non démarré ou mauvaise URL  
**Solution** :
- Vérifier que le backend tourne : `npm run start:dev`
- Vérifier l'URL de base
- Pour Android émulateur : utiliser `10.0.2.2` au lieu de `localhost`

---

## 📱 Configuration par Plateforme

### Flutter/Dart
```dart
static const String baseUrl = 'http://localhost:3000';
// Pour Android émulateur : 'http://10.0.2.2:3000'
```

### Android/Kotlin
```kotlin
private const val BASE_URL = "http://10.0.2.2:3000/"
// Pour device physique : "http://192.168.1.X:3000/"
```

### iOS/Swift
```swift
private let baseURL = "http://localhost:3000/"
// Pour device physique : "http://192.168.1.X:3000/"
```

### React/JavaScript
```javascript
const BASE_URL = 'http://localhost:3000';
```

---

## ✅ Checklist Complète d'Intégration

### Phase 1 : Configuration (1-2h)
- [ ] Installer les dépendances HTTP
- [ ] Configurer l'URL de base
- [ ] Tester la connexion au backend
- [ ] Configurer CORS si nécessaire

### Phase 2 : Authentification (2-4h)
- [ ] Implémenter l'inscription
- [ ] Implémenter la vérification email
- [ ] Implémenter la connexion
- [ ] Sauvegarder le token JWT
- [ ] Créer un intercepteur pour ajouter le token
- [ ] Gérer l'expiration du token
- [ ] Implémenter la déconnexion

### Phase 3 : Modèles de Données (1-2h)
- [ ] Créer le modèle User
- [ ] Créer le modèle Offer
- [ ] Créer le modèle Subscription
- [ ] Créer le modèle Child
- [ ] Créer le modèle Payment

### Phase 4 : Fonctionnalités Métier (4-8h)
- [ ] Lister les offres
- [ ] Afficher les détails d'une offre
- [ ] Créer/Modifier/Supprimer des offres (Académie)
- [ ] Ajouter des enfants (Parent)
- [ ] Créer des abonnements
- [ ] Lister mes abonnements
- [ ] Gérer les paiements Stripe
- [ ] Annuler/Renouveler un abonnement

### Phase 5 : UI/UX (4-8h)
- [ ] Écrans d'authentification
- [ ] Liste des offres
- [ ] Détails d'une offre
- [ ] Gestion des enfants
- [ ] Liste des abonnements
- [ ] Processus de paiement
- [ ] Écran de confirmation

### Phase 6 : Tests et Débogage (2-4h)
- [ ] Tester avec Swagger
- [ ] Tester sur émulateur
- [ ] Tester sur device physique
- [ ] Gérer les cas d'erreur
- [ ] Tests de bout en bout

---

## 📞 Support

### Documentation
- **Swagger** : `http://localhost:3000/api`
- **Guides** : Ce dossier contient 4 guides complets

### Débogage
1. Vérifier les logs du backend (console)
2. Utiliser Swagger pour tester les endpoints
3. Décoder le JWT sur jwt.io
4. Vérifier les headers de requête

### Ressources Externes
- [NestJS Documentation](https://docs.nestjs.com/)
- [Stripe Documentation](https://stripe.com/docs)
- [JWT.io](https://jwt.io)
- [Postman](https://www.postman.com/)

---

## 🎓 Prochaines Étapes

1. **Lire le guide adapté à votre technologie**
   - Flutter/Dart → `GUIDE_INTEGRATION_FRONTEND.md`
   - Android/Kotlin → `GUIDE_INTEGRATION_ANDROID_KOTLIN.md`

2. **Comprendre les flux**
   - Lire `GUIDE_FLUX_APPELS_API.md`

3. **Garder la référence à portée**
   - Bookmark `REFERENCE_RAPIDE_API.md`

4. **Commencer par l'authentification**
   - C'est la base de tout

5. **Tester avec Swagger**
   - Avant d'implémenter dans le frontend

6. **Implémenter progressivement**
   - Authentification → Offres → Abonnements → Paiements

---

**Bonne intégration ! 🚀**

---

**Dernière mise à jour** : Janvier 2024  
**Version API** : 1.0  
**Auteur** : Documentation générée pour SportyConnect Kids
