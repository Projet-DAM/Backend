# 🇹🇳 Mise à Jour - Devise en Dinar Tunisien (TND)

## ✅ Modification Effectuée

La devise par défaut pour toutes les options d'abonnement a été changée de **EUR** à **TND** (Dinar Tunisien).

---

## 📋 Options Mises à Jour

| Option | Prix | Devise | Description |
|--------|------|--------|-------------|
| 🏃 Tenue sportive | 50 | **TND** | Tenue sportive complète (maillot, short, chaussettes) |
| 🛡️ Assurance | 30 | **TND** | Assurance accident et responsabilité civile |
| 🚌 Transport | 40 | **TND** | Service de transport aller-retour |

---

## 🔧 Fichiers Modifiés

### 1. `src/subscriptions/subscription-options.service.ts`
```typescript
// AVANT: currency: 'EUR'
// APRÈS: currency: 'TND'

getAvailableOptions(): AvailableOption[] {
  return [
    {
      type: SubscriptionOptionType.SPORTS_OUTFIT,
      price: 50,
      currency: 'TND', // ✅ Changé
    },
    // ...
  ];
}
```

### 2. `src/subscriptions/schemas/subscription-option.schema.ts`
```typescript
// AVANT: @Prop({ default: 'EUR' })
// APRÈS: @Prop({ default: 'TND' })

@Prop({ default: 'TND' })
currency: string;
```

### 3. `src/subscriptions/dto/subscription-option.dto.ts`
```typescript
// AVANT: currency?: string = 'EUR';
// APRÈS: currency?: string = 'TND';

@ApiProperty({
  example: 'TND',
  default: 'TND',
})
currency?: string = 'TND';
```

### 4. `src/subscriptions/dto/create-subscription.dto.ts`
```typescript
// Exemples mis à jour
example: [
  { type: 'SPORTS_OUTFIT', price: 50, currency: 'TND' },
  { type: 'INSURANCE', price: 30, currency: 'TND' }
]
```

---

## 💰 Exemples de Calcul (en TND)

### Exemple 1: Offre Basique + 2 Options
```
Offre: 100 TND (10% de réduction)
Options: Tenue sportive (50 TND) + Assurance (30 TND)

Calcul:
  Prix de base: 100 × (1 - 0.10) = 90 TND
  Options: 50 + 30 = 80 TND
  ─────────────────────────────────────
  TOTAL: 170 TND
```

### Exemple 2: Offre Premium + Toutes les Options
```
Offre: 150 TND (20% de réduction)
Options: Tenue (50) + Assurance (30) + Transport (40)

Calcul:
  Prix de base: 150 × (1 - 0.20) = 120 TND
  Options: 50 + 30 + 40 = 120 TND
  ─────────────────────────────────────
  TOTAL: 240 TND
```

### Exemple 3: Abonnement Simple
```
Offre: 80 TND (0% de réduction)
Options: Transport uniquement (40 TND)

Calcul:
  Prix de base: 80 TND
  Options: 40 TND
  ─────────────────────────────────────
  TOTAL: 120 TND
```

---

## 🔗 API - Exemples Mis à Jour

### 1. Obtenir les Options Disponibles
```http
GET /subscriptions/available-options
```

**Réponse:**
```json
[
  {
    "type": "SPORTS_OUTFIT",
    "name": "Tenue sportive",
    "description": "Tenue sportive complète (maillot, short, chaussettes)",
    "price": 50,
    "currency": "TND"
  },
  {
    "type": "INSURANCE",
    "name": "Assurance",
    "description": "Assurance accident et responsabilité civile",
    "price": 30,
    "currency": "TND"
  },
  {
    "type": "TRANSPORT",
    "name": "Transport",
    "description": "Service de transport aller-retour",
    "price": 40,
    "currency": "TND"
  }
]
```

