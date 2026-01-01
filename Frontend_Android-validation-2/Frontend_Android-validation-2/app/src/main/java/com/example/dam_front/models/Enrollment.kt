package com.example.dam_front.models

import com.google.gson.annotations.SerializedName
import java.util.Date

// Request model for creating enrollments (prepared for future backend integration)
data class CreateEnrollmentRequest(
    val programId: String,
    val childIds: List<String>
)

// Response wrapper from backend
data class EnrollmentResultResponse(
    val success: Boolean,
    val enrollments: List<EnrollmentResponse>,
    val message: String
)

// Response model from backend (generic/create)
data class EnrollmentResponse(
    @SerializedName("_id") val id: String,
    @SerializedName("program") val program: ProgramSummary?,
    @SerializedName("child") val child: ChildSummary?,
    @SerializedName("parent") val parent: ParentSummary?,
    val status: String,
    val amountPaid: Double,
    val enrollmentDate: String
)

// Response model for fetching enrollments by program (program is ID)
data class ProgramEnrollment(
    @SerializedName("_id") val id: String,
    @SerializedName("program") val programId: String, // When not populated
    @SerializedName("child") val child: ChildSummary?,
    @SerializedName("parent") val parent: ParentSummary?,
    val status: String,
    val amountPaid: Double,
    val enrollmentDate: String
)

@com.google.gson.annotations.JsonAdapter(ProgramSummaryDeserializer::class)
data class ProgramSummary(
    @SerializedName("_id") val id: String,
    @SerializedName("nom_programme") val nomProgramme: String? = null,
    val prix: Double? = null
)

@com.google.gson.annotations.JsonAdapter(ChildSummaryDeserializer::class)
data class ChildSummary(
    @SerializedName("_id") val id: String,
    val prenom: String? = null,
    val nom: String? = null,
    val dateNaissance: String? = null,
    val photoProfil: String? = null
)

@com.google.gson.annotations.JsonAdapter(ParentSummaryDeserializer::class)
data class ParentSummary(
    @SerializedName("_id") val id: String,
    val prenom: String? = null,
    val nom: String? = null,
    val email: String? = null,
    val telephone: String? = null
)

class ParentSummaryDeserializer : com.google.gson.JsonDeserializer<ParentSummary> {
    override fun deserialize(json: com.google.gson.JsonElement?, typeOfT: java.lang.reflect.Type?, context: com.google.gson.JsonDeserializationContext?): ParentSummary {
        if (json == null || json.isJsonNull) return ParentSummary(id = "")
        return if (json.isJsonPrimitive) {
            ParentSummary(id = json.asString)
        } else {
            val obj = json.asJsonObject
            ParentSummary(
                id = obj.get("_id")?.asString ?: "",
                prenom = obj.get("prenom")?.asString,
                nom = obj.get("nom")?.asString,
                email = obj.get("email")?.asString,
                telephone = obj.get("telephone")?.asString
            )
        }
    }
}

class ProgramSummaryDeserializer : com.google.gson.JsonDeserializer<ProgramSummary> {
    override fun deserialize(json: com.google.gson.JsonElement?, typeOfT: java.lang.reflect.Type?, context: com.google.gson.JsonDeserializationContext?): ProgramSummary {
        if (json == null || json.isJsonNull) return ProgramSummary(id = "")
        return if (json.isJsonPrimitive) {
            ProgramSummary(id = json.asString)
        } else {
            val obj = json.asJsonObject
            ProgramSummary(
                id = obj.get("_id")?.asString ?: "",
                nomProgramme = obj.get("nom_programme")?.asString,
                prix = obj.get("prix")?.asDouble
            )
        }
    }
}

class ChildSummaryDeserializer : com.google.gson.JsonDeserializer<ChildSummary> {
    override fun deserialize(json: com.google.gson.JsonElement?, typeOfT: java.lang.reflect.Type?, context: com.google.gson.JsonDeserializationContext?): ChildSummary {
        if (json == null || json.isJsonNull) return ChildSummary(id = "")
        return if (json.isJsonPrimitive) {
            ChildSummary(id = json.asString)
        } else {
            val obj = json.asJsonObject
            ChildSummary(
                id = obj.get("_id")?.asString ?: "",
                prenom = obj.get("prenom")?.asString,
                nom = obj.get("nom")?.asString,
                dateNaissance = obj.get("dateNaissance")?.asString,
                photoProfil = obj.get("photoProfil")?.asString
            )
        }
    }
}

// Local enrollment model for client-side storage
data class Enrollment(
    val id: String,
    val programId: String,
    val programName: String,
    val childId: String,
    val childName: String,
    val amountPaid: Double,
    val enrollmentDate: Long = System.currentTimeMillis(),
    val status: EnrollmentStatus = EnrollmentStatus.ACTIVE
)

enum class EnrollmentStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    CANCELLED
}
