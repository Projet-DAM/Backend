# Options Supplémentaires pour les Abonnements

## Vue d'ensemble

Le système d'abonnement prend désormais en charge les **options supplémentaires** que les parents peuvent ajouter lors de la création ou de la modification d'un abonnement. Chaque option augmente le prix total de l'abonnement.

## Options Disponibles

### 1. **Tenue Sportive** (`SPORTS_OUTFIT`)
- **Prix**: 50 EUR
- **Description**: Tenue sportive complète (maillot, short, chaussettes)

### 2. **Assurance** (`INSURANCE`)
- **Prix**: 30 EUR
- **Description**: Assurance accident et responsabilité civile

### 3. **Transport** (`TRANSPORT`)
- **Prix**: 40 EUR
- **Description**: Service de transport aller-retour

## Calcul du Prix Total

Le prix total d'un abonnement est calculé comme suit:

```
Prix Total = Prix de l'offre (avec réduction) + Somme des options sélectionnées
```

**Exemple:**
- Offre de base: 100 EUR (avec 10% de réduction = 90 EUR)
- Tenue sportive: 50 EUR
- Assurance: 30 EUR
- **Prix Total**: 90 + 50 + 30 = **170 EUR**

## API Endpoints

### 1. Obtenir les Options Disponibles

**GET** `/subscriptions/available-options`

**Authentification**: Requise (JWT)

**Rôles autorisés**: PARENT, COACH, ACADEMIE, ADMIN

**Réponse**:
```json
[
  {
    "type": "SPORTS_OUTFIT",
    "name": "Tenue sportive",
    "description": "Tenue sportive complète (maillot, short, chaussettes)",
    "price": 50,
    "currency": "EUR"
  },
  {
    "type": "INSURANCE",
    "name": "Assurance",
    "description": "Assurance accident et responsabilité civile",
    "price": 30,
    "currency": "EUR"
  },
  {
    "type": "TRANSPORT",
    "name": "Transport",
    "description": "Service de transport aller-retour",
    "price": 40,
    "currency": "EUR"
  }
]
```

### 2. Créer un Abonnement avec Options

**POST** `/subscriptions`

**Body**:
```json
{
  "childId": "65b1f0998d614e72c9b1ab55",
  "offerId": "65b1f0998d614e72c9b1ab56",
  "autoRenew": true,
  "selectedOptions": [
    {
      "type": "SPORTS_OUTFIT",
      "price": 50,
      "currency": "EUR",
      "description": "Tenue sportive complète"
    },
    {
      "type": "INSURANCE",
      "price": 30,
      "currency": "EUR",
      "description": "Assurance accident"
    }
  ]
}
```

### 3. Modifier les Options d'un Abonnement

**PATCH** `/subscriptions/:id`

**Body**:
```json
{
  "selectedOptions": [
    {
      "type": "TRANSPORT",
      "price": 40,
      "currency": "EUR",
      "description": "Service de transport"
    }
  ]
}
```

## Structure de Données

### Schema: SubscriptionOption

```typescript
{
  type: SubscriptionOptionType;  // SPORTS_OUTFIT | INSURANCE | TRANSPORT
  price: number;                 // Prix de l'option (minimum 0)
  currency: string;              // Devise (par défaut: 'EUR')
  description?: string;          // Description optionnelle
}
```

### Schema: Subscription (mis à jour)

Le schéma `Subscription` inclut maintenant:

```typescript
{
  // ... champs existants ...
  selectedOptions: SubscriptionOption[];  // Options sélectionnées
}
```

## Logique de Paiement

Lorsqu'un paiement est enregistré, le système:

1. Calcule le **prix total** (offre + options)
2. Compare le **total payé** avec le **prix total**
3. Met à jour le statut de paiement:
   - `PAID`: Si total payé ≥ prix total
   - `PARTIAL`: Si 0 < total payé < prix total
   - `UNPAID`: Si total payé = 0

## Permissions

### Parents
- ✅ Peuvent sélectionner des options lors de la création d'un abonnement
- ✅ Peuvent modifier les options d'un abonnement existant (sauf si annulé)
- ✅ Peuvent consulter les options disponibles

### Admin/Académie
- ✅ Peuvent modifier toutes les options
- ✅ Peuvent consulter les options disponibles

## Exemples d'Utilisation

### Frontend (Android/Kotlin)

```kotlin
// 1. Récupérer les options disponibles
val options = api.getAvailableOptions()

// 2. Créer un abonnement avec options
val subscription = CreateSubscriptionDto(
    childId = "...",
    offerId = "...",
    selectedOptions = listOf(
        SubscriptionOptionDto(
            type = "SPORTS_OUTFIT",
            price = 50.0,
            currency = "EUR"
        ),
        SubscriptionOptionDto(
            type = "INSURANCE",
            price = 30.0,
            currency = "EUR"
        )
    )
)
api.createSubscription(subscription)
```

### Frontend (React/TypeScript)

```typescript
// 1. Récupérer les options disponibles
const options = await fetch('/subscriptions/available-options', {
  headers: { 'Authorization': `Bearer ${token}` }
}).then(r => r.json());

// 2. Créer un abonnement avec options
const subscription = {
  childId: "...",
  offerId: "...",
  selectedOptions: [
    { type: "SPORTS_OUTFIT", price: 50, currency: "EUR" },
    { type: "INSURANCE", price: 30, currency: "EUR" }
  ]
};

await fetch('/subscriptions', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify(subscription)
});
```

## Fichiers Modifiés

1. **Nouveaux fichiers**:
   - `src/subscriptions/schemas/subscription-option.schema.ts`
   - `src/subscriptions/dto/subscription-option.dto.ts`
   - `src/subscriptions/subscription-options.service.ts`

2. **Fichiers modifiés**:
   - `src/subscriptions/schemas/subscription.schema.ts`
   - `src/subscriptions/dto/create-subscription.dto.ts`
   - `src/subscriptions/dto/update-subscription.dto.ts`
   - `src/subscriptions/subscriptions.service.ts`
   - `src/subscriptions/subscriptions.controller.ts`
   - `src/subscriptions/subscriptions.module.ts`

## Notes Importantes

- Les prix des options sont **fixes** et définis dans `SubscriptionOptionsService`
- Pour modifier les prix, il faut mettre à jour le service `SubscriptionOptionsService`
- Les options sont **optionnelles** - un abonnement peut être créé sans options
- Le calcul du prix total prend en compte les **réductions** de l'offre de base
