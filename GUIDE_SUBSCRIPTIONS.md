# cURL prêts à l'emploi

Assume base URL: http://localhost:3000

1) Login parent (email/motDePasse existants après seed)

```bash
curl -s -X POST http://localhost:3000/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"parent@example.com","motDePasse":"password"}'
```

2) Créer un abonnement (PARENT)

```bash
PARENT_TOKEN=REPLACE_WITH_TOKEN
CHILD_ID=REPLACE_CHILD_ID
OFFER_ID=REPLACE_OFFER_ID
curl -s -X POST http://localhost:3000/subscriptions \
  -H 'Authorization: Bearer '"$PARENT_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"childId":"'"$CHILD_ID"'","offerId":"'"$OFFER_ID"'","autoRenew":true}'
```

3) Enregistrer un paiement (PARENT ou ACADEMIE)

```bash
SUB_ID=REPLACE_SUBSCRIPTION_ID
curl -s -X POST http://localhost:3000/subscriptions/$SUB_ID/pay \
  -H 'Authorization: Bearer '"$PARENT_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"amount":50,"currency":"EUR","method":"CASH"}'
```

4) Lister mes abonnements (PARENT)

```bash
curl -s http://localhost:3000/subscriptions/mine \
  -H 'Authorization: Bearer '"$PARENT_TOKEN" 
```

5) Annuler un abonnement (PARENT)

```bash
curl -s -X POST http://localhost:3000/subscriptions/$SUB_ID/cancel \
  -H 'Authorization: Bearer '"$PARENT_TOKEN" 
```



