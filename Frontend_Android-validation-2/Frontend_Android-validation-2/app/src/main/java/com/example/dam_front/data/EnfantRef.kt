package com.example.dam_front.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

@com.google.gson.annotations.JsonAdapter(EnfantRefDeserializer::class)
data class EnfantRef(
    @SerializedName("_id") val id: String? = null,
    val prenom: String? = null,
    val nom: String? = null
)

class EnfantRefDeserializer : JsonDeserializer<EnfantRef> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): EnfantRef {
        if (json == null || json.isJsonNull) return EnfantRef()
        return if (json.isJsonPrimitive) {
            // server returned just the id as a string
            EnfantRef(id = json.asString)
        } else {
            val obj = json.asJsonObject
            val id = if (obj.has("_id") && !obj.get("_id").isJsonNull) obj.get("_id").asString else null
            val prenom = if (obj.has("prenom") && !obj.get("prenom").isJsonNull) obj.get("prenom").asString else null
            val nom = if (obj.has("nom") && !obj.get("nom").isJsonNull) obj.get("nom").asString else null
            EnfantRef(id = id, prenom = prenom, nom = nom)
        }
    }
}
