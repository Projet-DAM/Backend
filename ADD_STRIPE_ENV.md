# ⚠️ URGENT : Ajouter les variables Stripe dans .env

## Erreur actuelle
```
Error: STRIPE_SECRET_KEY is not configured
```

## Solution

Ouvrez le fichier `.env` dans `C:\Users\hatem\OneDrive\Bureau\Backend\` et ajoutez ces lignes :

```env
# Stripe Configuration
STRIPE_SECRET_KEY=sk_test_51SToB0QkN55ayShybsJotVNLoTT01Mp5IaN5yB45geP5KD3X9Yw3tTslBS3zlhdlksa2iU2PeUxTcrnHK1BbRwnA00DhYhJlIb
STRIPE_PUBLISHABLE_KEY=pk_test_51SToB0QkN55ayShyAGms4vrDrYbnnV3va3lM4d7wK2cb69auigr5FinMoCDruQHqDwyDvWZfywjfHabxreHnUXis00f2q0xUzS
```

## Étapes

1. Ouvrez le fichier `.env` dans `C:\Users\hatem\OneDrive\Bureau\Backend\`
2. Ajoutez les deux lignes ci-dessus à la fin du fichier
3. Sauvegardez le fichier
4. Redémarrez le serveur backend

## Vérification

Après avoir ajouté les variables et redémarré le serveur, vous ne devriez plus voir l'erreur `STRIPE_SECRET_KEY is not configured`.





