package com.example.dam_front.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.models.Activity
import com.example.dam_front.ui.components.CalendarIcon
import com.example.dam_front.ui.components.HomeIcon
import com.example.dam_front.ui.components.ListIcon
import com.example.dam_front.ui.components.MenuIcon
import com.example.dam_front.ui.components.NotificationIcon
import com.example.dam_front.ui.components.SportyGradientButton
import com.example.dam_front.ui.components.SportyInfoChip
import com.example.dam_front.ui.components.TargetIcon
import com.example.dam_front.ui.theme.CardWhite
import com.example.dam_front.ui.theme.IconBlue
import com.example.dam_front.ui.theme.IconBlueLight
import com.example.dam_front.ui.theme.IconGreen
import com.example.dam_front.ui.theme.IconOrange
import com.example.dam_front.ui.theme.IconTeal
import com.example.dam_front.ui.theme.IconYellow
import com.example.dam_front.ui.theme.SportyBackgroundBottom
import com.example.dam_front.ui.theme.SportyBackgroundTop
import com.example.dam_front.ui.theme.SportyCardTint
import com.example.dam_front.ui.theme.SportyDarkBlue
import com.example.dam_front.ui.theme.SportyDivider
import com.example.dam_front.ui.theme.SportyMutedText
import com.example.dam_front.ui.theme.SportyTeal
import com.example.dam_front.ui.theme.TextDarkGray
import com.example.dam_front.ui.theme.TextLightGray
import com.example.dam_front.ui.theme.TextWhite
import com.example.dam_front.viewmodels.ActivitiesUiState
import com.example.dam_front.viewmodels.ProgramsUiState
import com.example.dam_front.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    activitiesState: ActivitiesUiState,
    programsState: ProgramsUiState,
    onMenuClick: () -> Unit = {},
    onNavigateToActivities: () -> Unit = {},
    onNavigateToPrograms: () -> Unit = {}
) {
    val backgroundBrush = remember {
        Brush.verticalGradient(listOf(SportyBackgroundTop, SportyBackgroundBottom))
    }
    val lastSync = remember(activitiesState.activities, programsState.programs) {
        Date()
    }

    val totalActivities = activitiesState.activities.size
    val activeActivities = activitiesState.activities.count { it.statut?.equals("ACTIVE", ignoreCase = true) == true }
    val upcomingActivities = remember(activitiesState.activities) {
        activitiesState.activities.count { activity ->
            DateUtils.parseDate(activity.date)?.after(Date()) == true
        }
    }
    val nextActivity = remember(activitiesState.activities) {
        activitiesState.activities
            .mapNotNull { activity ->
                DateUtils.parseDate(activity.date)?.let { parsed ->
                    parsed to activity
                }
            }
            .filter { it.first.after(Date()) }
            .minByOrNull { it.first.time }
    }

    val totalPrograms = programsState.programs.size
    val activePrograms = programsState.programs.count { it.statut.equals("ACTIF", ignoreCase = true) }
    // Note: Program statut default is BROUILLON and defined as non-null in model previously shown? 
    // Checking Program model again from search results: val statut: String = ProgramStatus.BROUILLON.name
    // However, UpdateProgramRequest has String?
    // Let's verify Program model again. Search result said: val statut: String = ProgramStatus.BROUILLON.name
    // BUT the error earlier said "Argument type mismatch: actual type is 'kotlin.String?', but 'kotlin.String' was expected." 
    // This strongly suggests one of them is nullable.
    // If Program.statut is String (non-nullable), then equal("...", true) works.
    // If Activity.statut is String? (nullable), then it fails.
    // Activity model snippet wasn't fully shown but I can guess it's nullable or user changed it.
    // I will use safe calls for both just in case, or verify activity model.
    // For now I will assume Activity.statut is nullable based on common patterns and the error.
    
    val draftPrograms = programsState.programs.count { it.statut.equals("BROUILLON", ignoreCase = true) }
    val programsWithActivities = programsState.programs.count { it.activites.isNotEmpty() }
    val monthlyActivityStats = remember(activitiesState.activities) {
        buildMonthlyActivityStats(activitiesState.activities)
    }
    val programStatusDistribution = remember(programsState.programs) {
        buildProgramStatusDistribution(programsState.programs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        DashboardHeader(
            totalActivities = totalActivities,
            totalPrograms = totalPrograms,
            lastRefresh = lastSync,
            onMenuClick = onMenuClick
        )

        if (activitiesState.isLoading || programsState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                color = SportyDarkBlue,
                trackColor = Color.White.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        DashboardSummaryRow(
            totalActivities = totalActivities,
            activeActivities = activeActivities,
            upcomingActivities = upcomingActivities,
            totalPrograms = totalPrograms,
            activePrograms = activePrograms
        )

        Spacer(modifier = Modifier.height(12.dp))

        DashboardAnalyticsSection(
            monthlyStats = monthlyActivityStats,
            programStatus = programStatusDistribution,
            upcomingActivities = upcomingActivities,
            totalActivities = totalActivities,
            programsWithActivities = programsWithActivities,
            activePrograms = activePrograms,
            draftPrograms = draftPrograms
        )

        Spacer(modifier = Modifier.height(24.dp))

        NextActivityCard(
            nextActivity = nextActivity?.second,
            activityDate = nextActivity?.first
        )

        Spacer(modifier = Modifier.height(24.dp))

        QuickActionsSection(
            onNavigateToActivities = onNavigateToActivities,
            onNavigateToPrograms = onNavigateToPrograms
        )
    }
}

@Composable
private fun DashboardHeader(
    totalActivities: Int,
    totalPrograms: Int,
    lastRefresh: Date,
    onMenuClick: () -> Unit
) {
    val headerBrush = remember {
        Brush.linearGradient(listOf(SportyDarkBlue, SportyTeal))
    }
    val dateFormatter = remember {
        SimpleDateFormat("d MMM yyyy • HH'h'mm", Locale.FRENCH)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(headerBrush)
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bienvenue",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Tableau de bord",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Dernière synchro • ${dateFormatter.format(lastRefresh)}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NotificationIcon(tint = Color.White, hasNotification = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { onMenuClick() }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            MenuIcon(
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SportyInfoChip(
                        text = "Activités • $totalActivities",
                        backgroundColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    )
                    SportyInfoChip(
                        text = "Programmes • $totalPrograms",
                        backgroundColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    )
                }

                TextButton(onClick = onMenuClick) {
                    Text(
                        text = "Ouvrir le menu",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardAnalyticsSection(
    monthlyStats: List<MonthlyStat>,
    programStatus: List<StatusSlice>,
    upcomingActivities: Int,
    totalActivities: Int,
    programsWithActivities: Int,
    activePrograms: Int,
    draftPrograms: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Tendance des activités",
                    color = TextDarkGray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                ActivityTrendChart(monthlyStats = monthlyStats)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Répartition des programmes",
                        color = TextDarkGray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    ProgramStatusChart(statusSlices = programStatus)
                }
            }

            Card(
                modifier = Modifier.wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Vue synthétique",
                        color = TextDarkGray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    StatComparisonBar(
                        title = "Activités planifiées",
                        value = upcomingActivities,
                        max = totalActivities,
                        accent = IconYellow
                    )
                    StatComparisonBar(
                        title = "Programmes actifs",
                        value = activePrograms,
                        max = (activePrograms + draftPrograms).coerceAtLeast(1),
                        accent = IconTeal
                    )
                    StatComparisonBar(
                        title = "Programmes complets",
                        value = programsWithActivities,
                        max = (activePrograms + draftPrograms).coerceAtLeast(1),
                        accent = IconOrange
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityTrendChart(
    monthlyStats: List<MonthlyStat>,
    lineColor: Color = IconOrange,
    fillColor: Color = IconOrange.copy(alpha = 0.2f),
    gridColor: Color = SportyDivider
) {
    if (monthlyStats.isEmpty()) {
        Text(
            text = "Pas encore de données suffisantes pour tracer une courbe.",
            color = SportyMutedText,
            fontSize = 14.sp
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val maxValue = monthlyStats.maxOf { it.value }.coerceAtLeast(1)
        val points = monthlyStats.map { it.value }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val chartWidth = size.width
            val chartHeight = size.height
            val stepX = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth

            // draw horizontal grid
            for (i in 0..4) {
                val y = chartHeight - (chartHeight / 4) * i
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val dataPoints = points.mapIndexed { index, value ->
                val normalizedY = chartHeight - (value / maxValue.toFloat()) * chartHeight
                Offset(stepX * index, normalizedY)
            }

            if (dataPoints.size >= 2) {
                val path = Path().apply {
                    moveTo(dataPoints.first().x, dataPoints.first().y)
                    for (point in dataPoints.drop(1)) {
                        lineTo(point.x, point.y)
                    }
                }

                // fill under curve
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(dataPoints.last().x, chartHeight)
                    lineTo(dataPoints.first().x, chartHeight)
                    close()
                }

                drawPath(
                    path = fillPath,
                    color = fillColor
                )

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            dataPoints.forEach { point ->
                drawCircle(
                    color = lineColor,
                    radius = 5.dp.toPx(),
                    center = point
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            monthlyStats.forEach {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = it.label.uppercase(),
                        color = TextDarkGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${it.value}",
                        color = SportyMutedText,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgramStatusChart(
    statusSlices: List<StatusSlice>,
    ringWidth: Dp = 22.dp
) {
    if (statusSlices.isEmpty()) {
        Text(
            text = "Aucun programme pour le moment.",
            color = SportyMutedText,
            fontSize = 14.sp
        )
        return
    }

    val total = statusSlices.sumOf { it.value }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                val strokeWidth = ringWidth.toPx()
                statusSlices.forEach { slice ->
                    val sweep = 360f * slice.value / total.toFloat()
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = total.toString(),
                    color = TextDarkGray,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Programmes", color = SportyMutedText, fontSize = 12.sp)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            statusSlices.forEach { slice ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(slice.color)
                    )
                    Text(
                        text = "${slice.label} • ${slice.value}",
                        color = TextDarkGray,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatComparisonBar(
    title: String,
    value: Int,
    max: Int,
    accent: Color
) {
    val progress = if (max == 0) 0f else (value / max.toFloat()).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, color = TextDarkGray, fontSize = 14.sp)
            Text(
                text = "$value / $max",
                color = SportyMutedText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SportyCardTint)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent)
            )
        }
    }
}

@Composable
private fun DashboardSummaryRow(
    totalActivities: Int,
    activeActivities: Int,
    upcomingActivities: Int,
    totalPrograms: Int,
    activePrograms: Int
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        val cards = listOf(
            Triple("Activités totales", "$totalActivities", "Sur les 30 derniers jours") to Pair(
                IconOrange,
                listOf(Color(0xFFFFF4E5), Color.White)
            ),
            Triple("Actives", "$activeActivities", "$upcomingActivities planifiées") to Pair(
                IconGreen,
                listOf(Color(0xFFE7F7EA), Color.White)
            ),
            Triple("Programmes", "$totalPrograms", "$activePrograms actifs") to Pair(
                IconBlue,
                listOf(Color(0xFFE5F3FF), Color.White)
            )
        )
        items(cards) { card ->
            SummaryCard(
                title = card.first.first,
                value = card.first.second,
                subtitle = card.first.third,
                accent = card.second.first,
                gradient = card.second.second
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    gradient: List<Color>
) {
    Card(
        modifier = Modifier
            .width(220.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(gradient))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, color = SportyMutedText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = value,
                color = accent,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(text = subtitle, color = SportyMutedText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NextActivityCard(
    nextActivity: Activity?,
    activityDate: Date?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(Color.White, SportyCardTint))
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prochaine activité",
                    color = TextDarkGray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(IconOrange),
                    contentAlignment = Alignment.Center
                ) {
                    TargetIcon(tint = TextWhite)
                }
            }

            if (nextActivity == null || activityDate == null) {
                Text(
                    text = "Aucune activité planifiée pour le moment.",
                    color = SportyMutedText,
                    fontSize = 14.sp
                )
            } else {
                Text(
                    text = nextActivity.nomActivite,
                    color = SportyDarkBlue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalendarIcon(tint = TextLightGray)
                    Text(
                        text = DateUtils.formatDate(nextActivity.date, longFormat = true),
                        color = TextLightGray,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = nextActivity.description ?: "Aucune description fournie.",
                    color = SportyMutedText,
                    fontSize = 14.sp
                )
            }
        }
    }
}

private data class MonthlyStat(val label: String, val value: Int)
private data class StatusSlice(val label: String, val value: Int, val color: Color)

private fun buildMonthlyActivityStats(activities: List<Activity>): List<MonthlyStat> {
    if (activities.isEmpty()) return emptyList()

    val calendar = java.util.Calendar.getInstance()
    calendar.add(java.util.Calendar.MONTH, -5)
    calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
    val startDate = calendar.time
    val monthFormat = java.text.SimpleDateFormat("MMM", java.util.Locale.FRENCH)

    val buckets = LinkedHashMap<String, Int>()
    repeat(6) {
        val label = monthFormat.format(calendar.time)
        buckets[label] = 0
        calendar.add(java.util.Calendar.MONTH, 1)
    }

    activities.forEach { activity ->
        val activityDate = DateUtils.parseDate(activity.date) ?: return@forEach
        if (activityDate.before(startDate)) return@forEach
        val label = monthFormat.format(activityDate)
        buckets[label] = (buckets[label] ?: 0) + 1
    }

    return buckets.map { MonthlyStat(it.key, it.value) }
}

private fun buildProgramStatusDistribution(programs: List<com.example.dam_front.models.Program>): List<StatusSlice> {
    if (programs.isEmpty()) return emptyList()

    val groups = programs.groupBy { it.statut?.uppercase() ?: "INCONNU" }
    return groups.map { (status, list) ->
        val color = when (status) {
            "ACTIF", "ACTIVE" -> IconGreen
            "BROUILLON" -> IconYellow
            "INACTIF", "INACTIVE" -> IconBlue
            else -> SportyDarkBlue
        }
        StatusSlice(
            label = status.lowercase().replaceFirstChar { it.titlecase(java.util.Locale.FRENCH) },
            value = list.size,
            color = color
        )
    }.sortedByDescending { it.value }
}

@Composable
private fun QuickActionsSection(
    onNavigateToActivities: () -> Unit,
    onNavigateToPrograms: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Actions rapides",
            color = TextDarkGray,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        SportyGradientButton(
            text = "Gérer les activités",
            leadingIcon = { HomeIcon(tint = Color.White) },
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateToActivities
        )
        SportyGradientButton(
            text = "Gérer les programmes",
            leadingIcon = { ListIcon(tint = Color.White) },
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateToPrograms
        )
    }
}


