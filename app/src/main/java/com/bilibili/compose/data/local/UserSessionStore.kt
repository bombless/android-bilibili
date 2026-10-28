package com.bilibili.compose.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bilibili.compose.data.model.NavData
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.net.URI

private val Context.dataStore by preferencesDataStore(name = "bili_user_prefs")

class UserSessionStore(private val context: Context) {

    private val gson = Gson()

    companion object {
        val KEY_SESSDATA = stringPreferencesKey("sessdata")
        val KEY_BILI_JCT = stringPreferencesKey("bili_jct")
        val KEY_DEDE_USER_ID = stringPreferencesKey("dede_user_id")
        val KEY_DEDE_USER_ID_MD5 = stringPreferencesKey("dede_user_id_md5")
        val KEY_USER_NAV = stringPreferencesKey("user_nav_json")
    }

    val sessdataFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_SESSDATA]?.takeIf { it.isNotBlank() }
    }

    val isLoggedInFlow: Flow<Boolean> = sessdataFlow.map { it != null }

    val userNavFlow: Flow<NavData?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAV]?.let { json ->
            try {
                gson.fromJson(json, NavData::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getCookieHeader(): String {
        val prefs = context.dataStore.data.first()
        val sessdata = prefs[KEY_SESSDATA]
        if (sessdata.isNullOrBlank()) return ""
        val builder = StringBuilder("SESSDATA=$sessdata")
        prefs[KEY_BILI_JCT]?.let { if (it.isNotBlank()) builder.append("; bili_jct=$it") }
        prefs[KEY_DEDE_USER_ID]?.let { if (it.isNotBlank()) builder.append("; DedeUserID=$it") }
        prefs[KEY_DEDE_USER_ID_MD5]?.let { if (it.isNotBlank()) builder.append("; DedeUserID__ckMd5=$it") }
        return builder.toString()
    }

    suspend fun saveCookies(cookies: Map<String, String>) {
        context.dataStore.edit { prefs ->
            cookies["SESSDATA"]?.let { prefs[KEY_SESSDATA] = it }
            cookies["bili_jct"]?.let { prefs[KEY_BILI_JCT] = it }
            cookies["DedeUserID"]?.let { prefs[KEY_DEDE_USER_ID] = it }
            cookies["DedeUserID__ckMd5"]?.let { prefs[KEY_DEDE_USER_ID_MD5] = it }
        }
    }

    suspend fun saveFromUrl(url: String) {
        if (url.isBlank()) return
        try {
            val uri = URI(url)
            val query = uri.query ?: (if (url.contains("?")) url.substringAfter("?") else "")
            val cookies = mutableMapOf<String, String>()
            query.split("&").forEach { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) {
                    cookies[parts[0]] = parts[1]
                }
            }
            saveCookies(cookies)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveFromSetCookieHeaders(headers: List<String>) {
        val cookies = mutableMapOf<String, String>()
        for (header in headers) {
            val cookiePart = header.substringBefore(";")
            val parts = cookiePart.split("=", limit = 2)
            if (parts.size == 2) {
                val name = parts[0].trim()
                val value = parts[1].trim()
                if (name in listOf("SESSDATA", "bili_jct", "DedeUserID", "DedeUserID__ckMd5")) {
                    cookies[name] = value
                }
            }
        }
        if (cookies.isNotEmpty()) {
            saveCookies(cookies)
        }
    }

    suspend fun saveUserNav(nav: NavData) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_NAV] = gson.toJson(nav)
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SESSDATA)
            prefs.remove(KEY_BILI_JCT)
            prefs.remove(KEY_DEDE_USER_ID)
            prefs.remove(KEY_DEDE_USER_ID_MD5)
            prefs.remove(KEY_USER_NAV)
        }
    }
}
