# ⚠️ URGENT : Ajouter les variables Stripe dans .env

## Erreur actuelle
```
Error: STRIPE_SECRET_KEY is not configured
```

## Solution

Ouvrez le fichier `.env` dans `C:\Users\hatem\OneDrive\Bureau\Backend\` et ajoutez ces lignes :

```env
# Stripe Configuration
STRIPE_SECRET_KEY=sk_test_51SV9DGHQ95HR9DAI2oVkJgxhwzJGTIaspfxrziZUsglPtwGuBmOXdBEhQj7X3nZSoJhLNRI0B9hrX23IiOiBBUel00KvsbelPY
STRIPE_PUBLISHABLE_KEY=pk_test_51SV9DGHQ95HR9DAIM06ZwtZcdrI4VWkwDvZ5SToJa1gWrRdk4mKrm6ZmNmlrYZmIdBubtxOyzD5POs5ImqCVBk6W00VGzt9lMG
```

## Étapes

1. Ouvrez le fichier `.env` dans `C:\Users\hatem\OneDrive\Bureau\Backend\`
2. Ajoutez les deux lignes ci-dessus à la fin du fichier
3. Sauvegardez le fichier
4. Redémarrez le serveur backend

## Vérification

Après avoir ajouté les variables et redémarré le serveur, vous ne devriez plus voir l'erreur `STRIPE_SECRET_KEY is not configured`.










