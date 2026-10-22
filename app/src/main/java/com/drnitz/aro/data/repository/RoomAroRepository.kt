package com.drnitz.aro.data.repository

import com.drnitz.aro.data.db.AroDatabase
import com.drnitz.aro.data.db.DateProvider
import com.drnitz.aro.data.db.SystemDateProvider
import com.drnitz.aro.data.db.entity.DayEntity
import com.drnitz.aro.data.db.entity.FastingSessionEntity
import com.drnitz.aro.data.db.entity.PlanCompletionEntity
import com.drnitz.aro.data.db.mapper.buildDiaryDay
import com.drnitz.aro.data.db.mapper.logWeight
import com.drnitz.aro.data.db.mapper.planOccurrencesForDay
import com.drnitz.aro.data.db.mapper.planOccurrencesInRange
import com.drnitz.aro.data.db.mapper.toCuratedFoodItem
import com.drnitz.aro.data.db.mapper.toDomain
import com.drnitz.aro.data.db.mapper.toEntity
import com.drnitz.aro.data.db.mapper.toFavoriteEntity
import com.drnitz.aro.data.db.mapper.toRecentEntity
import com.drnitz.aro.domain.model.CuratedFoodItem
import com.drnitz.aro.domain.model.DiaryDay
import com.drnitz.aro.domain.model.DoctorConsultation
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.PlanItem
import com.drnitz.aro.domain.model.PlanOccurrence
import com.drnitz.aro.domain.model.UserProfile
import com.drnitz.aro.domain.model.VitalsEntry
import com.drnitz.aro.domain.model.WorkoutLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Room-backed [AroRepository].
 *
 * Two patterns are worth calling out:
 *
 *  - **Day assembly is a `combine` of three streams, not three queries per render.**
 *    `observeDiaryDay` merges food, workouts and day-state into one flow, so the diary
 *    recomposes once per change rather than three times.
 *  - **Every day is passed in, never inferred.** This is what keeps yesterday and today
 *    isolated: there is no code path where a query can quietly target "today" when the
 *    caller meant yesterday.
 */
