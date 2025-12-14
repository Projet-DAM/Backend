# Script PowerShell pour démarrer Stripe CLI et afficher le webhook secret
# Utilisation: .\start-stripe-webhook.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Démarrage de Stripe CLI Webhook" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Vérifier si Stripe CLI est installé
try {
    $stripeVersion = stripe --version 2>&1
    Write-Host "✅ Stripe CLI détecté: $stripeVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Stripe CLI n'est pas installé!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Pour installer Stripe CLI:" -ForegroundColor Yellow
    Write-Host "1. Installer Scoop (si pas déjà fait):" -ForegroundColor Yellow
    Write-Host "   Set-ExecutionPolicy RemoteSigned -Scope CurrentUser" -ForegroundColor White
    Write-Host "   irm get.scoop.sh | iex" -ForegroundColor White
    Write-Host ""
    Write-Host "2. Installer Stripe CLI:" -ForegroundColor Yellow
    Write-Host "   scoop install stripe" -ForegroundColor White
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "🔐 Vérification de l'authentification Stripe..." -ForegroundColor Yellow

# Vérifier si l'utilisateur est connecté
$loginStatus = stripe config --list 2>&1
if ($loginStatus -match "not logged in" -or $loginStatus -match "No account") {
    Write-Host "⚠️  Vous n'êtes pas connecté à Stripe" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Exécution de 'stripe login'..." -ForegroundColor Cyan
    stripe login
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Échec de la connexion à Stripe" -ForegroundColor Red
        exit 1
    }
}

Write-Host "✅ Authentification Stripe OK" -ForegroundColor Green
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Démarrage du forwarding de webhooks" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📡 Forward des webhooks vers: http://localhost:3000/payments/webhook" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  IMPORTANT:" -ForegroundColor Yellow
Write-Host "   1. Copiez le 'webhook signing secret' affiché ci-dessous" -ForegroundColor Yellow
Write-Host "   2. Mettez-le dans le fichier .env:" -ForegroundColor Yellow
Write-Host "      STRIPE_WEBHOOK_SECRET=whsec_xxxxx" -ForegroundColor Yellow
Write-Host "   3. Redémarrez le serveur NestJS (npm run start:dev)" -ForegroundColor Yellow
Write-Host "   4. Gardez ce terminal OUVERT pendant le développement" -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Démarrer le forwarding
stripe listen --forward-to localhost:3000/payments/webhook
