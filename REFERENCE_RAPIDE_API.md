# 📚 Référence Rapide API - Tableau des Endpoints

## 🎯 URL de Base
```
http://localhost:3000
```

## 📖 Documentation Interactive
```
http://localhost:3000/api (Swagger)
```

---

## 🔐 AUTHENTIFICATION

| Endpoint | Méthode | Auth | Rôles | Description | Body | Réponse |
|----------|---------|------|-------|-------------|------|---------|
| `/auth/register` | POST | ❌ Non | Public | Inscription | `{nom, prenom, email, motDePasse, role}` | `{userId, email, message}` |
| `/auth/login` | POST | ❌ Non | Public | Connexion | `{email, motDePasse}` | `{access_token, user}` |
| `/auth/verify-email` | POST | ❌ Non | Public | Vérifier email | `{userId, code}` | `{access_token, user}` |

---

## 👤 UTILISATEURS

| Endpoint | Méthode | Auth | Rôles | Description | Body/Query | Réponse |
|----------|---------|------|-------|-------------|------------|---------|
| `/users/me` | GET | ✅ Oui | Tous | Mon profil | - | `{user}` |
| `/users/me` | PATCH | ✅ Oui | Tous | Modifier profil | `{nom?, prenom?, telephone?}` | `{user}` |
| `/users/children` | GET | ✅ Oui | PARENT | Mes enfants | - | `[children]` |
| `/users/children` | POST | ✅ Oui | PARENT | Ajouter enfant | `{nom, prenom, dateNaissance, genre}` | `{child}` |

---

## 🎯 OFFRES

| Endpoint | Méthode | Auth | Rôles | Description | Body/Query | Réponse |
|----------|---------|------|-------|-------------|------------|---------|
| `/offers` | GET | ✅ Oui | Tous | Lister offres | `?isActive=true&page=1&limit=10` | `{data: [offers], total, page}` |
| `/offers/:id` | GET | ❌ Non | Public | Détails offre | - | `{offer}` |
| `/offers` | POST | ✅ Oui | ACADEMIE, ADMIN | Créer offre | `{name, description, type, durationDays, price}` | `{offer}` |
| `/offers/:id` | PATCH | ✅ Oui | ACADEMIE, ADMIN | Modifier offre | `{price?, isActive?}` | `{offer}` |
| `/offers/:id` | DELETE | ✅ Oui | ACADEMIE, ADMIN | Supprimer offre | - | `204 No Content` |
| `/offers/all-subscribers` | GET | ✅ Oui | ACADEMIE, ADMIN | Tous les inscrits | - | `[{offerId, offerName, subscribers, total}]` |

---

## 📝 ABONNEMENTS

| Endpoint | Méthode | Auth | Rôles | Description | Body/Query | Réponse |
|----------|---------|------|-------|-------------|------------|---------|
| `/subscriptions` | POST | ✅ Oui | PARENT | Créer abonnement | `{childId, offerId, autoRenew}` | `{subscription}` |
| `/subscriptions/my` | GET | ✅ Oui | PARENT | Mes abonnements | - | `[subscriptions]` |
| `/subscriptions` | GET | ✅ Oui | ACADEMIE, ADMIN | Tous abonnements | `?status=ACTIVE&page=1` | `{data: [subs], total}` |
| `/subscriptions/:id` | GET | ✅ Oui | Tous | Détails abonnement | - | `{subscription}` |
| `/subscriptions/:id` | PATCH | ✅ Oui | PARENT, ACADEMIE | Modifier abonnement | `{autoRenew?, startDate?}` | `{subscription}` |
| `/subscriptions/:id/pay` | POST | ✅ Oui | PARENT, ACADEMIE | Enregistrer paiement | `{amount, currency, method}` | `{subscription}` |
| `/subscriptions/:id/cancel` | POST | ✅ Oui | PARENT, ACADEMIE | Annuler abonnement | - | `{subscription}` |
| `/subscriptions/:id/suspend` | POST | ✅ Oui | ACADEMIE, ADMIN | Suspendre abonnement | - | `{subscription}` |
| `/subscriptions/:id/resume` | POST | ✅ Oui | ACADEMIE, ADMIN | Reprendre abonnement | - | `{subscription}` |
| `/subscriptions/:id/renew` | POST | ✅ Oui | PARENT, ACADEMIE | Renouveler abonnement | - | `{subscription}` |
| `/subscriptions/forecast/revenue` | GET | ✅ Oui | ACADEMIE, ADMIN | Prévisions revenus | - | `{currentMonth, forecast, growth}` |

---

## 💳 PAIEMENTS

| Endpoint | Méthode | Auth | Rôles | Description | Body | Réponse |
|----------|---------|------|-------|-------------|------|---------|
| `/payments/create-payment-intent` | POST | ❌ Non | Public | Créer intent Stripe | `{offerId, selectedOptions}` | `{clientSecret, paymentIntentId}` |
| `/payments/complete` | POST | ✅ Oui | PARENT | Compléter paiement | `{paymentIntentId, childId, offerId}` | `{success, subscription}` |

