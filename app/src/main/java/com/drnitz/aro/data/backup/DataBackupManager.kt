package com.drnitz.aro.data.backup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase
import com.drnitz.aro.data.db.AroDatabase
import com.drnitz.aro.domain.model.MedicationReminder
import com.drnitz.aro.notifications.NotificationPreferences
import com.drnitz.aro.notifications.NotificationSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Base64

/** Result of a backup or restore operation, shown in the Data Management screen. */
data class BackupSummary(
    val tableCount: Int,
    val rowCount: Int,
    val schemaVersion: Int,
    val createdAt: Long,
)

/**
 * Complete on-device JSON backup / restore.
 *
 * The backup is intentionally schema-driven rather than a set of hand-written DTOs:
 * every user table is dumped through SQLite metadata, so a future column or table is
 * included automatically instead of being silently forgotten. Restore validates the
 * archive, deletes the current rows inside one transaction and re-inserts the exact
 * values, so a failed import rolls back to the pre-import database.
 *
 * The notification/medication settings live in DataStore rather than Room; they are
 * embedded in the same archive so a device migration keeps reminders and adherence.
 */
class DataBackupManager(
    private val database: AroDatabase,
    private val notificationPreferences: NotificationPreferences,
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    suspend fun export(context: Context, uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val sqlite = database.openHelper.readableDatabase
        val tables = orderedTables(sqlite)
        val settings = notificationPreferences.currentSettings()
        val createdAt = System.currentTimeMillis()

        var rowCount = 0
        val tablesJson = buildJsonObject {
            tables.forEach { table ->
                val rows = exportTable(sqlite, table)
                rowCount += rows.size
                put(table, rows)
            }
        }

        val root = buildJsonObject {
            put("format", FORMAT_NAME)
            put("formatVersion", FORMAT_VERSION)
            put("schemaVersion", AroDatabase.VERSION)
            put("createdAt", createdAt)
            put("tables", tablesJson)
            put("preferences", settingsToJson(settings))
        }

        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.writer(Charsets.UTF_8).use { it.write(json.encodeToString(JsonObject.serializer(), root)) }
        } ?: throw IOException("Could not open the selected destination for writing.")

        BackupSummary(
            tableCount = tables.size,
            rowCount = rowCount,
            schemaVersion = AroDatabase.VERSION,
            createdAt = createdAt,
        )
    }

    suspend fun import(context: Context, uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.reader(Charsets.UTF_8).readText()
        } ?: throw IOException("Could not open the selected backup file.")

        val root = try {
            json.parseToJsonElement(text).jsonObject
        } catch (error: Exception) {
            throw IllegalArgumentException("This file is not a valid Aro JSON backup.", error)
        }

        if (root["format"]?.jsonPrimitive?.contentOrNull != FORMAT_NAME) {
            throw IllegalArgumentException("This file is not an Aro backup.")
        }
        val formatVersion = root["formatVersion"]?.jsonPrimitive?.intOrNull ?: 0
        if (formatVersion !in 1..FORMAT_VERSION) {
            throw IllegalArgumentException("Unsupported backup format version $formatVersion.")
        }
        val schemaVersion = root["schemaVersion"]?.jsonPrimitive?.intOrNull ?: 0
        if (schemaVersion !in 1..AroDatabase.VERSION) {
            throw IllegalArgumentException(
                "This backup was created by a newer app version (schema $schemaVersion).",
            )
        }

        val tablesJson = root["tables"] as? JsonObject
            ?: throw IllegalArgumentException("The backup does not contain any tables.")
        if (tablesJson.isEmpty()) {
            throw IllegalArgumentException("The backup contains no data.")
        }

        val sqlite = database.openHelper.writableDatabase
        val existingTables = orderedTables(sqlite)
        val tablesInBackup = existingTables.filter { tablesJson[it] is JsonArray }
        if (tablesInBackup.isEmpty()) {
            throw IllegalArgumentException("The backup does not contain any recognised Aro tables.")
        }

        // Validate the settings payload before touching the database, so a malformed
        // archive cannot leave the user with an empty diary and no reminders.
        val settings = parseSettings(root["preferences"])

        var rowCount = 0
        sqlite.beginTransaction()
        try {
            // Deferred checks let us delete and re-insert across foreign keys in one
            // atomic transaction; the final state is validated on commit.
            sqlite.execSQL("PRAGMA defer_foreign_keys = ON")
            existingTables.asReversed().forEach { table ->
                sqlite.execSQL("DELETE FROM `$table`")
            }

            tablesInBackup.forEach { table ->
                val columns = tableColumns(sqlite, table)
                val rows = tablesJson[table]?.jsonArray
                    ?: throw IllegalArgumentException("Table $table is not a JSON array.")
                rows.forEach { element ->
                    val row = element as? JsonObject
                        ?: throw IllegalArgumentException("Table $table contains a non-object row.")
                    val values = ContentValues()
                    row.forEach { (column, value) ->
                        // Unknown columns are ignored so a backup from an older schema
                        // restores into the current one instead of failing wholesale.
                        if (column in columns) {
                            putJsonValue(values, column, value)
                        }
                    }
                    val result = sqlite.insert(table, SQLiteDatabase.CONFLICT_ABORT, values)
                    if (result == -1L) {
                        throw IllegalArgumentException(
                            "Could not restore a row into $table. The backup may be incomplete.",
                        )
                    }
                    rowCount++
                }
            }

            // Backups made before schema 8 only carry days.water_ml. Reconstruct one
            // historical entry per such day so restoring an older archive does not
            // produce a non-zero reservoir with an empty drink log.
            sqlite.execSQL(
                """
                INSERT INTO `water_logs` (`id`, `epoch_day`, `amount_ml`, `logged_at`, `source`)
                SELECT 'water-restore-' || `epoch_day`, `epoch_day`, `water_ml`, `updated_at`, 'BACKFILL'
                FROM `days`
                WHERE `water_ml` > 0
                  AND NOT EXISTS (
                      SELECT 1 FROM `water_logs` WHERE `water_logs`.`epoch_day` = `days`.`epoch_day`
                  )
                """.trimIndent(),
            )

            sqlite.setTransactionSuccessful()
        } finally {
            sqlite.endTransaction()
        }

        if (settings != null) {
            notificationPreferences.replaceSettings(settings)
        }

        // Raw SQL writes are invisible to Room's invalidation tracker; nudge every
        // active Flow so the UI reloads the restored data immediately.
        database.invalidationTracker.refreshAsync()

        BackupSummary(
            tableCount = tablesInBackup.size,
            rowCount = rowCount,
            schemaVersion = schemaVersion,
            createdAt = root["createdAt"]?.jsonPrimitive?.longOrNull ?: 0L,
        )
    }

    fun suggestedFileName(today: LocalDate = LocalDate.now()): String =
        "aro-backup-${today.format(DateTimeFormatter.ISO_LOCAL_DATE)}.json"

    // ---- Export helpers ---------------------------------------------------

    private fun exportTable(sqlite: SupportSQLiteDatabase, table: String): JsonArray =
        buildJsonArray {
            sqlite.query("SELECT * FROM `$table`").use { cursor ->
                while (cursor.moveToNext()) {
                    add(rowToJson(cursor))
                }
            }
        }

    private fun rowToJson(cursor: Cursor): JsonObject = buildJsonObject {
        for (index in 0 until cursor.columnCount) {
            val column = cursor.getColumnName(index)
            when (cursor.getType(index)) {
                Cursor.FIELD_TYPE_NULL -> put(column, JsonNull)
                Cursor.FIELD_TYPE_INTEGER -> put(column, cursor.getLong(index))
                Cursor.FIELD_TYPE_FLOAT -> put(column, cursor.getDouble(index))
                Cursor.FIELD_TYPE_STRING -> put(column, cursor.getString(index))
                Cursor.FIELD_TYPE_BLOB -> putJsonObject(column) {
                    put("__blob__", Base64.getEncoder().encodeToString(cursor.getBlob(index)))
                }
                else -> put(column, JsonNull)
            }
        }
    }

    private fun settingsToJson(settings: NotificationSettings): JsonObject = buildJsonObject {
        put("hydrationEnabled", settings.hydrationEnabled)
        put("hydrationIntervalHours", settings.hydrationIntervalHours)
        put("hydrationStartHour", settings.hydrationStartHour)
        put("hydrationEndHour", settings.hydrationEndHour)
        put("fastingEnabled", settings.fastingEnabled)
        putJsonArray("medications") {
            settings.medicationReminders.forEach { reminder ->
                add(
                    buildJsonObject {
                        put("id", reminder.id)
                        put("name", reminder.name)
                        put("dosage", reminder.dosage)
                        put("hour", reminder.hour)
                        put("minute", reminder.minute)
                        put("enabled", reminder.enabled)
                        put("createdAt", reminder.createdAt)
                    },
                )
            }
        }
    }

    // ---- Restore helpers --------------------------------------------------

    private fun parseSettings(element: JsonElement?): NotificationSettings? {
        val root = element as? JsonObject ?: return null
        val medications = root["medications"]?.jsonArray?.mapNotNull { entry ->
            val med = entry as? JsonObject ?: return@mapNotNull null
            val name = med["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            MedicationReminder(
                id = med["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                name = name,
                dosage = med["dosage"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                hour = (med["hour"]?.jsonPrimitive?.intOrNull ?: 8).coerceIn(0, 23),
                minute = (med["minute"]?.jsonPrimitive?.intOrNull ?: 0).coerceIn(0, 59),
                enabled = med["enabled"]?.jsonPrimitive?.booleanOrNull ?: true,
                createdAt = med["createdAt"]?.jsonPrimitive?.longOrNull ?: 0L,
            )
        }.orEmpty()

        return NotificationSettings(
            hydrationEnabled = root["hydrationEnabled"]?.jsonPrimitive?.booleanOrNull ?: true,
            hydrationIntervalHours = (root["hydrationIntervalHours"]?.jsonPrimitive?.intOrNull ?: 2)
                .coerceIn(1, 8),
            hydrationStartHour = (root["hydrationStartHour"]?.jsonPrimitive?.intOrNull ?: 8)
                .coerceIn(0, 23),
            hydrationEndHour = (root["hydrationEndHour"]?.jsonPrimitive?.intOrNull ?: 21)
                .coerceIn(0, 23),
            fastingEnabled = root["fastingEnabled"]?.jsonPrimitive?.booleanOrNull ?: true,
            medicationReminders = medications,
        )
    }

    private fun putJsonValue(values: ContentValues, column: String, value: JsonElement) {
        when (value) {
            is JsonNull -> values.putNull(column)
            is JsonObject -> {
                val encoded = value["__blob__"]?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Column $column has an unsupported object value.")
                values.put(column, Base64.getDecoder().decode(encoded))
            }
            is JsonPrimitive -> when {
                value.isString -> values.put(column, value.content)
                value.booleanOrNull != null -> values.put(column, if (value.booleanOrNull == true) 1 else 0)
                value.longOrNull != null -> values.put(column, value.longOrNull)
                value.doubleOrNull != null -> values.put(column, value.doubleOrNull)
                else -> throw IllegalArgumentException("Column $column has an unsupported value.")
            }
            is JsonArray -> throw IllegalArgumentException("Column $column has an unsupported array value.")
        }
    }

    private fun tableColumns(sqlite: SupportSQLiteDatabase, table: String): Set<String> {
        val columns = mutableSetOf<String>()
        sqlite.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(nameIndex))
            }
        }
        return columns
    }

    private fun orderedTables(sqlite: SupportSQLiteDatabase): List<String> {
        val found = mutableListOf<String>()
        sqlite.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' " +
                "AND name NOT LIKE 'sqlite_%' " +
                "AND name NOT IN ('android_metadata', 'room_master_table') " +
                "ORDER BY rootpage",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                found.add(cursor.getString(0))
            }
        }
        return found.sortedBy { table ->
            TABLE_ORDER.indexOf(table).takeIf { it >= 0 } ?: TABLE_ORDER.size
        }
    }

    companion object {
        const val FORMAT_NAME = "aro-backup"
        const val FORMAT_VERSION = 1

        /**
         * Parent tables first. Restore deletes in reverse and inserts in this order, so
         * foreign keys are satisfied throughout the transaction.
         */
        private val TABLE_ORDER = listOf(
            "profile",
            "weight_entries",
            "fasting_sessions",
            "days",
            "water_logs",
            "food_logs",
            "workouts",
            "workout_sets",
            "custom_foods",
            "favorite_foods",
            "recent_foods",
            "meal_templates",
            "meal_template_items",
            "vitals_entries",
            "doctor_consultations",
            "plan_series",
            "plan_completions",
            "medication_logs",
        )
    }
}
