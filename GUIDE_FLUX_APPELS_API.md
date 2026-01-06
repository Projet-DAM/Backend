# 🔄 Flux d'Appels API - Diagrammes et Scénarios

## 📋 Table des Matières
1. [Flux d'Authentification](#flux-dauthentification)
2. [Flux de Création d'Abonnement](#flux-de-création-dabonnement)
3. [Flux de Paiement Stripe](#flux-de-paiement-stripe)
4. [Flux de Gestion des Offres](#flux-de-gestion-des-offres)
5. [Scénarios Complets](#scénarios-complets)

---

## 🔐 Flux d'Authentification

### Scénario 1: Inscription et Vérification Email

```
┌─────────┐          ┌──────────┐          ┌─────────┐
│ Frontend│          │  Backend │          │   DB    │
└────┬────┘          └────┬─────┘          └────┬────┘
     │                    │                     │
     │ POST /auth/register│                     │
     │ {nom, prenom,      │                     │
     │  email, password}  │                     │
     ├───────────────────>│                     │
     │                    │                     │
     │                    │ 1. Vérifier si      │
     │                    │    email existe     │
     │                    ├────────────────────>│
     │                    │<────────────────────┤
     │                    │                     │
     │                    │ 2. Hash password    │
     │                    │                     │
     │                    │ 3. Générer code     │
     │                    │    vérification     │
     │                    │                     │
     │                    │ 4. Créer user       │
     │                    ├────────────────────>│
     │                    │<────────────────────┤
     │                    │                     │
     │                    │ 5. Envoyer email    │
     │                    │    avec code        │
     │                    │                     │
     │ 201 Created        │                     │
     │ {userId, email}    │                     │
     │<───────────────────┤                     │
     │                    │                     │
     │ Afficher écran     │                     │
     │ de vérification    │                     │
     │                    │                     │
     │ POST /auth/verify  │                     │
     │ {userId, code}     │                     │
     ├───────────────────>│                     │
     │                    │                     │
     │                    │ 1. Vérifier code    │
     │                    ├────────────────────>│
     │                    │<────────────────────┤
     │                    │                     │
     │                    │ 2. Marquer email    │
     │                    │    comme vérifié    │
     │                    ├────────────────────>│
     │                    │<────────────────────┤
     │                    │                     │
     │                    │ 3. Générer JWT      │
     │                    │                     │
     │ 200 OK             │                     │
     │ {access_token,     │                     │
     │  user}             │                     │
     │<───────────────────┤                     │
     │                    │                     │
     │ Sauvegarder token  │                     │
     │ Rediriger vers app │                     │
     │                    │                     │
```

### Scénario 2: Connexion Simple

```
┌─────────┐          ┌──────────┐          ┌─────────┐
│ Frontend│          │  Backend │          │   DB    │
└────┬────┘          └────┬─────┘          └────┬────┘
     │                    │                     │
     │ POST /auth/login   │                     │
     │ {email, password}  │                     │
     ├───────────────────>│                     │
     │                    │                     │
     │                    │ 1. Trouver user     │
     │                    │    par email        │
     │                    ├────────────────────>│
     │                    │<────────────────────┤
     │                    │                     │
     │                    │ 2. Comparer hash    │
     │                    │    password         │
     │                    │                     │
     │                    │ 3. Vérifier email   │
     │                    │    vérifié          │
     │                    │                     │
     │                    │ 4. Générer JWT      │
     │                    │                     │
     │ 200 OK             │                     │
     │ {access_token,     │                     │
     │  user}             │                     │
     │<───────────────────┤                     │
     │                    │                     │
     │ Sauvegarder token  │                     │
     │ Rediriger vers app │                     │
     │                    │                     │
```

---

## 📝 Flux de Création d'Abonnement

### Scénario Complet: Parent Crée un Abonnement pour son Enfant

```
┌─────────┐          ┌──────────┐          ┌─────────┐
│ Frontend│          │  Backend │          │   DB    │
└────┬────┘          └────┬─────┘          └────┬────┘
     │                    │                     │
     │ 1. Charger les offres disponibles       │
     │                    │                     │
     │ GET /offers        │                     │
     │ ?isActive=true     │                     │
     │ Header: Bearer JWT │                     │
     ├───────────────────>│                     │
     │                    │                     │
     │                    │ 1. Vérifier JWT     │
     │                    │                     │
     │                    │ 2. Récupérer offres │
     │                    ├────────────────────>│
     │                    │<────────────────────┤
     │                    │                     │
     │ 200 OK             │                     │
     │ {data: [offers]}   │                     │
     │<───────────────────┤                     │
     │                    │                     │
     │ Afficher liste     │                     │
     │ des offres         │                     │
     │                    │                     │
     │ 2. Charger les enfants du parent        │
     │                    │                     │
     │ GET /users/children│                     │
     │ Header: Bearer JWT │                     │
     ├───────────────────>│                     │
     │                    │                     │
     │                    │ 1. Vérifier JWT     │
     │                    │ 2. Extraire userId  │
     │                    │                     │
     │                    │ 3. Récupérer enfants│
     │                    ├────────────────────>│
     │                    │<────────────────────┤
     │                    │                     │
     │ 200 OK             │                     │
     │ [children]         │                     │
     │<───────────────────┤                     │
     │                    │                     │
     │ User sélectionne   │                     │
     │ enfant + offre     │                     │
     │                    │                     │
     │ 3. Créer l'abonnement                   │
     │                    │                     │
     │ POST /subscriptions│                     │
     │ {childId, offerId, │                     │
     │  autoRenew: true}  │                     │
     │ Header: Bearer JWT │                     │
     ├───────────────────>│                     │
     │                    │                     │
     │                    │ 1. Vérifier JWT     │
     │                    │ 2. Vérifier que     │
     │                    │    l'enfant         │
     │                    │    appartient au    │
     │                    │    parent           │
     │                    ├────────────────────>│
     │                    │<────────────────────┤
     │                    │                     │
     │                    │ 3. Récupérer offre  │
     │                    ├────────────────────>│
     │                    │<────────────────────┤
     │                    │                     │
     │                    │ 4. Calculer dates   │
     │                    │    (start, end)     │
     │                    │                     │
     │                    │ 5. Créer            │
     │                    │    subscription     │
     │                    │    status: PENDING  │
     │                    ├────────────────────>│
     │                    │<────────────────────┤
     │                    │                     │
     │ 201 Created        │                     │
     │ {subscription}     │                     │
     │<───────────────────┤                     │
     │                    │                     │
     │ Rediriger vers     │                     │
     │ paiement           │                     │
     │                    │                     │
```

---

## 💳 Flux de Paiement Stripe

### Scénario Complet: Paiement d'un Abonnement

```
┌─────────┐    ┌──────────┐    ┌─────────┐    ┌─────────┐
│ Frontend│    │  Backend │    │   DB    │    │  Stripe │
└────┬────┘    └────┬─────┘    └────┬────┘    └────┬────┘
     │              │               │              │
     │ 1. Créer Payment Intent                    │
     │              │               │              │
     │ POST /payments/│              │              │
     │ create-payment-│              │              │
     │ intent         │              │              │
     │ {offerId}      │              │              │
     ├──────────────>│              │              │
     │              │               │              │
     │              │ 1. Récupérer  │              │
     │              │    offre      │              │
     │              ├──────────────>│              │
     │              │<──────────────┤              │
     │              │               │              │
     │              │ 2. Créer Payment Intent      │
     │              ├─────────────────────────────>│
     │              │<─────────────────────────────┤
     │              │               │              │
     │ 200 OK       │               │              │
     │ {clientSecret,│              │              │
     │  paymentIntentId}            │              │
     │<──────────────┤              │              │
     │              │               │              │
     │ 2. Afficher Stripe Payment Sheet            │
     │              │               │              │
     │ User entre   │               │              │
     │ carte bancaire│              │              │
     │              │               │              │
     │ Stripe.confirmPayment()      │              │
     ├─────────────────────────────────────────────>│
     │              │               │              │
     │              │               │ Traiter      │
     │              │               │ paiement     │
     │              │               │              │
     │<─────────────────────────────────────────────┤
     │ Payment Success│              │              │
     │              │               │              │
     │ 3. Compléter le paiement côté backend       │
     │              │               │              │
     │ POST /payments/│              │              │
     │ complete       │              │              │
     │ {paymentIntentId,│            │              │
     │  childId,      │              │              │
     │  offerId}      │              │              │
     │ Header: Bearer JWT            │              │
     ├──────────────>│              │              │
     │              │               │              │
     │              │ 1. Vérifier   │              │
     │              │    payment     │              │
     │              │    status      │              │
     │              ├─────────────────────────────>│
     │              │<─────────────────────────────┤
     │              │               │              │
     │              │ 2. Créer/Update│             │
     │              │    subscription│             │
     │              │    status: ACTIVE            │
     │              ├──────────────>│              │
     │              │<──────────────┤              │
     │              │               │              │
     │              │ 3. Enregistrer│              │
     │              │    paiement   │              │
     │              ├──────────────>│              │
     │              │<──────────────┤              │
     │              │               │              │
     │              │ 4. Envoyer email│            │
     │              │    confirmation│             │
     │              │               │              │
     │ 200 OK       │               │              │
     │ {success: true,│              │              │
     │  subscription}│              │              │
     │<──────────────┤              │              │
     │              │               │              │
     │ Afficher     │               │              │
     │ confirmation │               │              │
     │              │               │              │
```

---

## 🎯 Flux de Gestion des Offres (Académie)

### Scénario: Académie Crée et Gère ses Offres

```
┌─────────┐          ┌──────────┐          ┌─────────┐
│ Frontend│          │  Backend │          │   DB    │
│(Académie)│         │          │          │         │
└────┬────┘          └────┬─────┘          └────┬────┘
     │                    │                     │
     │ 1. Créer une nouvelle offre             │
     │                    │                     │
     │ POST /offers       │                     │
     │ {name, description,│                     │
     │  type, price, ...} │                     │
     │ Header: Bearer JWT │                     │
     ├───────────────────>│                     │
     │                    │                     │
     │                    │ 1. Vérifier JWT     │
     │                    │ 2. Vérifier role    │
     │                    │    = ACADEMIE       │
     │                    │                     │
     │                    │ 3. Extraire         │
     │                    │    academyId du JWT │
     │                    │                     │
     │                    │ 4. Créer offre avec │
     │                    │    academyId auto   │
     │                    ├────────────────────>│
     │                    │<────────────────────┤
     │                    │                     │
     │ 201 Created        │                     │
     │ {offer}            │                     │
     │<───────────────────┤                     │
     │                    │                     │
     │ 2. Lister mes offres                    │
     │                    │                     │
     │ GET /offers        │                     │
     │ Header: Bearer JWT │                     │
     ├───────────────────>│                     │
     │                    │                     │
     │                    │ 1. Vérifier JWT     │
     │                    │ 2. Si role =        │
     │                    │    ACADEMIE,        │
     │                    │    filtrer par      │
     │                    │    academyId auto   │
     │                    ├────────────────────>│
     │                    │<────────────────────┤
     │                    │                     │
     │ 200 OK             │                     │
     │ {data: [offers]}   │                     │
     │<───────────────────┤                     │
     │                    │                     │
     │ 3. Modifier une offre                   │
     │                    │                     │
     │ PATCH /offers/:id  │                     │
     │ {price: 45,        │                     │
     │  isActive: false}  │                     │
     │ Header: Bearer JWT │                     │
     ├───────────────────>│                     │
     │                    │                     │
     │                    │ 1. Vérifier JWT     │
     │                    │ 2. Vérifier que     │
     │                    │    l'offre          │
     │                    │    appartient à     │
     │                    │    l'académie       │
     │                    ├────────────────────>│
     │                    │<────────────────────┤
     │                    │                     │
     │                    │ 3. Mettre à jour    │
     │                    ├────────────────────>│
     │                    │<────────────────────┤
     │                    │                     │
     │ 200 OK             │                     │
     │ {offer}            │                     │
     │<───────────────────┤                     │
     │                    │                     │
     │ 4. Voir tous les inscrits par offre     │
     │                    │                     │
     │ GET /offers/       │                     │
     │ all-subscribers    │                     │
     │ Header: Bearer JWT │                     │
     ├───────────────────>│                     │
     │                    │                     │
     │                    │ 1. Vérifier JWT     │
     │                    │ 2. Récupérer toutes │
     │                    │    les subscriptions│
     │                    │    de l'académie    │
     │                    ├────────────────────>│
     │                    │<────────────────────┤
     │                    │                     │
     │                    │ 3. Grouper par offre│
     │                    │    avec détails     │
     │                    │    subscribers      │
     │                    │                     │
     │ 200 OK             │                     │
     │ [{offerId,         │                     │
     │   offerName,       │                     │
     │   subscribers: [], │                     │
     │   total}]          │                     │
     │<───────────────────┤                     │
     │                    │                     │
```

---

## 📊 Scénarios Complets

### Scénario A: Parcours Parent Complet

**Objectif**: Un parent s'inscrit, ajoute son enfant, et souscrit à une offre

```
1. INSCRIPTION
   POST /auth/register
   → Reçoit userId
   
2. VÉRIFICATION EMAIL
   POST /auth/verify-email
   → Reçoit access_token
   → Sauvegarde token
   
3. AJOUTER UN ENFANT
   POST /users/children
   Headers: Authorization: Bearer {token}
   → Reçoit child avec childId
   
4. CONSULTER LES OFFRES
   GET /offers?isActive=true
   Headers: Authorization: Bearer {token}
   → Reçoit liste des offres
   
5. CRÉER UN ABONNEMENT
   POST /subscriptions
   Body: {childId, offerId, autoRenew: true}
   Headers: Authorization: Bearer {token}
   → Reçoit subscription (status: PENDING)
   
6. CRÉER PAYMENT INTENT
   POST /payments/create-payment-intent
   Body: {offerId}
   → Reçoit clientSecret
   
7. CONFIRMER PAIEMENT (Stripe SDK)
   Stripe.confirmPayment(clientSecret)
   → Paiement réussi
   
8. COMPLÉTER PAIEMENT
   POST /payments/complete
   Body: {paymentIntentId, childId, offerId}
   Headers: Authorization: Bearer {token}
   → Subscription activée (status: ACTIVE)
   
9. CONSULTER MES ABONNEMENTS
   GET /subscriptions/my
   Headers: Authorization: Bearer {token}
   → Voit son abonnement actif
```

### Scénario B: Parcours Académie Complet

**Objectif**: Une académie se connecte, crée des offres, et consulte ses inscrits

```
1. CONNEXION
   POST /auth/login
   Body: {email, motDePasse}
   → Reçoit access_token (role: academie)
   → Sauvegarde token
   
2. CRÉER UNE OFFRE MENSUELLE
   POST /offers
   Body: {
     name: "Mensuel",
     description: "Accès 1 mois",
     type: "MONTHLY",
     durationDays: 30,
     price: 50
   }
   Headers: Authorization: Bearer {token}
   → Offre créée avec academyId automatique
   
3. CRÉER UNE OFFRE ANNUELLE
   POST /offers
   Body: {
     name: "Annuel",
     description: "Accès 1 an",
     type: "YEARLY",
     durationDays: 365,
     price: 500,
     discountPct: 10
   }
   Headers: Authorization: Bearer {token}
   → Offre créée
   
4. CONSULTER MES OFFRES
   GET /offers
   Headers: Authorization: Bearer {token}
   → Reçoit uniquement ses offres (filtré auto par academyId)
   
5. CONSULTER TOUS LES INSCRITS
   GET /offers/all-subscribers
   Headers: Authorization: Bearer {token}
   → Reçoit liste groupée par offre:
     [
       {
         offerId: "...",
         offerName: "Mensuel",
         subscribers: [
           {parentName: "Dupont", childName: "Marie", ...}
         ],
         totalSubscribers: 15
       },
       {
         offerId: "...",
         offerName: "Annuel",
         subscribers: [...],
         totalSubscribers: 8
       }
     ]
   
6. CONSULTER LES ABONNEMENTS ACTIFS
   GET /subscriptions?status=ACTIVE
   Headers: Authorization: Bearer {token}
   → Reçoit tous les abonnements actifs de son académie
   
7. CONSULTER LES PRÉVISIONS DE REVENUS
   GET /subscriptions/forecast/revenue
   Headers: Authorization: Bearer {token}
   → Reçoit:
     {
       currentMonthRevenue: 5000,
       nextMonthForecast: 5500,
       growthRate: 10,
       activeSubscriptions: 100
     }
```

### Scénario C: Gestion d'un Abonnement Existant

**Objectif**: Un parent modifie et renouvelle un abonnement

```
1. CONSULTER MES ABONNEMENTS
   GET /subscriptions/my
   Headers: Authorization: Bearer {token}
   → Reçoit liste des abonnements
   
2. MODIFIER UN ABONNEMENT (ex: désactiver auto-renouvellement)
   PATCH /subscriptions/{id}
   Body: {autoRenew: false}
   Headers: Authorization: Bearer {token}
   → Abonnement mis à jour
   
3. ANNULER UN ABONNEMENT
   POST /subscriptions/{id}/cancel
   Headers: Authorization: Bearer {token}
   → Status changé à CANCELLED
   
4. RENOUVELER UN ABONNEMENT EXPIRÉ
   POST /subscriptions/{id}/renew
   Headers: Authorization: Bearer {token}
   → Nouvel abonnement créé avec nouvelles dates
   → Status: PENDING (en attente de paiement)
```

---

## 🔍 Points Importants à Retenir

### 1. Authentification
- **Toujours** inclure le header `Authorization: Bearer {token}` pour les routes protégées
- Le token contient: `userId`, `email`, `role`
- Le backend extrait automatiquement ces infos du token

### 2. Permissions Automatiques
- **PARENT**: Ne voit que ses propres données (enfants, abonnements)
- **ACADEMIE**: Ne voit que ses propres offres et abonnements
- **ADMIN**: Voit tout

### 3. Filtrage Automatique
- Quand une académie appelle `GET /offers`, le backend filtre automatiquement par son `academyId`
- Pas besoin de passer `academyId` en paramètre

### 4. Création Automatique
- Quand une académie crée une offre, le `academyId` est automatiquement extrait du JWT
- Pas besoin de l'inclure dans le body

### 5. Gestion des Erreurs
- **401**: Token invalide ou expiré → Rediriger vers login
- **403**: Permissions insuffisantes → Afficher message d'erreur
- **404**: Ressource introuvable
- **409**: Conflit (ex: email déjà utilisé)

### 6. Dates
- Toutes les dates sont au format ISO 8601: `2024-01-15T10:00:00.000Z`
- Le backend calcule automatiquement `endDate` = `startDate` + `durationDays`

### 7. Status des Abonnements
- **PENDING**: En attente de paiement
- **ACTIVE**: Actif et payé
- **EXPIRED**: Expiré
- **CANCELLED**: Annulé
- **SUSPENDED**: Suspendu

### 8. Status des Paiements
- **PENDING**: En attente
- **PAID**: Payé
- **FAILED**: Échoué
- **REFUNDED**: Remboursé

---

## 📞 Checklist d'Intégration

- [ ] Configuration de Retrofit/HTTP client
- [ ] Gestion du token JWT (sauvegarde, récupération)
- [ ] Intercepteur pour ajouter le token aux requêtes
- [ ] Modèles de données (User, Offer, Subscription, etc.)
- [ ] Repository pattern pour les appels API
- [ ] ViewModel/State management
- [ ] Gestion des erreurs (401, 403, 404, etc.)
- [ ] Loading states
- [ ] Refresh token (si implémenté)
- [ ] Tests des endpoints avec Swagger
- [ ] Gestion de la déconnexion

---

**Dernière mise à jour**: Janvier 2024  
**Version API**: 1.0
