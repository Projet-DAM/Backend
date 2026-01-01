package com.example.dam_front.email

import com.google.gson.annotations.SerializedName

// Structure globale de la requête pour EmailJS
data class EmailRequest(
    @SerializedName("service_id") val serviceId: String,
    @SerializedName("template_id") val templateId: String,
    @SerializedName("user_id") val userId: String, // C'est votre Public Key
    @SerializedName("template_params") val templateParams: Any
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
    @SerializedName("parent_name") val parentName: String,
    @SerializedName("child_name") val childName: String,
    @SerializedName("summary_content") val summaryContent: String,
    @SerializedName("strengths") val strengths: String,
    @SerializedName("improvements") val improvements: String,
    @SerializedName("counsel") val counsel: String,
    @SerializedName("goals") val goals: String,
    @SerializedName("email") val userEmail: String,
    @SerializedName("reply_to") val replyTo: String = "hadil.aroua@esprit.tn"
)
