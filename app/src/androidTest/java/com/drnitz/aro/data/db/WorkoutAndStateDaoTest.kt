package com.drnitz.aro.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drnitz.aro.data.db.mapper.toDomain
import com.drnitz.aro.domain.model.WorkoutType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class WorkoutDaoTest : DatabaseTestBase() {

    private val dao get() = db.workoutDao()

    private fun workout(
        id: String,
        date: LocalDate,
        type: String = WorkoutType.STRENGTH.name,
        loggedAt: Long = 1_700_000_000_000,
    ) = com.drnitz.aro.data.db.entity.WorkoutEntity(
        id = id,
        epochDay = date.toEpochDay(),
        type = type,
        exerciseName = "Bench Press",
        category = "Gym & Strength",
        met = 6.0,
        durationMinutes = 45,
        bodyWeightKg = 80.0,
        muscle = "Chest",
        equipment = "Barbell",
        loggedAt = loggedAt,
        createdAt = loggedAt,
    )

    private fun set(id: String, index: Int, weight: Double, reps: Int) =
        com.drnitz.aro.data.db.entity.WorkoutSetEntity(
            workoutId = id,
            setIndex = index,
            weightKg = weight,
            reps = reps,
        )

    @Test
    fun aSessionSavesWithItsSetsInOneTransaction() = runBlocking {
        dao.insertWorkoutWithSets(
            workout("w1", today),
            listOf(set("w1", 1, 100.0, 5), set("w1", 2, 100.0, 5), set("w1", 3, 90.0, 8)),
        )

        val stored = dao.findWorkout("w1")!!
        assertEquals(3, stored.sets.size)
        assertEquals(listOf(1, 2, 3), stored.sets.map { it.setIndex }.sorted())
        assertEquals(1720.0, stored.sets.sumOf { it.weightKg * it.reps }, 0.0001)
    }

    @Test
    fun deletingASessionCascadesToItsSets() = runBlocking {
        dao.insertWorkoutWithSets(workout("w1", today), listOf(set("w1", 1, 100.0, 5)))
        assertEquals(1, dao.countSetsForWorkout("w1"))

        assertEquals(1, dao.deleteWorkout("w1"))

        assertNull(dao.findWorkout("w1"))
        assertEquals("orphan sets must not survive their workout", 0, dao.countSetsForWorkout("w1"))
    }

    @Test
    fun workoutsAreIsolatedByDay() = runBlocking {
        dao.insertWorkoutWithSets(workout("t1", today, loggedAt = 2), listOf(set("t1", 1, 100.0, 5)))
        dao.insertWorkoutWithSets(workout("y1", yesterday, loggedAt = 1), listOf(set("y1", 1, 80.0, 8)))

        assertEquals(1, dao.workoutsForDay(today.toEpochDay()).size)
        assertEquals(1, dao.workoutsForDay(yesterday.toEpochDay()).size)
        assertEquals("t1", dao.workoutsForDay(today.toEpochDay()).single().workout.id)
    }

    @Test
    fun relationReadsBringBackSetsWithTheWorkout() = runBlocking {
        dao.insertWorkoutWithSets(
            workout("w1", today),
            listOf(set("w1", 1, 100.0, 5), set("w1", 2, 95.0, 6)),
        )

        val observed = dao.observeWorkoutsForDay(today.toEpochDay()).first().single()
        assertEquals("w1", observed.workout.id)
        assertEquals(2, observed.sets.size)
    }

    @Test
    fun reSavingASessionReplacesItsSetsRatherThanAppending() = runBlocking {
        dao.insertWorkoutWithSets(workout("w1", today), listOf(set("w1", 1, 100.0, 5)))
        dao.insertWorkoutWithSets(
            workout("w1", today),
            listOf(set("w1", 1, 110.0, 3), set("w1", 2, 110.0, 3)),
        )

        assertEquals(2, dao.countSetsForWorkout("w1"))
        assertEquals(220.0, dao.findWorkout("w1")!!.sets.sumOf { it.weightKg }, 0.0001)
    }

    @Test
    fun cardioSessionsHaveNoSets() = runBlocking {
        dao.insertWorkoutWithSets(workout("w1", today, type = WorkoutType.CARDIO.name), emptyList())
        assertTrue(dao.findWorkout("w1")!!.sets.isEmpty())
    }

    @Test
    fun aRangeQuerySpansDays() = runBlocking {
        dao.insertWorkoutWithSets(workout("d1", dayBeforeYesterday), emptyList())
        dao.insertWorkoutWithSets(workout("y1", yesterday), emptyList())
        dao.insertWorkoutWithSets(workout("t1", today), emptyList())

        val between = dao.observeWorkoutsBetween(yesterday.toEpochDay(), today.toEpochDay()).first()
        assertEquals(2, between.size)
    }
}

@RunWith(AndroidJUnit4::class)
class FastingDaoTest : DatabaseTestBase() {

    private val dao get() = db.fastingDao()

    private fun session(
        id: String,
        date: LocalDate,
        startedAt: Long,
        targetHours: Int = 16,
        endedAt: Long? = null,
    ) = com.drnitz.aro.data.db.entity.FastingSessionEntity(
        id = id,
        epochDay = date.toEpochDay(),
        targetHours = targetHours,
        startedAt = startedAt,
        endedAt = endedAt,
        createdAt = startedAt,
    )

