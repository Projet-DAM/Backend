package com.example.dam_front.email

import com.google.gson.annotations.SerializedName

// Structure globale de la requête pour EmailJS
data class EmailRequest<T>(
    @SerializedName("service_id") val serviceId: String,
    @SerializedName("template_id") val templateId: String,
    @SerializedName("user_id") val userId: String, // C'est votre Public Key
    @SerializedName("template_params") val templateParams: T
)

// Les variables qui correspondent aux {{...}} dans votre template HTML
data class PaymentParams(
    @SerializedName("parent_name") val parentName: String,
    @SerializedName("child_name") val childName: String,
    @SerializedName("program_name") val programName: String,
    @SerializedName("amount") val amount: String,
    @SerializedName("date") val date: String,
    @SerializedName("email") val userEmail: String, // Email du destinataire (parent)
    @SerializedName("reply_to") val replyTo: String = "contact@sportykids.com" // Optionnel
)

data class AiSummaryParams(
    @SerializedName("to_email") val userEmail: String,
    @SerializedName("parent_name") val parentName: String = "Parent",
    @SerializedName("child_name") val childName: String,
    
    // Multiple keys to ensure compatibility with different template versions
    @SerializedName("message") val message: String,
    @SerializedName("summary_content") val summaryContent: String = "", // Added for user template
    @SerializedName("resume_general") val resumeGeneral: String = "",
    
    @SerializedName("strengths") val strengths: String = "", // Added for user template
    @SerializedName("points_forts") val pointsForts: String = "",
    
    @SerializedName("improvements") val improvements: String = "", // Added for user template
    @SerializedName("axes_amelioration") val axesAmelioration: String = "",
    @SerializedName("axes") val axes: String = "",
    
    @SerializedName("counsel") val counsel: String = "", // Added for user template
    @SerializedName("conseils") val conseils: String = "",
    @SerializedName("conseils_parents") val conseilsParents: String = "",
    
    @SerializedName("goals") val goals: String = "", // Added for user template
    @SerializedName("objectifs") val objectifs: String = "",
    @SerializedName("prochain_defi") val prochainDefi: String = "",
    
    @SerializedName("summary_text") val summaryText: String = "",
    @SerializedName("date") val date: String,
    @SerializedName("from_name") val fromName: String = "SportyKids",
    @SerializedName("reply_to") val replyTo: String = "hadil.aroua@esprit.tn"
)
