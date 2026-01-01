package com.example.dam_front.models

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

@JsonAdapter(ActivitiesResponseDeserializer::class)
data class ActivitiesResponse(
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 10,
    @SerializedName("items")
    private val activities: List<Activity> = emptyList()
) {
    fun getActivitiesList(): List<Activity> = activities
    val hasMore: Boolean get() = page * limit < total
}

class ActivitiesResponseDeserializer : JsonDeserializer<ActivitiesResponse> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ActivitiesResponse {
        if (json == null) {
            throw JsonParseException("Invalid JSON")
        }

        val gson = Gson()
        val activityListType = object : TypeToken<List<Activity>>() {}.type

        return when {
            json.isJsonArray -> {
                val activities = gson.fromJson<List<Activity>>(json, activityListType) ?: emptyList()
                ActivitiesResponse(
                    total = activities.size,
                    page = 1,
                    limit = activities.size.coerceAtLeast(1),
                    activities = activities
                )
            }

            json.isJsonObject -> mapObject(json.asJsonObject, gson, activityListType)
            else -> throw JsonParseException("Expected JSON array or object, got: ${json.javaClass.simpleName}")
        }
    }

    private fun mapObject(
        obj: JsonObject,
        gson: Gson,
        activityListType: Type
    ): ActivitiesResponse {
        val total = obj.get("total")?.asInt ?: obj.get("count")?.asInt ?: 0
        val page = obj.get("page")?.asInt ?: 1
        val limit = obj.get("limit")?.asInt ?: obj.get("perPage")?.asInt ?: 10

        val activitiesArray = when {
            obj.has("items") && obj.get("items").isJsonArray -> obj.get("items")
            obj.has("results") && obj.get("results").isJsonArray -> obj.get("results")
            obj.has("activities") && obj.get("activities").isJsonArray -> obj.get("activities")
            obj.has("data") && obj.get("data").isJsonArray -> obj.get("data")
            else -> obj.entrySet().firstOrNull { it.value.isJsonArray }?.value
        }

        val activities = activitiesArray?.let {
            gson.fromJson<List<Activity>>(it, activityListType)
        } ?: emptyList()

        return ActivitiesResponse(
            total = if (total == 0) activities.size else total,
            page = page,
            limit = if (limit == 0) activities.size.coerceAtLeast(1) else limit,
            activities = activities
        )
    }
}