    @Test
    fun anActiveSessionIsDiscoverableAfterRestart() = runBlocking {
        dao.upsertSession(session("f1", today, startedAt = 1_700_000_000_000))

        val active = dao.activeSession()
        assertNotNull("a fast must survive process death", active)
        assertTrue(active!!.isActive)
        assertEquals(16, active.targetHours)
    }

    @Test
    fun onlyTheLatestOpenSessionIsActive() = runBlocking {
        dao.upsertSession(session("old", yesterday, startedAt = 1, targetHours = 12))
        dao.upsertSession(session("new", today, startedAt = 2, targetHours = 18))

        assertEquals("new", dao.activeSession()!!.id)
    }

    @Test
    fun startingAFastClosesAnyOpenOne() = runBlocking {
        dao.upsertSession(session("old", yesterday, startedAt = 1))
        dao.startSession(session("new", today, startedAt = 2))

        val sessions = dao.observeRecentSessions(10).first()
        assertEquals(2, sessions.size)
        assertFalse(sessions.first { it.id == "old" }.isActive)
        assertTrue(sessions.first { it.id == "new" }.isActive)
    }

    @Test
    fun endingAFastStampsTheEndAndClearsTheActiveSlot() = runBlocking {
        dao.upsertSession(session("f1", today, startedAt = 1))
        assertEquals(1, dao.endSession("f1", endedAt = 99))

        val active = dao.activeSession()
        assertNull(active)
        assertFalse(dao.observeRecentSessions(10).first().single().isActive)
    }

    @Test
    fun sessionsAreListedPerDay() = runBlocking {
        dao.upsertSession(session("t1", today, startedAt = 5))
        dao.upsertSession(session("y1", yesterday, startedAt = 1, endedAt = 9))

        assertEquals(1, dao.observeSessionsForDay(today.toEpochDay()).first().size)
        assertEquals(1, dao.observeSessionsForDay(yesterday.toEpochDay()).first().size)
        assertEquals("t1", dao.observeSessionsForDay(today.toEpochDay()).first().single().id)
    }
}

@RunWith(AndroidJUnit4::class)
class WeightDaoTest : DatabaseTestBase() {

    private val dao get() = db.weightDao()

    private fun entry(id: String, date: LocalDate, weight: Double) =
        com.drnitz.aro.data.db.entity.WeightEntryEntity(
            id = id,
            epochDay = date.toEpochDay(),
            weightKg = weight,
            createdAt = 0,
        )

    @Test
    fun historyIsNewestFirst() = runBlocking {
        dao.upsert(entry("w1", dayBeforeYesterday, 76.0))
        dao.upsert(entry("w2", yesterday, 75.4))
        dao.upsert(entry("w3", today, 75.0))

        val history = dao.observeAll().first()
        assertEquals(listOf(75.0, 75.4, 76.0), history.map { it.weightKg })
    }

    @Test
    fun reweighingTheSameDayReplacesRatherThanDuplicates() = runBlocking {
        dao.upsert(entry("w1", today, 76.0))
        dao.recordWeight(entry("w2", today, 74.8))

        val history = dao.observeAll().first()
        assertEquals(1, history.size)
        assertEquals(74.8, history.single().weightKg, 0.0001)
    }

    @Test
    fun latestIsTheMostRecentDay() = runBlocking {
        dao.upsert(entry("w1", yesterday, 76.0))
        dao.upsert(entry("w2", today, 75.0))

        assertEquals(75.0, dao.latest()!!.weightKg, 0.0001)
        assertEquals(today, dao.observeLatest().first()!!.toDomain().first)
    }

    @Test
    fun rangeQueriesAreInclusiveAndIsolated() = runBlocking {
        dao.upsert(entry("d1", dayBeforeYesterday, 77.0))
        dao.upsert(entry("y1", yesterday, 76.0))
        dao.upsert(entry("t1", today, 75.0))

        val window = dao.observeBetween(yesterday.toEpochDay(), today.toEpochDay()).first()
        assertEquals(listOf(76.0, 75.0), window.map { it.weightKg })
    }

    @Test
    fun anEmptyRangeReturnsNothingRatherThanThrowing() = runBlocking {
        dao.upsert(entry("t1", today, 75.0))
        assertTrue(dao.observeBetween(0L, 1L).first().isEmpty())
        assertEquals(1, dao.observeAll().first().size)
    }

    /**
     * Regression guard: `@Upsert` resolves conflicts on the **primary key only**. With a
     * unique index on `epoch_day` (a different column), a plain upsert of a second
     * weigh-in was silently discarded and the chart kept showing the stale weight.
     */
    @Test
    fun aPlainUpsertCannotReplaceARowConflictingOnAUniqueIndex() = runBlocking {
        dao.upsert(entry("w1", today, 76.0))
        dao.upsert(entry("w2", today, 74.8))

        // This documents the trap rather than the behaviour: recordWeight is required.
        assertEquals(1, dao.observeAll().first().size)
        assertEquals(76.0, dao.observeAll().first().single().weightKg, 0.0001)
    }
}