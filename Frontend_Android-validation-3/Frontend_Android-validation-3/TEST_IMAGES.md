# 🔍 Guide de débogage des images

## Problème : Les images ne se chargent pas

Si les images ne s'affichent pas dans l'application, suivez ces étapes pour diagnostiquer le problème.

## 📋 Étapes de diagnostic

### 1. Vérifier les logs dans Logcat

1. Ouvrir **Logcat** dans Android Studio
2. Filtrer par les tags suivants :
   - `TournoiImage` - Pour voir les logs du composant d'image
   - `ImageUtils` - Pour voir la construction des URLs
   - `TournoisViewModel` - Pour voir les données reçues du backend

### 2. Vérifier les données reçues

Dans les logs, vous devriez voir :
```
TournoisViewModel: 📋 Détails des tournois:
TournoisViewModel:   Tournoi #1:
TournoisViewModel:     - Image path: /uploads/tournois/image.png
TournoisViewModel:     - Image URL complète: http://10.0.2.2:3000/uploads/tournois/image.png
```

### 3. Vérifier la construction de l'URL

Les logs devraient montrer :
```
ImageUtils: Construction URL relative (commence par /):
ImageUtils:   Base URL: http://10.0.2.2:3000/
ImageUtils:   Path: /uploads/tournois/image.png
ImageUtils:   URL complète: http://10.0.2.2:3000/uploads/tournois/image.png
ImageUtils: ✅ URL finale construite: http://10.0.2.2:3000/uploads/tournois/image.png
```

### 4. Vérifier le chargement de l'image

Les logs devraient montrer :
```
TournoiImage: === Début chargement image ===
TournoiImage: Image path reçu: /uploads/tournois/image.png
TournoiImage: URL complète construite: http://10.0.2.2:3000/uploads/tournois/image.png
TournoiImage: ⏳ Chargement en cours: http://10.0.2.2:3000/uploads/tournois/image.png
```

### 5. Vérifier les erreurs

Si une erreur se produit, vous verrez :
```
TournoiImage: ❌ Erreur chargement image: http://10.0.2.2:3000/uploads/tournois/image.png
TournoiImage: Erreur: [message d'erreur]
TournoiImage: Type: [type d'erreur]
```

## 🔧 Solutions aux problèmes courants

### Problème 1 : L'URL n'est pas construite correctement

**Symptôme** : Les logs montrent `Image path: null` ou `Image URL complète: null`

**Solution** :
1. Vérifier que le backend renvoie bien le champ `image` dans le JSON
2. Vérifier que le modèle `Tournoi` a bien le champ `image` avec `@SerializedName("image")`
3. Vérifier les logs du ViewModel pour voir les données reçues

### Problème 2 : Erreur de connexion

**Symptôme** : Les logs montrent `UnknownHostException` ou `ConnectException`

**Solution** :
1. Vérifier que le serveur NestJS est démarré
2. Vérifier l'URL de base dans `ApiConfig.kt`
3. Pour l'émulateur, utiliser `http://10.0.2.2:3000`
4. Pour un appareil physique, utiliser l'IP locale de votre machine (ex: `http://192.168.1.100:3000`)

### Problème 3 : Erreur 404 (Not Found)

**Symptôme** : Les logs montrent une erreur 404

**Solution** :
1. Vérifier que le serveur sert bien les fichiers statiques depuis `/uploads/tournois/`
2. Vérifier que le fichier existe sur le serveur
3. Tester l'URL dans un navigateur : `http://localhost:3000/uploads/tournois/image.png`

### Problème 4 : Erreur de permission (Cleartext traffic)

**Symptôme** : Les logs montrent `Cleartext HTTP traffic not permitted`

**Solution** :
1. Vérifier que `AndroidManifest.xml` contient :
   ```xml
   <application
       android:usesCleartextTraffic="true"
       ...>
   ```
2. Si le problème persiste, ajouter un réseau de sécurité (voir ci-dessous)

### Problème 5 : Timeout

**Symptôme** : Les logs montrent `SocketTimeoutException`

**Solution** :
1. Vérifier la connexion réseau
2. Vérifier que le serveur répond rapidement
3. Augmenter le timeout dans Coil (déjà configuré à 30 secondes)

## 🧪 Test manuel de l'URL

Pour tester si l'URL fonctionne :

1. **Sur votre PC** (où le serveur tourne) :
   - Ouvrir un navigateur
   - Aller à : `http://localhost:3000/uploads/tournois/tournoi-1762646491834-253928140.png`
   - L'image devrait s'afficher

2. **Depuis l'émulateur Android** :
   - Ouvrir le navigateur de l'émulateur
   - Aller à : `http://10.0.2.2:3000/uploads/tournois/tournoi-1762646491834-253928140.png`
   - L'image devrait s'afficher

## 🔍 Vérifications supplémentaires

### Vérifier la configuration du serveur NestJS

Le serveur doit servir les fichiers statiques. Vérifier dans `main.ts` :

```typescript
app.use('/uploads', express.static('uploads'));
```

### Vérifier le format du JSON

Le backend doit renvoyer :
```json
{
  "image": "/uploads/tournois/image.png"
}
```

Et non :
```json
{
  "imageUrl": "/uploads/tournois/image.png"
}
```

### Vérifier le modèle Tournoi

Le modèle doit avoir :
```kotlin
@SerializedName("image")
val image: String? = null
```

## 📱 Test avec une URL directe

Pour tester avec une URL directe, modifiez temporairement `ImageUtils.kt` :

```kotlin
fun buildImageUrl(imagePath: String?): String? {
    // TEST : Utiliser une URL de test
    return "https://picsum.photos/400/300"
}
```

Si cette URL fonctionne, le problème vient de la construction de l'URL ou du serveur.

## 🚀 Commandes utiles

### Vérifier la connectivité depuis l'émulateur

```bash
adb shell
ping 10.0.2.2
```

### Vérifier les logs en temps réel

```bash
adb logcat -s TournoiImage ImageUtils TournoisViewModel
```

### Nettoyer le cache de l'application

```bash
adb shell pm clear com.example.dam_front
```

## 📞 Support

Si le problème persiste après avoir suivi ces étapes :

1. Copier tous les logs de Logcat (filtre : `TournoiImage`, `ImageUtils`, `TournoisViewModel`)
2. Vérifier que l'URL construite est accessible depuis un navigateur
3. Vérifier la configuration du serveur NestJS pour servir les fichiers statiques

