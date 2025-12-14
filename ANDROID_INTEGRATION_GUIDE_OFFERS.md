# Guide d'Intégration Android : Offres Avancées (Niveaux, Quotas, Liste des Inscrits)

Ce document décrit les modifications nécessaires dans l'application Android pour supporter les nouvelles fonctionnalités backend : **Niveaux**, **Quotas (places restantes)** et **Liste des inscrits**.

## 1. Mise à jour des Modèles de Données

Vous devez mettre à jour vos data classes Kotlin pour correspondre à la nouvelle structure de l'API.

### Fichier : `response/OfferResponse.kt` (ou votre modèle `Offer`)

Ajoutez les champs pour les niveaux, la capacité et les infos calculées par le backend.

```kotlin
data class Offer(
    // Champs existants...
    val _id: String,
    val name: String,
    val price: Double,
    val description: String?,
    // ...

    // --- NOUVEAUX CHAMPS À AJOUTER ---
    val maxCapacity: Int? = null,        // Quota total (ex: 20)
    val levels: List<OfferLevel>? = null, // Liste des niveaux
    
    // Champs calculés par le backend (lecture seule)
    val subscribersCount: Int? = 0,      // Nombre d'inscrits actuels
    val remainingPlaces: Int? = null,    // Places restantes (ex: 8)
    val isFull: Boolean? = false         // Vrai si plus de place
)

data class OfferLevel(
    val name: String,
    val price: Double? = null,       // Prix spécifique si différent
    val sessionCount: Int? = null,
    val minAge: Int? = null,
    val maxAge: Int? = null
)
```

### Nouveau modèle : `response/SubscriberResponse.kt`

Pour la liste des inscrits (Vue Académie).

```kotlin
data class SubscriberResponse(
    val subscriptionId: String,
    val status: String, // ACTIVE, PENDING...
    val startDate: String,
    val endDate: String,
    val child: ChildSummary,
    val parent: ParentSummary
)

data class ChildSummary(
    val _id: String,
    val nom: String,
    val prenom: String,
    val photoProfil: String?
)

data class ParentSummary(
    val _id: String,
    val nom: String,
    val prenom: String,
    val email: String,
    val phoneNumber: String?
)
```

---

## 2. Mise à jour des Services API (Retrofit)

### Fichier : `network/ApiService.kt`

Ajoutez l'endpoint pour récupérer les inscrits.

```kotlin
interface ApiService {
    // ... existants

    // Récupérer la liste des abonnés pour une offre (Académie uniquement)
    @GET("offers/{id}/subscribers")
    suspend fun getOfferSubscribers(@Path("id") offerId: String): Response<List<SubscriberResponse>>
}
```

---

## 3. Modifications UI (Côté Parent)

### Affichage des "Places restantes"

Dans votre **Adapter** ou **Composable** qui affiche la carte de l'offre (`OfferItem`):

1.  **Badge de disponibilité** :
    *   Si `remainingPlaces` n'est pas null, affichez un texte : *"Il reste X places"*.
    *   Couleur suggérée : Orange si < 5 places, Vert sinon.
2.  **Offre complète** :
    *   Si `isFull == true` (ou `remainingPlaces == 0`), désactivez le bouton "S'abonner" et changez le texte en *"Complet"*.
    *   Grisez la carte pour indiquer l'indisponibilité.

**Exemple logique :**
```kotlin
if (offer.isFull == true) {
    statusText.text = "COMPLET"
    statusText.setTextColor(Color.RED)
    subscribeButton.isEnabled = false
} else if (offer.remainingPlaces != null) {
    statusText.text = "${offer.remainingPlaces} places restantes"
}
```

---

## 4. Modifications UI (Côté Académie)

### A. Création/Modification d'Offre (`CreateOfferScreen`)

Ajoutez des champs de saisie pour définir les quotas et niveaux :

1.  **Champ "Capacité Max"** (`EditText`, type number) : Envoyer dans le champ `maxCapacity` du JSON.
2.  **Section "Niveaux"** (Optionnel/Avancé) :
    *   Permettre d'ajouter dynamiquement des niveaux (Nom, Age min, Age max).
    *   *Note : Si c'est trop complexe pour la V1, vous pouvez ignorer les niveaux lors de la création et n'utiliser que `maxCapacity`.*

### B. Détail de l'Offre (`OfferDetailsScreen`)

Quand l'académie clique sur une de ses offres :

1.  **Compteur d'inscrits** : Affichez *"Inscrits : X / Y"* (utilisez `subscribersCount` et `maxCapacity`).
2.  **Bouton "Voir les inscrits"** :
    *   Ce bouton doit ouvrir un nouvel écran ou une modal : `SubscribersListActivity`.
    *   Au clic, appelez l'API `getOfferSubscribers(offerId)`.

### C. Écran Liste des Inscrits (`SubscribersListActivity`)

Créez un écran simple avec une `RecyclerView` pour afficher la liste reçue de `getOfferSubscribers`.

*   **Item de liste** :
    *   Photo & Nom de l'enfant.
    *   Nom du parent & Téléphone (bouton cliquable pour appeler).
    *   Statut de l'abonnement (Actif/En attente).
    *   Dates (Du... Au...).

---

## Résumé du JSON à envoyer pour la création (Exemple)

Si vous implémentez le formulaire complet :

```json
{
  "name": "Judo Enfants",
  "type": "ANNUAL",
  "price": 500,
  "durationDays": 365,
  "maxCapacity": 20,  <-- Nouveau
  "levels": [         <-- Nouveau (Optionnel)
    { "name": "Débutant", "minAge": 6, "maxAge": 8 },
    { "name": "Avancé", "minAge": 9, "maxAge": 12 }
  ]
}
```
