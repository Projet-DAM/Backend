package com.example.dam_front.repository

import android.content.Context
import android.util.Log
import com.example.dam_front.api.ApiService
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.api.SuiviEnfantApiService
import com.example.dam_front.data.SuiviEnfant
import com.example.dam_front.models.User

class CoachRepository(context: Context) {

    private val apiService: ApiService = RetrofitClient.createAuthenticatedService(context, ApiService::class.java)
    private val suiviService: SuiviEnfantApiService = RetrofitClient.createAuthenticatedService(context, SuiviEnfantApiService::class.java)

    suspend fun getCoach(userId: String): User = apiService.getUser(userId)

    // Derive players from SuiviEnfant entries instead of hitting a potentially restricted
    // Prefer fetching users by role if available — this returns all children (ENFANT).
    // If the endpoint is restricted or returns empty, fall back to deriving players
    // from existing suivi entries.
    suspend fun getCoachPlayers(): List<User> {
        Log.d("CoachRepository", "getCoachPlayers: starting...")
        return try {
            // First try coach-scoped children 
            val coachChildren = try { 
                Log.d("CoachRepository", "getCoachPlayers: trying getCoachChildren()")
                suiviService.getCoachChildren() 
            } catch (e: Exception) { 
                Log.w("CoachRepository", "getCoachChildren failed: ${e.message}")
                emptyList() 
            }
            
            if (coachChildren.isNotEmpty()) {
                Log.d("CoachRepository", "getCoachPlayers: got ${coachChildren.size} coach children")
                coachChildren
            } else {
                // Skip the forbidden getUsersByRole and go directly to working endpoint
                Log.d("CoachRepository", "getCoachPlayers: trying getEnfants()")
                val enfants = try { apiService.getEnfants() } catch (e: Exception) { 
                    Log.w("CoachRepository", "getEnfants failed: ${e.message}")
                    emptyList() 
                }
                
                if (enfants.isNotEmpty()) {
                    Log.d("CoachRepository", "getCoachPlayers: got ${enfants.size} enfants")
                    enfants
                } else {
                    // Final fallback: derive from suivis
                    Log.d("CoachRepository", "getCoachPlayers: deriving from suivis")
                    val allSuivis: List<SuiviEnfant> = try { suiviService.getAllSuiviEnfants() } catch (_: Exception) { emptyList() }
                    val enfantIds: List<String> = allSuivis
                        .mapNotNull { suivi -> suivi.enfant?.id ?: suivi.enfantId }
                        .distinct()
                    val derived = enfantIds.mapNotNull { enfantId ->
                        try { apiService.getUser(enfantId) } catch (_: Exception) { null }
                    }
                    Log.d("CoachRepository", "getCoachPlayers: derived ${derived.size} children from suivis")
                    derived
                }
            }
        } catch (e: Exception) {
            Log.e("CoachRepository", "getCoachPlayers exception", e)
            emptyList()
        }
    }

    suspend fun getChildById(childId: String): User = apiService.getUser(childId)

    // Return all users accessible to the coach
    suspend fun getAllChildren(): List<User> {
        println("🔍 CoachRepository.getAllChildren() - Attempting to fetch all children")
        
        // 1. Try to fetch from the dedicated endpoint first
        try {
            val children = apiService.getEnfants()
            if (children.isNotEmpty()) {
                println("✅ getAllChildren: Retrieved ${children.size} children from getEnfants()")
                children.forEach { child ->
                    println("  👶 ${child.prenom} ${child.nom} - ID: ${child.id}")
                }
                return children.sortedBy { "${it.prenom} ${it.nom}".lowercase() }
            } else {
                println("⚠️ getAllChildren: getEnfants() returned empty list")
            }
        } catch (e: Exception) {
            println("⚠️ getAllChildren: getEnfants() failed: ${e.message}")
        }

        // 2. Fallback: Derive from existing suivis if the direct endpoint failed or yielded nothing
        println("⚠️ getAllChildren: Falling back to deriving from existing suivis")
        return try {
            // Get all suivis first
            val allSuivis = suiviService.getAllSuiviEnfants()
            println("📊 Found ${allSuivis.size} total suivis")
            
            // Extract child IDs and their names from suivis
            val childData = allSuivis.mapNotNull { suivi ->
                val childId = suivi.enfant?.id ?: suivi.enfantId
                val childName = suivi.enfant?.let { "${it.prenom} ${it.nom}" } ?: suivi.enfantName ?: "Unknown"
                if (!childId.isNullOrBlank()) {
                    println("👶 Found: $childName (ID: $childId)")
                    childId to childName
                } else null
            }.distinctBy { it.first }
            
            println("🎯 Found ${childData.size} unique children from suivis")
            
            // Fetch full user details for each child
            val children = childData.mapNotNull { (childId, childName) ->
                try {
                    val user = apiService.getUser(childId)
                    println("✅ Loaded: ${user.prenom} ${user.nom} - ID: '${user.id}' - Photo: '${user.photoProfil}'")
                    user
                } catch (e: Exception) {
                    println("❌ Failed to load user $childId ($childName): ${e.message}")
                    null
                }
            }
            
            val sortedChildren = children.sortedBy { "${it.prenom} ${it.nom}".lowercase() }
            println("📦 Final derived children list: ${sortedChildren.size}")
            sortedChildren
            
        } catch (e: Exception) {
            println("💥 getAllChildren failed completely: ${e.message}")
            Log.e("CoachRepository", "getAllChildren exception", e)
            emptyList()
        }
    }

    // Direct call to the compact enfants endpoint (returns all registered children)
    suspend fun getEnfants(): List<User> = try {
        apiService.getEnfants()
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun getPlayerSuivi(childId: String): List<SuiviEnfant> =
        suiviService.getSuiviEnfantsByEnfantId(childId)
}
