package com.drnitz.aro.data.db.mapper

import com.drnitz.aro.data.db.NutrientColumns
import com.drnitz.aro.data.db.entity.ConsultationEntity
import com.drnitz.aro.data.db.entity.DayEntity
import com.drnitz.aro.data.db.entity.FastingSessionEntity
import com.drnitz.aro.data.db.entity.FavoriteFoodEntity
import com.drnitz.aro.data.db.entity.FoodLogEntity
import com.drnitz.aro.data.db.entity.MedicationLogEntity
import com.drnitz.aro.data.db.entity.ProfileEntity
import com.drnitz.aro.data.db.entity.RecentFoodEntity
import com.drnitz.aro.data.db.entity.VitalsEntryEntity
import com.drnitz.aro.data.db.entity.WaterLogEntity
import com.drnitz.aro.data.db.entity.WeightEntryEntity
import com.drnitz.aro.data.db.entity.WorkoutEntity
import com.drnitz.aro.data.db.entity.WorkoutSetEntity
import com.drnitz.aro.data.db.entity.WorkoutWithSets
import com.drnitz.aro.domain.model.ActivityLevel
import com.drnitz.aro.domain.model.BloodGlucose
import com.drnitz.aro.domain.model.BloodPressure
import com.drnitz.aro.domain.model.CuratedFoodItem
import com.drnitz.aro.domain.model.DiaryDay
import com.drnitz.aro.domain.model.DoctorConsultation
import com.drnitz.aro.domain.model.FastingSession
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.GlucoseMeasurementContext
import com.drnitz.aro.domain.model.Goal
import com.drnitz.aro.domain.model.MacroPreset
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.MedicationLog
import com.drnitz.aro.domain.model.MedicationLogSource
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.Sex
import com.drnitz.aro.domain.model.SetEntry
import com.drnitz.aro.domain.model.UserProfile
import com.drnitz.aro.domain.model.VitalsEntry
import com.drnitz.aro.domain.model.WaterLog
import com.drnitz.aro.domain.model.WaterLogSource
import com.drnitz.aro.domain.model.WorkoutLog
import com.drnitz.aro.domain.model.WorkoutType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Entity ⇄ domain mapping.
 *
 * Every enum round-trips through its **name** and falls back to a safe default, so a row
 * written by a future version (or hand-edited in sqlite3) cannot crash the app. Every
 * nutrient is copied through `NutrientColumns.read`, which preserves `null` — the
 * reported/unreported distinction the clinical audit depends on.
 *
 * This file is pure Kotlin with no Android types, so it is unit tested on the JVM with a
 * round-trip over every [NutrientKey].
 */

// ---- Food ------------------------------------------------------------------

fun FoodLogEntity.toDomain(): FoodLogEntry = FoodLogEntry(
    id = id,
    name = name,
    meal = mealSlot,
    grams = grams,
    nutrients = toNutrientTotals(),
    brand = brand,
    foodId = foodId,
    isCustom = isCustom,
)

fun FoodLogEntry.toEntity(epochDay: Long, createdAt: Long): FoodLogEntity {
    val n = nutrients
    fun v(key: NutrientKey): Double? = NutrientColumns.valueOf(n, key)
    return FoodLogEntity(
        id = id,
        epochDay = epochDay,
        mealSlot = meal,
        name = name,
        brand = brand,
        foodId = foodId,
        grams = grams,
        isCustom = isCustom,
        createdAt = createdAt,
        calories = v(NutrientKey.CALORIES),
        protein = v(NutrientKey.PROTEIN),
        carbs = v(NutrientKey.CARBS),
        fat = v(NutrientKey.FAT),
        saturatedFat = v(NutrientKey.SATURATED_FAT),
        fiber = v(NutrientKey.FIBER),
        sugar = v(NutrientKey.SUGAR),
        sodium = v(NutrientKey.SODIUM),
        potassium = v(NutrientKey.POTASSIUM),
        calcium = v(NutrientKey.CALCIUM),
        magnesium = v(NutrientKey.MAGNESIUM),
    )
}

// ---- Saved food snapshots --------------------------------------------------

/**
 * Favourite and recent rows store nutrients **already scaled to [FavoriteFoodEntity.grams]**.
 * Recovering a per-100 g library entry therefore means scaling back, so a saved 30 g
 * serving still previews correctly when the portion dialog reopens at 100 g.
 */
