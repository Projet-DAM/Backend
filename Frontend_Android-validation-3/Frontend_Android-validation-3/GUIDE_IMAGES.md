# Guide de gestion des images dans l'application Android

## 📋 Vue d'ensemble

L'application utilise **Jetpack Compose** avec **Coil** pour charger et afficher les images des tournois depuis le backend NestJS.

## 🔧 Configuration

### 1. URL de base du serveur

L'URL de base est configurée dans `ApiConfig.kt` :

```kotlin
const val BASE_URL = "http://10.0.2.2:3000/" // Émulateur Android
```

**Note** : Pour un appareil physique, utilisez l'IP de votre machine au lieu de `10.0.2.2`.

### 2. Format des images du backend

Le backend envoie des chemins relatifs dans le champ `image`, par exemple :
```json
{
  "image": "/uploads/tournois/tournoi-1762646491834-253928140.png"
}
```

## 🛠️ Fonctionnalités implémentées

### 1. Construction automatique de l'URL complète

La fonction `ImageUtils.buildImageUrl()` construit automatiquement l'URL complète à partir du chemin relatif :

```kotlin
// Exemple : "/uploads/tournois/image.png"
// Résultat : "http://10.0.2.2:3000/uploads/tournois/image.png"
```

### 2. Composant réutilisable `TournoiImage`

Le composant `TournoiImage` gère automatiquement :
- ✅ **Placeholder pendant le chargement** : Affiche un indicateur de chargement avec un dégradé orange
- ✅ **Image d'erreur** : Affiche une icône de sport si le chargement échoue
- ✅ **Pas d'image** : Affiche un placeholder avec le nom du sport

### 3. Gestion des états d'image

Le composant gère trois états :
1. **Loading** : Affiche un `CircularProgressIndicator` orange
2. **Error** : Affiche une icône de sport avec le nom du sport
3. **Success** : Affiche l'image chargée

## 📁 Structure des fichiers

```
app/src/main/java/com/example/dam_front/
├── utils/
│   └── ImageUtils.kt              # Utilitaires pour construire les URLs d'images
├── ui/
│   ├── components/
│   │   └── TournoiImage.kt        # Composant réutilisable pour afficher les images
│   └── screens/
│       ├── TournoisListScreen.kt  # Liste des tournois
│       └── TournoiDetailScreen.kt # Détail d'un tournoi
└── config/
    └── ApiConfig.kt               # Configuration de l'URL de base
```

## 💻 Utilisation

### Dans une liste de tournois

```kotlin
@Composable
fun TournoiCard(tournoi: Tournoi) {
    Card {
        // Afficher l'image du tournoi
        TournoiImage(
            imagePath = tournoi.image,
            contentDescription = tournoi.nom,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop,
            showSportName = true,
            sportName = tournoi.sport
        )
        
        // Autres éléments de la carte...
    }
}
```

### Dans une page de détail

```kotlin
@Composable
fun TournoiDetailScreen(tournoi: Tournoi) {
    Column {
        // Image en en-tête
        TournoiImage(
            imagePath = tournoi.image,
            contentDescription = tournoi.nom,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentScale = ContentScale.Crop
        )
        
        // Autres informations...
    }
}
```

## 🔍 Exemples de chemins d'images gérés

| Chemin reçu | URL complète construite |
|-------------|------------------------|
| `/uploads/tournois/image.png` | `http://10.0.2.2:3000/uploads/tournois/image.png` |
| `uploads/tournois/image.png` | `http://10.0.2.2:3000/uploads/tournois/image.png` |
| `http://example.com/image.png` | `http://example.com/image.png` (URL absolue conservée) |
| `null` ou `""` | `null` (affiche un placeholder) |

## 🐛 Dépannage

### Les images ne s'affichent pas

1. **Vérifier l'URL de base** :
   - Ouvrir `ApiConfig.kt`
   - Vérifier que `BASE_URL` correspond à votre environnement

2. **Vérifier les logs** :
   - Ouvrir Logcat dans Android Studio
   - Filtrer par `TournoiCard` ou `TournoiDetailScreen`
   - Vérifier les URLs construites

3. **Vérifier les permissions** :
   - Le `AndroidManifest.xml` doit contenir :
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
   ```

4. **Vérifier le serveur** :
   - Vérifier que le serveur NestJS est démarré
   - Vérifier que les images sont accessibles via l'URL complète dans un navigateur

### Les images s'affichent en placeholder

- Vérifier que le chemin de l'image est correct dans la réponse JSON
- Vérifier que le serveur sert correctement les fichiers statiques
- Vérifier les logs pour voir l'URL complète construite

## 📚 Dépendances

Les dépendances nécessaires sont déjà configurées dans `build.gradle.kts` :

```kotlin
// Coil pour le chargement d'images
implementation("io.coil-kt:coil-compose:2.5.0")
```

## ✅ Fonctionnalités

- ✅ Construction automatique de l'URL complète
- ✅ Placeholder pendant le chargement
- ✅ Image d'erreur si le chargement échoue
- ✅ Placeholder si pas d'image
- ✅ Affichage du nom du sport dans le placeholder
- ✅ Gestion des URL absolues et relatives
- ✅ Cache automatique des images avec Coil
- ✅ Logs de débogage pour le diagnostic

## 🚀 Prochaines étapes

Pour améliorer encore la gestion des images :

1. Ajouter la compression des images côté serveur
2. Implémenter le lazy loading pour les listes longues
3. Ajouter un système de cache personnalisé
4. Implémenter le préchargement des images

