package com.example.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.tveoDataStore: DataStore<Preferences> by preferencesDataStore(name = "tveoclean_user_prefs")

data class UserPreferencesState(
    val language: AppLanguage = AppLanguage.ES, // 100% Spanish strictly by default
    val themeMode: ThemeMode = ThemeMode.DARK,
    val reduceMotion: Boolean = false,
    val showAdvancedAdbDetails: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val activeMobileUiPackId: String? = null
)

interface UserPreferencesRepository {
    val preferencesFlow: Flow<UserPreferencesState>
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setReduceMotion(enabled: Boolean)
    suspend fun setShowAdvancedAdbDetails(enabled: Boolean)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setActiveMobileUiPackId(packId: String?)
}

class DataStoreUserPreferencesRepository(
    private val context: Context
) : UserPreferencesRepository {

    private object Keys {
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val ADVANCED_ADB = booleanPreferencesKey("advanced_adb")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val ACTIVE_MOBILE_PACK_ID = stringPreferencesKey("active_mobile_pack_id")
    }

    override val preferencesFlow: Flow<UserPreferencesState> = context.tveoDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val langName = prefs[Keys.APP_LANGUAGE] ?: AppLanguage.ES.name
            val language = runCatching { AppLanguage.valueOf(langName) }.getOrDefault(AppLanguage.ES)
            val modeName = prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name
            val mode = runCatching { ThemeMode.valueOf(modeName) }.getOrDefault(ThemeMode.DARK)
            UserPreferencesState(
                language = language,
                themeMode = mode,
                reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
                showAdvancedAdbDetails = prefs[Keys.ADVANCED_ADB] ?: false,
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                activeMobileUiPackId = prefs[Keys.ACTIVE_MOBILE_PACK_ID]
            )
        }

    override suspend fun setLanguage(language: AppLanguage) {
        context.tveoDataStore.edit { it[Keys.APP_LANGUAGE] = language.name }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.tveoDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setReduceMotion(enabled: Boolean) {
        context.tveoDataStore.edit { it[Keys.REDUCE_MOTION] = enabled }
    }

    override suspend fun setShowAdvancedAdbDetails(enabled: Boolean) {
        context.tveoDataStore.edit { it[Keys.ADVANCED_ADB] = enabled }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.tveoDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setActiveMobileUiPackId(packId: String?) {
        context.tveoDataStore.edit { prefs ->
            if (packId == null) {
                prefs.remove(Keys.ACTIVE_MOBILE_PACK_ID)
            } else {
                prefs[Keys.ACTIVE_MOBILE_PACK_ID] = packId
            }
        }
    }
}

class InMemoryUserPreferencesRepository(
    initial: UserPreferencesState = UserPreferencesState()
) : UserPreferencesRepository {
    private val state = kotlinx.coroutines.flow.MutableStateFlow(initial)
    override val preferencesFlow: Flow<UserPreferencesState> = state

    override suspend fun setLanguage(language: AppLanguage) {
        state.value = state.value.copy(language = language)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = state.value.copy(themeMode = mode)
    }

    override suspend fun setReduceMotion(enabled: Boolean) {
        state.value = state.value.copy(reduceMotion = enabled)
    }

    override suspend fun setShowAdvancedAdbDetails(enabled: Boolean) {
        state.value = state.value.copy(showAdvancedAdbDetails = enabled)
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        state.value = state.value.copy(onboardingCompleted = completed)
    }

    override suspend fun setActiveMobileUiPackId(packId: String?) {
        state.value = state.value.copy(activeMobileUiPackId = packId)
    }
}
