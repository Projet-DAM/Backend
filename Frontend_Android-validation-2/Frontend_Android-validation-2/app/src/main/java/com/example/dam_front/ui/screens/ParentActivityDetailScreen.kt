package com.example.dam_front.ui.screens

import androidx.compose.foundation.background
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
import com.example.dam_front.models.Activity
import com.example.dam_front.ui.components.*
import com.example.dam_front.ui.theme.*
import com.example.dam_front.utils.DateUtils

@Composable
fun ParentActivityDetailScreen(
    activity: Activity,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ParentActivityDetailHeader(
                activityName = activity.nomActivite,
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
                // Hero Image Card with Category Badge
                val imagePath = activity.image
                if (!imagePath.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(28.dp))
                    ) {
                        val imageUrl = if (imagePath!!.startsWith("http")) {
                            imagePath
                        } else {
                            val cleanPath = if (imagePath!!.startsWith("/")) imagePath.substring(1) else imagePath
                            "${ApiConfig.BASE_URL}/$cleanPath"
                        }

                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Image de l'activité",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Category Badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SportyDarkBlue.copy(alpha = 0.9f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = (activity.categorie ?: "SPORT").uppercase(),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        // Price Badge
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
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = String.format("%.2f", activity.prix ?: 0.0),
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "TND",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Info Cards Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date Card
                    InfoCard(
                        icon = { CalendarIcon(tint = IconBlue, modifier = Modifier.size(20.dp)) },
                        title = "Date",
                        value = DateUtils.formatDate(activity.date, longFormat = false),
                        backgroundColor = IconBlue.copy(alpha = 0.15f),
                        iconColor = IconBlue,
                        modifier = Modifier.weight(1f)
                    )

                    // Duration Card
                    InfoCard(
                        icon = { ClockIcon(modifier = Modifier.size(20.dp), tint = IconOrange) },
                        title = "Durée",
                        value = "${activity.duree ?: 0} min",
                        backgroundColor = IconOrange.copy(alpha = 0.15f),
                        iconColor = IconOrange,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Time Card
                    InfoCard(
                        icon = { ClockIcon(modifier = Modifier.size(20.dp), tint = IconGreen) },
                        title = "Heure",
                        value = activity.heure ?: "--:--",
                        backgroundColor = IconGreen.copy(alpha = 0.15f),
                        iconColor = IconGreen,
                        modifier = Modifier.weight(1f)
                    )

                    // Capacity Card
                    InfoCard(
                        icon = { PeopleIcon(tint = IconTeal, modifier = Modifier.size(20.dp)) },
                        title = "Places",
                        value = "${activity.capaciteMax ?: 0}",
                        backgroundColor = IconTeal.copy(alpha = 0.15f),
                        iconColor = IconTeal,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Description Card
                if (!activity.description.isNullOrBlank()) {
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
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(IconBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DescriptionIcon(tint = IconBlue, modifier = Modifier.size(20.dp))
                                }
                                Text(
                                    text = "À propos de l'activité",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkGray
                                )
                            }
                            Text(
                                text = activity.description,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = TextLightGray
                            )
                        }
                    }
                }

                // Activity Details Card
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(IconOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                ListIcon(tint = IconOrange, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = "Informations détaillées",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkGray
                            )
                        }

                        DetailRow(
                            icon = { CalendarIcon(tint = SportyMutedText, modifier = Modifier.size(18.dp)) },
                            label = "Date complète",
                            value = DateUtils.formatDate(activity.date, longFormat = true)
                        )

                        Divider(color = SportyDivider)

                        DetailRow(
                            icon = { ClockIcon(modifier = Modifier.size(18.dp), tint = SportyMutedText) },
                            label = "Horaire",
                            value = activity.heure ?: "Non défini"
                        )

                        Divider(color = SportyDivider)

                        DetailRow(
                            icon = { ClockIcon(modifier = Modifier.size(18.dp), tint = SportyMutedText) },
                            label = "Durée de la séance",
                            value = "${activity.duree ?: 0} minutes"
                        )

                        Divider(color = SportyDivider)

                        DetailRow(
                            icon = { PeopleIcon(tint = SportyMutedText, modifier = Modifier.size(18.dp)) },
                            label = "Capacité maximale",
                            value = "${activity.capaciteMax ?: 0} participants"
                        )

                        Divider(color = SportyDivider)

                        DetailRow(
                            icon = { TargetIcon(tint = SportyMutedText, modifier = Modifier.size(18.dp)) },
                            label = "Catégorie",
                            value = activity.categorie ?: "Non spécifiée"
                        )
                    }
                }



                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ParentActivityDetailHeader(
    activityName: String,
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
                    text = "Détail de l'activité",
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