---

## 📊 TYPES DE DONNÉES

### UserRole (Enum)
```typescript
PARENT = 'parent'
COACH = 'coach'
ACADEMIE = 'academie'
ADMIN = 'admin'
```

### OfferType (Enum)
```typescript
MONTHLY = 'MONTHLY'
QUARTERLY = 'QUARTERLY'
YEARLY = 'YEARLY'
CUSTOM = 'CUSTOM'
```

### SubscriptionStatus (Enum)
```typescript
PENDING = 'PENDING'      // En attente de paiement
ACTIVE = 'ACTIVE'        // Actif
EXPIRED = 'EXPIRED'      // Expiré
CANCELLED = 'CANCELLED'  // Annulé
SUSPENDED = 'SUSPENDED'  // Suspendu
```

### PaymentStatus (Enum)
```typescript
PENDING = 'PENDING'      // En attente
PAID = 'PAID'           // Payé
FAILED = 'FAILED'       // Échoué
REFUNDED = 'REFUNDED'   // Remboursé
```

### PaymentMethod (Enum)
```typescript
STRIPE = 'STRIPE'
CASH = 'CASH'
BANK_TRANSFER = 'BANK_TRANSFER'
CHECK = 'CHECK'
```

---

## 🔑 Headers Requis

### Pour les Routes Protégées
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

### Pour les Routes Publiques
```http
Content-Type: application/json
```

---

## 📋 Exemples de Body Complets

### 1. Inscription (Register)
```json
{
  "nom": "Dupont",
  "prenom": "Jean",
  "email": "jean.dupont@example.com",
  "motDePasse": "password123",
  "role": "parent"
}
```

### 2. Connexion (Login)
```json
{
  "email": "jean.dupont@example.com",
  "motDePasse": "password123"
}
```

### 3. Créer un Enfant
```json
{
  "nom": "Dupont",
  "prenom": "Marie",
  "dateNaissance": "2015-05-20",
  "genre": "F",
  "niveau": "Débutant"
}
```

### 4. Créer une Offre
```json
{
  "name": "Abonnement Mensuel",
  "description": "Accès complet pendant 1 mois",
  "type": "MONTHLY",
  "durationDays": 30,
  "price": 50,
  "currency": "EUR",
  "discountPct": 0,
  "conditions": "Non remboursable",
  "isActive": true
}
```

### 5. Créer un Abonnement
```json
{
  "childId": "65b1f0a1b2c3d4e5f6g7h8i3",
  "offerId": "65b1f0a1b2c3d4e5f6g7h8i9",
  "autoRenew": true
}
```

### 6. Créer Payment Intent
```json
{
  "offerId": "65b1f0a1b2c3d4e5f6g7h8i9",
  "selectedOptions": []
}
```

### 7. Compléter Paiement
```json
{
  "paymentIntentId": "pi_1234567890",
  "childId": "65b1f0a1b2c3d4e5f6g7h8i3",
  "offerId": "65b1f0a1b2c3d4e5f6g7h8i9"
}
```

### 8. Enregistrer un Paiement Manuel
```json
{
  "amount": 50,
  "currency": "EUR",
  "method": "CASH",
  "externalRef": "CASH-2024-001"
}
```

---

## 🎯 Query Parameters Communs

### Pagination
```
?page=1&limit=10
```

### Tri
```
?sort=price          // Croissant
?sort=-price         // Décroissant
?sort=-createdAt     // Plus récent d'abord
```

### Filtres Offres
```
?isActive=true
?academyId=65b1f0a1b2c3d4e5f6g7h8i0
```

### Filtres Abonnements
```
?status=ACTIVE
?paymentStatus=PAID
?parentId=65b1f0a1b2c3d4e5f6g7h8i2
?childId=65b1f0a1b2c3d4e5f6g7h8i3
```

---

## ⚠️ Codes d'Erreur HTTP

| Code | Signification | Exemple |
|------|---------------|---------|
| 200 | OK | Requête réussie |
| 201 | Created | Ressource créée |
| 204 | No Content | Suppression réussie |
| 400 | Bad Request | Données invalides |
| 401 | Unauthorized | Token invalide/expiré |
| 403 | Forbidden | Permissions insuffisantes |
| 404 | Not Found | Ressource introuvable |
| 409 | Conflict | Email déjà utilisé |
| 500 | Server Error | Erreur serveur |

---

## 🔄 Format des Réponses d'Erreur

### Erreur Simple
```json
{
  "statusCode": 404,
  "message": "Offre introuvable",
  "error": "Not Found"
}
```

