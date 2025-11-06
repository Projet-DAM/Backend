# 📚 Guide : Format des ObjectIds MongoDB

## ❌ Erreur courante : "academyId must be a mongodb id"

Cette erreur se produit lorsque l'ID fourni n'est pas un ObjectId MongoDB valide.

## 🔍 Format d'un ObjectId MongoDB

Un ObjectId MongoDB doit :
- ✅ Être exactement **24 caractères**
- ✅ Contenir uniquement des caractères **hexadécimaux** (0-9, a-f, A-F)
- ✅ Être une chaîne de caractères (string)

### Exemples

**✅ Valides :**
```
690cd9998d614e72c9b1ab55
507f1f77bcf86cd799439011
65b1f0a1b2c3d4e5f6a7b8c9
```

**❌ Invalides :**
```
690cd9998d614e72c9b1ab55y  // 25 caractères (trop long)
690cd9998d614e72c9b1ab5    // 23 caractères (trop court)
690cd9998d614e72c9b1ab5g   // Contient 'g' (pas hexadécimal)
690cd999-8d61-4e72-c9b1-ab55  // Contient des tirets
```

## 🔧 Comment obtenir le bon ObjectId

### Méthode 1 : Depuis MongoDB Compass

1. Ouvrez MongoDB Compass
2. Connectez-vous à votre base de données
3. Naviguez vers la collection `users`
4. Trouvez le document de l'académie (role: "academie")
5. **Copiez l'ID exactement tel qu'il apparaît** (sans modifications)

### Méthode 2 : Depuis l'API

1. Utilisez `GET /users` (si vous êtes ADMIN ou ACADEMIE)
2. Trouvez l'utilisateur avec `role: "academie"`
3. Copiez le champ `id` de la réponse

### Méthode 3 : Vérifier dans la réponse de création

Si vous venez de créer l'académie via `POST /auth/register`, l'ID est dans la réponse :
```json
{
  "access_token": "...",
  "user": {
    "id": "690cd9998d614e72c9b1ab55",  // ← Copiez cet ID
    "email": "clubafricain@example.com",
    "role": "academie"
  }
}
```

## ⚠️ Erreurs courantes à éviter

1. **Ajouter des caractères** : Ne pas ajouter de lettres ou chiffres à la fin
2. **Supprimer des caractères** : Ne pas tronquer l'ID
3. **Copier avec des espaces** : Vérifier qu'il n'y a pas d'espaces avant/après
4. **Utiliser un ID d'un autre type** : S'assurer que c'est bien un ObjectId MongoDB

## ✅ Vérification rapide

Avant d'utiliser un ID dans une requête, vérifiez :

1. **Longueur** : Exactement 24 caractères
2. **Format** : Uniquement 0-9, a-f, A-F
3. **Source** : Copié directement depuis MongoDB ou la réponse API

## 🧪 Test rapide

Pour tester si un ID est valide, vous pouvez utiliser cette regex :
```javascript
/^[0-9a-fA-F]{24}$/.test("690cd9998d614e72c9b1ab55")  // true
/^[0-9a-fA-F]{24}$/.test("690cd9998d614e72c9b1ab55y") // false
```

## 📝 Exemple de requête correcte

```json
{
  "name": "Mensuel",
  "description": "Accès mensuel",
  "type": "MONTHLY",
  "durationDays": 30,
  "price": 50,
  "discountPct": 0,
  "conditions": "Non remboursable",
  "academyId": "690cd9998d614e72c9b1ab55"
}
```

**Note :** Remplacez `690cd9998d614e72c9b1ab55` par l'ID réel de votre académie.