class RoomAroRepository(
    private val database: AroDatabase,
    private val dateProvider: DateProvider = SystemDateProvider(),
) : AroRepository {

    private val diaryDao get() = database.diaryDao()
    private val workoutDao get() = database.workoutDao()
    private val weightDao get() = database.weightDao()
    private val fastingDao get() = database.fastingDao()
    private val profileDao get() = database.profileDao()
    private val vitalsDao get() = database.vitalsDao()
    private val consultationDao get() = database.consultationDao()
    private val foodLibraryDao get() = database.foodLibraryDao()
    private val planDao get() = database.planDao()

    // ---- Profile ----------------------------------------------------------

    override fun observeProfile(): Flow<UserProfile?> =
        profileDao.observeProfile().map { it?.toDomain() }

    override suspend fun saveProfile(profile: UserProfile) {
        val existing = profileDao.getProfile()
        profileDao.upsertProfile(
            profile.toEntity(
                activeProtocolId = existing?.activeProtocolId ?: DEFAULT_PROTOCOL_ID,
                targetWaterMl = existing?.targetWaterMl ?: DEFAULT_WATER_ML,
                onboardingCompleted = existing?.onboardingCompleted ?: true,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    override fun observeProfileSettings(): Flow<ProfileSettings?> =
        profileDao.observeProfile().map { row ->
            row?.let {
                ProfileSettings(
                    activeProtocolId = it.activeProtocolId,
                    targetWaterMl = it.targetWaterMl,
                    onboardingCompleted = it.onboardingCompleted,
                )
            }
        }

    override suspend fun saveProfileSettings(settings: ProfileSettings) {
        val row = profileDao.getProfile() ?: return
        profileDao.upsertProfile(
            row.copy(
                activeProtocolId = settings.activeProtocolId,
                targetWaterMl = settings.targetWaterMl,
                onboardingCompleted = settings.onboardingCompleted,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    // ---- Diary ------------------------------------------------------------

    override fun observeDiaryDay(date: LocalDate): Flow<DiaryDay> {
        val epochDay = date.toEpochDay()
        return combine(
            diaryDao.observeFoodsForDay(epochDay),
            workoutDao.observeWorkoutsForDay(epochDay),
            diaryDao.observeDay(epochDay),
        ) { foods, workouts, day ->
            buildDiaryDay(date, foods, workouts, day)
        }.distinctUntilChanged()
    }

    override fun observeDayTotals(date: LocalDate): Flow<NutrientTotals> =
        diaryDao.observeDayTotals(date.toEpochDay()).map { it.toNutrientTotals() }

    override suspend fun addFood(entry: FoodLogEntry, date: LocalDate) {
        diaryDao.upsertFood(entry.toEntity(date.toEpochDay(), System.currentTimeMillis()))
        ensureDayRow(date)
    }

    override suspend fun updateFood(entry: FoodLogEntry, date: LocalDate) {
        diaryDao.upsertFood(entry.toEntity(date.toEpochDay(), System.currentTimeMillis()))
    }

    override suspend fun deleteFood(id: String) {
        diaryDao.deleteFood(id)
    }

    override suspend fun moveFood(id: String, toMeal: MealSlot, toDate: LocalDate) {
        diaryDao.moveFood(id, toDate.toEpochDay(), toMeal)
        ensureDayRow(toDate)
    }

    override suspend fun addWater(date: LocalDate, deltaMl: Int) {
        val epochDay = date.toEpochDay()
        val existing = diaryDao.getDay(epochDay)
        val current = existing?.waterMl ?: 0
        setWater(date, (current + deltaMl).coerceAtLeast(0))
    }

    override suspend fun setWater(date: LocalDate, waterMl: Int) {
        val epochDay = date.toEpochDay()
        val existing = diaryDao.getDay(epochDay)
        diaryDao.upsertDay(
            (existing ?: newDayRow(epochDay)).copy(
                waterMl = waterMl.coerceAtLeast(0),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun setNotes(date: LocalDate, notes: String) {
        val epochDay = date.toEpochDay()
        val existing = diaryDao.getDay(epochDay)
        diaryDao.upsertDay(
            (existing ?: newDayRow(epochDay)).copy(
                notes = notes,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    override fun observeRecentLoggedDays(limit: Int): Flow<List<LocalDate>> =
        diaryDao.observeRecentDays(limit).map { days -> days.map { LocalDate.ofEpochDay(it) } }

    override fun observeTotalsInRange(from: LocalDate, toInclusive: LocalDate): Flow<List<DayTotals>> =
        diaryDao.observeTotalsBetween(from.toEpochDay(), toInclusive.toEpochDay()).map { rows ->
            rows.map { DayTotals(it.date, it.toNutrientTotals()) }
        }

    // ---- Saved foods ------------------------------------------------------

    override fun observeFavoriteFoods(): Flow<List<CuratedFoodItem>> =
        foodLibraryDao.observeFavorites().map { rows -> rows.map { it.toCuratedFoodItem() } }

    override fun observeRecentFoods(): Flow<List<CuratedFoodItem>> =
        foodLibraryDao.observeRecents().map { rows -> rows.map { it.toCuratedFoodItem() } }

    override suspend fun toggleFavoriteFood(food: CuratedFoodItem) {
        if (foodLibraryDao.isFavorite(food.id)) {
            foodLibraryDao.deleteFavorite(food.id)
        } else {
            foodLibraryDao.starFavorite(food.toFavoriteEntity())
        }
    }

    override suspend fun recordFoodUsage(food: CuratedFoodItem, grams: Double) {
        foodLibraryDao.recordRecentUsage(food.toRecentEntity(grams))
    }

    // ---- Future-day plans -------------------------------------------------

    override fun observePlanOccurrences(date: LocalDate): Flow<List<PlanOccurrence>> {
        val epochDay = date.toEpochDay()
        return combine(
            planDao.observeSeriesStartingBy(epochDay),
            planDao.observeCompletionsForDay(epochDay),
        ) { series, completions ->
            planOccurrencesForDay(series, completions, date)
        }.distinctUntilChanged()
    }

    override fun observePlanOccurrencesInRange(
        from: LocalDate,
        toInclusive: LocalDate,
    ): Flow<List<PlanOccurrence>> {
        if (toInclusive.isBefore(from)) return kotlinx.coroutines.flow.flowOf(emptyList())
        return combine(
            planDao.observeSeriesStartingBy(toInclusive.toEpochDay()),
            planDao.observeCompletionsInRange(from.toEpochDay(), toInclusive.toEpochDay()),
        ) { series, completions ->
            planOccurrencesInRange(series, completions, from, toInclusive)
        }.distinctUntilChanged()
    }

    override suspend fun savePlanItem(item: PlanItem) {
        planDao.upsertSeries(item.toEntity())
    }

    override fun observePlanItems(): Flow<List<PlanItem>> =
        planDao.observeAllSeries().map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun getAllPlanItems(): List<PlanItem> =
        planDao.getAllSeries().mapNotNull { it.toDomain() }

    override suspend fun getPlanItem(id: String): PlanItem? =
        planDao.getSeries(id)?.toDomain()

    override suspend fun getPlanCompletionDates(
        planId: String,
        from: LocalDate,
        toInclusive: LocalDate,
    ): Set<LocalDate> {
        if (toInclusive.isBefore(from)) return emptySet()
        return planDao.getCompletionDays(
            planId = planId,
            fromEpochDay = from.toEpochDay(),
            toEpochDay = toInclusive.toEpochDay(),
        ).mapTo(mutableSetOf()) { LocalDate.ofEpochDay(it) }
    }

    override suspend fun isPlanOccurrenceCompleted(
        planId: String,
        occurrenceDate: LocalDate,
    ): Boolean = planDao.getCompletion(planId, occurrenceDate.toEpochDay()) != null

    override suspend fun deletePlanItem(id: String) {
        planDao.deleteSeries(id)
    }

    override suspend fun completePlanOccurrence(
        planId: String,
        occurrenceDate: LocalDate,
        actualLogId: String?,
    ) {
        planDao.upsertCompletion(
            PlanCompletionEntity(
                planId = planId,
                occurrenceEpochDay = occurrenceDate.toEpochDay(),
                completedAt = System.currentTimeMillis(),
                actualLogId = actualLogId,
            )
        )
    }

    override suspend fun skipPlanOccurrence(planId: String, occurrenceDate: LocalDate) {
        val now = System.currentTimeMillis()
        planDao.upsertCompletion(
            PlanCompletionEntity(
                planId = planId,
                occurrenceEpochDay = occurrenceDate.toEpochDay(),
                completedAt = now,
                actualLogId = null,
                skippedAt = now,
            )
        )
    }

    override suspend fun uncompletePlanOccurrence(planId: String, occurrenceDate: LocalDate) {
        planDao.deleteCompletion(planId, occurrenceDate.toEpochDay())
    }

    // ---- Workouts ---------------------------------------------------------

    override suspend fun logWorkout(workout: WorkoutLog, date: LocalDate) {
        workoutDao.insertWorkoutWithSets(
            workout = workout.toEntity(date.toEpochDay()),
            sets = workout.sets.mapIndexed { index, set -> set.toEntity(workout.id, index + 1) },
        )
        ensureDayRow(date)
    }

    override suspend fun deleteWorkout(id: String) {
        workoutDao.deleteWorkout(id)
    }

    // ---- Weight -----------------------------------------------------------

    override fun observeWeightHistory(): Flow<List<Pair<LocalDate, Double>>> =
        weightDao.observeAll().map { rows -> rows.map { LocalDate.ofEpochDay(it.epochDay) to it.weightKg } }

    override suspend fun logWeight(date: LocalDate, weightKg: Double) {
        // recordWeight (not upsert) so a second weigh-in on the same day replaces the
        // first instead of being silently dropped by the unique index on epoch_day.
        weightDao.recordWeight(
            logWeight(
                date = date,
                weightKg = weightKg,
                id = UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    // ---- Fasting ----------------------------------------------------------

    override fun observeActiveFast(): Flow<ActiveFast?> =
        fastingDao.observeActiveSession().map { session ->
            session?.let {
                ActiveFast.of(
                    id = it.id,
                    startedAt = Instant.ofEpochMilli(it.startedAt),
                    targetHours = it.targetHours,
                    now = Instant.now(),
                )
            }
        }

    override suspend fun startFast(targetHours: Int, startedAt: Instant) {
        fastingDao.startSession(
            FastingSessionEntity(
                id = UUID.randomUUID().toString(),
                epochDay = dateProvider.dayOf(startedAt).toEpochDay(),
                targetHours = targetHours,
                startedAt = startedAt.toEpochMilli(),
                endedAt = null,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun endFast(endedAt: Instant) {
        val active = fastingDao.activeSession() ?: return
        // A fast spanning midnight belongs to the day it started, which is why the day
        // was captured at start time rather than recomputed here.
        fastingDao.endSession(active.id, endedAt.toEpochMilli())
    }

    // ---- Vitals -----------------------------------------------------------

    override fun observeAllVitals(): Flow<List<VitalsEntry>> =
        vitalsDao.observeAllVitals().map { list -> list.map { it.toDomain() } }

    override fun observeVitalsForDate(date: LocalDate): Flow<List<VitalsEntry>> =
        vitalsDao.observeVitalsForDay(date.toEpochDay()).map { list -> list.map { it.toDomain() } }

    override suspend fun logVitals(entry: VitalsEntry) {
        vitalsDao.insertVitals(entry.toEntity())
    }

    override suspend fun deleteVitals(id: String) {
        vitalsDao.deleteVitals(id)
    }

    override suspend fun getVitalsInRange(from: LocalDate, toInclusive: LocalDate): List<VitalsEntry> =
        vitalsDao.getVitalsInRange(from.toEpochDay(), toInclusive.toEpochDay()).map { it.toDomain() }

    // ---- Doctor's Consultations -------------------------------------------

    override fun observeAllConsultations(): Flow<List<DoctorConsultation>> =
        consultationDao.observeAllConsultations().map { list -> list.map { it.toDomain() } }

    override fun observeConsultationById(id: String): Flow<DoctorConsultation?> =
        consultationDao.observeConsultationById(id).map { it?.toDomain() }

    override suspend fun logConsultation(consultation: DoctorConsultation) {
        consultationDao.insertConsultation(consultation.toEntity())
    }

    override suspend fun deleteConsultation(id: String) {
        consultationDao.deleteConsultation(id)
    }

    override suspend fun getConsultationsInRange(from: LocalDate, toInclusive: LocalDate): List<DoctorConsultation> =
        consultationDao.getConsultationsInRange(from.toEpochDay(), toInclusive.toEpochDay()).map { it.toDomain() }

    // ---- Helpers ----------------------------------------------------------

    private fun newDayRow(epochDay: Long, now: Long = System.currentTimeMillis()) =
        DayEntity(epochDay = epochDay, waterMl = 0, notes = "", createdAt = now, updatedAt = now)

    /** A day row exists once a day has been touched, so "no row" means "nothing logged". */
    private suspend fun ensureDayRow(date: LocalDate) {
        val epochDay = date.toEpochDay()
        if (diaryDao.getDay(epochDay) == null) {
            diaryDao.upsertDay(newDayRow(epochDay))
        }
    }

    private companion object {
        const val DEFAULT_PROTOCOL_ID = "aha_standard"
        const val DEFAULT_WATER_ML = 3000
    }
}

/** Human-readable elapsed time for the fasting timer. */
fun ActiveFast.elapsedText(): String {
    val total = elapsedMinutes
    return "%dh %02dm".format(total / 60, total % 60)
}

/** Time remaining before the target is reached, or zero once it is passed. */
fun ActiveFast.remainingText(): String {
    val remaining = (targetHours * 60 - elapsedMinutes).coerceAtLeast(0)
    return "%dh %02dm left".format(remaining / 60, remaining % 60)
}

/** Rough metabolic stage, for the Task 9 timer's indicator. */
fun ActiveFast.metabolicStage(): String = when {
    elapsedMinutes < 4 * 60 -> "Digestive rest"
    elapsedMinutes < 12 * 60 -> "Insulin drop"
    elapsedMinutes < 16 * 60 -> "Fat oxidation"
    else -> "Ketosis / autophagy"
}