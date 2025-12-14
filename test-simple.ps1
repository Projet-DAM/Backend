# Test simple de l'endpoint subscription-options
# Remplacez EMAIL et PASSWORD par vos vraies valeurs

$email = "parent@example.com"
$password = "password123"

Write-Host "Test en cours..." -ForegroundColor Yellow

try {
    # 1. Login
    $login = Invoke-RestMethod -Uri "http://localhost:3000/auth/login" -Method POST -Body (@{email=$email; password=$password} | ConvertTo-Json) -ContentType "application/json"
    Write-Host "Login OK" -ForegroundColor Green
    
    # 2. Get options
    $options = Invoke-RestMethod -Uri "http://localhost:3000/subscription-options" -Headers @{Authorization="Bearer $($login.accessToken)"}
    
    Write-Host "`nNombre d'options: $($options.Count)" -ForegroundColor Cyan
    foreach ($opt in $options) {
        Write-Host "  - $($opt.name): $($opt.price) $($opt.currency)" -ForegroundColor White
    }
    
    Write-Host "`nSUCCES!" -ForegroundColor Green
    
} catch {
    Write-Host "ERREUR: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "Details: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
}
