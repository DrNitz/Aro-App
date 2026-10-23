package com.drnitz.aro.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.drnitz.aro.domain.model.MedicationReminder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Snapshot of the notification configuration.
 *
 * Kept as a plain immutable data class so UI and schedulers can consume one consistent
 * view of the settings instead of reading several independent keys.
 */
data class NotificationSettings(
    val hydrationEnabled: Boolean = true,
    val hydrationIntervalHours: Int = 2,
    val hydrationStartHour: Int = 8,
    val hydrationEndHour: Int = 21,
    val fastingEnabled: Boolean = true,
    val medicationReminders: List<MedicationReminder> = emptyList(),
)

// DataStore is the single source of truth. The first read migrates the pre-DataStore
// SharedPreferences file in place, so existing reminders and toggles survive the upgrade.
private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = NotificationPreferences.DATASTORE_NAME,
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, NotificationPreferences.LEGACY_PREFS_NAME))
    },
)

/**
 * Notification and medication-reminder configuration backed by Jetpack DataStore.
 *
 * Reads are exposed as a [Flow] so the settings sheet and the medication adherence card
 * update automatically after a write. Writes are suspending and atomic.
 */
class NotificationPreferences(context: Context) {

    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    val settings: Flow<NotificationSettings> = appContext.notificationDataStore.data
        .catch { error ->
            // A corrupt preferences file should never crash a reminder alarm; fall back
            // to defaults and let the next write heal it.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::toSettings)

    suspend fun currentSettings(): NotificationSettings = settings.first()

    suspend fun setHydrationEnabled(value: Boolean) {
        appContext.notificationDataStore.edit { it[KEY_HYDRATION_ENABLED] = value }
    }

    suspend fun setHydrationIntervalHours(value: Int) {
        appContext.notificationDataStore.edit { it[KEY_HYDRATION_INTERVAL] = value.coerceIn(1, 8) }
    }

    suspend fun setHydrationStartHour(value: Int) {
        appContext.notificationDataStore.edit { it[KEY_HYDRATION_START_HOUR] = value.coerceIn(0, 23) }
    }

    suspend fun setHydrationEndHour(value: Int) {
        appContext.notificationDataStore.edit { it[KEY_HYDRATION_END_HOUR] = value.coerceIn(0, 23) }
    }

    suspend fun setFastingEnabled(value: Boolean) {
        appContext.notificationDataStore.edit { it[KEY_FASTING_ENABLED] = value }
    }

    suspend fun getMedicationReminders(): List<MedicationReminder> =
        currentSettings().medicationReminders

    suspend fun saveMedicationReminders(reminders: List<MedicationReminder>) {
        appContext.notificationDataStore.edit {
            it[KEY_MEDICATIONS] = json.encodeToString(reminders)
        }
    }

    suspend fun addMedicationReminder(reminder: MedicationReminder) {
        appContext.notificationDataStore.edit { prefs ->
            val current = decodeMedications(prefs[KEY_MEDICATIONS])
                .filterNot { it.id == reminder.id } + reminder
            prefs[KEY_MEDICATIONS] = json.encodeToString(current)
        }
    }

    suspend fun deleteMedicationReminder(id: String) {
        appContext.notificationDataStore.edit { prefs ->
            val current = decodeMedications(prefs[KEY_MEDICATIONS]).filterNot { it.id == id }
            prefs[KEY_MEDICATIONS] = json.encodeToString(current)
        }
    }

    /** Replaces every setting atomically, used when restoring a JSON backup. */
    suspend fun replaceSettings(settings: NotificationSettings) {
        appContext.notificationDataStore.edit { prefs ->
            prefs[KEY_HYDRATION_ENABLED] = settings.hydrationEnabled
            prefs[KEY_HYDRATION_INTERVAL] = settings.hydrationIntervalHours.coerceIn(1, 8)
            prefs[KEY_HYDRATION_START_HOUR] = settings.hydrationStartHour.coerceIn(0, 23)
            prefs[KEY_HYDRATION_END_HOUR] = settings.hydrationEndHour.coerceIn(0, 23)
            prefs[KEY_FASTING_ENABLED] = settings.fastingEnabled
            prefs[KEY_MEDICATIONS] = json.encodeToString(settings.medicationReminders)
        }
    }

    private fun toSettings(prefs: Preferences): NotificationSettings = NotificationSettings(
        hydrationEnabled = prefs[KEY_HYDRATION_ENABLED] ?: true,
        hydrationIntervalHours = (prefs[KEY_HYDRATION_INTERVAL] ?: 2).coerceIn(1, 8),
        hydrationStartHour = (prefs[KEY_HYDRATION_START_HOUR] ?: 8).coerceIn(0, 23),
        hydrationEndHour = (prefs[KEY_HYDRATION_END_HOUR] ?: 21).coerceIn(0, 23),
        fastingEnabled = prefs[KEY_FASTING_ENABLED] ?: true,
        medicationReminders = decodeMedications(prefs[KEY_MEDICATIONS]),
    )

    private fun decodeMedications(raw: String?): List<MedicationReminder> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<MedicationReminder>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        const val DATASTORE_NAME = "aro_notification_prefs"
        const val LEGACY_PREFS_NAME = "aro_notification_prefs"

        private val KEY_HYDRATION_ENABLED = booleanPreferencesKey("hydration_enabled")
        private val KEY_HYDRATION_INTERVAL = intPreferencesKey("hydration_interval")
        private val KEY_HYDRATION_START_HOUR = intPreferencesKey("hydration_start_hour")
        private val KEY_HYDRATION_END_HOUR = intPreferencesKey("hydration_end_hour")
        private val KEY_FASTING_ENABLED = booleanPreferencesKey("fasting_enabled")
        private val KEY_MEDICATIONS = stringPreferencesKey("medications_json")
    }
}
