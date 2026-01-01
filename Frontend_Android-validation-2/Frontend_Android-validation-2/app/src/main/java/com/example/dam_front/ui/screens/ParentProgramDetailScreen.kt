package com.example.dam_front.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dam_front.config.ApiConfig
import com.example.dam_front.models.Program
import com.example.dam_front.ui.components.*
import com.example.dam_front.ui.theme.*

@Composable
fun ParentProgramDetailScreen(
    program: Program,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    onActivityClick: (com.example.dam_front.models.ProgramActivity) -> Unit = {},
    onSubscribeClick: () -> Unit = {}
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ParentProgramDetailHeader(
                programName = program.nomProgramme,
                onBackClick = onBackClick,
                onMenuClick = onMenuClick
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
// ... rest of content remains same ...
                // Hero Image Card with Price Overlay
                if (!program.image.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(28.dp))
                    ) {
                        val imageUrl = if (program.image.startsWith("http")) {
                            program.image
                        } else {
                            val cleanPath = if (program.image.startsWith("/")) program.image.substring(1) else program.image
                            "${ApiConfig.BASE_URL}/$cleanPath"
                        }

                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Image du programme",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Price Badge Overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = SportyOrange
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = String.format("%.2f", program.prix ?: 0.0),
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "TND",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Level Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SportyDarkBlue.copy(alpha = 0.9f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = (program.niveau ?: "TOUS NIVEAUX").uppercase(),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // Quick Info Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Activities Count Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(IconBlueLight.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                ListIcon(tint = IconBlueLight, modifier = Modifier.size(24.dp))
                            }
                            Text(
                                text = "${program.activites.size}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkGray
                            )
                            Text(
                                text = "Activités",
                                fontSize = 12.sp,
                                color = SportyMutedText
                            )
                        }
                    }

                    // Status Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(SportyLime.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                TargetIcon(tint = SportyLime, modifier = Modifier.size(24.dp))
                            }
                            Text(
                                text = "ACTIF",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SportyLime
                            )
                            Text(
                                text = "Disponible",
                                fontSize = 12.sp,
                                color = SportyMutedText
                            )
                        }
                    }
                }

                // Program Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Objective
                        if (!program.objectif.isNullOrBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(IconOrange.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        TargetIcon(tint = IconOrange, modifier = Modifier.size(18.dp))
                                    }
                                    Text(
                                        text = "Objectif",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDarkGray
                                    )
                                }
                                Text(
                                    text = program.objectif,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    color = TextLightGray
                                )
                            }
                        }

                        Divider(color = SportyDivider)

                        // Description
                        if (!program.description.isNullOrBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(IconBlue.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DescriptionIcon(tint = IconBlue, modifier = Modifier.size(18.dp))
                                    }
                                    Text(
                                        text = "Description",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDarkGray
                                    )
                                }
                                Text(
                                    text = program.description,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    color = TextLightGray
                                )
                            }
                        }
                    }
                }

                // Activities Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(IconGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ListIcon(tint = IconGreen, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = "Activités incluses",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkGray
                                )
                            }
                            Text(
                                text = "${program.activites.size}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = IconGreen
                            )
                        }

                        if (program.activites.isEmpty()) {
                            Text(
                                text = "Aucune activité associée",
                                color = SportyMutedText,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                program.activites.forEach { activity ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onActivityClick(activity) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = SportyCardTint
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = activity.nomActivite ?: "Activité ${activity.id.take(6)}",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextDarkGray
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    CalendarIcon(tint = SportyMutedText, modifier = Modifier.size(14.dp))
                                                    Text(
                                                        text = activity.categorie ?: "Catégorie",
                                                        fontSize = 13.sp,
                                                        color = SportyMutedText
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(IconOrange.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                ArrowForwardIcon(tint = IconOrange, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // Fixed Bottom Subscribe Button (moved out of scroll)
             Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Capacity Information
                    if (program.capaciteMaximale != null) {
                        val nombreInscrits = program.nombreInscrits ?: 0
                        val placesDisponibles = program.capaciteMaximale - nombreInscrits
                        val isFull = placesDisponibles <= 0
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isFull) Color(0xFFFFEBEE) 
                                    else if (placesDisponibles <= 3) Color(0xFFFFF3E0)
                                    else Color(0xFFE8F5E9)
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PeopleIcon(
                                    tint = if (isFull) Color(0xFFD32F2F) 
                                           else if (placesDisponibles <= 3) Color(0xFFF57C00)
                                           else Color(0xFF388E3C),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isFull) "Programme complet" 
                                               else "Places disponibles",
                                        fontSize = 11.sp,
                                        color = SportyMutedText
                                    )
                                    Text(
                                        text = if (isFull) "Toutes les places sont réservées"
                                               else "$placesDisponibles place(s) restante(s)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFull) Color(0xFFD32F2F) 
                                                else if (placesDisponibles <= 3) Color(0xFFF57C00)
                                                else Color(0xFF388E3C)
                                    )
                                }
                            }
                            Text(
                                text = "$nombreInscrits/${program.capaciteMaximale}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SportyDarkBlue
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tarif du programme",
                                fontSize = 13.sp,
                                color = SportyMutedText
                            )
                            Text(
                                text = String.format("%.2f TND", program.prix ?: 0.0),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SportyOrange
                            )
                        }
                        
                        // Check if program is full
                        val nombreInscrits = program.nombreInscrits ?: 0
                        val capaciteMaximale = program.capaciteMaximale
                        val isFull = capaciteMaximale != null && (nombreInscrits >= capaciteMaximale)
                        
                        Button(
                            onClick = onSubscribeClick,
                            enabled = !isFull,
                            modifier = Modifier
                                .height(56.dp)
                                .weight(1f)
                                .padding(start = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (!isFull)
                                            Brush.horizontalGradient(listOf(SportyOrange, SportyLime))
                                        else
                                            Brush.horizontalGradient(listOf(Color.Gray, Color.Gray))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isFull) "Complet" else "S'inscrire maintenant",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentProgramDetailHeader(
    programName: String,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Transparent)
                ) {
                    ArrowBackIcon(tint = SportyDarkBlue, modifier = Modifier.size(24.dp))
                }
                Text(
                    text = "Détail du programme",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SportyDarkBlue
                )
            }
            
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Transparent)
            ) {
                MenuIcon(tint = SportyDarkBlue)
            }
        }
    }
}

// Helper icon composable if not exists
@Composable
fun ArrowForwardIcon(tint: Color, modifier: Modifier = Modifier) {
    // Use existing icon or create a simple arrow
    Text(
        text = "→",
        color = tint,
        fontSize = 18.sp,
        modifier = modifier
    )
}
