package com.drnitz.aro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FastingStageTest {

    @Test
    fun stages_transitionAtCorrectHourThresholds() {
        assertEquals(FastingStage.DIGESTIVE, FastingStage.forElapsedHours(0.0))
        assertEquals(FastingStage.DIGESTIVE, FastingStage.forElapsedHours(2.5))
        assertEquals(FastingStage.DIGESTIVE, FastingStage.forElapsedHours(3.99))

        assertEquals(FastingStage.FAT_BURNING_PREP, FastingStage.forElapsedHours(4.0))
        assertEquals(FastingStage.FAT_BURNING_PREP, FastingStage.forElapsedHours(8.0))
        assertEquals(FastingStage.FAT_BURNING_PREP, FastingStage.forElapsedHours(11.99))

        assertEquals(FastingStage.KETOSIS, FastingStage.forElapsedHours(12.0))
        assertEquals(FastingStage.KETOSIS, FastingStage.forElapsedHours(14.5))
        assertEquals(FastingStage.KETOSIS, FastingStage.forElapsedHours(15.99))

        assertEquals(FastingStage.AUTOPHAGY, FastingStage.forElapsedHours(16.0))
        assertEquals(FastingStage.AUTOPHAGY, FastingStage.forElapsedHours(24.0))
        assertEquals(FastingStage.AUTOPHAGY, FastingStage.forElapsedHours(36.0))
    }
}
