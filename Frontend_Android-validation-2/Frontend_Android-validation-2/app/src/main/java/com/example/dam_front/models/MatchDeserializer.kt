package com.example.dam_front.models

import com.google.gson.*
import java.lang.reflect.Type

class MatchDeserializer : JsonDeserializer<Match> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Match {
        if (json == null || !json.isJsonObject) {
            throw JsonParseException("Expected JSON object")
        }
        
        val jsonObject = json.asJsonObject
        val gson = Gson()
        
        // Extraire equipeA (peut être String, objet, ou null)
        val equipeAElement = jsonObject.get("equipeA")
        val equipeAId: String? = when {
            equipeAElement == null || equipeAElement.isJsonNull -> null
            equipeAElement.isJsonPrimitive -> equipeAElement.asString
            equipeAElement.isJsonObject -> {
                val equipeObj = equipeAElement.asJsonObject
                equipeObj.get("_id")?.takeIf { it.isJsonPrimitive }?.asString
            }
            else -> null
        }
        
        val equipeAObj: EquipeMatch? = when {
            equipeAElement != null && equipeAElement.isJsonObject -> {
                try {
                    gson.fromJson(equipeAElement, EquipeMatch::class.java)
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
        
        // Extraire equipeB (peut être String, objet, ou null)
        val equipeBElement = jsonObject.get("equipeB")
        val equipeBId: String? = when {
            equipeBElement == null || equipeBElement.isJsonNull -> null
            equipeBElement.isJsonPrimitive -> equipeBElement.asString
            equipeBElement.isJsonObject -> {
                val equipeObj = equipeBElement.asJsonObject
                equipeObj.get("_id")?.takeIf { it.isJsonPrimitive }?.asString
            }
            else -> null
        }
        
        val equipeBObj: EquipeMatch? = when {
            equipeBElement != null && equipeBElement.isJsonObject -> {
                try {
                    gson.fromJson(equipeBElement, EquipeMatch::class.java)
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
        
        // Extraire les autres champs
        fun getString(key: String): String? {
            val element = jsonObject.get(key) ?: return null
            return when {
                element.isJsonNull -> null
                element.isJsonPrimitive -> element.asString
                else -> null
            }
        }
        
        fun getInt(key: String): Int? {
            val element = jsonObject.get(key) ?: return null
            return when {
                element.isJsonNull -> null
                element.isJsonPrimitive -> {
                    try {
                        element.asInt
                    } catch (e: Exception) {
                        null
                    }
                }
                else -> null
            }
        }
        
        // Créer le Match directement avec tous les champs
        val match = Match(
            id = getString("_id"),
            tournoiId = getString("tournoiId") ?: "",
            equipeAId = equipeAId,
            equipeBId = equipeBId,
            equipe1 = equipeAObj,
            equipe2 = equipeBObj,
            scoreEquipe1 = getInt("scoreEquipeA"),
            scoreEquipe2 = getInt("scoreEquipeB"),
            dateMatch = getString("dateMatch"),
            heureMatch = getString("heureMatch"),
            statut = getString("statut"),
            phase = getString("phase"),
            round = getInt("round"),
            ordre = getInt("ordre"),
            vainqueur = getString("vainqueur"),
            matchSuivantId = getString("matchSuivantId"),
            createdAt = getString("createdAt"),
            updatedAt = getString("updatedAt")
        )
        
        return match
    }
}

