package com.example.dam_front.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.models.ChildResponse
import com.example.dam_front.ui.theme.*

@Composable
fun HeaderSection(
    title: String,
    onMenuClick: () -> Unit = {},
    children: List<ChildResponse> = emptyList(),
    selectedChildId: String? = null,
    onChildSelected: (String) -> Unit = {},
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    var showChildDropdown by remember { mutableStateOf(false) }
    val selectedChild = children.find { it.id == selectedChildId }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(HeaderBlue, HeaderBlueLight)
                )
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Section gauche : Bouton retour/Profil + Titre
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Bouton de retour OU Profile Picture
                if (showBackButton) {
                    // Bouton de retour
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    // Profile Picture - Clic pour ouvrir le menu (design moderne et compact)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        IconOrange.copy(alpha = 0.8f),
                                        IconOrangeAccent.copy(alpha = 0.6f)
                                    )
                                )
                            )
                            .clickable { 
                                onMenuClick() 
                            }
                            .padding(2.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            // Icône de profil moderne
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profil",
                                modifier = Modifier.size(24.dp),
                                tint = HeaderBlue
                            )
                        }
                    }
                }
                
                // Titre et sélecteur d'enfant
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        color = TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    // Sélecteur d'enfant
                    if (children.isNotEmpty()) {
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { showChildDropdown = true }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = selectedChild?.let { "${it.prenom} ${it.nom}" } ?: "Sélectionner un enfant",
                                    color = TextWhite.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Sélectionner un enfant",
                                    tint = TextWhite.copy(alpha = 0.9f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showChildDropdown,
                                onDismissRequest = { showChildDropdown = false }
                            ) {
                                children.forEach { child ->
                                    DropdownMenuItem(
                                        text = { Text("${child.prenom} ${child.nom}") },
                                        onClick = {
                                            onChildSelected(child.id)
                                            showChildDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Sporty ",
                                color = IconOrange,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Kids",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // Section droite : Notifications
            Box {
                IconButton(
                    onClick = { /* TODO: Ouvrir notifications */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = TextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                    // Badge de notification (si nécessaire)
                    // Vous pouvez ajouter un badge ici si vous avez des notifications non lues
                }
            }
        }
    }
}

