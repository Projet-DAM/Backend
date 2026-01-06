# Script de test pour le webhook Stripe
# Usage: .\test-stripe-webhook.ps1

Write-Host "🧪 Test du Webhook Stripe" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$baseUrl = "http://localhost:3000"
$webhookUrl = "$baseUrl/payments/webhook"

# Vérifier que le serveur est en cours d'exécution
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
Write-Host "🔧 Options de test disponibles:" -ForegroundColor Cyan
Write-Host "1. Tester avec Stripe CLI (Recommandé)"
Write-Host "2. Tester avec un événement simulé (Sans vérification de signature)"
Write-Host "3. Afficher les informations de configuration"
Write-Host ""

$choice = Read-Host "Choisissez une option (1-3)"

switch ($choice) {
    "1" {
        Write-Host ""
        Write-Host "📦 Test avec Stripe CLI" -ForegroundColor Cyan
        Write-Host "================================" -ForegroundColor Cyan
        Write-Host ""
        
        # Vérifier si Stripe CLI est installé
        try {
            $stripeVersion = stripe --version 2>&1
            Write-Host "✅ Stripe CLI installé: $stripeVersion" -ForegroundColor Green
        } catch {
            Write-Host "❌ Stripe CLI n'est pas installé" -ForegroundColor Red
            Write-Host ""
            Write-Host "Pour installer Stripe CLI:" -ForegroundColor Yellow
            Write-Host "1. Télécharger depuis: https://stripe.com/docs/stripe-cli" -ForegroundColor Yellow
            Write-Host "2. Ou avec Chocolatey: choco install stripe-cli" -ForegroundColor Yellow
            exit 1
        }
        
        Write-Host ""
        Write-Host "🎯 Étapes à suivre:" -ForegroundColor Cyan
        Write-Host "1. Ouvrez un nouveau terminal PowerShell" -ForegroundColor White
        Write-Host "2. Exécutez: stripe listen --forward-to $webhookUrl" -ForegroundColor Yellow
        Write-Host "3. Copiez le webhook secret (whsec_...) affiché" -ForegroundColor White
        Write-Host "4. Ajoutez-le dans .env: STRIPE_WEBHOOK_SECRET=whsec_..." -ForegroundColor Yellow
        Write-Host "5. Redémarrez le serveur backend" -ForegroundColor White
        Write-Host "6. Dans un autre terminal, testez: stripe trigger payment_intent.succeeded" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Appuyez sur Entrée pour continuer..." -ForegroundColor Gray
        Read-Host
    }
    
    "2" {
        Write-Host ""
        Write-Host "🧪 Test avec événement simulé" -ForegroundColor Cyan
        Write-Host "================================" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "⚠️  ATTENTION: Ce test ne vérifie PAS la signature Stripe" -ForegroundColor Yellow
        Write-Host "   Il fonctionne uniquement si STRIPE_WEBHOOK_SECRET n'est pas configuré" -ForegroundColor Yellow
        Write-Host ""
        
        $subscriptionId = Read-Host "Entrez un ID d'abonnement existant (ou laissez vide pour test_sub_123)"
        if ([string]::IsNullOrWhiteSpace($subscriptionId)) {
            $subscriptionId = "test_sub_123"
        }
        
        $eventData = @{
            type = "payment_intent.succeeded"
            data = @{
                object = @{
                    id = "pi_test_$(Get-Random -Minimum 1000 -Maximum 9999)"
                    amount = 7000
                    currency = "tnd"
                    status = "succeeded"
                    metadata = @{
                        subscriptionId = $subscriptionId
                    }
                }
            }
        } | ConvertTo-Json -Depth 10
        
        Write-Host ""
        Write-Host "📤 Envoi de l'événement au webhook..." -ForegroundColor Yellow
        Write-Host "URL: $webhookUrl" -ForegroundColor Gray
        Write-Host ""
        
        try {
            $headers = @{
                "Content-Type" = "application/json"
                "stripe-signature" = "test_signature"
            }
            
            $response = Invoke-WebRequest -Uri $webhookUrl -Method POST -Body $eventData -Headers $headers -ErrorAction Stop
            
            Write-Host "✅ Webhook appelé avec succès!" -ForegroundColor Green
            Write-Host "Status Code: $($response.StatusCode)" -ForegroundColor Gray
            Write-Host "Response: $($response.Content)" -ForegroundColor Gray
            Write-Host ""
            Write-Host "🔍 Vérifiez les logs du serveur pour voir le traitement" -ForegroundColor Cyan
        } catch {
            Write-Host "❌ Erreur lors de l'appel du webhook" -ForegroundColor Red
            Write-Host "Status Code: $($_.Exception.Response.StatusCode.Value__)" -ForegroundColor Red
            Write-Host "Message: $($_.Exception.Message)" -ForegroundColor Red
            
            if ($_.Exception.Response.StatusCode.Value__ -eq 400) {
                Write-Host ""
                Write-Host "💡 Cela peut être dû à:" -ForegroundColor Yellow
                Write-Host "   - STRIPE_WEBHOOK_SECRET est configuré (vérification de signature activée)" -ForegroundColor Yellow
                Write-Host "   - Utilisez l'option 1 (Stripe CLI) pour un test complet" -ForegroundColor Yellow
            }
        }
    }
    
    "3" {
        Write-Host ""
        Write-Host "📋 Informations de configuration" -ForegroundColor Cyan
        Write-Host "================================" -ForegroundColor Cyan
        Write-Host ""
        
        # Lire le fichier .env
        $envFile = ".env"
        if (Test-Path $envFile) {
            Write-Host "✅ Fichier .env trouvé" -ForegroundColor Green
            Write-Host ""
            
            $envContent = Get-Content $envFile
            
            # Vérifier STRIPE_SECRET_KEY
            $stripeKey = $envContent | Select-String "STRIPE_SECRET_KEY"
            if ($stripeKey) {
                $keyValue = ($stripeKey -split "=")[1]
                if ($keyValue -match "^sk_test_") {
                    Write-Host "✅ STRIPE_SECRET_KEY: Configurée (Mode Test)" -ForegroundColor Green
                } elseif ($keyValue -match "^sk_live_") {
                    Write-Host "✅ STRIPE_SECRET_KEY: Configurée (Mode Production)" -ForegroundColor Yellow
                } else {
                    Write-Host "⚠️  STRIPE_SECRET_KEY: Format invalide" -ForegroundColor Yellow
                }
            } else {
                Write-Host "❌ STRIPE_SECRET_KEY: Non configurée" -ForegroundColor Red
            }
            
            # Vérifier STRIPE_WEBHOOK_SECRET
            $webhookSecret = $envContent | Select-String "STRIPE_WEBHOOK_SECRET"
            if ($webhookSecret) {
                $secretValue = ($webhookSecret -split "=")[1]
                if ($secretValue -match "^whsec_") {
                    Write-Host "✅ STRIPE_WEBHOOK_SECRET: Configurée" -ForegroundColor Green
                } else {
                    Write-Host "⚠️  STRIPE_WEBHOOK_SECRET: Format invalide (doit commencer par whsec_)" -ForegroundColor Yellow
                }
            } else {
                Write-Host "⚠️  STRIPE_WEBHOOK_SECRET: Non configurée" -ForegroundColor Yellow
                Write-Host "   Les webhooks fonctionneront sans vérification de signature (développement uniquement)" -ForegroundColor Gray
            }
            
            # Vérifier STRIPE_PUBLISHABLE_KEY
            $pubKey = $envContent | Select-String "STRIPE_PUBLISHABLE_KEY"
            if ($pubKey) {
                Write-Host "✅ STRIPE_PUBLISHABLE_KEY: Configurée" -ForegroundColor Green
            } else {
                Write-Host "⚠️  STRIPE_PUBLISHABLE_KEY: Non configurée (nécessaire pour le frontend)" -ForegroundColor Yellow
            }
            
        } else {
            Write-Host "❌ Fichier .env non trouvé" -ForegroundColor Red
        }
        
        Write-Host ""
        Write-Host "🌐 Endpoints disponibles:" -ForegroundColor Cyan
        Write-Host "- POST $baseUrl/payments/create-intent (Créer un PaymentIntent)" -ForegroundColor Gray
        Write-Host "- POST $baseUrl/payments/confirm (Confirmer un paiement)" -ForegroundColor Gray
        Write-Host "- POST $baseUrl/payments/webhook (Webhook Stripe - Public)" -ForegroundColor Gray
        Write-Host ""
        Write-Host "📚 Documentation Swagger: $baseUrl/api" -ForegroundColor Cyan
    }
    
    default {
        Write-Host "❌ Option invalide" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "✅ Test terminé" -ForegroundColor Green
