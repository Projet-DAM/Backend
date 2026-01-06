package com.example.dam_front.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.ui.theme.DAM_frontTheme
import com.example.dam_front.ui.theme.*

enum class SidebarScreen {
    ACCUEIL, TOURNOIS, ACTIVITES, PROGRES, MON_ENFANT, AJOUTER_ENFANT, PARAMETRES, DECONNEXION
}

@Composable
fun Sidebar(
    currentScreen: SidebarScreen,
    onScreenSelected: (SidebarScreen) -> Unit,
    userName: String = "Utilisateur",
    userEmail: String = "",
    onLogout: () -> Unit = {},
    isCoach: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        // User Profile Section
        UserProfileSection(
            userName = userName,
            userEmail = userEmail
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Navigation Items
        SidebarMenuItem(
            icon = { HomeIcon(tint = TextDarkGray) },
            label = "Accueil",
            isSelected = currentScreen == SidebarScreen.ACCUEIL,
            onClick = { onScreenSelected(SidebarScreen.ACCUEIL) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        SidebarMenuItem(
            icon = { TrophyIcon(tint = IconGreen) },
            label = "Tournois",
            isSelected = currentScreen == SidebarScreen.TOURNOIS,
            onClick = { onScreenSelected(SidebarScreen.TOURNOIS) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        SidebarMenuItem(
            icon = { 
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val rectSize = 8.dp.toPx()
                        val spacing = 2.dp.toPx()
                        val startX = (size.width - rectSize * 2 - spacing) / 2
                        val startY = (size.height - rectSize * 2 - spacing) / 2
                        
                        // Draw two stacked rectangles
                        drawRect(
                            color = IconOrange,
                            topLeft = Offset(startX, startY),
                            size = Size(rectSize, rectSize)
                        )
                        drawRect(
                            color = IconOrange,
                            topLeft = Offset(startX + rectSize + spacing, startY),
                            size = Size(rectSize, rectSize)
                        )
                        drawRect(
                            color = IconOrange,
                            topLeft = Offset(startX, startY + rectSize + spacing),
                            size = Size(rectSize, rectSize)
                        )
                        drawRect(
                            color = IconOrange,
                            topLeft = Offset(startX + rectSize + spacing, startY + rectSize + spacing),
                            size = Size(rectSize, rectSize)
                        )
                    }
                }
            },
            label = "Activités",
            isSelected = currentScreen == SidebarScreen.ACTIVITES,
            onClick = { onScreenSelected(SidebarScreen.ACTIVITES) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        SidebarMenuItem(
            icon = { ProgressIcon(tint = IconGreen) },
            label = "Progrès",
            isSelected = currentScreen == SidebarScreen.PROGRES,
            onClick = { onScreenSelected(SidebarScreen.PROGRES) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        SidebarMenuItem(
            icon = { PeopleIcon(tint = IconGreen) },
            label = if (isCoach) "Enfant" else "Mon Enfant",
            isSelected = currentScreen == SidebarScreen.MON_ENFANT,
            onClick = { onScreenSelected(SidebarScreen.MON_ENFANT) }
        )
        
        if (!isCoach) {
            Spacer(modifier = Modifier.height(8.dp))
            
            SidebarMenuItem(
                icon = { 
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Person,
                        contentDescription = "Ajouter un enfant",
                        tint = IconGreen,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = "Ajouter un enfant",
                isSelected = false,
                onClick = { onScreenSelected(SidebarScreen.AJOUTER_ENFANT) }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        SidebarMenuItem(
            icon = { SettingsIcon(tint = IconGreen) },
            label = "Paramètres",
            isSelected = currentScreen == SidebarScreen.PARAMETRES,
            onClick = { onScreenSelected(SidebarScreen.PARAMETRES) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        SidebarMenuItem(
            icon = { LogoutIcon(tint = IconGreen) },
            label = "Déconnexion",
            isSelected = false,
            onClick = onLogout
        )
    }
}

@Composable
fun UserProfileSection(
    userName: String,
    userEmail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(NavGrayBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                modifier = Modifier.size(24.dp),
                tint = TextDarkGray.copy(alpha = 0.6f)
            )
        }
        
        Column {
            Text(
                text = userName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkGray
            )
            Text(
                text = userEmail,
                fontSize = 14.sp,
                color = TextDarkGray.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SidebarMenuItem(
    icon: @Composable () -> Unit,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) NavGrayBg else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        icon()
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = TextDarkGray
        )
    }
}

@Preview(showBackground = true, device = "spec:width=280dp,height=800dp")
@Composable
fun SidebarPreview() {
    DAM_frontTheme {
        Sidebar(
            currentScreen = SidebarScreen.ACCUEIL,
            onScreenSelected = {},
            userName = "Microsoft 365",
            userEmail = "asma.arbi@esprit.tn",
            onLogout = {}
        )
    }
}

@Preview(showBackground = true, device = "spec:width=280dp,height=800dp")
@Composable
fun SidebarPreviewWithSelected() {
    DAM_frontTheme {
        Sidebar(
            currentScreen = SidebarScreen.TOURNOIS,
            onScreenSelected = {},
            userName = "Microsoft 365",
            userEmail = "asma.arbi@esprit.tn",
            onLogout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UserProfileSectionPreview() {
    DAM_frontTheme {
        UserProfileSection(
            userName = "Microsoft 365",
            userEmail = "asma.arbi@esprit.tn"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SidebarMenuItemPreview() {
    DAM_frontTheme {
        Column(
            modifier = Modifier
                .width(280.dp)
                .background(Color.White)
                .padding(16.dp)
        ) {
            SidebarMenuItem(
                icon = { HomeIcon(tint = TextDarkGray) },
                label = "Accueil",
                isSelected = true,
                onClick = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            SidebarMenuItem(
                icon = { TrophyIcon(tint = IconGreen) },
                label = "Tournois",
                isSelected = false,
                onClick = {}
            )
        }
    }
}