### 2. Créer un Abonnement avec Options
```http
POST /subscriptions
Content-Type: application/json

{
  "childId": "65b1f0998d614e72c9b1ab55",
  "offerId": "65b1f0998d614e72c9b1ab56",
  "selectedOptions": [
    {
      "type": "SPORTS_OUTFIT",
      "price": 50,
      "currency": "TND"
    },
    {
      "type": "INSURANCE",
      "price": 30,
      "currency": "TND"
    }
  ]
}
```

### 3. Enregistrer un Paiement
```http
POST /subscriptions/{id}/pay
Content-Type: application/json

{
  "amount": 170,
  "currency": "TND",
  "method": "CARD",
  "externalRef": "PAY-123456"
}
```

---

## 📱 Frontend - Exemples Mis à Jour

### React/TypeScript
```typescript
// Créer un abonnement avec options en TND
const createSubscription = async (childId, offerId) => {
  const response = await fetch('/subscriptions', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      childId,
      offerId,
      selectedOptions: [
        { type: 'SPORTS_OUTFIT', price: 50, currency: 'TND' },
        { type: 'INSURANCE', price: 30, currency: 'TND' }
      ]
    })
  });
  return response.json();
};

// Afficher le prix avec la devise TND
const formatPrice = (amount: number) => {
  return `${amount.toFixed(2)} TND`;
};
```

### Kotlin/Android
```kotlin
// Créer un abonnement avec options en TND
data class SubscriptionOptionDto(
    val type: String,
    val price: Double,
    val currency: String = "TND"
)

suspend fun createSubscription(
    childId: String,
    offerId: String,
    selectedOptions: List<SubscriptionOptionDto>
): Subscription {
    return api.post("/subscriptions", CreateSubscriptionRequest(
        childId = childId,
        offerId = offerId,
        selectedOptions = selectedOptions
    ))
}

// Afficher le prix avec la devise TND
fun formatPrice(amount: Double): String {
    return "%.2f TND".format(amount)
}
```

---

## 🎨 UI - Affichage Recommandé

```
┌─────────────────────────────────────┐
│  Choisir une offre                  │
├─────────────────────────────────────┤
│  Offre Mensuelle - 100 TND          │
│  (10% de réduction = 90 TND)        │
├─────────────────────────────────────┤
│  Options supplémentaires:           │
│                                     │
│  ☑ Tenue sportive      +50 TND     │
│  ☑ Assurance           +30 TND     │
│  ☐ Transport           +40 TND     │
├─────────────────────────────────────┤
│  TOTAL: 170 TND                     │
├─────────────────────────────────────┤
│  [Confirmer l'abonnement]           │
└─────────────────────────────────────┘
```

---

## ⚠️ Notes Importantes

1. **Rétrocompatibilité**: Les abonnements existants avec EUR restent valides
2. **Nouveaux abonnements**: Utilisent automatiquement TND par défaut
3. **Flexibilité**: Le système accepte toujours d'autres devises si spécifiées
4. **Validation**: La devise est validée côté backend

---

## ✅ Vérifications Effectuées

- ✅ Build TypeScript réussi
- ✅ Compilation NestJS réussie
- ✅ Schémas mis à jour
- ✅ DTOs mis à jour
- ✅ Service mis à jour
- ✅ Exemples mis à jour

---

## 📊 Comparaison Avant/Après

| Élément | Avant | Après |
|---------|-------|-------|
| Devise par défaut | EUR | **TND** |
| Tenue sportive | 50 EUR | **50 TND** |
| Assurance | 30 EUR | **30 TND** |
| Transport | 40 EUR | **40 TND** |
| Exemple total | 170 EUR | **170 TND** |

---

## 🚀 Prochaines Étapes

### Backend
- ✅ Devise changée en TND
- ✅ Build réussi
- ✅ Prêt pour la production

### Frontend (À faire)
1. Mettre à jour l'affichage des prix pour montrer "TND"
2. Mettre à jour les exemples dans l'UI
3. Tester les paiements avec la nouvelle devise

---

**Date de mise à jour:** 2025-12-12  
**Version:** 1.1.0  
**Status:** ✅ Complété  
**Build:** ✅ Successful
