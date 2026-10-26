package com.drnitz.aro.ui.bodymap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BodyMapHitTest {

    @Test
    fun svgPathBuilder_parsesStandardPathCommands() {
        val svg = "M 10 10 L 50 10 L 50 50 L 10 50 Z"
        val path = SvgPathBuilder.build(svg)
        assertFalse(path.isEmpty)

        val region = PathCache.region(svg)
        assertFalse(region.isEmpty)
        assertTrue(region.contains(30, 30))
        assertFalse(region.contains(5, 5))
        assertFalse(region.contains(60, 60))
    }

    @Test
    fun hitTest_correctlyDetectsChestOnMaleFront() {
        val parts = BodyPathProvider.paths(BodyGender.Male, BodySide.Front)
        val viewBox = BodyPathProvider.viewBox(BodyGender.Male, BodySide.Front)

        // ViewBox for MaleFront: originX=0, originY=95, width=727, height=1280
        val canvasSize = Size(727f, 1280f)

        // In SVG coordinates, the left pec is around (310, 380).
        // Since viewBox.originY is 95, the canvas translates by offsetY = -95.
        // Therefore, on-screen Y = 380 + (-95) = 285.
        val result = hitTest(
            point = Offset(310f, 285f),
            size = canvasSize,
            parts = parts,
            viewBox = viewBox,
            hideSubGroups = true,
        )

        assertNotNull("Expected hitTest to hit a muscle", result)
        assertEquals(Muscle.Chest, result?.first)
    }

    @Test
    fun hitTest_correctlyDetectsTrapeziusAndUpperBackOnMaleBack() {
        val parts = BodyPathProvider.paths(BodyGender.Male, BodySide.Back)
        val viewBox = BodyPathProvider.viewBox(BodyGender.Male, BodySide.Back)

        val canvasSize = Size(727f, 1280f)
        // ViewBox for MaleBack: originX=718, originY=95, width=727, height=1280
        // Trapezius path is at X ~ 1071, Y ~ 330 in SVG coordinates.
        // Screen X = 1071 - 718 = 353
        // Screen Y = 330 - 95 = 235
        val result = hitTest(
            point = Offset(353f, 235f),
            size = canvasSize,
            parts = parts,
            viewBox = viewBox,
            hideSubGroups = true,
        )

        assertNotNull("Expected hitTest to hit Trapezius on MaleBack", result)
        assertEquals(Muscle.Trapezius, result?.first)
    }

    @Test
    fun bodySoundHaptics_runsGracefullyOnDevice() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Haptic pulse
        BodySoundHaptics.performHapticClick(context)

        // Sound pulse (unmuted and muted)
        BodySoundHaptics.playClickSound(isMuted = false)
        BodySoundHaptics.playClickSound(isMuted = true)
        BodySoundHaptics.playChimeSound(isMuted = false)

        // Combined muscle tap trigger
        BodySoundHaptics.onMuscleTap(context, isMuted = false)
        BodySoundHaptics.release()
    }
}
