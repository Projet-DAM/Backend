# Configuration des Variables d'Environnement Stripe

## Ajouter les clés Stripe dans le fichier .env

Ouvrez le fichier `.env` dans `C:\Users\hatem\OneDrive\Bureau\Backend\` et ajoutez les lignes suivantes :

```env
# Stripe Configuration
STRIPE_SECRET_KEY=sk_test_51SToB0QkN55ayShybsJotVNLoTT01Mp5IaN5yB45geP5KD3X9Yw3tTslBS3zlhdlksa2iU2PeUxTcrnHK1BbRwnA00DhYhJlIb
STRIPE_PUBLISHABLE_KEY=pk_test_51SToB0QkN55ayShyAGms4vrDrYbnnV3va3lM4d7wK2cb69auigr5FinMoCDruQHqDwyDvWZfywjfHabxreHnUXis00f2q0xUzS
```

## Vérification

Après avoir ajouté ces variables, redémarrez le serveur backend :

```bash
cd C:\Users\hatem\OneDrive\Bureau\Backend
npm run start:dev
```

Le module Payments devrait se charger correctement et les endpoints suivants seront disponibles :

- `POST /payments/create-intent` - Créer un PaymentIntent
- `POST /payments/confirm` - Confirmer un paiement

## Documentation Swagger

Une fois le serveur démarré, vous pouvez voir la documentation des endpoints dans Swagger :
- Ouvrez votre navigateur et allez à : `http://localhost:3000/api`