### Erreur de Validation
```json
{
  "statusCode": 400,
  "message": [
    "email must be an email",
    "motDePasse must be longer than or equal to 6 characters"
  ],
  "error": "Bad Request"
}
```

---

## 🚀 Exemples cURL

### Inscription
```bash
curl -X POST http://localhost:3000/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Dupont",
    "prenom": "Jean",
    "email": "jean.dupont@example.com",
    "motDePasse": "password123",
    "role": "parent"
  }'
```

### Connexion
```bash
curl -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jean.dupont@example.com",
    "motDePasse": "password123"
  }'
```

### Lister les Offres (avec token)
```bash
curl -X GET "http://localhost:3000/offers?isActive=true" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json"
```

### Créer une Offre
```bash
curl -X POST http://localhost:3000/offers \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mensuel",
    "description": "Accès 1 mois",
    "type": "MONTHLY",
    "durationDays": 30,
    "price": 50
  }'
```

### Créer un Abonnement
```bash
curl -X POST http://localhost:3000/subscriptions \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "childId": "CHILD_ID_HERE",
    "offerId": "OFFER_ID_HERE",
    "autoRenew": true
  }'
```

---

## 📱 Configuration IP pour Mobile

### Android Emulator
```kotlin
private const val BASE_URL = "http://10.0.2.2:3000/"
```

### iOS Simulator
```swift
private let baseURL = "http://localhost:3000/"
```

### Device Physique (même réseau WiFi)
```
http://192.168.1.X:3000/
```
> Remplacez X par l'IP de votre machine

### Trouver votre IP locale

**Windows:**
```bash
ipconfig
# Cherchez "Adresse IPv4"
```

**macOS/Linux:**
```bash
ifconfig
# ou
ip addr show
```

---

## 🔐 Structure du Token JWT

Le token JWT contient les informations suivantes :

```json
{
  "userId": "65b1f0a1b2c3d4e5f6g7h8i2",
  "email": "jean.dupont@example.com",
  "role": "parent",
  "iat": 1705315200,
  "exp": 1705401600
}
```

**Décodage**: Utilisez [jwt.io](https://jwt.io) pour décoder et vérifier vos tokens.

---

## ✅ Checklist d'Intégration

### Configuration Initiale
- [ ] Installer les dépendances HTTP (Retrofit, Axios, http, etc.)
- [ ] Configurer l'URL de base
- [ ] Configurer l'IP correcte (émulateur vs device)
- [ ] Tester la connexion au serveur

### Authentification
- [ ] Implémenter l'inscription
- [ ] Implémenter la vérification email
- [ ] Implémenter la connexion
- [ ] Sauvegarder le token JWT
- [ ] Ajouter le token aux headers des requêtes
- [ ] Gérer l'expiration du token (401)
- [ ] Implémenter la déconnexion

### Gestion des Données
- [ ] Créer les modèles de données
- [ ] Implémenter les appels API
- [ ] Gérer les états de chargement
- [ ] Gérer les erreurs
- [ ] Implémenter le cache si nécessaire

### Fonctionnalités Métier
- [ ] Lister les offres
- [ ] Créer/Modifier/Supprimer des offres (Académie)
- [ ] Ajouter des enfants (Parent)
- [ ] Créer des abonnements
- [ ] Gérer les paiements Stripe
- [ ] Consulter les abonnements
- [ ] Gérer le renouvellement

### Tests
- [ ] Tester avec Swagger
- [ ] Tester avec Postman
- [ ] Tests unitaires du service API
- [ ] Tests d'intégration

---

## 📞 Support et Débogage

### Logs Backend
Le backend affiche automatiquement les logs dans la console. Vérifiez :
- Les requêtes reçues
- Les erreurs éventuelles
- Les tokens JWT

### Tester avec Swagger
1. Ouvrir `http://localhost:3000/api`
2. Cliquer sur "Authorize"
3. Entrer le token JWT (sans "Bearer")
4. Tester les endpoints

### Erreurs Courantes

**401 Unauthorized**
- Token manquant ou invalide
- Token expiré
- Solution: Se reconnecter

**403 Forbidden**
- Rôle insuffisant
- Tentative d'accès à une ressource d'une autre académie
- Solution: Vérifier les permissions

**404 Not Found**
- ID invalide
- Ressource supprimée
- Solution: Vérifier l'ID

**409 Conflict**
- Email déjà utilisé
- Ressource en conflit
- Solution: Utiliser un autre email

---

## 🎓 Ressources Utiles

- **Documentation Swagger**: `http://localhost:3000/api`
- **Décoder JWT**: [jwt.io](https://jwt.io)
- **Tester API**: [Postman](https://www.postman.com/)
- **Documentation Stripe**: [stripe.com/docs](https://stripe.com/docs)

---

**Dernière mise à jour**: Janvier 2024  
**Version API**: 1.0  
**Contact**: Consultez la documentation Swagger pour plus de détails
