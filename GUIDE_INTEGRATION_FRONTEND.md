# 🚀 Guide d'Intégration Frontend - Backend NestJS

## 📋 Table des Matières
1. [Configuration de Base](#configuration-de-base)
2. [Architecture de l'API](#architecture-de-lapi)
3. [Authentification JWT](#authentification-jwt)
4. [Endpoints Principaux](#endpoints-principaux)
5. [Exemples de Code](#exemples-de-code)
6. [Gestion des Erreurs](#gestion-des-erreurs)
7. [Bonnes Pratiques](#bonnes-pratiques)

---

## 🔧 Configuration de Base

### URL de Base
```
http://localhost:3000
```
> **Note**: Le serveur cherche automatiquement un port libre entre 3000 et 3010 si le port par défaut est occupé.

### Documentation Swagger
```
http://localhost:3000/api
```
> La documentation Swagger est disponible et contient tous les endpoints avec leurs schémas de données.

### CORS
Le backend accepte toutes les origines (`origin: '*'`) en développement. En production, vous devrez configurer l'origine spécifique de votre frontend.

---

## 🏗️ Architecture de l'API

### Structure des Endpoints
Tous les endpoints suivent le pattern REST standard :
```
/auth          - Authentification (register, login, verify-email)
/users         - Gestion des utilisateurs
/offers        - Gestion des offres d'abonnement
/subscriptions - Gestion des abonnements
/payments      - Gestion des paiements (Stripe)
/inscriptions  - Inscriptions aux offres
/equipes       - Gestion des équipes
/matches       - Gestion des matchs
/tournoi       - Gestion des tournois
/messages      - Messagerie
/chatbot       - Assistant IA
/analytics     - Statistiques et analyses
```

### Rôles Utilisateurs
```typescript
enum UserRole {
  PARENT = 'parent',
  COACH = 'coach',
  ACADEMIE = 'academie',
  ADMIN = 'admin'
}
```

---

## 🔐 Authentification JWT

### 1. Inscription (Register)

**Endpoint**: `POST /auth/register`  
**Public**: ✅ Oui (pas besoin de token)

**Body**:
```json
{
  "nom": "Dupont",
  "prenom": "Jean",
  "email": "jean.dupont@example.com",
  "motDePasse": "password123",
  "role": "parent"
}
```

**Réponse Success (201)**:
```json
{
  "message": "Inscription réussie. Un code de vérification a été envoyé à votre adresse email.",
  "userId": "507f1f77bcf86cd799439011",
  "email": "jean.dupont@example.com"
}
```

**Exemple Flutter/Dart**:
```dart
Future<Map<String, dynamic>> register({
  required String nom,
  required String prenom,
  required String email,
  required String password,
  required String role,
}) async {
  final response = await http.post(
    Uri.parse('$baseUrl/auth/register'),
    headers: {'Content-Type': 'application/json'},
    body: jsonEncode({
      'nom': nom,
      'prenom': prenom,
      'email': email,
      'motDePasse': password,
      'role': role,
    }),
  );

  if (response.statusCode == 201) {
    return jsonDecode(response.body);
  } else {
    throw Exception('Erreur lors de l\'inscription');
  }
}
```

### 2. Vérification Email

**Endpoint**: `POST /auth/verify-email`  
**Public**: ✅ Oui

**Body**:
```json
{
  "userId": "507f1f77bcf86cd799439011",
  "code": "123456"
}
```

**Réponse Success (200)**:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "507f1f77bcf86cd799439011",
    "email": "jean.dupont@example.com",
    "nom": "Dupont",
    "prenom": "Jean",
    "role": "parent",
    "photoProfil": null
  }
}
```

### 3. Connexion (Login)

**Endpoint**: `POST /auth/login`  
**Public**: ✅ Oui

**Body**:
```json
{
  "email": "jean.dupont@example.com",
  "motDePasse": "password123"
}
```

**Réponse Success (200)**:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "507f1f77bcf86cd799439011",
    "email": "jean.dupont@example.com",
    "nom": "Dupont",
    "prenom": "Jean",
    "role": "parent",
    "photoProfil": null,
    "emailVerified": true
  }
}
```

**Exemple Flutter/Dart**:
```dart
Future<Map<String, dynamic>> login(String email, String password) async {
  final response = await http.post(
    Uri.parse('$baseUrl/auth/login'),
    headers: {'Content-Type': 'application/json'},
    body: jsonEncode({
      'email': email,
      'motDePasse': password,
    }),
  );

  if (response.statusCode == 200) {
    final data = jsonDecode(response.body);
    // Sauvegarder le token
    await _saveToken(data['access_token']);
    return data;
  } else {
    throw Exception('Email ou mot de passe incorrect');
  }
}

Future<void> _saveToken(String token) async {
  final prefs = await SharedPreferences.getInstance();
  await prefs.setString('access_token', token);
}
```

### 4. Utilisation du Token JWT

**Toutes les routes protégées nécessitent le header suivant**:
```
Authorization: Bearer <votre_token_jwt>
```

**Exemple Flutter/Dart**:
```dart
Future<String?> _getToken() async {
  final prefs = await SharedPreferences.getInstance();
  return prefs.getString('access_token');
}

Future<http.Response> _authenticatedRequest(String endpoint) async {
  final token = await _getToken();
  
  return await http.get(
    Uri.parse('$baseUrl$endpoint'),
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer $token',
    },
  );
}
```

---

## 📌 Endpoints Principaux

### 🎯 OFFERS (Offres d'Abonnement)

#### 1. Lister toutes les offres

**Endpoint**: `GET /offers`  
**Auth**: ✅ Requise  
**Rôles**: Tous

**Query Parameters**:
- `isActive` (boolean, optional): Filtrer par statut actif
- `academyId` (string, optional): Filtrer par académie
- `page` (number, optional): Numéro de page (défaut: 1)
- `limit` (number, optional): Nombre d'éléments par page (défaut: 10)
- `sort` (string, optional): Tri (ex: "price", "-price")

**Exemple**:
```
GET /offers?isActive=true&page=1&limit=10
```

**Réponse Success (200)**:
```json
{
  "data": [
    {
      "_id": "65b1f0a1b2c3d4e5f6g7h8i9",
      "name": "Abonnement Mensuel",
      "description": "Accès complet pendant 1 mois",
      "type": "MONTHLY",
      "durationDays": 30,
      "price": 50,
      "currency": "EUR",
      "discountPct": 0,
      "isActive": true,
      "academyId": "65b1f0a1b2c3d4e5f6g7h8i0",
      "conditions": "Non remboursable",
      "createdAt": "2024-01-15T10:00:00.000Z",
      "updatedAt": "2024-01-15T10:00:00.000Z"
    }
  ],
  "total": 5,
  "page": 1,
  "limit": 10
}
```

**Exemple Flutter/Dart**:
```dart
class Offer {
  final String id;
  final String name;
  final String description;
  final String type;
  final int durationDays;
  final double price;
  final String currency;
  final double discountPct;
  final bool isActive;

  Offer({
    required this.id,
    required this.name,
    required this.description,
    required this.type,
    required this.durationDays,
    required this.price,
    required this.currency,
    required this.discountPct,
    required this.isActive,
  });

  factory Offer.fromJson(Map<String, dynamic> json) {
    return Offer(
      id: json['_id'],
      name: json['name'],
      description: json['description'],
      type: json['type'],
      durationDays: json['durationDays'],
      price: (json['price'] as num).toDouble(),
      currency: json['currency'],
      discountPct: (json['discountPct'] as num).toDouble(),
      isActive: json['isActive'],
    );
  }
}

Future<List<Offer>> getOffers({bool? isActive}) async {
  final token = await _getToken();
  
  String url = '$baseUrl/offers';
  if (isActive != null) {
    url += '?isActive=$isActive';
  }
  
  final response = await http.get(
    Uri.parse(url),
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer $token',
    },
  );

  if (response.statusCode == 200) {
    final data = jsonDecode(response.body);
    return (data['data'] as List)
        .map((json) => Offer.fromJson(json))
        .toList();
  } else {
    throw Exception('Erreur lors du chargement des offres');
  }
}
```

#### 2. Créer une offre (ACADEMIE/ADMIN uniquement)

**Endpoint**: `POST /offers`  
**Auth**: ✅ Requise  
**Rôles**: ACADEMIE, ADMIN

**Body**:
```json
{
  "name": "Abonnement Annuel",
  "description": "Accès complet pendant 1 an",
  "type": "YEARLY",
  "durationDays": 365,
  "price": 500,
  "currency": "EUR",
  "discountPct": 10,
  "conditions": "Engagement annuel",
  "isActive": true
}
```

**Note**: Le champ `academyId` est automatiquement ajouté depuis le token JWT.

**Réponse Success (201)**:
```json
{
  "_id": "65b1f0a1b2c3d4e5f6g7h8i9",
  "name": "Abonnement Annuel",
  "academyId": "65b1f0a1b2c3d4e5f6g7h8i0",
  ...
}
```

#### 3. Modifier une offre

**Endpoint**: `PATCH /offers/:id`  
**Auth**: ✅ Requise  
**Rôles**: ACADEMIE, ADMIN

**Body** (tous les champs sont optionnels):
```json
{
  "price": 45,
  "discountPct": 15,
  "isActive": false
}
```

#### 4. Supprimer une offre

**Endpoint**: `DELETE /offers/:id`  
**Auth**: ✅ Requise  
**Rôles**: ACADEMIE, ADMIN

**Réponse Success (204)**: Pas de contenu

#### 5. Obtenir tous les inscrits par offre

**Endpoint**: `GET /offers/all-subscribers`  
**Auth**: ✅ Requise  
**Rôles**: ACADEMIE, ADMIN

**Réponse Success (200)**:
```json
[
  {
    "offerId": "65b1f0a1b2c3d4e5f6g7h8i9",
    "offerName": "Abonnement Mensuel",
    "subscribers": [
      {
        "subscriptionId": "65b1f0a1b2c3d4e5f6g7h8i1",
        "parentId": "65b1f0a1b2c3d4e5f6g7h8i2",
        "parentName": "Jean Dupont",
        "childId": "65b1f0a1b2c3d4e5f6g7h8i3",
        "childName": "Marie Dupont",
        "status": "ACTIVE",
        "startDate": "2024-01-15T10:00:00.000Z",
        "endDate": "2024-02-15T10:00:00.000Z"
      }
    ],
    "totalSubscribers": 1
  }
]
```

---

### 📝 SUBSCRIPTIONS (Abonnements)

#### 1. Créer un abonnement (PARENT uniquement)

**Endpoint**: `POST /subscriptions`  
**Auth**: ✅ Requise  
**Rôles**: PARENT

**Body**:
```json
{
  "childId": "65b1f0a1b2c3d4e5f6g7h8i3",
  "offerId": "65b1f0a1b2c3d4e5f6g7h8i9",
  "autoRenew": true
}
```

**Réponse Success (201)**:
```json
{
  "_id": "65b1f0a1b2c3d4e5f6g7h8i1",
  "parentId": "65b1f0a1b2c3d4e5f6g7h8i2",
  "childId": "65b1f0a1b2c3d4e5f6g7h8i3",
  "offerId": "65b1f0a1b2c3d4e5f6g7h8i9",
  "status": "PENDING",
  "paymentStatus": "PENDING",
  "startDate": "2024-01-15T10:00:00.000Z",
  "endDate": "2024-02-15T10:00:00.000Z",
  "autoRenew": true,
  "totalAmount": 50,
  "currency": "EUR",
  "createdAt": "2024-01-15T10:00:00.000Z"
}
```

#### 2. Lister mes abonnements (PARENT)

**Endpoint**: `GET /subscriptions/my`  
**Auth**: ✅ Requise  
**Rôles**: PARENT

**Réponse Success (200)**:
```json
[
  {
    "_id": "65b1f0a1b2c3d4e5f6g7h8i1",
    "parentId": "65b1f0a1b2c3d4e5f6g7h8i2",
    "childId": {
      "_id": "65b1f0a1b2c3d4e5f6g7h8i3",
      "nom": "Dupont",
      "prenom": "Marie"
    },
    "offerId": {
      "_id": "65b1f0a1b2c3d4e5f6g7h8i9",
      "name": "Abonnement Mensuel",
      "price": 50
    },
    "status": "ACTIVE",
    "paymentStatus": "PAID",
    "startDate": "2024-01-15T10:00:00.000Z",
    "endDate": "2024-02-15T10:00:00.000Z"
  }
]
```

**Exemple Flutter/Dart**:
```dart
class Subscription {
  final String id;
  final String status;
  final String paymentStatus;
  final DateTime startDate;
  final DateTime endDate;
  final double totalAmount;
  final String currency;
  final bool autoRenew;

  Subscription({
    required this.id,
    required this.status,
    required this.paymentStatus,
    required this.startDate,
    required this.endDate,
    required this.totalAmount,
    required this.currency,
    required this.autoRenew,
  });

  factory Subscription.fromJson(Map<String, dynamic> json) {
    return Subscription(
      id: json['_id'],
      status: json['status'],
      paymentStatus: json['paymentStatus'],
      startDate: DateTime.parse(json['startDate']),
      endDate: DateTime.parse(json['endDate']),
      totalAmount: (json['totalAmount'] as num).toDouble(),
      currency: json['currency'],
      autoRenew: json['autoRenew'],
    );
  }
}

Future<List<Subscription>> getMySubscriptions() async {
  final token = await _getToken();
  
  final response = await http.get(
    Uri.parse('$baseUrl/subscriptions/my'),
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer $token',
    },
  );

  if (response.statusCode == 200) {
    final List<dynamic> data = jsonDecode(response.body);
    return data.map((json) => Subscription.fromJson(json)).toList();
  } else {
    throw Exception('Erreur lors du chargement des abonnements');
  }
}
```

#### 3. Lister tous les abonnements (ACADEMIE/ADMIN)

**Endpoint**: `GET /subscriptions`  
**Auth**: ✅ Requise  
**Rôles**: ACADEMIE, ADMIN

**Query Parameters**:
- `status`: PENDING | ACTIVE | EXPIRED | CANCELLED | SUSPENDED
- `paymentStatus`: PENDING | PAID | FAILED | REFUNDED
- `parentId`: ID du parent
- `childId`: ID de l'enfant
- `page`: Numéro de page
- `limit`: Nombre d'éléments par page
- `sort`: Tri (ex: "-createdAt")

**Exemple**:
```
GET /subscriptions?status=ACTIVE&page=1&limit=20
```

#### 4. Enregistrer un paiement

**Endpoint**: `POST /subscriptions/:id/pay`  
**Auth**: ✅ Requise  
**Rôles**: PARENT, ACADEMIE

**Body**:
```json
{
  "amount": 50,
  "currency": "EUR",
  "method": "STRIPE",
  "externalRef": "pi_1234567890"
}
```

**Méthodes de paiement disponibles**:
- `STRIPE`
- `CASH`
- `BANK_TRANSFER`
- `CHECK`

#### 5. Annuler un abonnement

**Endpoint**: `POST /subscriptions/:id/cancel`  
**Auth**: ✅ Requise  
**Rôles**: PARENT, ACADEMIE, ADMIN

#### 6. Renouveler un abonnement

**Endpoint**: `POST /subscriptions/:id/renew`  
**Auth**: ✅ Requise  
**Rôles**: PARENT, ACADEMIE, ADMIN

#### 7. Prévisions de revenus (ACADEMIE/ADMIN)

**Endpoint**: `GET /subscriptions/forecast/revenue`  
**Auth**: ✅ Requise  
**Rôles**: ACADEMIE, ADMIN

**Réponse Success (200)**:
```json
{
  "currentMonthRevenue": 5000,
  "nextMonthForecast": 5500,
  "growthRate": 10,
  "activeSubscriptions": 100,
  "forecast": [
    {
      "month": "2024-02",
      "predictedRevenue": 5500,
      "confidence": 0.85
    }
  ]
}
```

---

### 💳 PAYMENTS (Paiements Stripe)

#### 1. Créer un Payment Intent

**Endpoint**: `POST /payments/create-payment-intent`  
**Auth**: ❌ Non requise (Public)

**Body**:
```json
{
  "offerId": "65b1f0a1b2c3d4e5f6g7h8i9",
  "selectedOptions": ["option1", "option2"]
}
```

**Réponse Success (200)**:
```json
{
  "clientSecret": "pi_1234567890_secret_abcdefghijklmnop",
  "paymentIntentId": "pi_1234567890",
  "amount": 5000,
  "currency": "eur"
}
```

**Exemple Flutter/Dart avec Stripe**:
```dart
import 'package:flutter_stripe/flutter_stripe.dart';

Future<void> processPayment(String offerId) async {
  try {
    // 1. Créer le Payment Intent
    final response = await http.post(
      Uri.parse('$baseUrl/payments/create-payment-intent'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'offerId': offerId,
        'selectedOptions': [],
      }),
    );

    if (response.statusCode != 200) {
      throw Exception('Erreur lors de la création du paiement');
    }

    final paymentData = jsonDecode(response.body);
    final clientSecret = paymentData['clientSecret'];

    // 2. Initialiser le Payment Sheet
    await Stripe.instance.initPaymentSheet(
      paymentSheetParameters: SetupPaymentSheetParameters(
        paymentIntentClientSecret: clientSecret,
        merchantDisplayName: 'SportyConnect Kids',
        style: ThemeMode.light,
      ),
    );

    // 3. Afficher le Payment Sheet
    await Stripe.instance.presentPaymentSheet();

    // 4. Compléter le paiement côté backend
    await completePayment(
      paymentIntentId: paymentData['paymentIntentId'],
      childId: 'child_id_here',
      offerId: offerId,
    );

    print('Paiement réussi !');
  } catch (e) {
    print('Erreur de paiement: $e');
    rethrow;
  }
}
```

#### 2. Compléter un paiement

**Endpoint**: `POST /payments/complete`  
**Auth**: ✅ Requise  
**Rôles**: PARENT

**Body**:
```json
{
  "paymentIntentId": "pi_1234567890",
  "childId": "65b1f0a1b2c3d4e5f6g7h8i3",
  "offerId": "65b1f0a1b2c3d4e5f6g7h8i9"
}
```

**Réponse Success (200)**:
```json
{
  "success": true,
  "subscription": {
    "_id": "65b1f0a1b2c3d4e5f6g7h8i1",
    "status": "ACTIVE",
    "paymentStatus": "PAID"
  },
  "message": "Paiement confirmé et abonnement activé"
}
```

---

### 👥 USERS (Utilisateurs)

#### 1. Obtenir mon profil

**Endpoint**: `GET /users/me`  
**Auth**: ✅ Requise  
**Rôles**: Tous

**Réponse Success (200)**:
```json
{
  "_id": "65b1f0a1b2c3d4e5f6g7h8i2",
  "email": "jean.dupont@example.com",
  "nom": "Dupont",
  "prenom": "Jean",
  "role": "parent",
  "telephone": "+33612345678",
  "photoProfil": "https://example.com/photo.jpg",
  "emailVerified": true,
  "createdAt": "2024-01-15T10:00:00.000Z"
}
```

#### 2. Mettre à jour mon profil

**Endpoint**: `PATCH /users/me`  
**Auth**: ✅ Requise  
**Rôles**: Tous

**Body** (tous les champs sont optionnels):
```json
{
  "nom": "Nouveau Nom",
  "prenom": "Nouveau Prénom",
  "telephone": "+33612345678"
}
```

#### 3. Ajouter un enfant (PARENT uniquement)

**Endpoint**: `POST /users/children`  
**Auth**: ✅ Requise  
**Rôles**: PARENT

**Body**:
```json
{
  "nom": "Dupont",
  "prenom": "Marie",
  "dateNaissance": "2015-05-20",
  "genre": "F",
  "niveau": "Débutant"
}
```

#### 4. Lister mes enfants (PARENT)

**Endpoint**: `GET /users/children`  
**Auth**: ✅ Requise  
**Rôles**: PARENT

**Réponse Success (200)**:
```json
[
  {
    "_id": "65b1f0a1b2c3d4e5f6g7h8i3",
    "nom": "Dupont",
    "prenom": "Marie",
    "dateNaissance": "2015-05-20T00:00:00.000Z",
    "genre": "F",
    "niveau": "Débutant",
    "parentId": "65b1f0a1b2c3d4e5f6g7h8i2"
  }
]
```

---

## 🎨 Exemples de Code Complets

### Service API Flutter/Dart Complet

```dart
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

class ApiService {
  static const String baseUrl = 'http://localhost:3000';
  
  // ==================== AUTH ====================
  
  Future<Map<String, dynamic>> register({
    required String nom,
    required String prenom,
    required String email,
    required String password,
    required String role,
  }) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/register'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'nom': nom,
        'prenom': prenom,
        'email': email,
        'motDePasse': password,
        'role': role,
      }),
    );

    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> verifyEmail({
    required String userId,
    required String code,
  }) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/verify-email'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'userId': userId,
        'code': code,
      }),
    );

    final data = _handleResponse(response);
    
    // Sauvegarder le token
    if (data.containsKey('access_token')) {
      await _saveToken(data['access_token']);
    }
    
    return data;
  }

  Future<Map<String, dynamic>> login(String email, String password) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/login'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'email': email,
        'motDePasse': password,
      }),
    );

    final data = _handleResponse(response);
    
    // Sauvegarder le token
    if (data.containsKey('access_token')) {
      await _saveToken(data['access_token']);
    }
    
    return data;
  }

  Future<void> logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('access_token');
  }

  // ==================== OFFERS ====================
  
  Future<List<Offer>> getOffers({bool? isActive, String? academyId}) async {
    final queryParams = <String, String>{};
    if (isActive != null) queryParams['isActive'] = isActive.toString();
    if (academyId != null) queryParams['academyId'] = academyId;

    final uri = Uri.parse('$baseUrl/offers').replace(queryParameters: queryParams);
    final response = await _authenticatedGet(uri);
    
    final data = _handleResponse(response);
    return (data['data'] as List)
        .map((json) => Offer.fromJson(json))
        .toList();
  }

  Future<Offer> createOffer(Map<String, dynamic> offerData) async {
    final response = await _authenticatedPost(
      Uri.parse('$baseUrl/offers'),
      offerData,
    );
    
    return Offer.fromJson(_handleResponse(response));
  }

  Future<Offer> updateOffer(String offerId, Map<String, dynamic> updates) async {
    final response = await _authenticatedPatch(
      Uri.parse('$baseUrl/offers/$offerId'),
      updates,
    );
    
    return Offer.fromJson(_handleResponse(response));
  }

  Future<void> deleteOffer(String offerId) async {
    final response = await _authenticatedDelete(
      Uri.parse('$baseUrl/offers/$offerId'),
    );
    
    if (response.statusCode != 204) {
      throw Exception('Erreur lors de la suppression de l\'offre');
    }
  }

  // ==================== SUBSCRIPTIONS ====================
  
  Future<Subscription> createSubscription({
    required String childId,
    required String offerId,
    bool autoRenew = true,
  }) async {
    final response = await _authenticatedPost(
      Uri.parse('$baseUrl/subscriptions'),
      {
        'childId': childId,
        'offerId': offerId,
        'autoRenew': autoRenew,
      },
    );
    
    return Subscription.fromJson(_handleResponse(response));
  }

  Future<List<Subscription>> getMySubscriptions() async {
    final response = await _authenticatedGet(
      Uri.parse('$baseUrl/subscriptions/my'),
    );
    
    final List<dynamic> data = _handleResponse(response);
    return data.map((json) => Subscription.fromJson(json)).toList();
  }

  Future<List<Subscription>> getAllSubscriptions({
    String? status,
    String? paymentStatus,
    int page = 1,
    int limit = 10,
  }) async {
    final queryParams = <String, String>{
      'page': page.toString(),
      'limit': limit.toString(),
    };
    if (status != null) queryParams['status'] = status;
    if (paymentStatus != null) queryParams['paymentStatus'] = paymentStatus;

    final uri = Uri.parse('$baseUrl/subscriptions').replace(queryParameters: queryParams);
    final response = await _authenticatedGet(uri);
    
    final data = _handleResponse(response);
    return (data['data'] as List)
        .map((json) => Subscription.fromJson(json))
        .toList();
  }

  Future<void> cancelSubscription(String subscriptionId) async {
    await _authenticatedPost(
      Uri.parse('$baseUrl/subscriptions/$subscriptionId/cancel'),
      {},
    );
  }

  Future<void> renewSubscription(String subscriptionId) async {
    await _authenticatedPost(
      Uri.parse('$baseUrl/subscriptions/$subscriptionId/renew'),
      {},
    );
  }

  // ==================== PAYMENTS ====================
  
  Future<Map<String, dynamic>> createPaymentIntent({
    required String offerId,
    List<String> selectedOptions = const [],
  }) async {
    final response = await http.post(
      Uri.parse('$baseUrl/payments/create-payment-intent'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'offerId': offerId,
        'selectedOptions': selectedOptions,
      }),
    );

    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> completePayment({
    required String paymentIntentId,
    required String childId,
    required String offerId,
  }) async {
    final response = await _authenticatedPost(
      Uri.parse('$baseUrl/payments/complete'),
      {
        'paymentIntentId': paymentIntentId,
        'childId': childId,
        'offerId': offerId,
      },
    );

    return _handleResponse(response);
  }

  // ==================== USERS ====================
  
  Future<Map<String, dynamic>> getMyProfile() async {
    final response = await _authenticatedGet(
      Uri.parse('$baseUrl/users/me'),
    );
    
    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> updateMyProfile(Map<String, dynamic> updates) async {
    final response = await _authenticatedPatch(
      Uri.parse('$baseUrl/users/me'),
      updates,
    );
    
    return _handleResponse(response);
  }

  Future<List<Map<String, dynamic>>> getMyChildren() async {
    final response = await _authenticatedGet(
      Uri.parse('$baseUrl/users/children'),
    );
    
    return List<Map<String, dynamic>>.from(_handleResponse(response));
  }

  Future<Map<String, dynamic>> addChild(Map<String, dynamic> childData) async {
    final response = await _authenticatedPost(
      Uri.parse('$baseUrl/users/children'),
      childData,
    );
    
    return _handleResponse(response);
  }

  // ==================== HELPERS ====================
  
  Future<String?> _getToken() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('access_token');
  }

  Future<void> _saveToken(String token) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('access_token', token);
  }

  Future<http.Response> _authenticatedGet(Uri uri) async {
    final token = await _getToken();
    if (token == null) throw Exception('Non authentifié');

    return await http.get(
      uri,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
    );
  }

  Future<http.Response> _authenticatedPost(Uri uri, Map<String, dynamic> body) async {
    final token = await _getToken();
    if (token == null) throw Exception('Non authentifié');

    return await http.post(
      uri,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
      body: jsonEncode(body),
    );
  }

  Future<http.Response> _authenticatedPatch(Uri uri, Map<String, dynamic> body) async {
    final token = await _getToken();
    if (token == null) throw Exception('Non authentifié');

    return await http.patch(
      uri,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
      body: jsonEncode(body),
    );
  }

  Future<http.Response> _authenticatedDelete(Uri uri) async {
    final token = await _getToken();
    if (token == null) throw Exception('Non authentifié');

    return await http.delete(
      uri,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
    );
  }

  dynamic _handleResponse(http.Response response) {
    if (response.statusCode >= 200 && response.statusCode < 300) {
      if (response.body.isEmpty) return {};
      return jsonDecode(response.body);
    } else {
      final error = jsonDecode(response.body);
      throw ApiException(
        statusCode: response.statusCode,
        message: error['message'] ?? 'Une erreur est survenue',
      );
    }
  }
}

class ApiException implements Exception {
  final int statusCode;
  final String message;

  ApiException({required this.statusCode, required this.message});

  @override
  String toString() => 'ApiException($statusCode): $message';
}
```

### Modèles de Données

```dart
class Offer {
  final String id;
  final String name;
  final String description;
  final String type;
  final int durationDays;
  final double price;
  final String currency;
  final double discountPct;
  final bool isActive;
  final String? academyId;
  final String? conditions;

  Offer({
    required this.id,
    required this.name,
    required this.description,
    required this.type,
    required this.durationDays,
    required this.price,
    required this.currency,
    required this.discountPct,
    required this.isActive,
    this.academyId,
    this.conditions,
  });

  factory Offer.fromJson(Map<String, dynamic> json) {
    return Offer(
      id: json['_id'],
      name: json['name'],
      description: json['description'],
      type: json['type'],
      durationDays: json['durationDays'],
      price: (json['price'] as num).toDouble(),
      currency: json['currency'],
      discountPct: (json['discountPct'] as num).toDouble(),
      isActive: json['isActive'],
      academyId: json['academyId'],
      conditions: json['conditions'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'description': description,
      'type': type,
      'durationDays': durationDays,
      'price': price,
      'currency': currency,
      'discountPct': discountPct,
      'isActive': isActive,
      'conditions': conditions,
    };
  }
}

class Subscription {
  final String id;
  final String parentId;
  final String childId;
  final String offerId;
  final String status;
  final String paymentStatus;
  final DateTime startDate;
  final DateTime endDate;
  final double totalAmount;
  final String currency;
  final bool autoRenew;

  Subscription({
    required this.id,
    required this.parentId,
    required this.childId,
    required this.offerId,
    required this.status,
    required this.paymentStatus,
    required this.startDate,
    required this.endDate,
    required this.totalAmount,
    required this.currency,
    required this.autoRenew,
  });

  factory Subscription.fromJson(Map<String, dynamic> json) {
    return Subscription(
      id: json['_id'],
      parentId: json['parentId'] is String ? json['parentId'] : json['parentId']['_id'],
      childId: json['childId'] is String ? json['childId'] : json['childId']['_id'],
      offerId: json['offerId'] is String ? json['offerId'] : json['offerId']['_id'],
      status: json['status'],
      paymentStatus: json['paymentStatus'],
      startDate: DateTime.parse(json['startDate']),
      endDate: DateTime.parse(json['endDate']),
      totalAmount: (json['totalAmount'] as num).toDouble(),
      currency: json['currency'],
      autoRenew: json['autoRenew'],
    );
  }
}
```

---

## ⚠️ Gestion des Erreurs

### Codes d'Erreur HTTP

| Code | Signification | Action Frontend |
|------|---------------|-----------------|
| 200 | OK | Succès |
| 201 | Created | Ressource créée avec succès |
| 204 | No Content | Suppression réussie |
| 400 | Bad Request | Données invalides - afficher les erreurs de validation |
| 401 | Unauthorized | Token invalide ou expiré - rediriger vers login |
| 403 | Forbidden | Permissions insuffisantes - afficher message d'erreur |
| 404 | Not Found | Ressource introuvable |
| 409 | Conflict | Conflit (ex: email déjà utilisé) |
| 500 | Internal Server Error | Erreur serveur - réessayer plus tard |

### Format des Erreurs

```json
{
  "statusCode": 400,
  "message": "Validation failed",
  "error": "Bad Request"
}
```

Ou pour les erreurs de validation détaillées :

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

### Gestion des Erreurs Flutter

```dart
try {
  final offers = await apiService.getOffers(isActive: true);
  // Traiter les offres
} on ApiException catch (e) {
  if (e.statusCode == 401) {
    // Token expiré - rediriger vers login
    Navigator.pushReplacementNamed(context, '/login');
  } else if (e.statusCode == 403) {
    // Permissions insuffisantes
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Vous n\'avez pas les permissions nécessaires')),
    );
  } else {
    // Autre erreur
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(e.message)),
    );
  }
} catch (e) {
  // Erreur réseau ou autre
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(content: Text('Erreur de connexion au serveur')),
  );
}
```

---

## ✅ Bonnes Pratiques

### 1. Gestion du Token JWT

- **Stockage sécurisé**: Utilisez `SharedPreferences` (ou `flutter_secure_storage` pour plus de sécurité)
- **Expiration**: Le token expire après un certain temps. Gérez le cas 401 pour redemander une connexion
- **Refresh**: Implémentez un mécanisme de refresh token si nécessaire

### 2. Gestion du State

Utilisez un state management (Provider, Riverpod, Bloc, etc.) pour :
- Stocker l'utilisateur connecté
- Gérer le token JWT
- Cacher les données fréquemment utilisées

```dart
class AuthProvider extends ChangeNotifier {
  String? _token;
  Map<String, dynamic>? _user;

  bool get isAuthenticated => _token != null;
  Map<String, dynamic>? get user => _user;

  Future<void> login(String email, String password) async {
    final data = await apiService.login(email, password);
    _token = data['access_token'];
    _user = data['user'];
    notifyListeners();
  }

  Future<void> logout() async {
    await apiService.logout();
    _token = null;
    _user = null;
    notifyListeners();
  }
}
```

### 3. Optimisation des Requêtes

- **Pagination**: Utilisez toujours la pagination pour les listes
- **Cache**: Mettez en cache les données qui changent rarement (ex: offres)
- **Loading States**: Affichez des indicateurs de chargement
- **Error Handling**: Gérez toujours les erreurs avec des messages clairs

### 4. Sécurité

- **HTTPS**: En production, utilisez TOUJOURS HTTPS
- **Validation**: Validez les données côté frontend ET backend
- **Tokens**: Ne stockez jamais le token en clair dans le code
- **Permissions**: Vérifiez les rôles avant d'afficher certaines fonctionnalités

### 5. Tests

Pour tester l'API, utilisez :
- **Swagger UI**: `http://localhost:3000/api`
- **Postman**: Importez la collection depuis Swagger
- **Tests unitaires**: Testez votre service API

---

## 🔍 Débogage

### Activer les Logs Backend

Le backend log automatiquement les requêtes. Vérifiez la console du serveur.

### Tester avec Swagger

1. Ouvrez `http://localhost:3000/api`
2. Cliquez sur "Authorize" en haut à droite
3. Entrez votre token JWT (sans le préfixe "Bearer")
4. Testez les endpoints directement

### Vérifier le Token JWT

Décodez votre token sur [jwt.io](https://jwt.io) pour voir son contenu :

```json
{
  "userId": "65b1f0a1b2c3d4e5f6g7h8i2",
  "email": "jean.dupont@example.com",
  "role": "parent",
  "iat": 1705315200,
  "exp": 1705401600
}
```

---

## 📞 Support

Pour toute question ou problème :
1. Vérifiez la documentation Swagger
2. Consultez les logs du backend
3. Vérifiez que le token JWT est valide
4. Assurez-vous que l'utilisateur a les bonnes permissions (rôle)

---

**Dernière mise à jour**: Janvier 2024  
**Version API**: 1.0
