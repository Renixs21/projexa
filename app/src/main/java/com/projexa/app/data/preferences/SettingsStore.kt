package com.projexa.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.projexa.app.domain.models.AppSettings
import androidx.datastore.preferences.preferencesDataStore
import com.projexa.app.domain.models.GeoPoint
import com.projexa.app.domain.models.Place
import com.projexa.app.domain.models.SavedPlaces
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "projexa_settings")

class SettingsStore(private val context: Context) {
    private val lastAddressKey = stringPreferencesKey("last_belt_address")
    private val lastNameKey = stringPreferencesKey("last_belt_name")
    private val homeKey = stringPreferencesKey("place_home")
    private val workKey = stringPreferencesKey("place_work")
    private val historyKey = stringPreferencesKey("place_history")

    /** Last belt the user connected to, as (name, address). */
    val lastBelt: Flow<Pair<String, String>?> = context.dataStore.data.map { p ->
        val addr = p[lastAddressKey]
        val name = p[lastNameKey]
        if (addr != null && name != null) name to addr else null
    }

    suspend fun saveLastBelt(name: String, address: String) {
        context.dataStore.edit {
            it[lastAddressKey] = address
            it[lastNameKey] = name
        }
    }

    // ---- App settings ----

    private val fontKey = floatPreferencesKey("font_scale")
    private val contrastKey = booleanPreferencesKey("high_contrast")
    private val simpleKey = booleanPreferencesKey("simple_mode")
    private val pulseKey = floatPreferencesKey("pulse_scale")
    private val cueDistKey = intPreferencesKey("cue_distance_m")
    private val speechKey = floatPreferencesKey("speech_rate")
    private val onboardedKey = booleanPreferencesKey("onboarding_done")

    val appSettings: Flow<AppSettings> = context.dataStore.data.map { p ->
        val d = AppSettings()
        AppSettings(
            fontScale = p[fontKey] ?: d.fontScale,
            highContrast = p[contrastKey] ?: d.highContrast,
            simpleMode = p[simpleKey] ?: d.simpleMode,
            pulseScale = p[pulseKey] ?: d.pulseScale,
            cueDistanceM = p[cueDistKey] ?: d.cueDistanceM,
            speechRate = p[speechKey] ?: d.speechRate,
            onboardingDone = p[onboardedKey] ?: d.onboardingDone,
        )
    }

    suspend fun saveAppSettings(s: AppSettings) {
        context.dataStore.edit {
            it[fontKey] = s.fontScale
            it[contrastKey] = s.highContrast
            it[simpleKey] = s.simpleMode
            it[pulseKey] = s.pulseScale
            it[cueDistKey] = s.cueDistanceM
            it[speechKey] = s.speechRate
            it[onboardedKey] = s.onboardingDone
        }
    }

    // ---- Saved places & navigation history (stored as small JSON strings) ----

    val savedPlaces: Flow<SavedPlaces> = context.dataStore.data.map { p ->
        SavedPlaces(
            home = p[homeKey]?.let(::decodePlace),
            work = p[workKey]?.let(::decodePlace),
            history = p[historyKey]?.let(::decodePlaces).orEmpty(),
        )
    }

    suspend fun setHome(place: Place) = context.dataStore.edit { it[homeKey] = encodePlace(place).toString() }
    suspend fun setWork(place: Place) = context.dataStore.edit { it[workKey] = encodePlace(place).toString() }

    suspend fun addHistory(place: Place) {
        context.dataStore.edit { prefs ->
            val current = prefs[historyKey]?.let(::decodePlaces).orEmpty()
            val updated = (listOf(place) + current.filter { it.point != place.point }).take(MAX_HISTORY)
            prefs[historyKey] = JSONArray(updated.map(::encodePlace)).toString()
        }
    }

    private fun encodePlace(p: Place) =
        JSONObject().put("n", p.name).put("la", p.point.lat).put("lo", p.point.lon)

    private fun decodePlace(s: String): Place? = try {
        val o = JSONObject(s)
        Place(o.getString("n"), GeoPoint(o.getDouble("la"), o.getDouble("lo")))
    } catch (_: Exception) {
        null
    }

    private fun decodePlaces(s: String): List<Place> = try {
        val a = JSONArray(s)
        (0 until a.length()).mapNotNull { decodePlace(a.getJSONObject(it).toString()) }
    } catch (_: Exception) {
        emptyList()
    }

    private companion object {
        const val MAX_HISTORY = 10
    }
}
