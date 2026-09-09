package com.loopworks.nibblemath.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * User preferences backed by DataStore. [currency] is an ISO-4217 code;
 * [priceRounding] is the number of decimal places for displayed prices.
 */
class SettingsRepository(private val context: Context) {
    private val dataStore = context.settingsDataStore

    val currency: Flow<String> = dataStore.data.map { prefs ->
        prefs[CURRENCY] ?: DEFAULT_CURRENCY
    }

    val priceRounding: Flow<Int> = dataStore.data.map { prefs ->
        prefs[ROUNDING] ?: DEFAULT_ROUNDING
    }

    suspend fun setCurrency(code: String) {
        dataStore.edit { it[CURRENCY] = code }
    }

    suspend fun setPriceRounding(places: Int) {
        dataStore.edit { it[ROUNDING] = places }
    }

    private companion object {
        val CURRENCY = stringPreferencesKey("currency")
        val ROUNDING = intPreferencesKey("price_rounding")
        const val DEFAULT_CURRENCY = "AUD"
        const val DEFAULT_ROUNDING = 2
    }
}
