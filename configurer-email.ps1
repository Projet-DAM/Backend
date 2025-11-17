# Script de configuration email
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Configuration Email - Académie Sportive" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Pour activer l'envoi d'emails, vous devez configurer SMTP_PASS" -ForegroundColor White
Write-Host ""
Write-Host "ETAPES:" -ForegroundColor Green
Write-Host "1. Allez sur: https://myaccount.google.com/apppasswords" -ForegroundColor White
Write-Host "2. Générez un mot de passe d'application Gmail" -ForegroundColor White
Write-Host "3. Entrez le mot de passe ci-dessous" -ForegroundColor White
Write-Host ""

$smtpPass = Read-Host "Entrez le mot de passe d'application Gmail (16 caractères)"

if ($smtpPass -and $smtpPass.Length -gt 0) {
    # Lire le fichier .env
    $envContent = Get-Content .env -Raw
    
    # Remplacer SMTP_PASS
    $envContent = $envContent -replace 'SMTP_PASS=.*', "SMTP_PASS=$smtpPass"
    
    # Écrire le fichier
    Set-Content -Path .env -Value $envContent -NoNewline
    
    Write-Host ""
    Write-Host "✅ SMTP_PASS configuré avec succès!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Redémarrez le backend pour appliquer les changements:" -ForegroundColor Yellow
    Write-Host "  npm run start:dev" -ForegroundColor White
    Write-Host ""
    Write-Host "Testez avec:" -ForegroundColor Yellow
    Write-Host "  npm run test:email" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "❌ Aucun mot de passe entré. Configuration annulée." -ForegroundColor Red
    Write-Host ""
}






