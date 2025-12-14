# 🚀 API Quick Reference - Subscription Options

## 📋 Available Options

| Option | Type | Price | Description |
|--------|------|-------|-------------|
| 🏃 Tenue sportive | `SPORTS_OUTFIT` | 50 EUR | Tenue sportive complète (maillot, short, chaussettes) |
| 🛡️ Assurance | `INSURANCE` | 30 EUR | Assurance accident et responsabilité civile |
| 🚌 Transport | `TRANSPORT` | 40 EUR | Service de transport aller-retour |

---

## 🔗 API Endpoints

### 1. Get Available Options
```http
GET /subscriptions/available-options
Authorization: Bearer {token}
```

**Response:**
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

---

### 2. Create Subscription with Options
```http
POST /subscriptions
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
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

**Response:**
```json
{
  "_id": "65b1f0998d614e72c9b1ab57",
  "childId": "65b1f0998d614e72c9b1ab55",
  "parentId": "65b1f0998d614e72c9b1ab54",
  "offerId": "65b1f0998d614e72c9b1ab56",
  "startDate": "2025-12-12T00:00:00.000Z",
  "endDate": "2026-01-11T00:00:00.000Z",
  "status": "PENDING",
  "paymentStatus": "UNPAID",
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
  ],
  "transactions": [],
  "autoRenew": true,
  "expirationWarningSent": false,
  "createdAt": "2025-12-12T14:15:51.000Z",
  "updatedAt": "2025-12-12T14:15:51.000Z"
}
```

---

### 3. Update Subscription Options
```http
PATCH /subscriptions/{subscriptionId}
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "selectedOptions": [
    {
      "type": "SPORTS_OUTFIT",
      "price": 50,
      "currency": "EUR"
    },
    {
      "type": "TRANSPORT",
      "price": 40,
      "currency": "EUR"
    }
  ]
}
```

---

### 4. Record Payment (with options)
```http
POST /subscriptions/{subscriptionId}/pay
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "amount": 170,
  "currency": "EUR",
  "method": "CARD",
  "externalRef": "PAY-123456"
}
```

**Note:** The backend automatically calculates the total price including options.

---

## 💰 Price Calculation Examples

### Example 1: Basic Offer + 2 Options
```
Offer: 100 EUR (10% discount)
Options: SPORTS_OUTFIT (50) + INSURANCE (30)

Calculation:
  Base price: 100 × (1 - 0.10) = 90 EUR
  Options: 50 + 30 = 80 EUR
  ─────────────────────────────────────
  TOTAL: 170 EUR
```

### Example 2: Premium Offer + All Options
```
Offer: 150 EUR (20% discount)
Options: SPORTS_OUTFIT (50) + INSURANCE (30) + TRANSPORT (40)

Calculation:
  Base price: 150 × (1 - 0.20) = 120 EUR
  Options: 50 + 30 + 40 = 120 EUR
  ─────────────────────────────────────
  TOTAL: 240 EUR
```

### Example 3: No Discount + 1 Option
```
Offer: 80 EUR (0% discount)
Options: TRANSPORT (40)

Calculation:
  Base price: 80 × (1 - 0.00) = 80 EUR
  Options: 40 EUR
  ─────────────────────────────────────
  TOTAL: 120 EUR
```

---

## 🔐 Authentication & Permissions

| Endpoint | PARENT | COACH | ACADEMIE | ADMIN |
|----------|--------|-------|----------|-------|
| GET /available-options | ✅ | ✅ | ✅ | ✅ |
| POST /subscriptions | ✅ | ❌ | ❌ | ❌ |
| PATCH /subscriptions/:id | ✅* | ❌ | ✅ | ✅ |
| POST /subscriptions/:id/pay | ✅* | ❌ | ✅ | ❌ |

*\* = Own subscriptions only*

---

## 📱 Frontend Integration Examples

### React/TypeScript
```typescript
// 1. Fetch available options
const fetchOptions = async () => {
  const response = await fetch('/subscriptions/available-options', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  return response.json();
};

// 2. Create subscription with options
const createSubscription = async (childId, offerId, selectedOptions) => {
  const response = await fetch('/subscriptions', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      childId,
      offerId,
      selectedOptions
    })
  });
  return response.json();
};

// 3. Calculate total price
const calculateTotal = (offer, selectedOptions) => {
  const basePrice = offer.price * (1 - (offer.discountPct || 0) / 100);
  const optionsTotal = selectedOptions.reduce((sum, opt) => sum + opt.price, 0);
  return basePrice + optionsTotal;
};
```

### Kotlin/Android
```kotlin
// 1. Fetch available options
suspend fun getAvailableOptions(): List<SubscriptionOption> {
    return api.get("/subscriptions/available-options")
}

// 2. Create subscription with options
data class CreateSubscriptionRequest(
    val childId: String,
    val offerId: String,
    val selectedOptions: List<SubscriptionOptionDto>
)

suspend fun createSubscription(request: CreateSubscriptionRequest): Subscription {
    return api.post("/subscriptions", request)
}

// 3. Calculate total price
fun calculateTotal(offer: Offer, selectedOptions: List<SubscriptionOption>): Double {
    val basePrice = offer.price * (1 - (offer.discountPct ?: 0.0) / 100)
    val optionsTotal = selectedOptions.sumOf { it.price }
    return basePrice + optionsTotal
}
```

---

## ⚠️ Important Notes

1. **Option Prices**: Prices are fixed and defined in the backend
2. **Currency**: All options use EUR by default
3. **Validation**: Options are validated on the backend
4. **Total Calculation**: Always done server-side for security
5. **Payment**: Total includes base offer price + all selected options

---

## 🐛 Common Errors

### 400 Bad Request
```json
{
  "statusCode": 400,
  "message": ["selectedOptions.0.type must be a valid enum value"],
  "error": "Bad Request"
}
```
**Solution:** Use valid option types: `SPORTS_OUTFIT`, `INSURANCE`, or `TRANSPORT`

### 403 Forbidden
```json
{
  "statusCode": 403,
  "message": "Accès refusé",
  "error": "Forbidden"
}
```
**Solution:** Check user role and permissions

### 404 Not Found
```json
{
  "statusCode": 404,
  "message": "Abonnement introuvable",
  "error": "Not Found"
}
```
**Solution:** Verify subscription ID exists

---

## 📞 Support

For questions or issues:
- Check `SUBSCRIPTION_OPTIONS.md` for detailed documentation
- Review `ARCHITECTURE_DIAGRAM.md` for system architecture
- See `CHANGES_SUMMARY.md` for implementation details
- Use `test-subscription-options.js` for testing examples

---

**Last Updated:** 2025-12-12  
**Version:** 1.0.0  
**Status:** ✅ Production Ready
