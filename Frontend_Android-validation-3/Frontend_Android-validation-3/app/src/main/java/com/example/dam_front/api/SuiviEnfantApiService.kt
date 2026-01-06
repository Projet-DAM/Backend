package com.example.dam_front.api

import com.example.dam_front.data.CreateSuiviEnfantDto
import com.example.dam_front.data.SuiviEnfant
import com.example.dam_front.models.User
import com.example.dam_front.data.UpdateSuiviEnfantDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PATCH
import retrofit2.http.DELETE

interface SuiviEnfantApiService {
    @POST("suivi-enfant")
    suspend fun createSuiviEnfant(@Body dto: CreateSuiviEnfantDto): SuiviEnfant

    @GET("suivi-enfant")
    suspend fun getAllSuiviEnfants(): List<SuiviEnfant>

    @GET("suivi-enfant/{id}")
    suspend fun getSuiviEnfantById(@Path("id") id: String): SuiviEnfant

    @GET("suivi-enfant/enfant/{enfantId}")
    suspend fun getSuiviEnfantsByEnfantId(@Path("enfantId") enfantId: String): List<SuiviEnfant>

    // Returns the list of child users that the authenticated coach is associated with
    @GET("suivi-enfant/coach/children")
    suspend fun getCoachChildren(): List<User>

    // Get available parents for coaches (parents who have children with suivis by this coach)
    @GET("suivi-enfant/available-parents")
    suspend fun getAvailableParents(): List<User>
    
    // Get available coaches for parents (coaches who have made suivis for this parent's children)
    @GET("suivi-enfant/available-coaches")
    suspend fun getAvailableCoaches(): List<User>

    @PATCH("suivi-enfant/{id}")
    suspend fun updateSuiviEnfant(
        @Path("id") id: String,
        @Body dto: UpdateSuiviEnfantDto
    ): SuiviEnfant

    @DELETE("suivi-enfant/{id}")
    suspend fun deleteSuiviEnfant(@Path("id") id: String)
}