private fun snapshotToCuratedFood(
    foodId: String,
    name: String,
    brand: String?,
    grams: Double,
    columns: NutrientColumns,
): CuratedFoodItem {
    val factor = if (grams > 0.0) 100.0 / grams else 1.0
    fun valueOf(key: NutrientKey): Double = (columns.read(key) ?: 0.0) * factor

    return CuratedFoodItem(
        id = foodId,
        name = name,
        brand = brand,
        category = "Saved",
        calories = valueOf(NutrientKey.CALORIES),
        protein = valueOf(NutrientKey.PROTEIN),
        carbs = valueOf(NutrientKey.CARBS),
        fat = valueOf(NutrientKey.FAT),
        saturatedFat = valueOf(NutrientKey.SATURATED_FAT),
        fiber = valueOf(NutrientKey.FIBER),
        sugar = valueOf(NutrientKey.SUGAR),
        sodium = valueOf(NutrientKey.SODIUM),
        potassium = valueOf(NutrientKey.POTASSIUM),
        calcium = valueOf(NutrientKey.CALCIUM),
        magnesium = valueOf(NutrientKey.MAGNESIUM),
        servingSize = 100.0,
    )
}

fun FavoriteFoodEntity.toCuratedFoodItem(): CuratedFoodItem =
    snapshotToCuratedFood(foodId, name, brand, grams, this)

fun RecentFoodEntity.toCuratedFoodItem(): CuratedFoodItem =
    snapshotToCuratedFood(foodId, name, brand, grams, this)

/** Snapshots the food at its common serving so the star keeps meaning after an offline edit. */
fun CuratedFoodItem.toFavoriteEntity(
    id: String = UUID.randomUUID().toString(),
    createdAt: Long = System.currentTimeMillis(),
): FavoriteFoodEntity {
    val grams = commonServing?.grams ?: servingSize
    val scaled = per100gNutrients.scaledToGrams(grams)

    fun valueOf(key: NutrientKey): Double? = NutrientColumns.valueOf(scaled, key)
    return FavoriteFoodEntity(
        id = id,
        foodId = this.id,
        name = name,
        brand = brand,
        grams = grams,
        createdAt = createdAt,
        calories = valueOf(NutrientKey.CALORIES),
        protein = valueOf(NutrientKey.PROTEIN),
        carbs = valueOf(NutrientKey.CARBS),
        fat = valueOf(NutrientKey.FAT),
        saturatedFat = valueOf(NutrientKey.SATURATED_FAT),
        fiber = valueOf(NutrientKey.FIBER),
        sugar = valueOf(NutrientKey.SUGAR),
        sodium = valueOf(NutrientKey.SODIUM),
        potassium = valueOf(NutrientKey.POTASSIUM),
        calcium = valueOf(NutrientKey.CALCIUM),
        magnesium = valueOf(NutrientKey.MAGNESIUM),
    )
}

fun CuratedFoodItem.toRecentEntity(
    grams: Double,
    lastUsedAt: Long = System.currentTimeMillis(),
    id: String = UUID.randomUUID().toString(),
): RecentFoodEntity {
    val servingGrams = grams.coerceAtLeast(1.0)
    val scaled = per100gNutrients.scaledToGrams(servingGrams)

    fun valueOf(key: NutrientKey): Double? = NutrientColumns.valueOf(scaled, key)
    return RecentFoodEntity(
        id = id,
        foodId = this.id,
        name = name,
        brand = brand,
        grams = servingGrams,
        lastUsedAt = lastUsedAt,
        calories = valueOf(NutrientKey.CALORIES),
        protein = valueOf(NutrientKey.PROTEIN),
        carbs = valueOf(NutrientKey.CARBS),
        fat = valueOf(NutrientKey.FAT),
        saturatedFat = valueOf(NutrientKey.SATURATED_FAT),
        fiber = valueOf(NutrientKey.FIBER),
        sugar = valueOf(NutrientKey.SUGAR),
        sodium = valueOf(NutrientKey.SODIUM),
        potassium = valueOf(NutrientKey.POTASSIUM),
        calcium = valueOf(NutrientKey.CALCIUM),
        magnesium = valueOf(NutrientKey.MAGNESIUM),
    )
}

// ---- Diary -----------------------------------------------------------------

