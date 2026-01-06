package com.example.dam_front.api

import com.example.dam_front.models.AuthResponse
import com.example.dam_front.models.ForgotPasswordRequest
import com.example.dam_front.models.LoginRequest
import com.example.dam_front.models.SignUpRequest
import com.example.dam_front.models.Tournoi
import com.example.dam_front.models.UpdateTournoiDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/signup")
    suspend fun signup(@Body request: SignUpRequest): Response<AuthResponse>

    // Alternative endpoint pour signup
    @POST("auth/register")
    suspend fun register(@Body request: SignUpRequest): Response<AuthResponse>

    // Mot de passe oublié - Essayer plusieurs endpoints possibles
    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ResponseBody>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ForgotPasswordRequest): Response<ResponseBody>

    @POST("auth/password/forgot")
    suspend fun forgotPasswordVariant1(@Body request: ForgotPasswordRequest): Response<ResponseBody>

    @POST("auth/password/reset")
    suspend fun resetPasswordVariant1(@Body request: ForgotPasswordRequest): Response<ResponseBody>

    @POST("users/forgot-password")
    suspend fun forgotPasswordVariant2(@Body request: ForgotPasswordRequest): Response<ResponseBody>

    @POST("users/reset-password")
    suspend fun resetPasswordVariant2(@Body request: ForgotPasswordRequest): Response<ResponseBody>

    // Récupérer le ResponseBody brut pour parser manuellement et gérer les deux formats
    @GET("tournois")
    suspend fun getTournois(): Response<ResponseBody>

    @POST("tournois")
    suspend fun createTournoi(@Body tournoi: Tournoi): Response<Tournoi>

    // Endpoint pour créer un tournoi avec image (multipart/form-data)
    @Multipart
    @POST("tournois")
    suspend fun createTournoiWithImage(
        @Part("nom") nom: RequestBody,
        @Part("sport") sport: RequestBody,
        @Part("categorieAge") categorieAge: RequestBody,
        @Part("dateDebut") dateDebut: RequestBody,
        @Part("dateFin") dateFin: RequestBody,
        @Part("lieu") lieu: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("nombreParticipantsMax") nombreParticipantsMax: RequestBody?,
        @Part("fraisParticipation") fraisParticipation: RequestBody?,
        @Part("etat") etat: RequestBody?,
        @Part("niveau") niveau: RequestBody?,
        @Part("recompense") recompense: RequestBody?,
        @Part image: MultipartBody.Part?
    ): Response<Tournoi>

    @PATCH("tournois/{id}")
    suspend fun updateTournoi(
        @Path("id") id: String,
        @Body updateDto: UpdateTournoiDto
    ): Response<Tournoi>

    // Endpoint pour mettre à jour un tournoi avec image (multipart/form-data)
    @Multipart
    @PATCH("tournois/{id}")
    suspend fun updateTournoiWithImage(
        @Path("id") id: String,
        @Part("nom") nom: RequestBody?,
        @Part("description") description: RequestBody?,
        @Part("sport") sport: RequestBody?,
        @Part("categorieAge") categorieAge: RequestBody?,
        @Part("dateDebut") dateDebut: RequestBody?,
        @Part("dateFin") dateFin: RequestBody?,
        @Part("lieu") lieu: RequestBody?,
        @Part("nombreParticipantsMax") nombreParticipantsMax: RequestBody?,
        @Part("fraisParticipation") fraisParticipation: RequestBody?,
        @Part("etat") etat: RequestBody?,
        @Part("niveau") niveau: RequestBody?,
        @Part("recompense") recompense: RequestBody?,
        @Part image: MultipartBody.Part?
    ): Response<Tournoi>

    @DELETE("tournois/{id}")
    suspend fun deleteTournoi(@Path("id") id: String): Response<ResponseBody>

    // Inscription à un tournoi - Essayer plusieurs endpoints possibles
    @POST("inscriptions")
    suspend fun inscrireAuTournoi(
        @Body inscription: com.example.dam_front.models.InscriptionRequest
    ): Response<com.example.dam_front.models.Participant>

    // Alternative 1: inscription avec tournoiId dans le path
    @POST("tournois/{tournoiId}/inscriptions")
    suspend fun inscrireAuTournoiWithPath(
        @Path("tournoiId") tournoiId: String,
        @Body inscription: com.example.dam_front.models.InscriptionRequest
    ): Response<com.example.dam_front.models.Participant>

    // Alternative 2: inscription avec tournoiId dans le body
    @POST("tournois/inscriptions")
    suspend fun inscrireAuTournoiAlternative(
        @Body inscription: com.example.dam_front.models.InscriptionRequest
    ): Response<com.example.dam_front.models.Participant>

    // Récupérer les participants d'un tournoi
    @GET("tournois/{tournoiId}/participants")
    suspend fun getParticipants(@Path("tournoiId") tournoiId: String): Response<List<com.example.dam_front.models.Participant>>

    // Alternative: récupérer les participants
    @GET("inscriptions/tournoi/{tournoiId}")
    suspend fun getParticipantsAlternative(@Path("tournoiId") tournoiId: String): Response<List<com.example.dam_front.models.Participant>>

    // Modifier une inscription
    @PATCH("inscriptions/{id}")
    suspend fun updateInscription(
        @Path("id") id: String,
        @Body updateDto: com.example.dam_front.models.UpdateInscriptionDto
    ): Response<com.example.dam_front.models.Participant>

    // Supprimer une inscription
    @DELETE("inscriptions/{id}")
    suspend fun deleteInscription(@Path("id") id: String): Response<okhttp3.ResponseBody>

    // Gestion des enfants
    @POST("users/{parentId}/children")
    suspend fun createChild(
        @Path("parentId") parentId: String,
        @Body request: com.example.dam_front.models.CreateChildRequest
    ): Response<com.example.dam_front.models.ChildResponse>

    // Créer un enfant avec image (multipart/form-data)
    @Multipart
    @POST("users/{parentId}/children")
    suspend fun createChildWithImage(
        @Path("parentId") parentId: String,
        @Part("prenom") prenom: RequestBody,
        @Part("nom") nom: RequestBody,
        @Part("dateNaissance") dateNaissance: RequestBody,
        @Part("sportPratique") sportPratique: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<com.example.dam_front.models.ChildResponse>

    @GET("users/{parentId}/children")
    suspend fun getChildren(@Path("parentId") parentId: String): Response<List<com.example.dam_front.models.ChildResponse>>

    // New compact children endpoint for dropdowns
    @GET("users/enfants")
    suspend fun getEnfants(): List<com.example.dam_front.models.User>

    @GET("users")
    suspend fun getUsersByRole(@Query("role") role: String? = null): List<com.example.dam_front.models.User>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") userId: String): com.example.dam_front.models.User


    @GET("users/{parentId}/children/{childId}")
    suspend fun getChild(
        @Path("parentId") parentId: String,
        @Path("childId") childId: String
    ): Response<com.example.dam_front.models.ChildResponse>

    @PATCH("users/{parentId}/children/{childId}")
    suspend fun updateChild(
        @Path("parentId") parentId: String,
        @Path("childId") childId: String,
        @Body request: com.example.dam_front.models.CreateChildRequest
    ): Response<com.example.dam_front.models.ChildResponse>

    @DELETE("users/{parentId}/children/{childId}")
    suspend fun deleteChild(
        @Path("parentId") parentId: String,
        @Path("childId") childId: String
    ): Response<okhttp3.ResponseBody>

    // Gestion des équipes
    @POST("equipes")
    suspend fun createEquipe(
        @Body request: com.example.dam_front.models.CreateEquipeRequest
    ): Response<com.example.dam_front.models.Equipe>

    @GET("equipes/tournoi/{tournoiId}")
    suspend fun getTournamentEquipes(
        @Path("tournoiId") tournoiId: String
    ): Response<List<com.example.dam_front.models.Equipe>>

    @PATCH("equipes/{id}/enfants")
    suspend fun updateEquipe(
        @Path("id") id: String,
        @Body request: com.example.dam_front.models.UpdateEquipeEnfantsRequest
    ): Response<com.example.dam_front.models.Equipe>

    @DELETE("equipes/{id}")
    suspend fun deleteEquipe(
        @Path("id") id: String
    ): Response<okhttp3.ResponseBody>

    // Générer l'arbre de tournoi
    @POST("matches/tournoi/{tournoiId}/generate-bracket")
    suspend fun generateBracket(
        @Path("tournoiId") tournoiId: String
    ): Response<okhttp3.ResponseBody>

    // Récupérer les matchs d'un tournoi
    @GET("matches/tournoi/{tournoiId}")
    suspend fun getTournamentMatches(
        @Path("tournoiId") tournoiId: String,
        @Query("showFuture") showFuture: Boolean = false
    ): Response<okhttp3.ResponseBody>

    // Mettre à jour un match
    @PATCH("matches/{matchId}")
    suspend fun updateMatch(
        @Path("matchId") matchId: String,
        @Body updateData: com.example.dam_front.models.UpdateMatchDto
    ): Response<okhttp3.ResponseBody>

    // Enregistrer le token FCM pour les notifications
    @POST("users/{userId}/fcm-token")
    suspend fun registerFCMToken(
        @Path("userId") userId: String,
        @Body request: com.example.dam_front.models.FCMTokenRequest
    ): Response<okhttp3.ResponseBody>

    // Alternative endpoint pour enregistrer le token FCM
    @POST("fcm-tokens")
    suspend fun registerFCMTokenAlternative(
        @Body request: com.example.dam_front.models.FCMTokenRequest
    ): Response<okhttp3.ResponseBody>

    @GET("activities")
    suspend fun getActivities(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("categorie") categorie: String? = null,
        @Query("coach") coach: String? = null,
        @Query("academie") academie: String? = null,
        @Query("programme") programmeId: String? = null,
        @Query("statut") statut: String? = null,
        @Query("date") date: String? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("order") order: String? = null
    ): Response<com.example.dam_front.models.ActivitiesResponse>

    @GET("activities/{id}")
    suspend fun getActivityById(@Path("id") id: String): Response<com.example.dam_front.models.Activity>

    @DELETE("activities/{id}")
    suspend fun deleteActivity(@Path("id") id: String): Response<Unit>

    @POST("activities")
    suspend fun createActivity(@Body request: com.example.dam_front.models.CreateActivityRequest): Response<com.example.dam_front.models.Activity>

    @PUT("activities/{id}")
    suspend fun updateActivity(
        @Path("id") id: String,
        @Body request: com.example.dam_front.models.CreateActivityRequest
    ): Response<com.example.dam_front.models.Activity>

    @PATCH("activities/{id}")
    suspend fun updateActivityPatch(
        @Path("id") id: String,
        @Body request: com.example.dam_front.models.CreateActivityRequest
    ): Response<com.example.dam_front.models.Activity>

    @Multipart
    @POST("activities")
    suspend fun createActivityWithImage(
        @PartMap partMap: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part?
    ): Response<com.example.dam_front.models.Activity>

    @Multipart
    @PATCH("activities/{id}")
    suspend fun updateActivityWithImage(
        @Path("id") id: String,
        @PartMap partMap: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part?
    ): Response<com.example.dam_front.models.Activity>

    @Multipart
    @PUT("activities/{id}")
    suspend fun updateActivityWithImagePut(
        @Path("id") id: String,
        @PartMap partMap: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part?
    ): Response<com.example.dam_front.models.Activity>


    @GET("programs")
    suspend fun getPrograms(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("statut") statut: String? = null,
        @Query("coach") coach: String? = null,
        @Query("academie") academie: String? = null,
        @Query("nom") nom: String? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("order") order: String? = null
    ): Response<com.example.dam_front.models.ProgramsResponse>

    @GET("programs/{id}")
    suspend fun getProgramById(@Path("id") id: String): Response<com.example.dam_front.models.Program>

    @POST("programs")
    suspend fun createProgram(@Body request: com.example.dam_front.models.CreateProgramRequest): Response<com.example.dam_front.models.Program>

    @Multipart
    @POST("programs")
    suspend fun createProgramWithImage(
        @PartMap partMap: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part image: MultipartBody.Part?
    ): Response<com.example.dam_front.models.Program>

    @PATCH("programs/{id}")
    suspend fun updateProgram(
        @Path("id") id: String,
        @Body request: com.example.dam_front.models.UpdateProgramRequest
    ): Response<com.example.dam_front.models.Program>

    @PATCH("programs/{id}/activities")
    suspend fun updateProgramActivities(
        @Path("id") id: String,
        @Body request: com.example.dam_front.models.ManageProgramActivitiesRequest
    ): Response<com.example.dam_front.models.Program>

    @DELETE("programs/{id}")
    suspend fun deleteProgram(@Path("id") id: String): Response<Unit>


    @GET("users/enfants")
    suspend fun getMyChildren(): Response<List<com.example.dam_front.models.Child>>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): Response<com.example.dam_front.models.User>

    @POST("users/children")
    suspend fun createChild(@Body request: com.example.dam_front.models.CreateChildRequest): Response<com.example.dam_front.models.CreateChildResponse>

    @PATCH("users/{id}/fcm-token")
    suspend fun updateFcmToken(
        @Path("id") id: String,
        @Body request: Map<String, String>
    ): Response<com.example.dam_front.models.User>

    @GET("enrollments")
    suspend fun getEnrollments(): Response<List<com.example.dam_front.models.EnrollmentResponse>>

    @POST("enrollments")
    suspend fun createEnrollments(@Body request: com.example.dam_front.models.CreateEnrollmentRequest): Response<com.example.dam_front.models.EnrollmentResultResponse>

    @GET("enrollments/check/{childId}/{programId}")
    suspend fun checkEnrollment(
        @Path("childId") childId: String,
        @Path("programId") programId: String
    ): Response<Map<String, Boolean>>

    @GET("enrollments/program/{programId}")
    suspend fun getProgramEnrollments(@Path("programId") programId: String): Response<List<com.example.dam_front.models.ProgramEnrollment>>

    @PATCH("users/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): Response<com.example.dam_front.models.User>

    @Multipart
    @POST("users/{id}/upload-photo")
    suspend fun uploadUserPhoto(
        @Path("id") id: String,
        @Part photo: MultipartBody.Part
    ): Response<Map<String, Any>>

    @POST("payments/create-payment-intent")
    suspend fun createPaymentIntent(@Body request: Map<String, @JvmSuppressWildcards Any>): Response<Map<String, String>>

    @POST("payments/confirm-payment")
    suspend fun confirmPayment(@Body request: Map<String, String>): Response<Map<String, Any>>

}

