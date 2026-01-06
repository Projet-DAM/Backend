# 🧪 Collection de Tests - Postman & cURL

Ce fichier contient des exemples de requêtes prêtes à l'emploi pour tester votre API.

---

## 📋 Table des Matières
1. [Configuration](#configuration)
2. [Tests d'Authentification](#tests-dauthentification)
3. [Tests des Offres](#tests-des-offres)
4. [Tests des Abonnements](#tests-des-abonnements)
5. [Tests des Paiements](#tests-des-paiements)
6. [Collection Postman](#collection-postman)

---

## ⚙️ Configuration

### Variables d'Environnement

Créez ces variables dans Postman ou exportez-les dans votre terminal :

```bash
# Postman Variables
BASE_URL = http://localhost:3000
TOKEN = (sera rempli après login)
USER_ID = (sera rempli après register)
CHILD_ID = (sera rempli après création enfant)
OFFER_ID = (sera rempli après création offre)
SUBSCRIPTION_ID = (sera rempli après création abonnement)

# Terminal (Linux/Mac)
export BASE_URL="http://localhost:3000"
export TOKEN="your_token_here"

# Terminal (Windows PowerShell)
$env:BASE_URL="http://localhost:3000"
$env:TOKEN="your_token_here"
```

---

## 🔐 Tests d'Authentification

### 1. Inscription Parent

**cURL:**
```bash
curl -X POST $BASE_URL/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Dupont",
    "prenom": "Jean",
    "email": "jean.dupont@example.com",
    "motDePasse": "password123",
    "role": "parent"
  }'
```

**Réponse attendue:**
```json
{
  "message": "Inscription réussie. Un code de vérification a été envoyé à votre adresse email.",
  "userId": "65b1f0a1b2c3d4e5f6g7h8i2",
  "email": "jean.dupont@example.com"
}
```

**Postman:**
- Method: `POST`
- URL: `{{BASE_URL}}/auth/register`
- Body (raw JSON):
```json
{
  "nom": "Dupont",
  "prenom": "Jean",
  "email": "jean.dupont@example.com",
  "motDePasse": "password123",
  "role": "parent"
}
```
- Tests (onglet Tests):
```javascript
pm.test("Status code is 201", function () {
    pm.response.to.have.status(201);
});

pm.test("Response has userId", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('userId');
    pm.environment.set("USER_ID", jsonData.userId);
});
```

### 2. Inscription Académie

**cURL:**
```bash
curl -X POST $BASE_URL/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Académie",
    "prenom": "Sport",
    "email": "academie@example.com",
    "motDePasse": "password123",
    "role": "academie"
  }'
```

### 3. Vérification Email

**cURL:**
```bash
curl -X POST $BASE_URL/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "65b1f0a1b2c3d4e5f6g7h8i2",
    "code": "123456"
  }'
```

**Réponse attendue:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "65b1f0a1b2c3d4e5f6g7h8i2",
    "email": "jean.dupont@example.com",
    "nom": "Dupont",
    "prenom": "Jean",
    "role": "parent"
  }
}
```

**Postman Tests:**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Save token", function () {
    var jsonData = pm.response.json();
    pm.environment.set("TOKEN", jsonData.access_token);
});
```

### 4. Connexion

**cURL:**
```bash
curl -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jean.dupont@example.com",
    "motDePasse": "password123"
  }'
```

**Postman:**
- Method: `POST`
- URL: `{{BASE_URL}}/auth/login`
- Body:
```json
{
  "email": "jean.dupont@example.com",
  "motDePasse": "password123"
}
```
- Tests:
```javascript
pm.test("Login successful", function () {
    pm.response.to.have.status(200);
    var jsonData = pm.response.json();
    pm.environment.set("TOKEN", jsonData.access_token);
});
```

---

## 👤 Tests Utilisateurs

### 1. Mon Profil

**cURL:**
```bash
curl -X GET $BASE_URL/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Postman:**
- Method: `GET`
- URL: `{{BASE_URL}}/users/me`
- Headers:
  - `Authorization`: `Bearer {{TOKEN}}`

### 2. Ajouter un Enfant

**cURL:**
```bash
curl -X POST $BASE_URL/users/children \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Dupont",
    "prenom": "Marie",
    "dateNaissance": "2015-05-20",
    "genre": "F",
    "niveau": "Débutant"
  }'
```

**Postman:**
- Method: `POST`
- URL: `{{BASE_URL}}/users/children`
- Headers:
  - `Authorization`: `Bearer {{TOKEN}}`
- Body:
```json
{
  "nom": "Dupont",
  "prenom": "Marie",
  "dateNaissance": "2015-05-20",
  "genre": "F",
  "niveau": "Débutant"
}
```
- Tests:
```javascript
pm.test("Child created", function () {
    pm.response.to.have.status(201);
    var jsonData = pm.response.json();
    pm.environment.set("CHILD_ID", jsonData._id);
});
```

### 3. Lister Mes Enfants

**cURL:**
```bash
curl -X GET $BASE_URL/users/children \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

---

## 🎯 Tests des Offres

### 1. Créer une Offre (Académie)

**cURL:**
```bash
curl -X POST $BASE_URL/offers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Abonnement Mensuel",
    "description": "Accès complet pendant 1 mois",
    "type": "MONTHLY",
    "durationDays": 30,
    "price": 50,
    "currency": "EUR",
    "discountPct": 0,
    "conditions": "Non remboursable",
    "isActive": true
  }'
```

**Postman:**
- Method: `POST`
- URL: `{{BASE_URL}}/offers`
- Headers:
  - `Authorization`: `Bearer {{TOKEN}}`
- Body:
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
- Tests:
```javascript
pm.test("Offer created", function () {
    pm.response.to.have.status(201);
    var jsonData = pm.response.json();
    pm.environment.set("OFFER_ID", jsonData._id);
});
```

### 2. Lister Toutes les Offres Actives

**cURL:**
```bash
curl -X GET "$BASE_URL/offers?isActive=true&page=1&limit=10" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Postman:**
- Method: `GET`
- URL: `{{BASE_URL}}/offers`
- Headers:
  - `Authorization`: `Bearer {{TOKEN}}`
- Params:
  - `isActive`: `true`
  - `page`: `1`
  - `limit`: `10`

### 3. Détails d'une Offre

**cURL:**
```bash
curl -X GET $BASE_URL/offers/$OFFER_ID \
  -H "Content-Type: application/json"
```

### 4. Modifier une Offre

**cURL:**
```bash
curl -X PATCH $BASE_URL/offers/$OFFER_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "price": 45,
    "discountPct": 10
  }'
```

### 5. Désactiver une Offre

**cURL:**
```bash
curl -X PATCH $BASE_URL/offers/$OFFER_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "isActive": false
  }'
```

### 6. Supprimer une Offre

**cURL:**
```bash
curl -X DELETE $BASE_URL/offers/$OFFER_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

### 7. Tous les Inscrits par Offre (Académie)

**cURL:**
```bash
curl -X GET $BASE_URL/offers/all-subscribers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

---

## 📝 Tests des Abonnements

### 1. Créer un Abonnement (Parent)

**cURL:**
```bash
curl -X POST $BASE_URL/subscriptions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "childId": "'"$CHILD_ID"'",
    "offerId": "'"$OFFER_ID"'",
    "autoRenew": true
  }'
