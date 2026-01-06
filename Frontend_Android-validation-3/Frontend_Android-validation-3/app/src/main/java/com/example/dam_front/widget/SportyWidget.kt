package com.example.dam_front.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.cornerRadius
import com.example.dam_front.MainActivity
import com.example.dam_front.R
import com.example.dam_front.models.Enrollment
import com.example.dam_front.models.Program
import com.example.dam_front.repository.EnrollmentRepository
import com.example.dam_front.repository.ProgramRepository
import com.example.dam_front.utils.TokenManager
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SportyWidget : GlanceAppWidget() {
    
    companion object {
        suspend fun manualUpdate(context: Context) {
            try {
                android.util.Log.d("SportyWidget", "� manualUpdate - Global Refresh Triggered")
                
                // On utilise directement updateAll qui est la méthode standard et la plus fiable
                SportyWidget().updateAll(context)
                
                android.util.Log.d("SportyWidget", "✅ updateAll - Command sent to Glance system")
            } catch (t: Throwable) {
                android.util.Log.e("SportyWidget", "❌ manualUpdate - Error", t)
            }
        }
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(100.dp, 100.dp),
            DpSize(160.dp, 160.dp),
            DpSize(250.dp, 250.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            android.util.Log.d("SportyWidget", "� provideGlance [START] - ID: $id")
            
            val tokenManager = TokenManager(context)
            
            android.util.Log.d("SportyWidget", "读取 DataStore mirror (SharedPreferences)...")
            val role = tokenManager.getWidgetRole()?.lowercase()
            val token = tokenManager.getWidgetToken()
            val userId = tokenManager.getWidgetUserId()
            val userFirstName = tokenManager.getWidgetPrenom() ?: ""
            val userLastName = tokenManager.getWidgetNom() ?: ""
            
            android.util.Log.d("SportyWidget", "📊 Data Read Finished (SYNC): token=${if (token.isNullOrBlank()) "EMPTY" else "OK"}, role=$role, user=$userFirstName")
            
            val fullName = "$userFirstName $userLastName".trim().ifEmpty { "Sportif" }
            val isConnected = !token.isNullOrBlank()

            // Fetch Data with Timeouts and Fallbacks
            var coachPrograms: List<Program>? = null
            var lastEnrollment: Enrollment? = null
            
            if (isConnected) {
                if (role?.contains("coach") == true || role?.contains("acadé") == true) {
                    android.util.Log.d("SportyWidget", "🔎 Loading Coach Programs for $userId")
                    val programRepo = ProgramRepository(context)
                    coachPrograms = if (userId != null) {
                        try {
                            // On tente le réseau avec un timeout court (3s)
                            kotlinx.coroutines.withTimeoutOrNull(3000L) {
                                programRepo.getPrograms(coach = userId).getOrNull()?.take(2)
                            } ?: programRepo.getProgramsFromCache().take(2) // Fallback cache si timeout
                        } catch (e: Exception) {
                            android.util.Log.e("SportyWidget", "⚠️ CoachPrograms network failed, using cache", e)
                            programRepo.getProgramsFromCache().take(2)
                        }
                    } else null
                } else {
                    android.util.Log.d("SportyWidget", "🔎 Loading Parent Enrollments (Local)")
                    val enrollmentRepo = EnrollmentRepository(context)
                    val enrollments = try { 
                        enrollmentRepo.getEnrollments() 
                    } catch (e: Exception) { 
                        android.util.Log.e("SportyWidget", "⚠️ ParentEnrollments read failed", e)
                        emptyList() 
                    }
                    lastEnrollment = enrollments.lastOrNull()
                }
            }
            
            android.util.Log.d("SportyWidget", "🎨 Rendering Content...")
            
            val tournoiPrefs = context.getSharedPreferences("tournoi_cache", Context.MODE_PRIVATE)
            val tournoiNom = tournoiPrefs.getString("last_tournoi_nom", null)
            val tournoiDate = tournoiPrefs.getLong("last_inscription_date", 0)
            val formattedTournoiDate = if (tournoiDate > 0) {
                SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(tournoiDate))
            } else null

            provideContent {
                android.util.Log.d("SportyWidget", "🎨 provideContent calling WidgetRouter...")
                GlanceTheme {
                    WidgetRouter(
                        isConnected = isConnected,
                        role = role ?: "guest",
                        userName = fullName,
                        enrollment = lastEnrollment,
                        tournoiNom = tournoiNom,
                        tournoiDate = formattedTournoiDate,
                        coachPrograms = coachPrograms
                    )
                }
            }
            android.util.Log.d("SportyWidget", "✅ provideGlance [FINISHED SUCCESS]")
        } catch (ce: kotlinx.coroutines.CancellationException) {
            // Re-throw CancellationException pour que Glance puisse fermer la session proprement
            android.util.Log.d("SportyWidget", "ℹ️ provideGlance cancelled (Normal Glance shutdown)")
            throw ce
        } catch (t: Throwable) {
            android.util.Log.e("SportyWidget", "💥 FATAL ERROR in provideGlance", t)
            provideContent {
                GlanceTheme {
                    Box(modifier = GlanceModifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
                        Text("Service indisponible", style = TextStyle(fontSize = 12.sp))
                    }
                }
            }
        }
    }

    @Composable
    private fun WidgetRouter(
        isConnected: Boolean,
        role: String,
        userName: String,
        enrollment: Enrollment?,
        tournoiNom: String?,
        tournoiDate: String?,
        coachPrograms: List<Program>?
    ) {
        // App background and container - FULLY TRANSPARENT OUTER
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(8.dp)
        ) {
            // Conteneur principal subtilement teinté pour la lisibilité
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xD9FFFFFF))) // 85% blanc pour mieux voir sur les fonds variés
                    .cornerRadius(24.dp)
                    .padding(16.dp)
            ) {
                if (!isConnected) {
                    GuestWidgetUI()
                } else if (role.contains("coach") || role.contains("acadé")) {
                    CoachWidgetUI(userName, coachPrograms)
                } else {
                    ParentWidgetUI(userName, enrollment, tournoiNom, tournoiDate)
                }
            }
        }
    }

    @Composable
    private fun GuestWidgetUI() {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(50.dp)
                    .background(ColorProvider(Color(0xFFE3F2FD)))
                    .cornerRadius(25.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "👋", style = TextStyle(fontSize = 24.sp))
            }
            Spacer(GlanceModifier.height(12.dp))
            Text(
                text = "Bienvenue !",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF0E5C75)),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.glance.text.TextAlign.Center
                )
            )
            Text(
                text = "Connectez-vous pour voir vos activités",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF6F7B91)),
                    fontSize = 11.sp,
                    textAlign = androidx.glance.text.TextAlign.Center
                )
            )
            Spacer(GlanceModifier.height(16.dp))
            ModernButton("SE CONNECTER", Color(0xFF0E5C75))
        }
    }

    @Composable
    private fun ColumnScope.CoachWidgetUI(userName: String, programs: List<Program>?) {
        val brandBlue = Color(0xFF0E5C75)
        val brandOrange = Color(0xFFF16A2D)
        
        HeaderBar("TABLEAU DE BORD COACH", brandBlue)
        Spacer(GlanceModifier.height(14.dp))
        
        Column(modifier = GlanceModifier.defaultWeight()) {
            // Section Profil aérée
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(44.dp)
                        .background(ColorProvider(Color(0xFFE3F2FD)))
                        .cornerRadius(22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "�", style = TextStyle(fontSize = 20.sp))
                }
                Spacer(GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Coach",
                        style = TextStyle(color = ColorProvider(Color(0xFF6F7B91)), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = userName,
                        style = TextStyle(color = ColorProvider(brandBlue), fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            }
            
            // Titre de section plus élégant
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    text = "VOS PROGRAMMES ACTIFS", 
                    style = TextStyle(color = ColorProvider(brandBlue), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = "VOIR TOUT",
                    modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
                    style = TextStyle(color = ColorProvider(brandOrange), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(GlanceModifier.height(8.dp))
            
            if (programs.isNullOrEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(ColorProvider(Color(0xFFF8FAFC)))
                        .cornerRadius(16.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun programme en cours", 
                        style = TextStyle(color = ColorProvider(Color(0xFF94A3B8)), fontSize = 12.sp, textAlign = androidx.glance.text.TextAlign.Center)
                    )
                }
            } else {
                programs.forEach { program ->
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(ColorProvider(Color(0xFFF0F7FF))) // Bleu très clair "pro"
                            .cornerRadius(16.dp)
                            .padding(12.dp)
                            .clickable(actionStartActivity<MainActivity>())
                    ) {
                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            // Icône du programme
                            Box(
                                modifier = GlanceModifier
                                    .size(32.dp)
                                    .background(ColorProvider(Color.White))
                                    .cornerRadius(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "📅", style = TextStyle(fontSize = 16.sp))
                            }
                            
                            Spacer(GlanceModifier.width(12.dp))
                            
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = program.nomProgramme,
                                    style = TextStyle(color = ColorProvider(brandBlue), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                                // Statut ou Catégorie
                                Text(
                                    text = program.niveau ?: "Tous niveaux",
                                    style = TextStyle(color = ColorProvider(Color(0xFF64748B)), fontSize = 10.sp)
                                )
                            }
                            
                            // Badge Inscrits
                            Column(horizontalAlignment = Alignment.Horizontal.End) {
                                Text(
                                    text = (program.nombreInscrits ?: 0).toString(),
                                    style = TextStyle(color = ColorProvider(brandBlue), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "ÉLÈVES",
                                    style = TextStyle(color = ColorProvider(brandBlue), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(GlanceModifier.height(12.dp))
        ModernButton("ACCÉDER À L'ADMINISTRATION", brandBlue)
    }

    @Composable
    private fun ColumnScope.ParentWidgetUI(userName: String, enrollment: Enrollment?, tournoiNom: String?, tournoiDate: String?) {
        val size = LocalSize.current
        val isSmall = size.width < 150.dp
        val brandBlue = Color(0xFF0E5C75)
        val brandOrange = Color(0xFFF16A2D)

        HeaderBar("ESPACE PARENT", brandBlue)
        Spacer(GlanceModifier.height(8.dp))
        
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "Bonjour $userName",
                style = TextStyle(
                    color = ColorProvider(brandBlue),
                    fontSize = if (isSmall) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Spacer(GlanceModifier.height(8.dp))
            
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                DashboardTile(
                    title = "PROGRAMME",
                    subtitle = enrollment?.programName ?: "Explorer l'App",
                    icon = "🚀",
                    accent = ColorProvider(brandBlue),
                    modifier = GlanceModifier.defaultWeight()
                )
                if (!isSmall) {
                    Spacer(GlanceModifier.width(8.dp))
                    if (tournoiNom != null) {
                        DashboardTile(
                            title = "TOURNOI",
                            subtitle = tournoiNom,
                            date = tournoiDate,
                            icon = "🏆",
                            accent = ColorProvider(brandOrange),
                            modifier = GlanceModifier.defaultWeight()
                        )
                    } else {
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight()
                                .background(ColorProvider(Color(0x1A0E5C75)))
                                .cornerRadius(16.dp)
                                .padding(8.dp),
                            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(text = "✨", style = TextStyle(fontSize = 16.sp))
                            Text(
                                text = "Prêt pour\nun tournoi ?",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF506070)),
                                    fontSize = 10.sp,
                                    textAlign = androidx.glance.text.TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(GlanceModifier.height(8.dp))
        ModernButton("ACCÉDER À MON ESPACE", brandBlue)
    }

    @Composable
    private fun HeaderBar(label: String, color: Color) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_launcher_logo),
                contentDescription = null,
                modifier = GlanceModifier.size(22.dp)
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = label,
                style = TextStyle(
                    color = ColorProvider(color),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.defaultWeight())
            // Signal visuel de fraîcheur
            Box(
                modifier = GlanceModifier
                    .size(6.dp)
                    .background(ColorProvider(Color(0xFF9AD93A))) // Sporty Green
                    .cornerRadius(3.dp)
            ) {}
        }
    }

    @Composable
    private fun DashboardTile(
        title: String,
        subtitle: String,
        date: String? = null,
        icon: String,
        accent: ColorProvider,
        modifier: GlanceModifier
    ) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .background(ColorProvider(Color(0xFFFFFFFF)))
                .cornerRadius(16.dp)
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Box(
                    modifier = GlanceModifier
                        .size(24.dp)
                        .background(ColorProvider(Color(0xFFF1F8FA)))
                        .cornerRadius(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, style = TextStyle(fontSize = 14.sp))
                }
                if (date != null) {
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        text = date,
                        style = TextStyle(color = ColorProvider(Color(0xFFF16A2D)), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = title,
                style = TextStyle(color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = TextStyle(
                    color = ColorProvider(Color(0xFF2D3748)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 2
            )
        }
    }

    @Composable
    private fun ModernButton(label: String, color: Color) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(48.dp)
                .background(ColorProvider(color))
                .cornerRadius(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

