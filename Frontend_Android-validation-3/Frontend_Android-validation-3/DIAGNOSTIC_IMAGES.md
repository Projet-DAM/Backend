# 🔍 Diagnostic du problème d'images

## Problème : Les images ne se chargent pas

## ✅ Vérifications à faire IMMÉDIATEMENT

### 1. Vérifier les logs dans Logcat

**Étape 1** : Ouvrir Logcat dans Android Studio
**Étape 2** : Filtrer par : `TournoiImage` ou `ImageUtils` ou `TournoisViewModel`
**Étape 3** : Rechercher ces messages :

#### ✅ Si vous voyez ces logs, l'URL est construite correctement :
```
ImageUtils: ✅ URL finale construite: http://10.0.2.2:3000/uploads/tournois/image.png
TournoiImage: URL complète construite: http://10.0.2.2:3000/uploads/tournois/image.png
```

#### ❌ Si vous voyez ces logs, il y a un problème :
```
ImageUtils: Image path est null ou vide
TournoiImage: ⚠️ Pas d'image path fourni
```
→ **Le backend ne renvoie pas le champ `image`**

#### ❌ Si vous voyez cette erreur :
```
TournoiImage: ❌ Erreur chargement image: http://10.0.2.2:3000/uploads/tournois/image.png
TournoiImage: Erreur: UnknownHostException
```
→ **Le serveur n'est pas accessible depuis l'émulateur**

#### ❌ Si vous voyez cette erreur :
```
TournoiImage: ❌ Erreur chargement image: http://10.0.2.2:3000/uploads/tournois/image.png
TournoiImage: Erreur: 404 Not Found
```
→ **Le fichier n'existe pas sur le serveur ou le chemin est incorrect**

### 2. Vérifier que le backend renvoie le champ `image`

Dans les logs, vous devriez voir :
```
TournoisViewModel:     - Image path: /uploads/tournois/tournoi-1762646491834-253928140.png
```

**Si vous voyez `Image path: null` ou `Image path: aucune`** :
- Le backend ne renvoie pas le champ `image` dans le JSON
- Vérifier la réponse JSON du backend dans Postman ou Swagger

### 3. Tester l'URL directement

**Sur votre PC** (où le serveur tourne) :
1. Ouvrir un navigateur
2. Aller à : `http://localhost:3000/uploads/tournois/tournoi-1762646491834-253928140.png`
3. L'image doit s'afficher

**Si l'image ne s'affiche pas dans le navigateur** :
- Le problème vient du serveur, pas de l'application Android
- Vérifier que le serveur NestJS sert bien les fichiers depuis `/uploads/`

**Si l'image s'affiche dans le navigateur mais pas dans l'app** :
- Le problème vient de la configuration Android
- Vérifier `AndroidManifest.xml` (usesCleartextTraffic="true")
- Vérifier l'URL dans `ApiConfig.kt` (http://10.0.2.2:3000 pour l'émulateur)

### 4. Vérifier la configuration du serveur NestJS

Le serveur doit servir les fichiers statiques. Dans `main.ts` :

```typescript
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { NestExpressApplication } from '@nestjs/platform-express';
import { join } from 'path';

async function bootstrap() {
  const app = await NestFactory.create<NestExpressApplication>(AppModule);
  
  // Servir les fichiers statiques depuis le dossier uploads
  app.useStaticAssets(join(__dirname, '..', 'uploads'), {
    prefix: '/uploads/',
  });
  
  await app.listen(3000);
}
bootstrap();
```

### 5. Vérifier le format JSON du backend

Le backend doit renvoyer :
```json
{
  "_id": "...",
  "nom": "Tournoi Test",
  "image": "/uploads/tournois/tournoi-1762646491834-253928140.png",
  ...
}
```

**PAS** :
```json
{
  "imageUrl": "/uploads/tournois/image.png"  // ❌ Mauvais nom de champ
}
```

## 🔧 Solutions aux problèmes courants

### Solution 1 : Le champ `image` est vide ou null

**Cause** : Le backend ne renvoie pas le champ `image` ou il est vide

**Solution** :
1. Vérifier la réponse JSON dans Postman/Swagger
2. Vérifier que le fichier est bien uploadé sur le serveur
3. Vérifier que le chemin est correct dans la base de données

### Solution 2 : Erreur de connexion (UnknownHostException)

**Cause** : L'émulateur ne peut pas accéder au serveur

**Solution** :
1. Vérifier que le serveur NestJS est démarré
2. Vérifier l'URL dans `ApiConfig.kt` : `http://10.0.2.2:3000`
3. Tester la connexion : `adb shell ping 10.0.2.2`

### Solution 3 : Erreur 404 (Not Found)

**Cause** : Le fichier n'existe pas ou le chemin est incorrect

**Solution** :
1. Vérifier que le fichier existe dans `uploads/tournois/` sur le serveur
2. Vérifier que le serveur sert bien les fichiers statiques
3. Tester l'URL dans un navigateur

### Solution 4 : Cleartext traffic not permitted

**Cause** : Android bloque les connexions HTTP non sécurisées

**Solution** :
Vérifier que `AndroidManifest.xml` contient :
```xml
<application
    android:usesCleartextTraffic="true"
    ...>
```

## 🧪 Test rapide

Pour tester rapidement si le problème vient de l'URL ou du serveur :

1. **Modifier temporairement `ImageUtils.kt`** :
```kotlin
fun buildImageUrl(imagePath: String?): String? {
    // TEST avec une image publique
    return "https://picsum.photos/400/300"
}
```

2. **Recompiler et tester** :
- Si l'image s'affiche : Le problème vient de l'URL ou du serveur
- Si l'image ne s'affiche pas : Le problème vient de Coil ou de la configuration Android

## 📋 Checklist de diagnostic

- [ ] Le serveur NestJS est démarré
- [ ] L'URL de base dans `ApiConfig.kt` est correcte (`http://10.0.2.2:3000`)
- [ ] Le backend renvoie le champ `image` dans le JSON
- [ ] Le fichier image existe sur le serveur
- [ ] L'URL est accessible depuis un navigateur (`http://localhost:3000/uploads/...`)
- [ ] `AndroidManifest.xml` contient `usesCleartextTraffic="true"`
- [ ] Les logs montrent l'URL construite correctement
- [ ] Les logs montrent une erreur spécifique (si applicable)

## 🚀 Prochaines étapes

1. **Copier les logs de Logcat** (filtre : `TournoiImage`, `ImageUtils`)
2. **Vérifier l'URL construite** dans les logs
3. **Tester l'URL dans un navigateur** pour confirmer qu'elle fonctionne
4. **Vérifier la réponse JSON du backend** pour confirmer que le champ `image` est présent

## 📞 Informations à fournir pour le support

Si le problème persiste, fournir :
1. Les logs complets de Logcat (filtre : `TournoiImage`, `ImageUtils`, `TournoisViewModel`)
2. Un exemple de réponse JSON du backend
3. L'URL testée dans le navigateur (fonctionne ou pas ?)
4. La configuration du serveur NestJS (servir les fichiers statiques)