```

**Postman:**
- Method: `POST`
- URL: `{{BASE_URL}}/subscriptions`
- Headers:
  - `Authorization`: `Bearer {{TOKEN}}`
- Body:
```json
{
  "childId": "{{CHILD_ID}}",
  "offerId": "{{OFFER_ID}}",
  "autoRenew": true
}
```
- Tests:
```javascript
pm.test("Subscription created", function () {
    pm.response.to.have.status(201);
    var jsonData = pm.response.json();
    pm.environment.set("SUBSCRIPTION_ID", jsonData._id);
});
```

### 2. Mes Abonnements (Parent)

**cURL:**
```bash
curl -X GET $BASE_URL/subscriptions/my \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

### 3. Tous les Abonnements (Académie/Admin)

**cURL:**
```bash
curl -X GET "$BASE_URL/subscriptions?status=ACTIVE&page=1&limit=10" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Postman:**
- Method: `GET`
- URL: `{{BASE_URL}}/subscriptions`
- Headers:
  - `Authorization`: `Bearer {{TOKEN}}`
- Params:
  - `status`: `ACTIVE`
  - `page`: `1`
  - `limit`: `10`

### 4. Détails d'un Abonnement

**cURL:**
```bash
curl -X GET $BASE_URL/subscriptions/$SUBSCRIPTION_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

