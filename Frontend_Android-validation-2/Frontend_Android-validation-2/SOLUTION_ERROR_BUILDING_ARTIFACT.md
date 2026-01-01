# 🔧 Solution pour "Error Building Artifact"

## ✅ Étape 1: Nettoyer le projet dans Android Studio

1. **Dans Android Studio:**
   - Menu: `Build` → `Clean Project`
   - Attendez la fin du nettoyage

2. **Invalider les caches:**
   - Menu: `File` → `Invalidate Caches...`
   - Cochez toutes les options:
     - ✅ Clear file system cache and Local History
     - ✅ Clear downloaded shared indexes
   - Cliquez sur `Invalidate and Restart`
   - Attendez le redémarrage d'Android Studio

## ✅ Étape 2: Vérifier la configuration du projet

1. **Vérifier le JDK:**
   - Menu: `File` → `Project Structure` → `SDK Location`
   - Vérifiez que le JDK est correctement configuré (JDK 17 recommandé)

2. **Vérifier le SDK Android:**
   - Menu: `File` → `Project Structure` → `SDK Location`
   - Vérifiez que le SDK Android est installé (SDK 34)

3. **Vérifier Gradle:**
   - Menu: `File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Gradle`
   - Vérifiez que Gradle est configuré correctement
   - Utilisez le wrapper Gradle: `Use Gradle from: 'gradle-wrapper.properties' file`

## ✅ Étape 3: Rebuild le projet

1. **Rebuild:**
   - Menu: `Build` → `Rebuild Project`
   - Attendez la fin de la compilation

2. **Sync Gradle:**
   - Menu: `File` → `Sync Project with Gradle Files`
   - Attendez la fin de la synchronisation

## ✅ Étape 4: Installer l'APK manuellement (si nécessaire)

Si Android Studio ne peut toujours pas installer l'APK, installez-le manuellement:

1. **Trouver l'APK:**
   - L'APK se trouve dans: `app\build\outputs\apk\debug\app-debug.apk`

2. **Installer via ADB:**
   ```powershell
   cd Frontend_Android
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

   Ou installez-le manuellement sur l'émulateur:
   - Glissez-déposez l'APK sur l'émulateur
   - Ou utilisez l'explorateur de fichiers de l'émulateur

## ✅ Étape 5: Vérifier la configuration de l'émulateur

1. **Vérifier que l'émulateur est démarré:**
   - Ouvrez l'émulateur Android
   - Vérifiez qu'il est visible dans Android Studio (Device Manager)

2. **Vérifier la connexion ADB:**
   ```powershell
   adb devices
   ```
   - Vous devriez voir votre émulateur listé

## ✅ Étape 6: Désinstaller l'ancienne version (si nécessaire)

Si l'application est déjà installée mais ne se met pas à jour:

```powershell
adb uninstall com.example.dam_front
```

Puis réinstallez l'application.

## 🔍 Vérification des logs

Pour voir les erreurs détaillées:

1. **Dans Android Studio:**
   - Ouvrez la fenêtre `Build` (en bas)
   - Regardez les erreurs détaillées

2. **Dans Logcat:**
   - Ouvrez `Logcat` (en bas)
   - Filtrez par `MainScreen` ou `TournoisListScreen`
   - Vérifiez les logs lors du lancement de l'app

## 📱 Résultat attendu

Après ces étapes, vous devriez:
- ✅ Pouvoir compiler le projet sans erreur
- ✅ Pouvoir installer l'APK sur l'émulateur
- ✅ Voir l'application s'afficher avec le bouton "Actualiser" et la liste des tournois

## 🚨 Si le problème persiste

1. **Vérifiez les logs Gradle:**
   - Ouvrez `gradle\wrapper\gradle-wrapper.properties`
   - Vérifiez que la version de Gradle est correcte

2. **Vérifiez les dépendances:**
   - Ouvrez `gradle\libs.versions.toml`
   - Vérifiez que toutes les versions sont compatibles

3. **Réinstallez Android Studio:**
   - En dernier recours, réinstallez Android Studio

## 📞 Commandes utiles

```powershell
# Nettoyer le projet
cd Frontend_Android
.\gradlew.bat clean

# Compiler l'APK
.\gradlew.bat assembleDebug

# Installer l'APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Vérifier les devices connectés
adb devices

# Désinstaller l'app
adb uninstall com.example.dam_front
```

