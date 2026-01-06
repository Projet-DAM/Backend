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

data class ProgramSummary(
    @SerializedName("_id") val id: String,
    @SerializedName("nom_programme") val nomProgramme: String,
    val prix: Double
)

data class ChildSummary(
    @SerializedName("_id") val id: String,
    val prenom: String,
    val nom: String,
    val dateNaissance: String? = null,
    val photoProfil: String? = null
)

data class ParentSummary(
    @SerializedName("_id") val id: String,
    val prenom: String,
    val nom: String,
    val email: String,
    val telephone: String? = null
)

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
