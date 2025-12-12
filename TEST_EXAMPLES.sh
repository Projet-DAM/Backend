#!/bin/bash

# 🧪 Script de test rapide pour SportyConnect Kids API
# Assurez-vous que l'application est démarrée sur http://localhost:3000

BASE_URL="http://localhost:3000"

echo "🚀 Démarrage des tests..."
echo ""

# Couleurs pour la sortie
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Test 1: Créer un parent
echo -e "${BLUE}Test 1: Création d'un parent${NC}"
PARENT_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Dupont",
    "prenom": "Jean",
    "email": "jean.dupont@test.com",
    "motDePasse": "password123",
    "role": "parent"
  }')

echo "$PARENT_RESPONSE" | jq '.'
PARENT_TOKEN=$(echo "$PARENT_RESPONSE" | jq -r '.access_token')
PARENT_ID=$(echo "$PARENT_RESPONSE" | jq -r '.user.id')

echo -e "${GREEN}✅ Parent créé - ID: $PARENT_ID${NC}"
echo -e "${GREEN}✅ Token: ${PARENT_TOKEN:0:50}...${NC}"
echo ""

# Test 2: Créer un enfant
echo -e "${BLUE}Test 2: Création d'un enfant${NC}"
CHILD_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Dupont",
    "prenom": "Marie",
    "email": "marie.dupont@test.com",
    "motDePasse": "password123",
    "role": "enfant"
  }')

echo "$CHILD_RESPONSE" | jq '.'
CHILD_ID=$(echo "$CHILD_RESPONSE" | jq -r '.user.id')

echo -e "${GREEN}✅ Enfant créé - ID: $CHILD_ID${NC}"
echo ""

# Test 3: Connexion
echo -e "${BLUE}Test 3: Connexion${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jean.dupont@test.com",
    "motDePasse": "password123"
  }')

echo "$LOGIN_RESPONSE" | jq '.'
LOGIN_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.access_token')

echo -e "${GREEN}✅ Connexion réussie${NC}"
echo ""

# Test 4: Récupérer un utilisateur (avec authentification)
echo -e "${BLUE}Test 4: Récupérer un utilisateur${NC}"
USER_RESPONSE=$(curl -s -X GET "$BASE_URL/users/$PARENT_ID" \
  -H "Authorization: Bearer $LOGIN_TOKEN")

echo "$USER_RESPONSE" | jq '.'
echo -e "${GREEN}✅ Utilisateur récupéré${NC}"
echo ""

# Test 5: Lier un enfant à un parent
echo -e "${BLUE}Test 5: Lier un enfant à un parent${NC}"
LINK_RESPONSE=$(curl -s -X POST "$BASE_URL/users/$PARENT_ID/link-child" \
  -H "Authorization: Bearer $LOGIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"childId\": \"$CHILD_ID\"
  }")

echo "$LINK_RESPONSE" | jq '.'
echo -e "${GREEN}✅ Enfant lié au parent${NC}"
echo ""

# Test 6: Récupérer les enfants d'un parent
echo -e "${BLUE}Test 6: Récupérer les enfants d'un parent${NC}"
CHILDREN_RESPONSE=$(curl -s -X GET "$BASE_URL/users/$PARENT_ID/children" \
  -H "Authorization: Bearer $LOGIN_TOKEN")

echo "$CHILDREN_RESPONSE" | jq '.'
echo -e "${GREEN}✅ Liste des enfants récupérée${NC}"
echo ""

# Test 7: Créer un utilisateur académie
echo -e "${BLUE}Test 7: Création d'un utilisateur académie${NC}"
ACADEMIE_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Académie",
    "prenom": "Sport",
    "email": "academie@test.com",
    "motDePasse": "password123",
    "role": "academie"
  }')

echo "$ACADEMIE_RESPONSE" | jq '.'
ACADEMIE_TOKEN=$(echo "$ACADEMIE_RESPONSE" | jq -r '.access_token')

echo -e "${GREEN}✅ Académie créée${NC}"
echo ""

# Test 8: Lister tous les utilisateurs (Académie uniquement)
echo -e "${BLUE}Test 8: Lister tous les utilisateurs${NC}"
USERS_RESPONSE=$(curl -s -X GET "$BASE_URL/users" \
  -H "Authorization: Bearer $ACADEMIE_TOKEN")

echo "$USERS_RESPONSE" | jq '.'
echo -e "${GREEN}✅ Liste des utilisateurs récupérée${NC}"
echo ""

# Test 9: Mettre à jour un utilisateur
echo -e "${BLUE}Test 9: Mettre à jour un utilisateur${NC}"
UPDATE_RESPONSE=$(curl -s -X PATCH "$BASE_URL/users/$PARENT_ID" \
  -H "Authorization: Bearer $LOGIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "prenom": "Jean-Pierre"
  }')

echo "$UPDATE_RESPONSE" | jq '.'
echo -e "${GREEN}✅ Utilisateur mis à jour${NC}"
echo ""

# Test 10: Test d'erreur - Email déjà utilisé
echo -e "${BLUE}Test 10: Test d'erreur - Email déjà utilisé${NC}"
ERROR_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Test",
    "prenom": "Test",
    "email": "jean.dupont@test.com",
    "motDePasse": "password123",
    "role": "parent"
  }')

echo "$ERROR_RESPONSE" | jq '.'
echo -e "${RED}✅ Erreur attendue (409 Conflict)${NC}"
echo ""

echo -e "${GREEN}🎉 Tous les tests sont terminés !${NC}"
echo ""
echo "📝 Note: Pour tester l'upload de photo, utilisez Swagger UI:"
echo "   http://localhost:3000/api"