/**
 * Assembles a whole day from already-fetched rows.
 *
 * Kept as a pure function so day assembly is unit tested without a database, and so the
 * ordering guarantee (meals always render breakfast→snacks, regardless of insertion
 * order) lives in one place.
 */
fun buildDiaryDay(
    date: LocalDate,
    foods: List<FoodLogEntity>,
    workouts: List<WorkoutWithSets>,
    day: DayEntity?,
): DiaryDay = DiaryDay(
    date = date,
    meals = MealSlot.ORDERED.associateWith { slot -> foods.filter { it.mealSlot == slot }.map { it.toDomain() } },
    workouts = workouts.map { it.toDomain() },
    waterMl = day?.waterMl ?: 0,
    notes = day?.notes.orEmpty(),
)

// ---- Intake logs -----------------------------------------------------------

fun WaterLogEntity.toDomain(): WaterLog = WaterLog(
    id = id,
    date = LocalDate.ofEpochDay(epochDay),
    amountMl = amountMl,
    loggedAt = Instant.ofEpochMilli(loggedAt),
    source = WaterLogSource.fromStorage(source),
)

fun FastingSessionEntity.toDomain(): FastingSession = FastingSession(
    id = id,
    date = LocalDate.ofEpochDay(epochDay),
    targetHours = targetHours,
    startedAt = Instant.ofEpochMilli(startedAt),
    endedAt = endedAt?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(createdAt),
    note = note.orEmpty(),
)

// ---- Workouts --------------------------------------------------------------

fun WorkoutSetEntity.toDomain(): SetEntry = SetEntry(weightKg = weightKg, reps = reps)

fun SetEntry.toEntity(workoutId: String, setIndex: Int): WorkoutSetEntity = WorkoutSetEntity(
    workoutId = workoutId,
    setIndex = setIndex,
    weightKg = weightKg,
    reps = reps,
)

fun WorkoutWithSets.toDomain(): WorkoutLog = WorkoutLog(
    id = workout.id,
    type = enumOrDefault(workout.type, WorkoutType.CARDIO),
    exerciseName = workout.exerciseName,
    loggedAt = Instant.ofEpochMilli(workout.loggedAt),
    met = workout.met,
    durationMinutes = workout.durationMinutes,
    distanceKm = workout.distanceKm,
    averageSpeedKph = workout.averageSpeedKph,
    steps = workout.steps,
    bodyWeightKg = workout.bodyWeightKg,
    sets = sets.sortedBy { it.setIndex }.map { it.toDomain() },
    category = workout.category,
    muscle = workout.muscle,
    equipment = workout.equipment,
)

fun WorkoutLog.toEntity(epochDay: Long, createdAt: Long = loggedAt.toEpochMilli()): WorkoutEntity =
    WorkoutEntity(
        id = id,
        epochDay = epochDay,
        type = type.name,
        exerciseName = exerciseName,
        category = category,
        met = met,
        durationMinutes = durationMinutes,
        distanceKm = distanceKm,
        averageSpeedKph = averageSpeedKph,
        steps = steps,
        bodyWeightKg = bodyWeightKg,
        muscle = muscle,
        equipment = equipment,
        loggedAt = loggedAt.toEpochMilli(),
        createdAt = createdAt,
    )

// ---- Profile ---------------------------------------------------------------

fun ProfileEntity.toDomain(): UserProfile = UserProfile(
    name = name,
    sex = enumOrDefault(sex, Sex.MALE),
    ageYears = ageYears,
    weightKg = weightKg,
    targetWeightKg = targetWeightKg,
    heightCm = heightCm,
    activityLevel = enumOrDefault(activityLevel, ActivityLevel.MODERATE),
    goal = enumOrDefault(goal, Goal.MILD_CUT),
    macroPreset = enumOrDefault(macroPreset, MacroPreset.HIGH_PROTEIN),
)

