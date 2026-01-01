package com.example.dam_front.api

import retrofit2.HttpException
import java.io.IOException

class ApiHttpException(val code: Int, message: String? = null, val body: String? = null) : Exception(message)

object NetworkErrorHandler {
    fun map(e: Throwable): Exception {
        return when (e) {
            is HttpException -> {
                val code = e.code()
                val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                when (code) {
                    400 -> ApiHttpException(400, "Requête invalide (400). Détails: ${body ?: "-"}", body)
                    401 -> ApiHttpException(401, "Non autorisé (401). Veuillez vous reconnecter.", body)
                    403 -> {
                        val parsedMessage = try {
                            if (body?.contains("message") == true) {
                                // Try to extract message from JSON response
                                val regex = """"message"\s*:\s*"([^"]*)"""".toRegex()
                                regex.find(body)?.groupValues?.get(1) ?: "Accès refusé - Permissions insuffisantes"
                            } else {
                                "Accès refusé - Seul le créateur peut effectuer cette action"
                            }
                        } catch (e: Exception) {
                            "Accès refusé - Permissions insuffisantes"
                        }
                        ApiHttpException(403, parsedMessage, body)
                    }
                    404 -> ApiHttpException(404, "Ressource introuvable (404).", body)
                    else -> ApiHttpException(code, "Erreur réseau ($code). ${body ?: e.message}", body)
                }
            }
            is IOException -> Exception("Impossible de joindre le serveur. Vérifiez votre connexion.")
            else -> Exception(e.message ?: "Erreur inconnue")
        }
    }
}
