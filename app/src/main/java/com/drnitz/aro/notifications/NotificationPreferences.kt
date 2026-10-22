package com.drnitz.aro.notifications

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class MedicationReminder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dosage: String = "",
    val hour: Int, // 0..23
    val minute: Int, // 0..59
    val enabled: Boolean = true,
)

class NotificationPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    var hydrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_HYDRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HYDRATION_ENABLED, value).apply()

    var hydrationIntervalHours: Int
        get() = prefs.getInt(KEY_HYDRATION_INTERVAL, 2)
        set(value) = prefs.edit().putInt(KEY_HYDRATION_INTERVAL, value.coerceIn(1, 8)).apply()

    var hydrationStartHour: Int
        get() = prefs.getInt(KEY_HYDRATION_START_HOUR, 8)
        set(value) = prefs.edit().putInt(KEY_HYDRATION_START_HOUR, value.coerceIn(0, 23)).apply()

    var hydrationEndHour: Int
        get() = prefs.getInt(KEY_HYDRATION_END_HOUR, 21)
        set(value) = prefs.edit().putInt(KEY_HYDRATION_END_HOUR, value.coerceIn(0, 23)).apply()

    var fastingEnabled: Boolean
        get() = prefs.getBoolean(KEY_FASTING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_FASTING_ENABLED, value).apply()

    fun getMedicationReminders(): List<MedicationReminder> {
        val raw = prefs.getString(KEY_MEDICATIONS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<MedicationReminder>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveMedicationReminders(reminders: List<MedicationReminder>) {
        val encoded = json.encodeToString(reminders)
        prefs.edit().putString(KEY_MEDICATIONS, encoded).apply()
    }

    fun addMedicationReminder(reminder: MedicationReminder) {
        val current = getMedicationReminders().toMutableList()
        current.removeAll { it.id == reminder.id }
        current.add(reminder)
        saveMedicationReminders(current)
    }

    fun deleteMedicationReminder(id: String) {
        val current = getMedicationReminders().filterNot { it.id == id }
        saveMedicationReminders(current)
    }

    companion object {
        private const val PREFS_NAME = "aro_notification_prefs"
        private const val KEY_HYDRATION_ENABLED = "hydration_enabled"
        private const val KEY_HYDRATION_INTERVAL = "hydration_interval"
        private const val KEY_HYDRATION_START_HOUR = "hydration_start_hour"
        private const val KEY_HYDRATION_END_HOUR = "hydration_end_hour"
        private const val KEY_FASTING_ENABLED = "fasting_enabled"
        private const val KEY_MEDICATIONS = "medications_json"
    }
}