fun UserProfile.toEntity(
    activeProtocolId: String = "aha_standard",
    targetWaterMl: Int = 3000,
    onboardingCompleted: Boolean = true,
    createdAt: Long = System.currentTimeMillis(),
    updatedAt: Long = System.currentTimeMillis(),
): ProfileEntity = ProfileEntity(
    id = ProfileEntity.SINGLETON_ID,
    name = name,
    sex = sex.name,
    ageYears = ageYears,
    weightKg = weightKg,
    targetWeightKg = targetWeightKg,
    heightCm = heightCm,
    activityLevel = activityLevel.name,
    goal = goal.name,
    macroPreset = macroPreset.name,
    activeProtocolId = activeProtocolId,
    targetWaterMl = targetWaterMl,
    onboardingCompleted = onboardingCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// ---- Weight ----------------------------------------------------------------

fun WeightEntryEntity.toDomain(): Pair<LocalDate, Double> =
    LocalDate.ofEpochDay(epochDay) to weightKg

fun logWeight(date: LocalDate, weightKg: Double, id: String, createdAt: Long): WeightEntryEntity =
    WeightEntryEntity(
        id = id,
        epochDay = date.toEpochDay(),
        weightKg = weightKg,
        createdAt = createdAt,
    )

// ---- Vitals ----------------------------------------------------------------

fun VitalsEntryEntity.toDomain(): VitalsEntry {
    val bp = if (systolic != null && diastolic != null) {
        try {
            BloodPressure(systolic = systolic, diastolic = diastolic, pulse = pulse)
        } catch (_: Exception) {
            null
        }
    } else null

    val bg = if (glucoseMgDl != null) {
        val ctx = GlucoseMeasurementContext.entries.find { it.name == glucoseContext }
            ?: GlucoseMeasurementContext.FASTING
        try {
            BloodGlucose(valueMgDl = glucoseMgDl, context = ctx)
        } catch (_: Exception) {
            null
        }
    } else null

    return VitalsEntry(
        id = id,
        date = LocalDate.ofEpochDay(epochDay),
        timestamp = Instant.ofEpochMilli(timestamp),
        bloodPressure = bp,
        bloodGlucose = bg,
        ketonesMmolL = ketonesMmolL,
        bodyTemperatureC = bodyTemperatureC,
        spo2Percent = spo2Percent,
        notes = notes,
    )
}

fun VitalsEntry.toEntity(): VitalsEntryEntity = VitalsEntryEntity(
    id = id,
    epochDay = date.toEpochDay(),
    timestamp = timestamp.toEpochMilli(),
    systolic = bloodPressure?.systolic,
    diastolic = bloodPressure?.diastolic,
    pulse = bloodPressure?.pulse,
    glucoseMgDl = bloodGlucose?.valueMgDl,
    glucoseContext = bloodGlucose?.context?.name,
    ketonesMmolL = ketonesMmolL,
    bodyTemperatureC = bodyTemperatureC,
    spo2Percent = spo2Percent,
    notes = notes,
)

// ---- Consultations ---------------------------------------------------------
fun ConsultationEntity.toDomain(): DoctorConsultation = DoctorConsultation(
    id = id,
    date = LocalDate.ofEpochDay(epochDay),
    doctorName = doctorName,
    specialty = specialty,
    reasonForVisit = reasonForVisit,
    diagnosisNotes = diagnosisNotes,
    prescribedMedications = prescribedMedications,
    recommendedTests = recommendedTests,
    followUpPlan = followUpPlan,
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun DoctorConsultation.toEntity(): ConsultationEntity = ConsultationEntity(
    id = id,
    epochDay = date.toEpochDay(),
    doctorName = doctorName,
    specialty = specialty,
    reasonForVisit = reasonForVisit,
    diagnosisNotes = diagnosisNotes,
    prescribedMedications = prescribedMedications,
    recommendedTests = recommendedTests,
    followUpPlan = followUpPlan,
    createdAt = createdAt.toEpochMilli(),
)

// ---- Medication adherence --------------------------------------------------

fun MedicationLogEntity.toDomain(): MedicationLog = MedicationLog(
    id = id,
    medicationId = medicationId,
    medicationName = medicationName,
    dosage = dosage,
    date = LocalDate.ofEpochDay(epochDay),
    scheduledHour = scheduledHour,
    scheduledMinute = scheduledMinute,
    takenAt = Instant.ofEpochMilli(takenAt),
    source = enumOrDefault(source, MedicationLogSource.MANUAL),
)

fun MedicationLog.toEntity(): MedicationLogEntity = MedicationLogEntity(
    id = id,
    medicationId = medicationId,
    medicationName = medicationName,
    dosage = dosage,
    epochDay = date.toEpochDay(),
    scheduledHour = scheduledHour,
    scheduledMinute = scheduledMinute,
    takenAt = takenAt.toEpochMilli(),
    source = source.name,
)

// ---- Helpers ---------------------------------------------------------------

private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default