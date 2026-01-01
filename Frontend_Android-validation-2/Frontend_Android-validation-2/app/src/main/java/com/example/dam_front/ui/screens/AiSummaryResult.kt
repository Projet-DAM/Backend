package com.example.dam_front.ui.screens

data class AiSummaryResult(
    val shortSummary: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val counsel: String,
    val goals: List<String>
)
