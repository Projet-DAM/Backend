# 📱 Guide d'Intégration Android - Endpoints Subscriptions

## ✅ Serveur Backend
**Status:** ✅ En cours d'exécution sur le port 3000

---

## 🔗 Configuration Retrofit pour Android

### 1. Base URL (Émulateur Android)
```kotlin
object ApiConfig {
    // ⚠️ IMPORTANT: Pour l'émulateur Android, utilisez 10.0.2.2 au lieu de localhost
    const val BASE_URL = "http://10.0.2.2:3000/"
    
    // Pour un appareil physique sur le même réseau WiFi:
    // const val BASE_URL = "http://192.168.1.X:3000/" // Remplacez X par votre IP
}
```

### 2. Service API
```kotlin
interface SubscriptionApiService {
    
    // Obtenir les options disponibles
    @GET("subscriptions/available-options")
    suspend fun getAvailableOptions(
        @Header("Authorization") authorization: String
    ): List<SubscriptionOption>
    
    // Obtenir mes abonnements
    @GET("subscriptions/my")
    suspend fun getMySubscriptions(
        @Header("Authorization") authorization: String
    ): List<Subscription>
    
    // Créer un abonnement
    @POST("subscriptions")
    suspend fun createSubscription(
        @Header("Authorization") authorization: String,
        @Body request: CreateSubscriptionRequest
    ): Subscription
    
    // Obtenir un abonnement par ID
    @GET("subscriptions/{id}")
    suspend fun getSubscription(
        @Path("id") id: String,
        @Header("Authorization") authorization: String
    ): Subscription
    
    // Mettre à jour un abonnement
    @PATCH("subscriptions/{id}")
    suspend fun updateSubscription(
        @Path("id") id: String,
        @Header("Authorization") authorization: String,
        @Body request: UpdateSubscriptionRequest
    ): Subscription
    
    // Enregistrer un paiement
    @POST("subscriptions/{id}/pay")
    suspend fun recordPayment(
        @Path("id") id: String,
        @Header("Authorization") authorization: String,
        @Body payment: PaymentRequest
    ): Subscription
    
    // Annuler un abonnement
    @POST("subscriptions/{id}/cancel")
    suspend fun cancelSubscription(
        @Path("id") id: String,
        @Header("Authorization") authorization: String
    ): Subscription
}
```

---

## 📦 Modèles de Données (Data Classes)

### SubscriptionOption
```kotlin
data class SubscriptionOption(
    val type: String,  // "SPORTS_OUTFIT", "INSURANCE", "TRANSPORT"
    val name: String,
    val description: String,
    val price: Double,
    val currency: String  // "TND"
)
```

### CreateSubscriptionRequest
```kotlin
data class CreateSubscriptionRequest(
    val childId: String,
    val offerId: String,
    val autoRenew: Boolean = false,
    val startDate: String? = null,  // Format ISO: "2025-12-12T00:00:00.000Z"
    val selectedOptions: List<SubscriptionOptionDto> = emptyList()
)

data class SubscriptionOptionDto(
    val type: String,
    val price: Double,
    val currency: String = "TND",
    val description: String? = null
)
```

---

## ⚠️ Points Importants

### 1. URL de Base
```kotlin
// ❌ FAUX (ne fonctionne pas dans l'émulateur)
const val BASE_URL = "http://localhost:3000/"

// ✅ CORRECT (émulateur Android)
const val BASE_URL = "http://10.0.2.2:3000/"
```

### 2. Token JWT
```kotlin
// ❌ FAUX
val token = getToken()
apiService.getAvailableOptions(token)

// ✅ CORRECT
val token = "Bearer ${getToken()}"
apiService.getAvailableOptions(token)
```

### 3. Permissions Manifest
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<application
    android:usesCleartextTraffic="true">
```

---

## 📋 Checklist de Vérification

- [ ] BASE_URL = `"http://10.0.2.2:3000/"` (pour émulateur)
- [ ] Token JWT avec préfixe `"Bearer "`
- [ ] Permissions INTERNET dans le manifest
- [ ] `usesCleartextTraffic="true"` dans le manifest
- [ ] Serveur backend démarré (`npm run start:dev`)
- [ ] Endpoint correct: `"subscriptions/available-options"` (avec 's')

---

## 🎉 Résumé

**URL complète pour l'émulateur:**
```
http://10.0.2.2:3000/subscriptions/available-options
```

**Header requis:**
```
Authorization: Bearer {votre_token_jwt}
```

**Serveur backend:**
```bash
cd c:\Users\hatem\Backend2
npm run start:dev
```

---

**Date:** 2025-12-12  
**Status:** ✅ Serveur en cours d'exécution  
**Port:** 3000
