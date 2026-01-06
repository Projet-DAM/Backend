# Script pour initialiser les options d'abonnement
# Usage: .\init-subscription-options.ps1

Write-Host "🎯 Initialisation des Options d'Abonnement" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:3000"
$seedUrl = "$baseUrl/subscription-options/seed"

# Vérifier que le serveur est accessible
Write-Host "📡 Vérification du serveur..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api" -Method GET -ErrorAction Stop
    Write-Host "✅ Serveur accessible" -ForegroundColor Green
} catch {
    Write-Host "❌ Erreur: Le serveur n'est pas accessible sur $baseUrl" -ForegroundColor Red
    Write-Host "   Assurez-vous que le serveur est démarré avec 'npm run start:dev'" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "🔑 Authentification requise" -ForegroundColor Cyan
Write-Host "Vous devez vous connecter en tant qu'ADMIN pour initialiser les options" -ForegroundColor Yellow
Write-Host ""

$email = Read-Host "Email admin"
$password = Read-Host "Mot de passe" -AsSecureString
$passwordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($password))

# Se connecter
Write-Host ""
Write-Host "🔐 Connexion..." -ForegroundColor Yellow

try {
    $loginBody = @{
        email = $email
        password = $passwordPlain
    } | ConvertTo-Json

    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -ErrorAction Stop
    $token = $loginResponse.access_token
    
    Write-Host "✅ Connexion réussie" -ForegroundColor Green
} catch {
    Write-Host "❌ Erreur de connexion: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Initialiser les options
Write-Host ""
Write-Host "📦 Initialisation des options..." -ForegroundColor Yellow

try {
    $headers = @{
        "Authorization" = "Bearer $token"
        "Content-Type" = "application/json"
    }
    
    $seedResponse = Invoke-RestMethod -Uri $seedUrl -Method POST -Headers $headers -ErrorAction Stop
    
    Write-Host "✅ Options initialisées avec succès!" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode.Value__ -eq 409) {
        Write-Host "⚠️  Les options sont déjà initialisées" -ForegroundColor Yellow
    } else {
        Write-Host "❌ Erreur lors de l'initialisation: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

# Vérifier les options créées
Write-Host ""
Write-Host "📋 Vérification des options créées..." -ForegroundColor Yellow

try {
    $optionsResponse = Invoke-RestMethod -Uri "$baseUrl/subscription-options/active" -Method GET -ErrorAction Stop
    
    Write-Host ""
    Write-Host "✅ Options disponibles:" -ForegroundColor Green
    Write-Host ""
    
    foreach ($option in $optionsResponse) {
        Write-Host "  📌 $($option.displayName)" -ForegroundColor Cyan
        Write-Host "     Type: $($option.type)" -ForegroundColor Gray
        Write-Host "     Description: $($option.description)" -ForegroundColor Gray
        Write-Host "     Prix: $($option.price) TND" -ForegroundColor Yellow
        Write-Host "     Active: $($option.isActive)" -ForegroundColor Gray
        Write-Host ""
    }
    
    Write-Host "✅ Total: $($optionsResponse.Count) options" -ForegroundColor Green
} catch {
    Write-Host "❌ Erreur lors de la récupération des options: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "🎉 Initialisation terminée!" -ForegroundColor Green
Write-Host ""
Write-Host "📚 Endpoints disponibles:" -ForegroundColor Cyan
Write-Host "  - GET  $baseUrl/subscription-options (toutes les options)" -ForegroundColor Gray
Write-Host "  - GET  $baseUrl/subscription-options/active (options actives)" -ForegroundColor Gray
Write-Host "  - POST $baseUrl/subscription-options (créer une option - ADMIN)" -ForegroundColor Gray
Write-Host ""
