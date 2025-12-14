# Script de test pour l'endpoint subscription-options
# Ce script teste si l'endpoint retourne bien les options

Write-Host "Test de l'endpoint /subscription-options" -ForegroundColor Cyan
Write-Host "=" * 60

# Configuration
$baseUrl = "http://localhost:3000"
$email = "parent@example.com"  # Changez avec un email valide de votre DB
$password = "password123"       # Changez avec le mot de passe

Write-Host "`nTentative de connexion..." -ForegroundColor Yellow

try {
    # Login pour obtenir un token
    $loginBody = @{
        email = $email
        password = $password
    } | ConvertTo-Json

    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    
    $token = $loginResponse.accessToken
    Write-Host "Connexion reussie!" -ForegroundColor Green
    Write-Host "   Token: $($token.Substring(0, 20))..." -ForegroundColor Gray

    Write-Host "`nTest de l'endpoint /subscription-options..." -ForegroundColor Yellow

    # Appeler l'endpoint
    $options = Invoke-RestMethod -Uri "$baseUrl/subscription-options" -Headers @{Authorization="Bearer $token"} -Method Get

    Write-Host "Endpoint fonctionne!" -ForegroundColor Green
    Write-Host "`nOptions recues:" -ForegroundColor Cyan
    
    if ($options -is [Array]) {
        Write-Host "   Type de reponse: Tableau (Array)" -ForegroundColor Green
        Write-Host "   Nombre d'options: $($options.Count)" -ForegroundColor White
        
        foreach ($option in $options) {
            Write-Host "`n   - $($option.name)" -ForegroundColor White
            Write-Host "      Type: $($option.type)" -ForegroundColor Gray
            Write-Host "      Prix: $($option.price) $($option.currency)" -ForegroundColor Gray
            Write-Host "      Description: $($option.description)" -ForegroundColor Gray
        }
    } else {
        Write-Host "   Type de reponse: Objet (Object)" -ForegroundColor Yellow
        Write-Host "   Donnees: $($options | ConvertTo-Json)" -ForegroundColor Gray
    }

    Write-Host "`nTEST REUSSI!" -ForegroundColor Green
    Write-Host "=" * 60

} catch {
    Write-Host "`nERREUR!" -ForegroundColor Red
    Write-Host "   Message: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.ErrorDetails.Message) {
        Write-Host "   Details: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
    
    Write-Host "`nSolutions possibles:" -ForegroundColor Yellow
    Write-Host "   1. Verifiez que le serveur est demarre (npm run start:dev)" -ForegroundColor White
    Write-Host "   2. Verifiez l'email et le mot de passe dans le script" -ForegroundColor White
    Write-Host "   3. Creez un utilisateur parent dans la base de donnees" -ForegroundColor White
    
    Write-Host "=" * 60
}

Write-Host "`nPour utiliser ce script:" -ForegroundColor Cyan
Write-Host "   1. Modifiez email et password avec vos identifiants" -ForegroundColor White
Write-Host "   2. Assurez-vous que le serveur est demarre" -ForegroundColor White
Write-Host "   3. Executez: .\test-subscription-options.ps1" -ForegroundColor White
