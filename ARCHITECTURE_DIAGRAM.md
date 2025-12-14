# Architecture des Options d'Abonnement

## 📐 Diagramme de Structure

```
┌─────────────────────────────────────────────────────────────┐
│                    SUBSCRIPTION OPTIONS                      │
└─────────────────────────────────────────────────────────────┘

┌──────────────────────┐
│   Available Options  │
├──────────────────────┤
│ 🏃 SPORTS_OUTFIT     │ → 50 EUR
│ 🛡️  INSURANCE         │ → 30 EUR
│ 🚌 TRANSPORT         │ → 40 EUR
└──────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    SUBSCRIPTION FLOW                         │
└─────────────────────────────────────────────────────────────┘

1️⃣  Parent consulte les options disponibles
    GET /subscriptions/available-options
    ↓
    [
      { type: "SPORTS_OUTFIT", price: 50, ... },
      { type: "INSURANCE", price: 30, ... },
      { type: "TRANSPORT", price: 40, ... }
    ]

2️⃣  Parent crée un abonnement avec options
    POST /subscriptions
    {
      childId: "...",
      offerId: "...",
      selectedOptions: [
        { type: "SPORTS_OUTFIT", price: 50 },
        { type: "INSURANCE", price: 30 }
      ]
    }
    ↓
    Subscription créé avec status: PENDING

3️⃣  Calcul du prix total
    Prix de l'offre: 100 EUR (avec 10% réduction = 90 EUR)
    + Tenue sportive: 50 EUR
    + Assurance: 30 EUR
    ─────────────────────
    TOTAL: 170 EUR

4️⃣  Parent effectue le paiement
    POST /subscriptions/:id/pay
    { amount: 170, currency: "EUR", method: "CARD" }
    ↓
    - Vérifie: 170 >= 170 ✅
    - paymentStatus: PAID
    - status: ACTIVE
    - Email de confirmation envoyé

┌─────────────────────────────────────────────────────────────┐
│                    DATA STRUCTURE                            │
└─────────────────────────────────────────────────────────────┘

Subscription Document:
{
  _id: ObjectId,
  childId: ObjectId,
  parentId: ObjectId,
  offerId: ObjectId,
  startDate: Date,
  endDate: Date,
  status: "PENDING" | "ACTIVE" | "SUSPENDED" | "CANCELLED" | "EXPIRED",
  paymentStatus: "UNPAID" | "PARTIAL" | "PAID" | "REFUNDED",
  
  ✨ selectedOptions: [                    ← NOUVEAU
    {
      type: "SPORTS_OUTFIT",
      price: 50,
      currency: "EUR",
      description: "Tenue sportive complète"
    },
    {
      type: "INSURANCE",
      price: 30,
      currency: "EUR",
      description: "Assurance accident"
    }
  ],
  
  transactions: [...],
  autoRenew: Boolean,
  notes: String,
  expirationWarningSent: Boolean
}

┌─────────────────────────────────────────────────────────────┐
│                    PRICE CALCULATION                         │
└─────────────────────────────────────────────────────────────┘

calculateTotalPrice(offer, selectedOptions) {
  
  Step 1: Calculate base price with discount
  ┌────────────────────────────────────┐
  │ basePrice = offer.price ×          │
  │             (1 - discountPct/100)  │
  └────────────────────────────────────┘
  
  Step 2: Sum all option prices
  ┌────────────────────────────────────┐
  │ optionsTotal = Σ(option.price)     │
  └────────────────────────────────────┘
  
  Step 3: Calculate total
  ┌────────────────────────────────────┐
  │ totalPrice = basePrice +           │
  │              optionsTotal          │
  └────────────────────────────────────┘
  
  return totalPrice
}

Example:
  Offer: 100 EUR with 10% discount
  Options: SPORTS_OUTFIT (50) + INSURANCE (30)
  
  basePrice = 100 × (1 - 0.10) = 90 EUR
  optionsTotal = 50 + 30 = 80 EUR
  totalPrice = 90 + 80 = 170 EUR ✅

┌─────────────────────────────────────────────────────────────┐
│                    FILE STRUCTURE                            │
└─────────────────────────────────────────────────────────────┘

src/subscriptions/
├── schemas/
│   ├── subscription.schema.ts          [MODIFIÉ]
│   └── subscription-option.schema.ts   [NOUVEAU] ✨
│
├── dto/
│   ├── create-subscription.dto.ts      [MODIFIÉ]
│   ├── update-subscription.dto.ts      [INCHANGÉ]
│   ├── record-payment.dto.ts           [INCHANGÉ]
│   └── subscription-option.dto.ts      [NOUVEAU] ✨
│
├── subscriptions.service.ts            [MODIFIÉ]
├── subscriptions.controller.ts         [MODIFIÉ]
├── subscriptions.module.ts             [MODIFIÉ]
├── subscription-options.service.ts     [NOUVEAU] ✨
└── subscriptions-scheduler.service.ts  [INCHANGÉ]

Documentation/
├── SUBSCRIPTION_OPTIONS.md             [NOUVEAU] ✨
├── CHANGES_SUMMARY.md                  [NOUVEAU] ✨
└── test-subscription-options.js        [NOUVEAU] ✨

┌─────────────────────────────────────────────────────────────┐
│                    API ENDPOINTS                             │
└─────────────────────────────────────────────────────────────┘

GET    /subscriptions/available-options     ✨ NEW
       → Liste des options disponibles
       → Accessible par: PARENT, COACH, ACADEMIE, ADMIN

POST   /subscriptions
       → Créer un abonnement (avec options)
       → Body: { ..., selectedOptions: [...] }  ✨ UPDATED

PATCH  /subscriptions/:id
       → Modifier un abonnement (incluant options)
       → Body: { selectedOptions: [...] }       ✨ UPDATED

POST   /subscriptions/:id/pay
       → Enregistrer un paiement
       → Calcul automatique avec options        ✨ UPDATED

┌─────────────────────────────────────────────────────────────┐
│                    PAYMENT LOGIC                             │
└─────────────────────────────────────────────────────────────┘

recordPayment(subscriptionId, amount) {
  
  1. Récupérer l'abonnement
     ↓
  2. Calculer le prix total (offre + options)
     totalPrice = calculateTotalPrice(offer, selectedOptions)
     ↓
  3. Ajouter la transaction
     transactions.push({ amount, currency, method, ... })
     ↓
  4. Calculer le total payé
     totalPaid = Σ(transactions where status = 'SUCCESS')
     ↓
  5. Mettre à jour le statut de paiement
     if (totalPaid >= totalPrice)      → PAID
     else if (totalPaid > 0)            → PARTIAL
     else                               → UNPAID
     ↓
  6. Activer si payé et en attente
     if (paymentStatus = PAID && status = PENDING)
       status = ACTIVE
     ↓
  7. Envoyer email de confirmation
     if (paymentStatus = PAID)
       sendPaymentConfirmation(...)
}

┌─────────────────────────────────────────────────────────────┐
│                    PERMISSIONS MATRIX                        │
└─────────────────────────────────────────────────────────────┘

Action                          PARENT  COACH  ACADEMIE  ADMIN
─────────────────────────────────────────────────────────────
View available options            ✅      ✅      ✅       ✅
Create subscription w/ options    ✅      ❌      ❌       ❌
Update own subscription options   ✅      ❌      ❌       ❌
Update any subscription options   ❌      ❌      ✅       ✅
View subscription details         ✅*     ✅*     ✅       ✅
Record payment                    ✅*     ❌      ✅       ❌

* = Avec restrictions de portée (own children/subscriptions)

┌─────────────────────────────────────────────────────────────┐
│                    TESTING CHECKLIST                         │
└─────────────────────────────────────────────────────────────┘

✅ Build successful (npm run build)
✅ Schema validation
✅ DTO validation
✅ Price calculation logic
✅ Payment status updates
✅ Email notifications
✅ Permissions enforcement

TODO (Frontend):
□ UI pour afficher les options disponibles
□ Checkboxes pour sélectionner les options
□ Calcul en temps réel du prix total
□ Mise à jour des écrans de création/modification
□ Tests d'intégration

┌─────────────────────────────────────────────────────────────┐
│                    EXAMPLE SCENARIOS                         │
└─────────────────────────────────────────────────────────────┘

Scenario 1: Abonnement complet avec toutes les options
─────────────────────────────────────────────────────────
Offre: 150 EUR (20% réduction)
Options: Tenue (50) + Assurance (30) + Transport (40)

Calcul:
  Base: 150 × 0.80 = 120 EUR
  Options: 50 + 30 + 40 = 120 EUR
  TOTAL: 240 EUR

Scenario 2: Abonnement sans options
─────────────────────────────────────────────────────────
Offre: 100 EUR (10% réduction)
Options: Aucune

Calcul:
  Base: 100 × 0.90 = 90 EUR
  Options: 0 EUR
  TOTAL: 90 EUR

Scenario 3: Modification des options après création
─────────────────────────────────────────────────────────
Initial: Tenue (50) + Assurance (30) = 80 EUR options
Modifié: Tenue (50) + Transport (40) = 90 EUR options

Le prix total est recalculé lors du prochain paiement

Scenario 4: Paiement partiel
─────────────────────────────────────────────────────────
Prix total: 170 EUR
Paiement 1: 100 EUR → Status: PARTIAL
Paiement 2: 70 EUR → Status: PAID → ACTIVE
