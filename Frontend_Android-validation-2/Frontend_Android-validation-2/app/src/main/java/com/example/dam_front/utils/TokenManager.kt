package com.example.dam_front.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "token_prefs")

class TokenManager(private val context: Context) {
    
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("access_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
        private val USER_NOM_KEY = stringPreferencesKey("user_nom")
        private val USER_PRENOM_KEY = stringPreferencesKey("user_prenom")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
    }
    
    private val widgetPrefs = context.getSharedPreferences("widget_sync_prefs", Context.MODE_PRIVATE)

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
        widgetPrefs.edit().putString("access_token", token).commit()
    }
    
    fun getToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }
    }
    
    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }
        widgetPrefs.edit().putString("user_id", userId).commit()
    }
    
    fun getUserId(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_ID_KEY]
        }
    }
    
    suspend fun saveUserRole(role: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ROLE_KEY] = role
        }
        widgetPrefs.edit().putString("user_role", role).commit()
    }
    
    fun getUserRole(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_ROLE_KEY]
        }
    }
    
    suspend fun saveUserName(nom: String, prenom: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NOM_KEY] = nom
            preferences[USER_PRENOM_KEY] = prenom
        }
        widgetPrefs.edit().putString("user_nom", nom).putString("user_prenom", prenom).commit()
    }
    
    fun getUserNom(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_NOM_KEY]
        }
    }
    
    fun getUserPrenom(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_PRENOM_KEY]
        }
    }
    
    suspend fun saveUserEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_EMAIL_KEY] = email
        }
        widgetPrefs.edit().putString("user_email", email).commit()
    }
    
    fun getUserEmail(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_EMAIL_KEY]
        }
    }
    
    suspend fun saveUserSession(token: String, userId: String, role: String, nom: String, prenom: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_ID_KEY] = userId
            preferences[USER_ROLE_KEY] = role
            preferences[USER_NOM_KEY] = nom
            preferences[USER_PRENOM_KEY] = prenom
        }
        // Miroir synchrone pour le widget
        android.util.Log.d("TokenManager", "💾 SYNC WRITE: role=$role, user=$prenom")
        widgetPrefs.edit()
            .putString("access_token", token)
            .putString("user_id", userId)
            .putString("user_role", role)
            .putString("user_nom", nom)
            .putString("user_prenom", prenom)
            .commit()
    }

    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
        widgetPrefs.edit().clear().commit()
    }

    // Méthodes pour le widget (lecture synchrone SharedPreferences)
    fun getWidgetToken(): String? = widgetPrefs.getString("access_token", null)
    fun getWidgetRole(): String? = widgetPrefs.getString("user_role", null)
    fun getWidgetUserId(): String? = widgetPrefs.getString("user_id", null)
    fun getWidgetNom(): String? = widgetPrefs.getString("user_nom", "")
    fun getWidgetPrenom(): String? = widgetPrefs.getString("user_prenom", "")
}