### 5. Modifier un Abonnement

**cURL:**
```bash
curl -X PATCH $BASE_URL/subscriptions/$SUBSCRIPTION_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "autoRenew": false
  }'
```

### 6. Annuler un Abonnement

**cURL:**
```bash
curl -X POST $BASE_URL/subscriptions/$SUBSCRIPTION_ID/cancel \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

### 7. Renouveler un Abonnement

**cURL:**
```bash
curl -X POST $BASE_URL/subscriptions/$SUBSCRIPTION_ID/renew \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

### 8. Prévisions de Revenus (Académie)

**cURL:**
```bash
curl -X GET $BASE_URL/subscriptions/forecast/revenue \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

---

## 💳 Tests des Paiements

### 1. Créer un Payment Intent

**cURL:**
```bash
curl -X POST $BASE_URL/payments/create-payment-intent \
  -H "Content-Type: application/json" \
  -d '{
    "offerId": "'"$OFFER_ID"'",
    "selectedOptions": []
  }'
```

**Postman:**
- Method: `POST`
- URL: `{{BASE_URL}}/payments/create-payment-intent`
- Body:
```json
{
  "offerId": "{{OFFER_ID}}",
  "selectedOptions": []
}
```
- Tests:
```javascript
pm.test("Payment intent created", function () {
    pm.response.to.have.status(200);
    var jsonData = pm.response.json();
    pm.environment.set("PAYMENT_INTENT_ID", jsonData.paymentIntentId);
    pm.environment.set("CLIENT_SECRET", jsonData.clientSecret);
});
```

### 2. Compléter un Paiement

**cURL:**
```bash
curl -X POST $BASE_URL/payments/complete \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentIntentId": "pi_1234567890",
    "childId": "'"$CHILD_ID"'",
    "offerId": "'"$OFFER_ID"'"
  }'
