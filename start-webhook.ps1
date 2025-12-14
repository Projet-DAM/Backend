# Script PowerShell pour démarrer Stripe Webhook avec le chemin complet
# Utilisation: .\start-webhook.ps1

$STRIPE_PATH = "C:\Users\hatem\scoop\apps\stripe\current\stripe.exe"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Stripe Webhook Forwarding" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Vérifier si Stripe CLI existe
if (-not (Test-Path $STRIPE_PATH)) {
    Write-Host "❌ Stripe CLI non trouvé à: $STRIPE_PATH" -ForegroundColor Red
    Write-Host ""
    Write-Host "Veuillez installer Stripe CLI avec:" -ForegroundColor Yellow
    Write-Host "  scoop bucket add stripe https://github.com/stripe/scoop-stripe-cli.git" -ForegroundColor White
    Write-Host "  scoop install stripe" -ForegroundColor White
    exit 1
}

Write-Host "✅ Stripe CLI trouvé" -ForegroundColor Green

# Vérifier la version
$version = & $STRIPE_PATH --version 2>&1
Write-Host "📦 Version: $version" -ForegroundColor Green
Write-Host ""

# Vérifier si l'utilisateur est connecté
Write-Host "🔐 Vérification de l'authentification..." -ForegroundColor Yellow
$configList = & $STRIPE_PATH config --list 2>&1 | Out-String

if ($configList -match "not logged in" -or $configList -match "No account") {
    Write-Host "⚠️  Vous n'êtes pas connecté à Stripe" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Exécution de 'stripe login'..." -ForegroundColor Cyan
    Write-Host ""
    
    & $STRIPE_PATH login
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "❌ Échec de la connexion" -ForegroundColor Red
        exit 1
    }
    
    Write-Host ""
    Write-Host "✅ Connexion réussie!" -ForegroundColor Green
} else {
    Write-Host "✅ Déjà connecté à Stripe" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Démarrage du Webhook Forwarding" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📡 Forward vers: http://localhost:3000/payments/webhook" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  IMPORTANT:" -ForegroundColor Yellow
Write-Host "   1. Copiez le 'webhook signing secret' ci-dessous" -ForegroundColor Yellow
Write-Host "   2. Ajoutez-le dans .env:" -ForegroundColor Yellow
Write-Host "      STRIPE_WEBHOOK_SECRET=whsec_xxxxx" -ForegroundColor Yellow
Write-Host "   3. Redémarrez le serveur NestJS" -ForegroundColor Yellow
Write-Host "   4. Gardez ce terminal OUVERT" -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Démarrer le forwarding
& $STRIPE_PATH listen --forward-to localhost:3000/payments/webhook
