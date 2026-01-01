package com.example.dam_front.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dam_front.api.GeminiService
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.api.SuiviEnfantApiService
import com.example.dam_front.data.SuiviEnfant
import com.example.dam_front.models.User
import com.example.dam_front.ui.screens.AiSummaryResult
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.dam_front.email.AiSummaryParams
import com.example.dam_front.email.EmailRequest
import com.example.dam_front.email.EmailRetrofitClient
import java.text.SimpleDateFormat
import java.util.*

class AiSummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val suiviService = RetrofitClient.createAuthenticatedService(application, SuiviEnfantApiService::class.java)
    private val gson = Gson()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _summaryResult = MutableStateFlow<AiSummaryResult?>(null)
    val summaryResult = _summaryResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun generateSummary(child: User) {
        viewModelScope.launch {
            _isGenerating.value = true
            _error.value = null
            
            try {
                // 1. Fetch real suivis for this child
                val childId = child.id ?: return@launch
                val suivis = try { 
                    suiviService.getSuiviEnfantsByEnfantId(childId) 
                } catch (e: Exception) {
                    emptyList<SuiviEnfant>()
                }

                if (suivis.isEmpty()) {
                    _error.value = "Pas assez de données de suivi pour générer un résumé."
                    _isGenerating.value = false
                    return@launch
                }

                // 2. Prepare context for Gemini
                val contextData = prepareContext(child, suivis)
                val prompt = """
                    Tu es un coach sportif expert et bienveillant. Analyse les suivis détaillés suivants pour l'enfant ${child.prenom} ${child.nom}.
                    Prends en compte le type d'activité, le niveau d'effort, l'état émotionnel, les zones de focus et les objectifs fixés par le coach pour générer une analyse très précise.
                    
                    Données de suivi (plusieurs sessions séparées par ---) :
                    ${contextData}
                    
                    Réponds UNIQUEMENT au format JSON structure comme suit :
                    {
                      "shortSummary": "string (un résumé global encourageant et analytique)",
                      "strengths": ["string", "string"],
                      "improvements": ["string", "string"],
                      "counsel": "string (conseil pratique précis pour les parents basé sur les données)",
                      "goals": ["string", "string"]
                    }
                """.trimIndent()

                // 3. Call Gemini
                val resultJson = GeminiService.generateSummary(prompt)
                
                if (resultJson != null) {
                    val result = parseGeminiResponse(resultJson)
                    if (result != null) {
                        _summaryResult.value = result
                    } else {
                        _error.value = "Format de réponse IA invalide."
                    }
                } else {
                    _error.value = "Erreur lors de la génération avec l'IA."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Une erreur est survenue : ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun prepareContext(child: User, suivis: List<SuiviEnfant>): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return suivis.joinToString("\n---\n") { s ->
            """
            - Date: ${sdf.format(s.dateSuivi)}
            - Activité: ${s.activityType ?: "N/A"}
            - Présence: ${if (s.presence) "Oui" else "Non"}
            - Performance: ${s.performance}/10
            - Niveau d'effort: ${s.effortLevel?.let { "$it/10" } ?: "N/A"}
            - État émotionnel: ${s.emotionalState ?: "N/A"}
            - Zones de focus: ${s.focusAreas?.joinToString(", ") ?: "N/A"}
            - Objectifs prochaine session: ${s.nextSessionGoals?.joinToString(", ") ?: "N/A"}
            - Commentaire du coach: ${s.commentaire ?: "N/A"}
            """.trimIndent()
        }
    }

    fun sendSummaryEmail(child: User, summary: AiSummaryResult, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val parentEmail = "hadilaroua52@gmail.com"
                
                android.util.Log.d("AiSummaryVM", "Sending email to: $parentEmail for child ${child.prenom}")

                val params = AiSummaryParams(
                    parentName = "Parent de ${child.prenom}",
                    childName = "${child.prenom} ${child.nom}",
                    summaryContent = summary.shortSummary,
                    strengths = summary.strengths.joinToString("\n• ", prefix = "• "),
                    improvements = summary.improvements.joinToString("\n• ", prefix = "• "),
                    counsel = summary.counsel,
                    goals = summary.goals.joinToString("\n• ", prefix = "• "),
                    userEmail = parentEmail
                )
                
                // Log the params for debugging
                android.util.Log.d("AiSummaryVM", "Email params: parent_name=${params.parentName}, child_name=${params.childName}, email=${params.userEmail}")

                val request = EmailRequest(
                    serviceId = "service_m51lm06", // Gmail Service ID
                    templateId = "template_lfol04n", // Template AI Summary SportyKids
                    userId = "Ol62DPXb49nRZL5re", // User's Public Key
                    templateParams = params
                )

                val response = EmailRetrofitClient.api.sendEmail(request)
                android.util.Log.d("AiSummaryVM", "Email send response: ${response.code()}")
                
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("AiSummaryVM", "Email error body: $errorBody")
                }
                
                onResult(response.isSuccessful)
            } catch (e: Exception) {
                android.util.Log.e("AiSummaryVM", "Email send exception", e)
                e.printStackTrace()
                onResult(false)
            }
        }
    }


    private fun parseGeminiResponse(json: String): AiSummaryResult? {
        return try {
            // Find JSON block if it exists
            val jsonStart = json.indexOf("{")
            val jsonEnd = json.lastIndexOf("}")
            if (jsonStart == -1 || jsonEnd == -1) return null
            
            val cleanJson = json.substring(jsonStart, jsonEnd + 1)
            gson.fromJson(cleanJson, AiSummaryResult::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
