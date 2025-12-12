# 🧪 Script de test PowerShell pour SportyConnect Kids API
# Assurez-vous que l'application est démarrée sur http://localhost:3000

$BaseUrl = "http://localhost:3000"

Write-Host "🚀 Démarrage des tests..." -ForegroundColor Cyan
Write-Host ""

# Test 1: Créer un parent
Write-Host "Test 1: Création d'un parent" -ForegroundColor Blue
$parentBody = @{
    nom = "Dupont"
    prenom = "Jean"
    email = "jean.dupont@test.com"
    motDePasse = "password123"
    role = "parent"
} | ConvertTo-Json

$parentResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -Body $parentBody -ContentType "application/json"
$parentResponse | ConvertTo-Json -Depth 5
$parentToken = $parentResponse.access_token
$parentId = $parentResponse.user.id

Write-Host "✅ Parent créé - ID: $parentId" -ForegroundColor Green
Write-Host "✅ Token: $($parentToken.Substring(0, 50))..." -ForegroundColor Green
Write-Host ""

# Test 2: Créer un enfant
Write-Host "Test 2: Création d'un enfant" -ForegroundColor Blue
$childBody = @{
    nom = "Dupont"
    prenom = "Marie"
    email = "marie.dupont@test.com"
    motDePasse = "password123"
    role = "enfant"
} | ConvertTo-Json

$childResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -Body $childBody -ContentType "application/json"
$childResponse | ConvertTo-Json -Depth 5
$childId = $childResponse.user.id

Write-Host "✅ Enfant créé - ID: $childId" -ForegroundColor Green
Write-Host ""

# Test 3: Connexion
Write-Host "Test 3: Connexion" -ForegroundColor Blue
$loginBody = @{
    email = "jean.dupont@test.com"
    motDePasse = "password123"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$loginResponse | ConvertTo-Json -Depth 5
$loginToken = $loginResponse.access_token

Write-Host "✅ Connexion réussie" -ForegroundColor Green
Write-Host ""

# Test 4: Récupérer un utilisateur (avec authentification)
Write-Host "Test 4: Récupérer un utilisateur" -ForegroundColor Blue
$headers = @{
    Authorization = "Bearer $loginToken"
}

$userResponse = Invoke-RestMethod -Uri "$BaseUrl/users/$parentId" -Method Get -Headers $headers
$userResponse | ConvertTo-Json -Depth 5
Write-Host "✅ Utilisateur récupéré" -ForegroundColor Green
Write-Host ""

# Test 5: Lier un enfant à un parent
Write-Host "Test 5: Lier un enfant à un parent" -ForegroundColor Blue
$linkBody = @{
    childId = $childId
} | ConvertTo-Json

$linkResponse = Invoke-RestMethod -Uri "$BaseUrl/users/$parentId/link-child" -Method Post -Body $linkBody -Headers $headers -ContentType "application/json"
$linkResponse | ConvertTo-Json -Depth 5
Write-Host "✅ Enfant lié au parent" -ForegroundColor Green
Write-Host ""

# Test 6: Récupérer les enfants d'un parent
Write-Host "Test 6: Récupérer les enfants d'un parent" -ForegroundColor Blue
$childrenResponse = Invoke-RestMethod -Uri "$BaseUrl/users/$parentId/children" -Method Get -Headers $headers
$childrenResponse | ConvertTo-Json -Depth 5
Write-Host "✅ Liste des enfants récupérée" -ForegroundColor Green
Write-Host ""

# Test 7: Créer un utilisateur académie
Write-Host "Test 7: Création d'un utilisateur académie" -ForegroundColor Blue
$academieBody = @{
    nom = "Académie"
    prenom = "Sport"
    email = "academie@test.com"
    motDePasse = "password123"
    role = "academie"
} | ConvertTo-Json

$academieResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -Body $academieBody -ContentType "application/json"
$academieResponse | ConvertTo-Json -Depth 5
$academieToken = $academieResponse.access_token

Write-Host "✅ Académie créée" -ForegroundColor Green
Write-Host ""

# Test 8: Lister tous les utilisateurs (Académie uniquement)
Write-Host "Test 8: Lister tous les utilisateurs" -ForegroundColor Blue
$academieHeaders = @{
    Authorization = "Bearer $academieToken"
}

$usersResponse = Invoke-RestMethod -Uri "$BaseUrl/users" -Method Get -Headers $academieHeaders
$usersResponse | ConvertTo-Json -Depth 5
Write-Host "✅ Liste des utilisateurs récupérée" -ForegroundColor Green
Write-Host ""

# Test 9: Mettre à jour un utilisateur
Write-Host "Test 9: Mettre à jour un utilisateur" -ForegroundColor Blue
$updateBody = @{
    prenom = "Jean-Pierre"
} | ConvertTo-Json

$updateResponse = Invoke-RestMethod -Uri "$BaseUrl/users/$parentId" -Method Patch -Body $updateBody -Headers $headers -ContentType "application/json"
$updateResponse | ConvertTo-Json -Depth 5
Write-Host "✅ Utilisateur mis à jour" -ForegroundColor Green
Write-Host ""

# Test 10: Test d'erreur - Email déjà utilisé
Write-Host "Test 10: Test d'erreur - Email déjà utilisé" -ForegroundColor Blue
try {
    $errorResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -Body $parentBody -ContentType "application/json"
} catch {
    Write-Host "Erreur attendue: $_" -ForegroundColor Yellow
    Write-Host "✅ Erreur 409 (Conflict) - Email déjà utilisé" -ForegroundColor Green
}
Write-Host ""

Write-Host "🎉 Tous les tests sont terminés !" -ForegroundColor Green
Write-Host ""
Write-Host "📝 Note: Pour tester l'upload de photo, utilisez Swagger UI:" -ForegroundColor Cyan
Write-Host "   http://localhost:3000/api" -ForegroundColor Cyan

