package com.drnitz.aro.data.repository

import com.drnitz.aro.domain.model.CuratedFoodItem
import com.drnitz.aro.domain.model.DiaryDay
import com.drnitz.aro.domain.model.DoctorConsultation
import com.drnitz.aro.domain.model.FastingSession
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.MedicationLog
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.PlanItem
import com.drnitz.aro.domain.model.PlanOccurrence
import com.drnitz.aro.domain.model.UserProfile
import com.drnitz.aro.domain.model.VitalsEntry
import com.drnitz.aro.domain.model.WaterLog
import com.drnitz.aro.domain.model.WorkoutLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

/**
 * Aro's persistence contract.
 *
 * Everything above this interface (ViewModels, UI) is storage-agnostic, which is what
 * makes the app testable with fake data and lets the Room implementation be swapped or
 * migrated without touching a single composable.
 *
 * Every read is a [Flow]: the UI subscribes once and re-renders on write. Nothing here
 * polls, and nothing here queries on the main thread.
 */
interface AroRepository {

    // ---- Profile ----------------------------------------------------------

    fun observeProfile(): Flow<UserProfile?>

    suspend fun saveProfile(profile: UserProfile)

    fun observeProfileSettings(): Flow<ProfileSettings?>

    suspend fun saveProfileSettings(settings: ProfileSettings)

    // ---- Diary ------------------------------------------------------------

    /**
     * A single day, assembled from its food, workouts and day state.
     *
     * Takes the day explicitly rather than defaulting to "today" internally: a caller
     * reading yesterday must not be able to accidentally receive today's row, and a
     * caller reading today must not silently get yesterday because the process started
     * before midnight.
     */
    fun observeDiaryDay(date: LocalDate): Flow<DiaryDay>

    /** Day totals as one indexed aggregate — the dashboard's single query. */
    fun observeDayTotals(date: LocalDate): Flow<NutrientTotals>

    suspend fun addFood(entry: FoodLogEntry, date: LocalDate)

    suspend fun updateFood(entry: FoodLogEntry, date: LocalDate)

    suspend fun deleteFood(id: String)

    suspend fun moveFood(id: String, toMeal: com.drnitz.aro.domain.model.MealSlot, toDate: LocalDate)

    suspend fun addWater(date: LocalDate, deltaMl: Int)

    /** Adds a drink at a chosen date and time, including backdated entries. */
    suspend fun addWaterLog(date: LocalDate, amountMl: Int, loggedAt: java.time.Instant) {}

    /** Corrects the volume, date or time of an existing drink. */
    suspend fun updateWaterLog(
        id: String,
        date: LocalDate,
        amountMl: Int,
        loggedAt: java.time.Instant,
    ) {}

    /** Undo path: restores the exact row and re-applies it to its day aggregate. */
    suspend fun restoreWaterLog(log: WaterLog) {}

    suspend fun setWater(date: LocalDate, waterMl: Int)

    /** Every individual drink, newest first. */
    fun observeAllWaterLogs(): Flow<List<WaterLog>> = flowOf(emptyList())

    /** The drinks logged on one day, newest first. */
    fun observeWaterLogsForDate(date: LocalDate): Flow<List<WaterLog>> = flowOf(emptyList())

    /** Removes one drink and keeps the day's aggregate in sync. */
    suspend fun deleteWaterLog(id: String) {}

    suspend fun setNotes(date: LocalDate, notes: String)

    /** Days with content, newest first. */
    fun observeRecentLoggedDays(limit: Int): Flow<List<LocalDate>>

    /** Daily totals over a window, for the Task 12 trend charts. */
    fun observeTotalsInRange(from: LocalDate, toInclusive: LocalDate): Flow<List<DayTotals>>

    /** Stars are snapshots: they survive an upstream Open Food Facts edit and resolve offline. */
    fun observeFavoriteFoods(): Flow<List<CuratedFoodItem>> = flowOf(emptyList())

    fun observeRecentFoods(): Flow<List<CuratedFoodItem>> = flowOf(emptyList())

    suspend fun toggleFavoriteFood(food: CuratedFoodItem) {}

    /** Records a logged portion, which is what feeds the "Recent" filter. */
    suspend fun recordFoodUsage(food: CuratedFoodItem, grams: Double) {}

    // ---- Future-day plans -------------------------------------------------

    /**
     * Plan occurrences for one day. Repeating series are expanded in the domain layer,
     * so a daily plan appears here every day without materialising a row per date.
     */
    fun observePlanOccurrences(date: LocalDate): Flow<List<PlanOccurrence>> = flowOf(emptyList())

    /** All plan occurrences in an inclusive date range, used by the calendar dots. */
    fun observePlanOccurrencesInRange(
        from: LocalDate,
        toInclusive: LocalDate,
    ): Flow<List<PlanOccurrence>> = flowOf(emptyList())

    suspend fun savePlanItem(item: PlanItem) {}

    /** Every plan series, used by the Plan view's upcoming/repeating list. */
    fun observePlanItems(): Flow<List<PlanItem>> = flowOf(emptyList())

    /** Reads every series for boot/app-start reminder rescheduling. */
    suspend fun getAllPlanItems(): List<PlanItem> = emptyList()

    suspend fun getPlanItem(id: String): PlanItem? = null

    /**
     * Completion days for one series in an inclusive range. The reminder scheduler only
     * needs upcoming completions, so this avoids loading a lifetime of history.
     */
    suspend fun getPlanCompletionDates(
        planId: String,
        from: LocalDate,
        toInclusive: LocalDate,
    ): Set<LocalDate> = emptySet()

