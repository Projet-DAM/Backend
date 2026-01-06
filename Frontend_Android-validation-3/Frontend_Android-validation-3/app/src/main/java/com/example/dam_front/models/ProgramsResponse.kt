package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class ProgramsResponse(
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 10,
    @SerializedName("items") val items: List<Program> = emptyList()
) {
    fun getProgramList(): List<Program> = items
}


