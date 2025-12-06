# Script PowerShell pour configurer le webhook Stripe
# Usage: .\configure-webhook.ps1

Write-Host "🚀 Configuration du Webhook Stripe pour WhatsApp" -ForegroundColor Green
Write-Host ""

# Vérifier si Stripe CLI est installé
Write-Host "📋 Étape 1/4 : Vérification de Stripe CLI..." -ForegroundColor Cyan
$stripePath = Get-Command stripe -ErrorAction SilentlyContinue

if (-not $stripePath) {
    Write-Host "❌ Stripe CLI n'est pas installé!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Installation rapide :" -ForegroundColor Yellow
    Write-Host "  Option 1 (Recommandé) : winget install stripe"
    Write-Host "  Option 2 : Téléchargez depuis https://github.com/stripe/stripe-cli/releases/latest"
    Write-Host ""
    exit 1
}

Write-Host "✅ Stripe CLI trouvé!" -ForegroundColor Green
Write-Host ""

# Login Stripe
Write-Host "🔑 Étape 2/4 : Connexion à Stripe..." -ForegroundColor Cyan
Write-Host "Une fenêtre de navigateur va s'ouvrir pour vous authentifier."
Write-Host ""
stripe login

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Échec de la connexion à Stripe" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "✅ Connecté à Stripe!" -ForegroundColor Green
Write-Host ""

# Démarrer le forwarding
Write-Host "🎯 Étape 3/4 : Démarrage du forwarding des webhooks..." -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  IMPORTANT : Ce terminal doit rester ouvert!" -ForegroundColor Yellow
Write-Host "⚠️  Copiez le 'webhook signing secret' qui s'affiche ci-dessous" -ForegroundColor Yellow
Write-Host "⚠️  Ajoutez-le dans votre fichier .env comme:" -ForegroundColor Yellow
Write-Host "    STRIPE_WEBHOOK_SECRET=whsec_xxxxx" -ForegroundColor Yellow
Write-Host ""
Write-Host "Appuyez sur Ctrl+C pour arrêter le forwarding." -ForegroundColor Gray
Write-Host ""
Write-Host "=".PadRight(70, "=") -ForegroundColor Gray
Write-Host ""

stripe listen --forward-to http://localhost:3000/payments/webhook