    suspend fun isPlanOccurrenceCompleted(planId: String, occurrenceDate: LocalDate): Boolean =
        false

    /** Deletes the whole series, including its future occurrences and completion rows. */
    suspend fun deletePlanItem(id: String) {}

    suspend fun completePlanOccurrence(
        planId: String,
        occurrenceDate: LocalDate,
        actualLogId: String? = null,
    ) {}

    /** Marks one occurrence as deliberately skipped without changing the series. */
    suspend fun skipPlanOccurrence(planId: String, occurrenceDate: LocalDate) {}

    suspend fun uncompletePlanOccurrence(planId: String, occurrenceDate: LocalDate) {}

    // ---- Workouts ---------------------------------------------------------

    suspend fun logWorkout(workout: WorkoutLog, date: LocalDate)

    suspend fun deleteWorkout(id: String)

    /** Retrieves the most recent workout for [exerciseName], with its logged sets. */
    suspend fun getLastWorkoutForExercise(exerciseName: String): WorkoutLog? = null

    // ---- Weight -----------------------------------------------------------

    fun observeWeightHistory(): Flow<List<Pair<LocalDate, Double>>>

    suspend fun logWeight(date: LocalDate, weightKg: Double)

    // ---- Fasting ----------------------------------------------------------

    fun observeActiveFast(): Flow<ActiveFast?>

    /** Completed and running sessions, newest first. */
    fun observeFastHistory(limit: Int = 200): Flow<List<FastingSession>> = flowOf(emptyList())

    suspend fun startFast(targetHours: Int, startedAt: java.time.Instant = java.time.Instant.now())

    /** Adds a completed fast directly, for backdating a missed session. */
    suspend fun addFastingSession(
        targetHours: Int,
        startedAt: java.time.Instant,
        endedAt: java.time.Instant,
        note: String = "",
    ) {}

    /** Corrects a completed fast's window, target or note. */
    suspend fun updateFastingSession(
        id: String,
        targetHours: Int,
        startedAt: java.time.Instant,
        endedAt: java.time.Instant,
        note: String = "",
    ) {}

    /** Undo path: restores a deleted completed session with its original id. */
    suspend fun restoreFastingSession(session: FastingSession) {}

    suspend fun endFast(endedAt: java.time.Instant = java.time.Instant.now())

    /** Deletes a completed session; a running fast must be ended first. */
    suspend fun deleteFastingSession(id: String) {}

    // ---- Vitals -----------------------------------------------------------

    fun observeAllVitals(): Flow<List<VitalsEntry>>

    fun observeVitalsForDate(date: LocalDate): Flow<List<VitalsEntry>>

    suspend fun logVitals(entry: VitalsEntry)

    suspend fun deleteVitals(id: String)

    suspend fun getVitalsInRange(from: LocalDate, toInclusive: LocalDate): List<VitalsEntry>

    // ---- Doctor's Consultations -------------------------------------------

    fun observeAllConsultations(): Flow<List<DoctorConsultation>>

    fun observeConsultationById(id: String): Flow<DoctorConsultation?>

    suspend fun logConsultation(consultation: DoctorConsultation)

    suspend fun deleteConsultation(id: String)

    suspend fun getConsultationsInRange(from: LocalDate, toInclusive: LocalDate): List<DoctorConsultation>

    // ---- Medication adherence ---------------------------------------------

    /** Dose log for the adherence window, newest first. */
    fun observeMedicationLogs(from: LocalDate): Flow<List<MedicationLog>> = flowOf(emptyList())

    fun observeAllMedicationLogs(): Flow<List<MedicationLog>> = flowOf(emptyList())

    suspend fun logMedicationTaken(log: MedicationLog) {}

    suspend fun deleteMedicationLog(id: String) {}

    suspend fun getMedicationLogsInRange(from: LocalDate, toInclusive: LocalDate): List<MedicationLog> =
        emptyList()

    suspend fun deleteMedicationLogsForReminder(medicationId: String) {}
}

/** Non-nutrition profile settings that Task 4's domain model deliberately excludes. */
data class ProfileSettings(
    val activeProtocolId: String,
    val targetWaterMl: Int,
    val onboardingCompleted: Boolean,
)

/** One day's aggregate totals, with the day attached. */
data class DayTotals(val date: LocalDate, val totals: NutrientTotals)

/**
 * A running fast.
 *
 * [startedAt] is absolute and [elapsedMinutes] is derived on read, never accumulated in
 * memory — so a fast survives process death and a reboot without drifting.
 */
data class ActiveFast(
    val id: String,
    val startedAt: java.time.Instant,
    val targetHours: Int,
    val elapsedMinutes: Long,
    val progress: Float,
) {
    companion object {
        fun of(
            id: String,
            startedAt: java.time.Instant,
            targetHours: Int,
            now: java.time.Instant,
        ): ActiveFast {
            val elapsed = java.time.Duration.between(startedAt, now).toMinutes().coerceAtLeast(0)
            val targetMinutes = targetHours * 60L
            return ActiveFast(
                id = id,
                startedAt = startedAt,
                targetHours = targetHours,
                elapsedMinutes = elapsed,
                progress = if (targetMinutes <= 0) 0f else (elapsed.toFloat() / targetMinutes).coerceIn(0f, 1f),
            )
        }
    }
}