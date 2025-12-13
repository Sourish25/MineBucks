package com.example.newapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {


    val modrinthToken: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[MODRINTH_TOKEN] }

    val curseForgeCookies: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[CURSEFORGE_COOKIES] }

    val curseForgePoints: Flow<Double> = context.dataStore.data
        .map { preferences -> preferences[CURSEFORGE_POINTS] ?: 0.0 }
        
    val userName: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[USER_NAME] }
        
    val targetCurrency: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[TARGET_CURRENCY] ?: "USD" }

    val themeMode: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[THEME_MODE] ?: "SYSTEM" }
        
    val userAvatar: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[USER_AVATAR] }

    suspend fun saveModrinthToken(token: String) {
        context.dataStore.edit { it[MODRINTH_TOKEN] = token }
    }

    suspend fun saveCurseForgePoints(points: Double) {
        context.dataStore.edit { it[CURSEFORGE_POINTS] = points }
    }
    
    suspend fun saveCurseForgeCookies(cookies: String) {
        context.dataStore.edit { it[CURSEFORGE_COOKIES] = cookies }
    }
    
    suspend fun saveUserName(name: String) {
        context.dataStore.edit { it[USER_NAME] = name }
    }
    
    suspend fun saveUserAvatar(url: String) {
        context.dataStore.edit { it[USER_AVATAR] = url }
    }
    
    suspend fun saveTargetCurrency(currency: String) {
        context.dataStore.edit { it[TARGET_CURRENCY] = currency }
    }
    
    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    val userId: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[USER_ID] }

    suspend fun saveUserId(id: String) {
        context.dataStore.edit { it[USER_ID] = id }
    }
    
    companion object {
        val MODRINTH_TOKEN = stringPreferencesKey("modrinth_token")
        val CURSEFORGE_POINTS = doublePreferencesKey("curseforge_points")
        val CURSEFORGE_COOKIES = stringPreferencesKey("curseforge_cookies")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_AVATAR = stringPreferencesKey("user_avatar")
        val USER_ID = stringPreferencesKey("user_id")
        val TARGET_CURRENCY = stringPreferencesKey("target_currency")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