```

**Postman:**
- Method: `POST`
- URL: `{{BASE_URL}}/payments/complete`
- Headers:
  - `Authorization`: `Bearer {{TOKEN}}`
- Body:
```json
{
  "paymentIntentId": "{{PAYMENT_INTENT_ID}}",
  "childId": "{{CHILD_ID}}",
  "offerId": "{{OFFER_ID}}"
}
```

---

## 📦 Collection Postman Complète

### Import dans Postman

Créez un fichier `SportyConnect_API.postman_collection.json` :

```json
{
  "info": {
    "name": "SportyConnect Kids API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Auth",
      "item": [
        {
          "name": "Register Parent",
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test(\"Status code is 201\", function () {",
                  "    pm.response.to.have.status(201);",
                  "});",
                  "",
                  "pm.test(\"Save userId\", function () {",
                  "    var jsonData = pm.response.json();",
                  "    pm.environment.set(\"USER_ID\", jsonData.userId);",
                  "});"
                ]
              }
            }
          ],
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"nom\": \"Dupont\",\n  \"prenom\": \"Jean\",\n  \"email\": \"jean.dupont@example.com\",\n  \"motDePasse\": \"password123\",\n  \"role\": \"parent\"\n}"
            },
            "url": {
              "raw": "{{BASE_URL}}/auth/register",
              "host": ["{{BASE_URL}}"],
              "path": ["auth", "register"]
            }
          }
        },
        {
          "name": "Login",
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test(\"Login successful\", function () {",
                  "    pm.response.to.have.status(200);",
                  "    var jsonData = pm.response.json();",
                  "    pm.environment.set(\"TOKEN\", jsonData.access_token);",
                  "});"
                ]
              }
            }
          ],
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"email\": \"jean.dupont@example.com\",\n  \"motDePasse\": \"password123\"\n}"
            },
            "url": {
              "raw": "{{BASE_URL}}/auth/login",
              "host": ["{{BASE_URL}}"],
              "path": ["auth", "login"]
            }
          }
        }
      ]
    },
    {
      "name": "Offers",
      "item": [
        {
          "name": "Get All Offers",
          "request": {
            "method": "GET",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{TOKEN}}"
              }
            ],
            "url": {
              "raw": "{{BASE_URL}}/offers?isActive=true",
              "host": ["{{BASE_URL}}"],
              "path": ["offers"],
              "query": [
                {
                  "key": "isActive",
                  "value": "true"
                }
              ]
            }
          }
        },
        {
          "name": "Create Offer",
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test(\"Offer created\", function () {",
                  "    pm.response.to.have.status(201);",
                  "    var jsonData = pm.response.json();",
                  "    pm.environment.set(\"OFFER_ID\", jsonData._id);",
                  "});"
                ]
              }
            }
          ],
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{TOKEN}}"
              },
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"name\": \"Abonnement Mensuel\",\n  \"description\": \"Accès complet pendant 1 mois\",\n  \"type\": \"MONTHLY\",\n  \"durationDays\": 30,\n  \"price\": 50,\n  \"currency\": \"EUR\",\n  \"discountPct\": 0,\n  \"isActive\": true\n}"
            },
            "url": {
              "raw": "{{BASE_URL}}/offers",
              "host": ["{{BASE_URL}}"],
              "path": ["offers"]
            }
          }
        }
      ]
    },
    {
      "name": "Subscriptions",
      "item": [
        {
          "name": "Create Subscription",
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test(\"Subscription created\", function () {",
                  "    pm.response.to.have.status(201);",
                  "    var jsonData = pm.response.json();",
                  "    pm.environment.set(\"SUBSCRIPTION_ID\", jsonData._id);",
                  "});"
                ]
              }
            }
          ],
          "request": {
            "method": "POST",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{TOKEN}}"
              },
              {
                "key": "Content-Type",
                "value": "application/json"
              }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"childId\": \"{{CHILD_ID}}\",\n  \"offerId\": \"{{OFFER_ID}}\",\n  \"autoRenew\": true\n}"
            },
            "url": {
              "raw": "{{BASE_URL}}/subscriptions",
              "host": ["{{BASE_URL}}"],
              "path": ["subscriptions"]
            }
          }
        },
        {
          "name": "My Subscriptions",
          "request": {
            "method": "GET",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{TOKEN}}"
              }
            ],
            "url": {
              "raw": "{{BASE_URL}}/subscriptions/my",
              "host": ["{{BASE_URL}}"],
              "path": ["subscriptions", "my"]
            }
          }
        }
      ]
    }
  ]
}
```

### Variables d'Environnement Postman

Créez un fichier `SportyConnect_Environment.postman_environment.json` :

```json
{
  "name": "SportyConnect Dev",
  "values": [
    {
      "key": "BASE_URL",
      "value": "http://localhost:3000",
      "enabled": true
    },
    {
      "key": "TOKEN",
      "value": "",
      "enabled": true
    },
    {
      "key": "USER_ID",
      "value": "",
      "enabled": true
    },
    {
      "key": "CHILD_ID",
      "value": "",
      "enabled": true
    },
    {
      "key": "OFFER_ID",
      "value": "",
      "enabled": true
    },
    {
      "key": "SUBSCRIPTION_ID",
      "value": "",
      "enabled": true
    },
    {
      "key": "PAYMENT_INTENT_ID",
      "value": "",
      "enabled": true
    }
  ]
}
```

---

## 🔍 Scripts de Test Automatisés

### Script Bash Complet (Linux/Mac)

Créez un fichier `test_api.sh` :

```bash
#!/bin/bash

