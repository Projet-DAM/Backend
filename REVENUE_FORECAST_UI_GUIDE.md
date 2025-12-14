# 🎨 Implémentation du Nouvel Écran de Prévision des Revenus

Ce guide contient le code complet pour mettre à jour votre écran `RevenueForecastScreen` (Jetpack Compose) afin d'inclure des graphiques (courbes et camemberts) et une meilleure explication, comme demandé.

## 1. Mise à jour des Modèles de Données

Ajoutez ces classes dans votre fichier de modèles (ex: `AnalyticsModels.kt`) pour correspondre à la nouvelle réponse du backend.

```kotlin
package com.sportyconnect.kids.models

import com.google.gson.annotations.SerializedName

data class RevenueForecastResponse(
    val history: List<RevenueDataPoint>,
    val forecast: List<RevenueDataPoint>,
    val distribution: List<RevenueDistributionItem> = emptyList(), // Nouveau
    val trend: String,
    val growthRate: Double,
    val explanation: String? = null, // Nouveau text explicatif
    val metrics: ForecastMetrics? = null // Nouvelles métriques
)

data class RevenueDataPoint(
    val month: String,
    val revenue: Double,
    val isPredicted: Boolean = false
)

data class RevenueDistributionItem(
    val name: String,
    val revenue: Double,
    val percentage: Double
)

data class ForecastMetrics(
    val rSquared: Double,
    val sampleSize: Int
)
```

## 2. Code Complet de l'Écran (RevenueForecastScreen.kt)

Remplacez le contenu de votre écran actuel par celui-ci. Il utilise `Canvas` pour dessiner les graphiques sans bibliothèque externe.

```kotlin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
// Assurez-vous d'importer vos modèles et couleurs correctement
// import com.sportyconnect.kids.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevenueForecastScreen(
    navController: NavController,
    viewModel: RevenueForecastViewModel = viewModel() // Assurez-vous d'avoir ce ViewModel
) {
    val forecastData by viewModel.forecastData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Couleurs "Premium"
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFFF5F7FA), Color(0xFFFFFFFF))
    )
    val primaryColor = Color(0xFF3B82F6) // Bleu moderne
    val secondaryColor = Color(0xFF10B981) // Vert succès

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prévision des Revenus", fontWeight = FontWeight.Bold) },
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
                .background(gradientBackground)
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (forecastData != null) {
                val data = forecastData!!
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. Carte d'Explication (IA Insight)
                    InsightCard(data)

                    // 2. Graphique de l'Historique et Prévision (Line Chart)
                    ChartCard(
                        title = "Évolution & Prédiction",
                        subtitle = "Historique (plein) vs Futur (pointillé)"
                    ) {
                        ForecastLineChart(
                            history = data.history,
                            forecast = data.forecast,
                            modifier = Modifier.height(250.dp).fillMaxWidth()
                        )
                    }

                    // 3. Répartition par Offre (Pie Chart)
                    if (data.distribution.isNotEmpty()) {
                        ChartCard(
                            title = "Sources de Revenus",
                            subtitle = "Répartition par offre"
                        ) {
                            DonutChart(
                                data = data.distribution,
                                modifier = Modifier.height(220.dp).fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                Text("Aucune donnée disponible", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

// --- Composants UI ---

@Composable
fun InsightCard(data: RevenueForecastResponse) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "IA",
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Analyse IA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = data.explanation ?: "Analyse en cours...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569),
                        lineHeight = 20.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Indicateurs Clés
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Tendance",
                    value = data.trend.capitalize(),
                    color = when (data.trend) {
                        "croissante" -> Color(0xFF10B981)
                        "décroissante" -> Color(0xFFEF4444)
                        else -> Color(0xFFF59E0B)
                    },
                    icon = when (data.trend) {
                        "croissante" -> Icons.Default.TrendingUp
                        "décroissante" -> Icons.Default.TrendingDown
                        else -> Icons.Default.TrendingFlat
                    }
                )
                
                MetricItem(
                    label = "Fiabilité",
                    value = if ((data.metrics?.rSquared ?: 0.0) > 0.6) "Haute" else "Moyenne",
                    color = Color(0xFF6366F1)
                )
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
fun ChartCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

// --- Graphique Linéaire (Canvas) ---
@Composable
fun ForecastLineChart(
    history: List<RevenueDataPoint>,
    forecast: List<RevenueDataPoint>,
    modifier: Modifier = Modifier
) {
    val allPoints = history + forecast
    if (allPoints.isEmpty()) return

    val maxRevenue = allPoints.maxOfOrNull { it.revenue }?.toFloat() ?: 100f
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 40f
        
        // Echelle Y
        val scaleY = (height - padding * 2) / maxRevenue
        val stepX = (width - padding * 2) / (allPoints.size - 1).coerceAtLeast(1)

        // Points
        val points = allPoints.mapIndexed { index, point ->
            Offset(
                x = padding + index * stepX,
                y = height - padding - (point.revenue.toFloat() * scaleY)
            )
        }

        // Dessiner la grille
        drawLine(Color.LightGray.copy(alpha=0.5f), Offset(padding, height-padding), Offset(width-padding, height-padding), strokeWidth = 2f)
        
        // Dessiner le chemin (History)
        val historyPath = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until history.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        drawPath(
            path = historyPath,
            color = Color(0xFF3B82F6),
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )

        // Dessiner le chemin (Forecast - Pointillé)
        if (forecast.isNotEmpty() && history.isNotEmpty()) {
            val lastHistoryPt = points[history.lastIndex]
            val forecastPath = Path().apply {
                moveTo(lastHistoryPt.x, lastHistoryPt.y)
                for (i in history.size until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = forecastPath,
                color = Color(0xFF10B981),
                style = Stroke(
                    width = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                    cap = StrokeCap.Round
                )
            )
        }

        // Dessiner les points
        points.forEachIndexed { index, point ->
            val color = if (index < history.size) Color(0xFF3B82F6) else Color(0xFF10B981)
            drawCircle(color = Color.White, radius = 8f, center = point)
            drawCircle(color = color, radius = 6f, center = point)
        }
    }
}

// --- Graphique Camembert (Donut) ---
@Composable
fun DonutChart(
    data: List<RevenueDistributionItem>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.revenue }
    var startAngle = -90f
    
    // Palette de couleurs
    val colors = listOf(
        Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), 
        Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899)
    )

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        // Le Graphique
        Canvas(modifier = Modifier.weight(1f).aspectRatio(1f)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 20f
            val donutHoleRadius = radius * 0.6f

            data.forEachIndexed { index, item ->
                val sweepAngle = (item.revenue / total).toFloat() * 360f
                val color = colors[index % colors.size]
                
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = radius - donutHoleRadius)
                )
                startAngle += sweepAngle
            }
        }

        // La Légende
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(colors[index % colors.size], CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(item.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("${item.revenue.toInt()} TND", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}
```

## Instructions
1.  **Copiez** ce code dans votre projet Android.
2.  **Remplacez** votre écran actuel.
3.  **Compilez** : Vous aurez maintenant une interface riche avec des graphiques natifs, sans bug de mise en page.
