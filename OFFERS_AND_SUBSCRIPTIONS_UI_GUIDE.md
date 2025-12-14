# 🎨 Guide d'Amélioration UI : Offres et Abonnements (Parents)

Ce guide contient le code pour refondre les écrans "Offres" et "Abonnements" du Parent avec un design Premium, un affichage correct des prix en TND et une liste optimisée (scrolling fluide).

## 1. Utilitaire de Formatage des Prix (TND)

Créez un fichier `Extensions.kt` ou ajoutez ceci dans votre fichier `Utils.kt`. Cela garantira que tous les prix s'affichent uniformément (ex: "120.00 TND").

```kotlin
package com.sportyconnect.kids.utils

import java.util.Locale

// Extension pour formater les Double en TND
fun Double?.toTnd(): String {
    return if (this != null) {
        String.format(Locale.US, "%.2f TND", this)
    } else {
        "0.00 TND"
    }
}

// Extension pour formater les Int en TND
fun Int?.toTnd(): String {
    return if (this != null) {
        String.format(Locale.US, "%.2f TND", this.toDouble())
    } else {
        "0.00 TND"
    }
}
```

---

## 2. Nouvel Écran des Offres (`ParentOffersScreen.kt`)

**Améliorations majeures :**
*   **LazyColumn** : Remplace le simple `Column` pour un défilement fluide et performant, même avec beaucoup d'offres.
*   **Design Carte Premium** : Ombres douces, coins arrondis, et mise en valeur du prix.
*   **Bouton d'Action** : Clair et accessible.

```kotlin
package com.sportyconnect.kids.ui.offers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sportyconnect.kids.models.Offer
import com.sportyconnect.kids.utils.toTnd // Importez votre extension

// Assurez-vous d'avoir vos ViewModels injectés ou passés
// import com.sportyconnect.kids.viewmodels.OffersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentOffersScreen(
    navController: NavController,
    offers: List<Offer>, // Ou via ViewModel
    isLoading: Boolean = false,
    onSubscribeClick: (Offer) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nos Offres", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC)) // Fond gris très clair
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (offers.isEmpty()) {
                EmptyStateMessage(modifier = Modifier.align(Alignment.Center))
            } else {
                // Utilisation de LazyColumn pour un scroll performant
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(offers) { offer ->
                        PremiumOfferCard(
                            offer = offer,
                            onClick = { onSubscribeClick(offer) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumOfferCard(offer: Offer, onClick: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header: Titre et Prix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = offer.title, // Assurez-vous que le champ s'appelle 'title' ou 'name'
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${offer.durationMonths} Mois",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                }
                
                // Badge Prix
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = offer.price.toTnd(), // Utilisation de l'extension
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description courte
            if (!offer.description.isNullOrBlank()) {
                Text(
                    text = offer.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bouton D'action
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB)
                )
            ) {
                Text("Souscrire maintenant", modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

@Composable
fun EmptyStateMessage(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.SportsSoccer, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Aucune offre disponible pour le moment", color = Color.Gray)
    }
}
```

---

## 3. Nouvel Écran des Abonnements (`ParentSubscriptionsScreen.kt`)

**Améliorations :**
*   **Badge de Statut** : Visuel clair (Vert = Actif, Rouge = Expiré).
*   **Structure Claire** : Dates et montants bien alignés.

```kotlin
package com.sportyconnect.kids.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sportyconnect.kids.models.Subscription // Votre modèle Subscription
import com.sportyconnect.kids.utils.toTnd
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentSubscriptionsScreen(
    navController: NavController,
    subscriptions: List<Subscription>,
    isLoading: Boolean = false
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes Abonnements", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (subscriptions.isEmpty()) {
                Text("Aucun abonnement actif", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(subscriptions) { sub ->
                        SubscriptionCard(sub)
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionCard(sub: Subscription) { // Adaptez 'Subscription' à votre modèle réel
    // Déterminer le statut (Logique d'exemple)
    val isActive = sub.status == "ACTIVE" || sub.status == "active"
    val statusColor = if (isActive) Color(0xFF10B981) else Color(0xFFEF4444)
    val statusText = if (isActive) "ACTIF" else "EXPIRÉ"

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sub.offerTitle ?: "Abonnement", // Adaptez le champ titre
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(12.dp))

            // Détails : Prix et Dates
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("PRIX", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(sub.amount.toTnd(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("FIN LE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        formatDate(sub.endDate), // Fonction helper simple pour formater la date
                        style = MaterialTheme.typography.bodyMedium, 
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Helper simple pour date string (à adapter selon votre format date entrant)
fun formatDate(dateString: String?): String {
    if (dateString == null) return "-"
    return try {
        // Supposons un format ISO venant du backend, on veut l'afficher joliment
        // Ceci est un exemple, adaptez selon votre besoin
        dateString.take(10) 
    } catch (e: Exception) {
        dateString
    }
}
```

## Instructions d'Intégration

1.  **Copiez** la fonction `toTnd()` dans vos utilitaires de projet.
2.  **Remplacez** le contenu de vos fichiers `ParentOffersScreen.kt` et `ParentSubscriptionsScreen.kt` avec le code ci-dessus.
3.  **Vérifiez** les imports et les noms des champs de vos modèles de données (`Offer`, `Subscription`) pour qu'ils correspondent (ex: `offer.title` vs `offer.nom`, `offer.price` vs `offer.prix`).
