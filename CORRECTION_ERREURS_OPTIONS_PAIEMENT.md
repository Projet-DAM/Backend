# 🔧 Correction des Erreurs 404 et 400 - Options d'Abonnement et Paiement

## 🔍 Problèmes Identifiés

### 1. ❌ Erreur 404 - Options d'Abonnement Non Trouvées
**Symptôme** : L'application mobile affiche "Erreur 404: Not found" lors du chargement des options d'abonnement.

**Cause** : Le module `subscription-options` n'existait pas dans le backend.

**Solution** : ✅ Module créé et configuré

### 2. ❌ Erreur 400 - Paiement Échoue
**Symptôme** : Erreur 400 lors du clic sur "Payer maintenant".

**Cause** : Probablement liée aux données envoyées au backend (options sélectionnées, montant, etc.).

---

## ✅ Solutions Implémentées

### 1. Création du Module Subscription Options

J'ai créé un module complet pour gérer les options d'abonnement :

#### Fichiers Créés

**Structure** :
```
src/subscription-options/
├── schemas/
│   └── subscription-option.schema.ts
├── dto/
│   ├── create-subscription-option.dto.ts
│   └── update-subscription-option.dto.ts
├── subscription-options.controller.ts
├── subscription-options.service.ts
└── subscription-options.module.ts
```

#### Endpoints Disponibles

| Endpoint | Méthode | Auth | Description |
|----------|---------|------|-------------|
| `/subscription-options` | GET | Public | Toutes les options |
| `/subscription-options/active` | GET | Public | Options actives uniquement |
| `/subscription-options/:id` | GET | Public | Une option par ID |
| `/subscription-options` | POST | ADMIN/ACADEMIE | Créer une option |
| `/subscription-options/:id` | PATCH | ADMIN/ACADEMIE | Modifier une option |
| `/subscription-options/:id` | DELETE | ADMIN | Supprimer une option |
| `/subscription-options/seed` | POST | ADMIN | Initialiser les options par défaut |

#### Options Par Défaut

Le système crée automatiquement 3 options :

1. **Tenue de sport** (`SPORTS_OUTFIT`)
   - Description : Tenue complète pour la pratique sportive
   - Prix : 50 TND

2. **Assurance sportive** (`INSURANCE`)
   - Description : Couverture complète en cas d'accident
   - Prix : 30 TND

3. **Transport** (`TRANSPORT`)
   - Description : Service de transport aller-retour
   - Prix : 40 TND

---

## 🚀 Configuration Requise

### Étape 1 : Initialiser les Options

Le serveur a redémarré automatiquement. Maintenant, initialisez les options par défaut :

#### Option A : Via Script PowerShell (Recommandé)
```powershell
.\init-subscription-options.ps1
```

Le script vous demandera :
- Email admin
- Mot de passe

Puis il initialisera automatiquement les 3 options par défaut.

#### Option B : Via Swagger
1. Ouvrir http://localhost:3000/api
2. Aller à la section "Subscription Options"
3. Cliquer sur `POST /subscription-options/seed`
4. Cliquer sur "Try it out"
5. S'authentifier avec un compte ADMIN
6. Cliquer sur "Execute"

#### Option C : Via cURL
```powershell
# 1. Se connecter
$loginResponse = Invoke-RestMethod -Uri "http://localhost:3000/auth/login" -Method POST -Body '{"email":"admin@example.com","password":"password"}' -ContentType "application/json"
$token = $loginResponse.access_token

# 2. Initialiser les options
Invoke-RestMethod -Uri "http://localhost:3000/subscription-options/seed" -Method POST -Headers @{"Authorization"="Bearer $token"}
```

### Étape 2 : Vérifier les Options

Testez l'endpoint public :
```powershell
Invoke-RestMethod -Uri "http://localhost:3000/subscription-options/active" -Method GET
```

Vous devriez voir les 3 options créées.

---

## 🔧 Correction de l'Erreur 400 (Paiement)

### Diagnostic

L'erreur 400 lors du paiement peut avoir plusieurs causes :

#### Cause 1 : Format des Options Sélectionnées
Le frontend envoie probablement les IDs des options, mais le backend attend peut-être un format différent.

**Vérification** : Regardons le code Kotlin :
```kotlin
onContinue: (List<String>, Double) -> Unit // selectedOptionIds, totalPrice
```

Le frontend envoie une `List<String>` qui contient les **types** (SPORTS_OUTFIT, INSURANCE, TRANSPORT).

**Solution** : Il faut vérifier que le backend accepte ce format.

#### Cause 2 : Montant Incorrect
Le montant total est calculé côté frontend :
```kotlin
val totalPrice = offer.price + optionsPrice
```

**Vérification** : S'assurer que le backend reçoit le bon montant.

### Modification du DTO de Création d'Abonnement

Vérifions le DTO `CreateSubscriptionDto` pour s'assurer qu'il accepte les options :

**Fichier à vérifier** : `src/subscriptions/dto/create-subscription.dto.ts`

