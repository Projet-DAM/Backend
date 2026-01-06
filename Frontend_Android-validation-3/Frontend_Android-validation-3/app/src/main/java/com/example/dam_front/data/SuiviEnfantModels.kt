package com.example.dam_front.data

import com.google.gson.annotations.SerializedName
import java.util.Date

data class SuiviEnfant(
    @SerializedName("_id") val id: String,
    @SerializedName("date_suivi") val dateSuivi: Date,
    val presence: Boolean,
    val performance: Int,
    val commentaire: String? = null,
    val enfant: EnfantRef? = null,
    val enfantName: String? = null,
    val enfantId: String? = null,
    val activityType: String? = null,
    val focusAreas: List<String>? = null,
    val nextSessionGoals: List<String>? = null,
    val effortLevel: Int? = null,
    val emotionalState: String? = null,
    @SerializedName("createdAt") val createdAt: Date,
    @SerializedName("updatedAt") val updatedAt: Date
)

data class CreateSuiviEnfantDto(
    @SerializedName("date_suivi") val dateSuivi: String,
    val presence: Boolean,
    val performance: Int,
    val commentaire: String? = null,
    @SerializedName("enfantId") val enfantId: String,
    val activityType: String? = null,
    val focusAreas: List<String>? = null,
    val nextSessionGoals: List<String>? = null,
    val effortLevel: Int? = null,
    val emotionalState: String? = null
)

data class UpdateSuiviEnfantDto(
    @SerializedName("date_suivi") val dateSuivi: String? = null,
    val presence: Boolean? = null,
    val performance: Int? = null,
    val commentaire: String? = null
    ,
    val focusAreas: List<String>? = null,
    val nextSessionGoals: List<String>? = null
)
