package com.example.dam_front.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.ui.screens.AiSummaryResult
import com.example.dam_front.ui.theme.*

@Composable
fun SummaryContent(result: AiSummaryResult) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SummarySection(title = "📌 Résumé court", content = result.shortSummary, iconColor = HeaderBlue)
        SummarySectionList(title = "💪 Points forts", items = result.strengths, iconColor = SportyKidsGreen)
        SummarySectionList(title = "⚠️ Points à améliorer", items = result.improvements, iconColor = SportyKidsRed)
        SummarySection(title = "🧭 Conseils aux parents", content = result.counsel, iconColor = IconOrange)
        SummarySectionList(title = "🎯 Objectifs à venir", items = result.goals, iconColor = Color(0xFF9C27B0))
    }
}

@Composable
fun SummarySection(title: String, content: String, iconColor: Color) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = iconColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text(content, fontSize = 15.sp, color = TextDarkGray, lineHeight = 20.sp)
    }
}

@Composable
fun SummarySectionList(title: String, items: List<String>, iconColor: Color) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = iconColor)
        Spacer(modifier = Modifier.height(4.dp))
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(iconColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(item, fontSize = 15.sp, color = TextDarkGray)
            }
        }
    }
}
