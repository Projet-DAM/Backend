package com.example.dam_front.api

import android.util.Log
import com.example.dam_front.config.ApiConfig
import com.example.dam_front.models.Tournoi
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

object TournoiSocketService {
    private const val TAG = "TournoiSocketService"
    private var socket: Socket? = null
    private val _newTournoi = MutableStateFlow<Tournoi?>(null)
    val newTournoi: StateFlow<Tournoi?> = _newTournoi.asStateFlow()
    
    private val gson = Gson()
    
    fun connect() {
        try {
            if (socket == null || !socket!!.connected()) {
                val options = IO.Options().apply {
                    reconnection = true
                    timeout = 20000
                }
                
                socket = IO.socket(ApiConfig.SOCKET_URL, options)
                
                socket?.on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "Socket connected")
                }
                
                socket?.on(Socket.EVENT_DISCONNECT) {
                    Log.d(TAG, "Socket disconnected")
                }
                
                socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                    Log.e(TAG, "Socket connection error: ${args[0]}")
                }
                
                // Écouter l'événement tournoiCreated
                socket?.on("tournoiCreated") { args ->
                    try {
                        if (args.isNotEmpty()) {
                            val tournoiJson = args[0] as? JSONObject
                            if (tournoiJson != null) {
                                val tournoi = gson.fromJson(tournoiJson.toString(), Tournoi::class.java)
                                Log.d(TAG, "Nouveau tournoi reçu: ${tournoi.nom}")
                                _newTournoi.value = tournoi
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur lors de la réception du tournoi", e)
                    }
                }
                
                socket?.connect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la connexion Socket.IO", e)
        }
    }
    
    fun disconnect() {
        socket?.disconnect()
        socket = null
        Log.d(TAG, "Socket disconnected and cleared")
    }
    
    fun isConnected(): Boolean {
        return socket?.connected() == true
    }
    
    fun clearNewTournoi() {
        _newTournoi.value = null
    }
}


