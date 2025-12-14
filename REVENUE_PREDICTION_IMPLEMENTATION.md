# 🔮 Module de Prévision des Revenus (IA)

Ce module implémente une fonctionnalité de prédiction des revenus futurs basée sur l'historique des transactions. Il utilise un modèle de régression linéaire (séries temporelles simples) pour estimer le chiffre d'affaires des prochains mois.

## ✅ Fonctionnalités Implémentées

1.  **Agrégation des Données Historiques** : 
    - Analyse toutes les transactions validées (`SUCCESS`).
    - Groupe les revenus par mois.
    - Filtre automatiquement les données pour l'académie connectée.

2.  **Moteur de Prédiction (IA)** :
    - Utilise une régression linéaire sur l'historique mensuel.
    - Prédit le chiffre d'affaires pour les 6 prochains mois (par défaut).
    - Détecte la tendance globale (Croissante, Stable, Décroissante).

3.  **API Rest** :
    - Endpoint sécurisé pour les Académies et Admins.

---

## 🔧 Documentation de l'API

### Endpoint

```http
GET /analytics/revenue-forecast
```

**Paramètres (Query Params)** :
- `months` (optionnel) : Nombre de mois à prédire (Défaut: 6).

**Headers** :
- `Authorization: Bearer <JWT_TOKEN>`

### Exemple de Réponse

```json
{
  "history": [
    {
      "month": "2023-10",
      "date": "2023-10-01T00:00:00.000Z",
      "revenue": 1500,
      "transactions": 15
    },
    {
      "month": "2023-11",
      "date": "2023-11-01T00:00:00.000Z",
      "revenue": 1800,
      "transactions": 18
    }
  ],
  "forecast": [
    {
      "month": "2023-12",
      "revenue": 2100,
      "isPredicted": true
    },
    {
      "month": "2024-01",
      "revenue": 2400,
      "isPredicted": true
    }
  ],
  "trend": "croissante",
  "growthRate": 300
}
```

---

## 📱 Guide d'Intégration Mobile (Suggestion)

Bien que vous ayez demandé l'intégration backend uniquement, voici comment l'afficher simplement sur mobile :

1.  **Bouton** : Dans le menu "Admin" -> "Prévision des revenus".
2.  **Affichage** :
    - Un texte en gros : *"Revenu estimé mois prochain : 2 400 TND"*
    - Une flèche verte si `trend` est "croissante".
    - Un graphique simple (BarChart ou LineChart) montrant l'historique (barres pleines) et le futur (barres hachurées ou couleur différente).

## ⚠️ Notes Importantes

- Le modèle a besoin d'au moins **2 mois d'historique** pour fonctionner. Si moins de 2 mois de données sont disponibles, l'API retournera un tableau `forecast` vide et un message explicatif.
- L'algorithme actuel est une **Régression Linéaire**. C'est robuste pour dégager une tendance, mais ne prend pas en compte la saisonnalité complexe (ex: baisse en été). Pour une version future, on pourra utiliser des moyennes mobiles ou Holt-Winters.