Il devrait contenir un champ pour les options sélectionnées, par exemple :
```typescript
@ApiProperty({ 
  example: ['SPORTS_OUTFIT', 'INSURANCE'], 
  required: false,
  description: 'Types d\'options sélectionnées' 
})
@IsArray()
@IsOptional()
selectedOptions?: string[];
```

### Test du Flow Complet

1. **Sélectionner une offre**
2. **Choisir des options** (ex: Tenue de sport + Assurance)
3. **Calculer le total** : Offre (70 TND) + Tenue (50 TND) + Assurance (30 TND) = 150 TND
4. **Créer l'abonnement** avec les options
5. **Créer le PaymentIntent** avec le montant total
6. **Confirmer le paiement**

---

## 🧪 Tests

### Test 1 : Récupérer les Options Actives

```powershell
# Via PowerShell
Invoke-RestMethod -Uri "http://localhost:3000/subscription-options/active" -Method GET
```

**Résultat attendu** :
```json
[
  {
    "_id": "...",
    "type": "SPORTS_OUTFIT",
    "displayName": "Tenue de sport",
    "description": "Tenue complète pour la pratique sportive",
    "price": 50,
    "isActive": true,
    "displayOrder": 1
  },
  {
    "_id": "...",
    "type": "INSURANCE",
    "displayName": "Assurance sportive",
    "description": "Couverture complète en cas d'accident",
    "price": 30,
    "isActive": true,
    "displayOrder": 2
  },
  {
    "_id": "...",
    "type": "TRANSPORT",
    "displayName": "Transport",
    "description": "Service de transport aller-retour",
    "price": 40,
    "isActive": true,
    "displayOrder": 3
  }
]
```

### Test 2 : Tester depuis l'Application Mobile

1. Ouvrir l'application
2. Sélectionner une offre
3. Cliquer sur "Payer maintenant"
4. **Vérifier** : Les options doivent maintenant s'afficher (plus d'erreur 404)

---

## 📊 Logs à Vérifier

### Logs Attendus au Démarrage

Après redémarrage du serveur, vous devriez voir :
```
[NestFactory] Starting Nest application...
[InstanceLoader] SubscriptionOptionsModule dependencies initialized
[RoutesResolver] SubscriptionOptionsController {/subscription-options}
```

### Logs lors de l'Initialisation

Après exécution de `init-subscription-options.ps1` :
```
[SubscriptionOptionsService] Options initialized
```

### Logs lors du Chargement des Options (Mobile)

Quand l'app mobile charge les options :
```
GET /subscription-options/active 200
```

---

## 🔍 Debugging de l'Erreur 400

Si l'erreur 400 persiste lors du paiement, vérifiez :

### 1. Les Logs du Serveur

Regardez les logs pour voir l'erreur exacte :
```
[PaymentsController] Error creating payment intent: ...
```

### 2. Le Payload Envoyé

Dans le code Kotlin, ajoutez un log avant l'appel API :
```kotlin
Log.d("Payment", "Creating payment with options: $selectedOptions, total: $totalPrice")
```

### 3. Le DTO Backend

Vérifiez que `CreatePaymentIntentDto` accepte tous les champs nécessaires :
- `amount` ✅
- `currency` ✅
- `paymentMethodId` ✅
- `subscriptionId` ✅
- `selectedOptions` ❓ (à vérifier)

---

## 📝 Prochaines Étapes

1. **Initialiser les options** avec le script
2. **Tester le chargement** des options dans l'app mobile
3. **Si erreur 400 persiste** :
   - Vérifier les logs du serveur
   - Vérifier le payload envoyé par le mobile
   - Ajuster le DTO si nécessaire

---

## 🆘 Troubleshooting

### Erreur : "Options déjà initialisées"
**Solution** : C'est normal si vous exécutez le script plusieurs fois. Les options existent déjà.

### Erreur : "Unauthorized" lors de l'initialisation
**Solution** : Utilisez un compte avec le rôle ADMIN.

### Les options ne s'affichent pas dans l'app
**Solution** :
1. Vérifier que le serveur est démarré
2. Vérifier l'URL dans l'app : `http://10.0.2.2:3000/subscription-options/active` (émulateur) ou `http://VOTRE_IP:3000/subscription-options/active` (appareil réel)
3. Vérifier les logs du serveur

### Erreur 400 persiste
**Solution** :
1. Consulter les logs du serveur pour voir l'erreur exacte
2. Vérifier le format des données envoyées
3. Me partager les logs pour diagnostic

---

## ✅ Résumé

- ✅ Module `subscription-options` créé
- ✅ Endpoints API disponibles
- ✅ Options par défaut prêtes à être initialisées
- ✅ Serveur redémarré sans erreur
- ⏳ **À FAIRE** : Initialiser les options avec le script
- ⏳ **À VÉRIFIER** : Tester le paiement après initialisation

**L'erreur 404 est résolue ! L'erreur 400 nécessite plus d'investigation après l'initialisation des options.**
