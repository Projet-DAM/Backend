# Script de nettoyage et compilation complète pour Android
Write-Host "🧹 Nettoyage du projet Android..." -ForegroundColor Cyan

# Aller dans le répertoire du projet
Set-Location $PSScriptRoot

# 1. Nettoyer Gradle
Write-Host "`n📦 Nettoyage Gradle..." -ForegroundColor Yellow
.\gradlew.bat clean --no-daemon

# 2. Supprimer les dossiers de build
Write-Host "`n🗑️  Suppression des dossiers build..." -ForegroundColor Yellow
if (Test-Path "app\build") {
    Remove-Item -Recurse -Force "app\build"
    Write-Host "  ✓ app\build supprimé" -ForegroundColor Green
}
if (Test-Path "build") {
    Remove-Item -Recurse -Force "build"
    Write-Host "  ✓ build supprimé" -ForegroundColor Green
}
if (Test-Path ".gradle") {
    Remove-Item -Recurse -Force ".gradle"
    Write-Host "  ✓ .gradle supprimé" -ForegroundColor Green
}

# 3. Supprimer les caches Gradle locaux
Write-Host "`n🗑️  Nettoyage des caches Gradle..." -ForegroundColor Yellow
.\gradlew.bat --stop --no-daemon

# 4. Recompiler le projet
Write-Host "`n🔨 Compilation du projet..." -ForegroundColor Yellow
.\gradlew.bat assembleDebug --no-daemon

Write-Host "`n✅ Nettoyage et compilation terminés !" -ForegroundColor Green
Write-Host "`n📱 Vous pouvez maintenant installer l'APK avec:" -ForegroundColor Cyan
Write-Host "   adb install app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor White

