package com.example.dam_front.models

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

@JsonAdapter(ProgramActivityDeserializer::class)
data class ProgramActivity(
    @SerializedName("_id") val id: String,
    @SerializedName("nom_activite") val nomActivite: String? = null,
    val categorie: String? = null,
    val date: String? = null,
    val heure: String? = null
)

class ProgramActivityDeserializer : JsonDeserializer<ProgramActivity> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ProgramActivity {
        if (json == null) {
            throw JsonParseException("Activity element is null")
        }

        return when {
            json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                ProgramActivity(id = json.asString)
            }
            json.isJsonObject -> mapObject(json.asJsonObject)
            else -> throw JsonParseException("Unsupported activity format")
        }
    }

    private fun mapObject(obj: JsonObject): ProgramActivity {
        val id = obj.get("_id")?.asString
            ?: obj.get("id")?.asString
            ?: throw JsonParseException("Activity id is missing")

        return ProgramActivity(
            id = id,
            nomActivite = obj.get("nom_activite")?.asString ?: obj.get("nomActivite")?.asString,
            categorie = obj.get("categorie")?.asString,
            date = obj.get("date")?.asString,
            heure = obj.get("heure")?.asString
        )
    }
}