BASE_URL="http://localhost:3000"

echo "=== Test 1: Register Parent ==="
REGISTER_RESPONSE=$(curl -s -X POST $BASE_URL/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Test",
    "prenom": "User",
    "email": "test'$(date +%s)'@example.com",
    "motDePasse": "password123",
    "role": "parent"
  }')

echo $REGISTER_RESPONSE | jq .

USER_ID=$(echo $REGISTER_RESPONSE | jq -r '.userId')
echo "User ID: $USER_ID"

echo ""
echo "=== Test 2: Login ==="
LOGIN_RESPONSE=$(curl -s -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "parent@example.com",
    "motDePasse": "parentpass"
  }')

echo $LOGIN_RESPONSE | jq .

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.access_token')
echo "Token: $TOKEN"

echo ""
echo "=== Test 3: Get My Profile ==="
curl -s -X GET $BASE_URL/users/me \
  -H "Authorization: Bearer $TOKEN" | jq .

echo ""
echo "=== Test 4: Get Offers ==="
curl -s -X GET "$BASE_URL/offers?isActive=true" \
  -H "Authorization: Bearer $TOKEN" | jq .

echo ""
echo "Tests completed!"
```

Rendez-le exécutable :
```bash
chmod +x test_api.sh
./test_api.sh
```

---

## 📊 Scénario de Test Complet

### Parcours Parent Complet

```bash
#!/bin/bash

BASE_URL="http://localhost:3000"

# 1. Register
echo "1. Registering parent..."
REGISTER=$(curl -s -X POST $BASE_URL/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "TestParent",
    "prenom": "User",
    "email": "testparent'$(date +%s)'@example.com",
    "motDePasse": "password123",
    "role": "parent"
  }')

USER_ID=$(echo $REGISTER | jq -r '.userId')
echo "✓ Registered with userId: $USER_ID"

# 2. Login (skip email verification for testing)
echo ""
echo "2. Logging in..."
LOGIN=$(curl -s -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "parent@example.com",
    "motDePasse": "parentpass"
  }')

TOKEN=$(echo $LOGIN | jq -r '.access_token')
echo "✓ Logged in, token: ${TOKEN:0:20}..."

# 3. Add child
echo ""
echo "3. Adding child..."
CHILD=$(curl -s -X POST $BASE_URL/users/children \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "TestChild",
    "prenom": "Marie",
    "dateNaissance": "2015-05-20",
    "genre": "F"
  }')

CHILD_ID=$(echo $CHILD | jq -r '._id')
echo "✓ Child added with id: $CHILD_ID"

# 4. Get offers
echo ""
echo "4. Getting offers..."
OFFERS=$(curl -s -X GET "$BASE_URL/offers?isActive=true" \
  -H "Authorization: Bearer $TOKEN")

OFFER_ID=$(echo $OFFERS | jq -r '.data[0]._id')
echo "✓ Found offer: $OFFER_ID"

# 5. Create subscription
echo ""
echo "5. Creating subscription..."
SUBSCRIPTION=$(curl -s -X POST $BASE_URL/subscriptions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "childId": "'$CHILD_ID'",
    "offerId": "'$OFFER_ID'",
    "autoRenew": true
  }')

SUBSCRIPTION_ID=$(echo $SUBSCRIPTION | jq -r '._id')
echo "✓ Subscription created: $SUBSCRIPTION_ID"

# 6. Get my subscriptions
echo ""
echo "6. Getting my subscriptions..."
curl -s -X GET $BASE_URL/subscriptions/my \
  -H "Authorization: Bearer $TOKEN" | jq .

echo ""
echo "✅ All tests passed!"
```

---

**Dernière mise à jour** : Janvier 2024  
**Version API** : 1.0
