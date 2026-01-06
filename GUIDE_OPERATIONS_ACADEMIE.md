# 🏟️ Guide Académie : Gestion & Opérations

Ce guide détaille les outils mis à disposition des gérants d'académie pour piloter leur activité, gérer les inscriptions et anticiper leurs revenus.

---

## 1. 📋 Gestion des Abonnements
L'académie dispose d'un contrôle total sur le cycle de vie des abonnements de ses élèves.

### Fonctionnalités Clés :
*   **Listing Filtré** : Visualisation de tous les abonnements avec filtres par statut (`ACTIVE`, `PENDING`, `EXPIRED`, `CANCELLED`), statut de paiement, parent ou enfant.
*   **Cycle de Vie** :
    *   **Aperçu** : Lecture des détails (dates, offre choisie, options sélectionnées).
    *   **Suspension / Reprise** : Possibilité de suspendre un abonnement (ex: blessure d'un élève) et de le reprendre plus tard.
    *   **Annulation** : Résiliation d'un abonnement en cas de départ définitif.
*   **Paiements Manuels** : En plus de Stripe, l'académie peut enregistrer des paiements reçus en **Espèces (CASH)** ou **Chèque** mettant ainsi à jour automatiquement le statut de l'élève.

---

## 2. 🏷️ Gestion des Offres
L'académie crée et configure les produits qu'elle vend aux parents.

*   **Capacité Maximale** : Chaque offre possède une limite de places. Le système vérifie automatiquement si l'offre est "Complète" (`isFull`) avant d'autoriser un nouvel achat.
*   **Options de l'Offre** : L'académie peut définir des options additionnelles (ex: tenue sportive, assurance, transport) qui s'ajoutent au prix de base lors du paiement.
*   **Réductions** : Gestion des remises en pourcentage (`discountPct`).

---

## 3. 📈 Prévisions & Analytics (IA Décisionnelle)
Un module d'intelligence décisionnelle a été implémenté pour aider l'académie à comprendre sa santé financière.

### Prévision des Revenus :
*   **Algorithme** : Utilisation d'une Régression Linéaire basée sur l'historique réel des transactions réussies.
*   **Détails fournis** :
    *   **Historique** : Agrégation des revenus passés mois par mois.
    *   **Prédiction** : Estimation des revenus pour les 6 prochains mois.
    *   **Indicateur de Tendance** : Analyse si l'activité est croissante, décroissante ou stable.
    *   **Taux de Croissance** : Estimation du gain (ou perte) moyen de chiffre d'affaires par mois (en TND).
    *   **Indice de Fiabilité** : Score de confiance de la prédiction fondé sur l'échantillon de données disponible (`rSquared`).

---

## 4. 🔔 Notifications Automatiques
L'académie bénéficie d'une automatisation complète pour réduire la charge administrative :

*   **Alertes d'Expiration (Cron)** : Un script tourne tous les matins à **09h00**. Il identifie les abonnements finissant dans **7 jours** et envoie automatiquement :
    *   Un **Email** premium au parent.
    *   Un **SMS** de rappel.
*   **Notifications Coaches** : Envoi de notifications push via Firebase aux entraîneurs dès qu'une nouvelle inscription est confirmée pour un tournoi ou un stage.

---

## 🧪 Accès Technique (Endpoints API)

Toutes les actions de prévision et de gestion globale sont sécurisées par le rôle `ACADEMIE`.

| Rôle | Méthode | Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **ACADEMIE** | `GET` | `/subscriptions` | Liste tous les abonnements |
| **ACADEMIE** | `POST` | `/subscriptions/:id/suspend` | Suspendre un élève |
| **ACADEMIE** | `POST` | `/subscriptions/:id/pay` | Enregistrer un paiement manuel |
| **ACADEMIE** | `GET` | `/analytics/revenue-forecast` | Prévisions financières (IA) |
| **ACADEMIE** | `PATCH` | `/offers/:id` | Modifier les tarifs ou capacités |

---
*Note : Ce guide est mis à jour selon les dernières implémentations du backend (Décembre 2025).*
