package com.drnitz.aro.data.db

import com.drnitz.aro.data.db.entity.FastingSessionEntity
import com.drnitz.aro.data.db.entity.WaterLogEntity
import com.drnitz.aro.data.db.mapper.toDomain
import com.drnitz.aro.domain.model.WaterLogSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class IntakeLogMapperTest {

    private val date = LocalDate.of(2026, 10, 23)

    @Test
    fun `a water row round-trips into the domain with its day and time`() {
        val log = WaterLogEntity(
            id = "water-1",
            epochDay = date.toEpochDay(),
            amountMl = 250,
            loggedAt = 1_700_000_000_000,
            source = WaterLogSource.MANUAL.name,
        ).toDomain()

        assertEquals("water-1", log.id)
        assertEquals(date, log.date)
        assertEquals(250, log.amountMl)
        assertEquals(Instant.ofEpochMilli(1_700_000_000_000), log.loggedAt)
        assertEquals(WaterLogSource.MANUAL, log.source)
        assertFalse(log.isBackfilled)
    }

    @Test
    fun `an unknown source falls back to manual instead of crashing`() {
        val log = WaterLogEntity(
            id = "water-2",
            epochDay = date.toEpochDay(),
            amountMl = 500,
            loggedAt = 0,
            source = "FROM_A_FUTURE_VERSION",
        ).toDomain()

        assertEquals(WaterLogSource.MANUAL, log.source)
    }

    @Test
    fun `a backfilled entry is marked so the UI can explain it`() {
        val log = WaterLogEntity(
            id = "water-backfill-1",
            epochDay = date.toEpochDay(),
            amountMl = 1500,
            loggedAt = 0,
            source = "BACKFILL",
        ).toDomain()

        assertTrue(log.isBackfilled)
    }

    @Test
    fun `a completed fast maps its duration and target result`() {
        val start = Instant.parse("2026-10-22T18:00:00Z")
        val end = Instant.parse("2026-10-23T10:00:00Z")
        val session = FastingSessionEntity(
            id = "fast-1",
            epochDay = date.toEpochDay(),
            targetHours = 16,
            startedAt = start.toEpochMilli(),
            endedAt = end.toEpochMilli(),
            createdAt = start.toEpochMilli(),
        ).toDomain()

        assertEquals(date, session.date)
        assertFalse(session.isActive)
        assertEquals(960L, session.durationMinutes())
        assertTrue(session.metTarget())
    }

    @Test
    fun `a running fast has no end and uses the supplied clock for elapsed time`() {
        val start = Instant.parse("2026-10-22T18:00:00Z")
        val session = FastingSessionEntity(
            id = "fast-2",
            epochDay = date.toEpochDay(),
            targetHours = 16,
            startedAt = start.toEpochMilli(),
            endedAt = null,
            createdAt = start.toEpochMilli(),
        ).toDomain()

        assertTrue(session.isActive)
        assertNull(session.endedAt)
        assertEquals(120L, session.durationMinutes(start.plusSeconds(2 * 3600)))
        assertFalse(session.metTarget(start.plusSeconds(2 * 3600)))
    }
}
