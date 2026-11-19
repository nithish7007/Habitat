package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lifeOsDataStore: DataStore<Preferences> by preferencesDataStore(name = "life_os_prefs")

class PreferencesRepository(private val context: Context) {

    private fun key(uid: String, name: String) = "${uid}_$name"

    fun observePreferences(uid: String): Flow<UserPreferencesModel> =
        context.lifeOsDataStore.data.map { prefs ->
            val rawWidgets = prefs[stringPreferencesKey(key(uid, "enabled_widgets"))]
            val widgets = if (rawWidgets == null) {
                DashboardWidgetType.DEFAULT_ORDER
            } else if (rawWidgets.isBlank()) {
                emptyList()
            } else {
                rawWidgets.split(",")
                    .mapNotNull { DashboardWidgetType.fromNameOrNull(it.trim()) }
                    .distinct()
            }

            val rawTheme = prefs[stringPreferencesKey(key(uid, "theme_mode"))] ?: ThemeMode.SYSTEM.name
            val theme = ThemeMode.entries.firstOrNull { it.name == rawTheme } ?: ThemeMode.SYSTEM

            UserPreferencesModel(
                enabledWidgets = widgets,
                themeMode = theme,
                weightTasks = prefs[intPreferencesKey(key(uid, "w_tasks"))] ?: 30,
                weightHabits = prefs[intPreferencesKey(key(uid, "w_habits"))] ?: 30,
                weightChallenges = prefs[intPreferencesKey(key(uid, "w_challenges"))] ?: 20,
                weightWater = prefs[intPreferencesKey(key(uid, "w_water"))] ?: 10,
                weightSteps = prefs[intPreferencesKey(key(uid, "w_steps"))] ?: 10,
                waterTargetMl = prefs[intPreferencesKey(key(uid, "water_target"))] ?: 2500,
                stepsTarget = prefs[intPreferencesKey(key(uid, "steps_target"))] ?: 8000,
                focusTargetMinutes = prefs[intPreferencesKey(key(uid, "focus_target"))] ?: 90,
                quietHoursStart = prefs[intPreferencesKey(key(uid, "quiet_start"))] ?: 22,
                quietHoursEnd = prefs[intPreferencesKey(key(uid, "quiet_end"))] ?: 7
            )
        }

    fun observeGlobalThemeMode(): Flow<ThemeMode> =
        context.lifeOsDataStore.data.map { prefs ->
            val rawTheme = prefs[stringPreferencesKey("global_theme_mode")] ?: ThemeMode.SYSTEM.name
            ThemeMode.entries.firstOrNull { it.name == rawTheme } ?: ThemeMode.SYSTEM
        }

    suspend fun savePreferences(uid: String, model: UserPreferencesModel) {
        context.lifeOsDataStore.edit { prefs ->
            prefs[stringPreferencesKey(key(uid, "enabled_widgets"))] =
                model.enabledWidgets.joinToString(",") { it.name }
            prefs[stringPreferencesKey(key(uid, "theme_mode"))] = model.themeMode.name
            prefs[stringPreferencesKey("global_theme_mode")] = model.themeMode.name
            prefs[intPreferencesKey(key(uid, "w_tasks"))] = model.weightTasks
            prefs[intPreferencesKey(key(uid, "w_habits"))] = model.weightHabits
            prefs[intPreferencesKey(key(uid, "w_challenges"))] = model.weightChallenges
            prefs[intPreferencesKey(key(uid, "w_water"))] = model.weightWater
            prefs[intPreferencesKey(key(uid, "w_steps"))] = model.weightSteps
            prefs[intPreferencesKey(key(uid, "water_target"))] = model.waterTargetMl
            prefs[intPreferencesKey(key(uid, "steps_target"))] = model.stepsTarget
            prefs[intPreferencesKey(key(uid, "focus_target"))] = model.focusTargetMinutes
            prefs[intPreferencesKey(key(uid, "quiet_start"))] = model.quietHoursStart
            prefs[intPreferencesKey(key(uid, "quiet_end"))] = model.quietHoursEnd
        }
    }

    fun observeTimerState(uid: String): Flow<PersistedTimerState> =
        context.lifeOsDataStore.data.map { prefs ->
            val rawMode = prefs[stringPreferencesKey(key(uid, "timer_mode"))] ?: FocusMode.POMODORO.name
            val mode = FocusMode.entries.firstOrNull { it.name == rawMode } ?: FocusMode.POMODORO
            PersistedTimerState(
                mode = mode,
                category = prefs[stringPreferencesKey(key(uid, "timer_cat"))] ?: "DSA",
                plannedMinutes = prefs[intPreferencesKey(key(uid, "timer_planned"))] ?: 25,
                breakMinutes = prefs[intPreferencesKey(key(uid, "timer_break"))] ?: 5,
                isBreakPhase = prefs[booleanPreferencesKey(key(uid, "timer_is_break"))] ?: false,
                running = prefs[booleanPreferencesKey(key(uid, "timer_running"))] ?: false,
                startedAtEpochMs = prefs[longPreferencesKey(key(uid, "timer_start_ms"))] ?: 0L,
                accumulatedElapsedSec = prefs[intPreferencesKey(key(uid, "timer_elapsed_sec"))] ?: 0,
                distractionsCount = prefs[intPreferencesKey(key(uid, "timer_distractions"))] ?: 0,
                sessionNotes = prefs[stringPreferencesKey(key(uid, "timer_notes"))] ?: ""
            )
        }

    suspend fun saveTimerState(uid: String, state: PersistedTimerState) {
        context.lifeOsDataStore.edit { prefs ->
            prefs[stringPreferencesKey(key(uid, "timer_mode"))] = state.mode.name
            prefs[stringPreferencesKey(key(uid, "timer_cat"))] = state.category
            prefs[intPreferencesKey(key(uid, "timer_planned"))] = state.plannedMinutes
            prefs[intPreferencesKey(key(uid, "timer_break"))] = state.breakMinutes
            prefs[booleanPreferencesKey(key(uid, "timer_is_break"))] = state.isBreakPhase
            prefs[booleanPreferencesKey(key(uid, "timer_running"))] = state.running
            prefs[longPreferencesKey(key(uid, "timer_start_ms"))] = state.startedAtEpochMs
            prefs[intPreferencesKey(key(uid, "timer_elapsed_sec"))] = state.accumulatedElapsedSec
            prefs[intPreferencesKey(key(uid, "timer_distractions"))] = state.distractionsCount
            prefs[stringPreferencesKey(key(uid, "timer_notes"))] = state.sessionNotes
        }
    }
}
