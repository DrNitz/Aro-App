//
// BodyView.kt
//
// Kotlin/Compose port of `BodyView` + `BodyRenderer` from the MIT-licensed
// MuscleMap Swift package (github.com/melihcolpan/MuscleMap,
// Copyright (c) 2026 Melih Colpan) enriched with sensory feedback for Aro.
//

package com.drnitz.aro.ui.bodymap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlin.math.min

/** Maps the SVG viewBox onto a given canvas [Size]; mirrors the Swift renderer math. */
internal data class BodyGeometry(val scale: Float, val offsetX: Float, val offsetY: Float) {
    companion object {
        fun of(viewBox: BodyViewBox, size: Size): BodyGeometry {
            val scale = min(size.width / viewBox.width, size.height / viewBox.height)
            val offsetX = (size.width - viewBox.width * scale) / 2f - viewBox.originX * scale
            val offsetY = (size.height - viewBox.height * scale) / 2f - viewBox.originY * scale
            return BodyGeometry(scale, offsetX, offsetY)
        }
    }
}

/**
 * Renders an anatomical body figure for the given [gender]/[side], optionally
 * highlighting muscles via [highlights] (e.g. a heatmap) and/or [selectedMuscles].
 *
 * When [onMuscleSelected] is provided, tapping a muscle region reports the muscle
 * and the side that was hit, with optional sensory click/haptic feedback.
 */
@Composable
fun BodyView(
    side: BodySide,
    modifier: Modifier = Modifier,
    gender: BodyGender = BodyGender.Male,
    highlights: Map<Muscle, MuscleHighlight> = emptyMap(),
    selectedMuscles: Set<Muscle> = emptySet(),
    style: BodyViewStyle = BodyViewStyle.AroDark,
    hideSubGroups: Boolean = true,
    enableSensoryFeedback: Boolean = true,
    isSoundMuted: Boolean = false,
    scale: Float = 1f,
    panOffset: Offset = Offset.Zero,
    onTransformChanged: ((scale: Float, pan: Offset) -> Unit)? = null,
    onMuscleSelected: ((Muscle, MuscleSide) -> Unit)? = null,
) {
    val context = LocalContext.current
    val parts = remember(gender, side) { BodyPathProvider.paths(gender, side) }
    val viewBox = remember(gender, side) { BodyPathProvider.viewBox(gender, side) }

    var internalScale by remember { mutableFloatStateOf(1f) }
    var internalPan by remember { mutableStateOf(Offset.Zero) }

    val currentScale = if (onTransformChanged != null) scale else internalScale
    val currentPan = if (onTransformChanged != null) panOffset else internalPan

    val updateTransform = { newScale: Float, newPan: Offset ->
        if (onTransformChanged != null) {
            onTransformChanged(newScale, newPan)
        } else {
            internalScale = newScale
            internalPan = newPan
        }
    }

    val gestureModifier = Modifier.pointerInput(gender, side, hideSubGroups, enableSensoryFeedback, isSoundMuted, currentScale, currentPan) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var zoom = 1f
            var pan = Offset.Zero
            var pastTouchSlop = false
            val touchSlop = viewConfiguration.touchSlop

            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.any { it.isConsumed }
                if (canceled) break

                val pointerCount = event.changes.count { it.pressed }
                if (pointerCount > 1) {
                    val zoomChange = event.calculateZoom()
                    val panChange = event.calculatePan()
                    zoom *= zoomChange
                    pan += panChange

                    val newScale = (currentScale * zoomChange).coerceIn(1.0f, 3.5f)
                    val newPan = if (newScale > 1.01f) {
                        val maxPanX = (size.width * (newScale - 1f)) / 2f + 60f
                        val maxPanY = (size.height * (newScale - 1f)) / 2f + 100f
                        Offset(
                            (currentPan.x + panChange.x).coerceIn(-maxPanX, maxPanX),
                            (currentPan.y + panChange.y).coerceIn(-maxPanY, maxPanY),
                        )
                    } else {
                        Offset.Zero
                    }
                    updateTransform(newScale, newPan)
                    event.changes.forEach { it.consume() }
                    pastTouchSlop = true
                } else if (pointerCount == 1 && currentScale > 1.05f) {
                    val panChange = event.calculatePan()
                    pan += panChange
                    if (!pastTouchSlop && pan.getDistance() > touchSlop) {
                        pastTouchSlop = true
                    }
                    if (pastTouchSlop) {
                        val maxPanX = (size.width * (currentScale - 1f)) / 2f + 60f
                        val maxPanY = (size.height * (currentScale - 1f)) / 2f + 100f
                        val newPan = Offset(
                            (currentPan.x + panChange.x).coerceIn(-maxPanX, maxPanX),
                            (currentPan.y + panChange.y).coerceIn(-maxPanY, maxPanY),
                        )
                        updateTransform(currentScale, newPan)
                        event.changes.firstOrNull()?.consume()
                    }
                } else if (pointerCount == 1) {
                    val panChange = event.calculatePan()
                    pan += panChange
                    if (pan.getDistance() > touchSlop) {
                        pastTouchSlop = true
                    }
                }
            } while (event.changes.any { it.pressed })

            if (!pastTouchSlop && onMuscleSelected != null) {
                val tap = down.position
                val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                val c = canvasSize.center
                val unzoomedPoint = if (currentScale > 0.001f) {
                    c + (tap - currentPan - c) / currentScale
                } else {
                    tap
                }
                hitTest(unzoomedPoint, canvasSize, parts, viewBox, hideSubGroups)?.let { (muscle, hitSide) ->
                    if (enableSensoryFeedback) {
                        BodySoundHaptics.onMuscleTap(context, isSoundMuted)
                    }
                    onMuscleSelected(muscle, hitSide)
                }
            }
        }
    }

    Canvas(modifier = modifier.then(gestureModifier)) {
        if (size.minDimension <= 0f) return@Canvas
        val geo = BodyGeometry.of(viewBox, size)
        withTransform({
            translate(currentPan.x, currentPan.y)
            scale(currentScale, currentScale, pivot = size.center)
            translate(geo.offsetX, geo.offsetY)
            scale(geo.scale, geo.scale, pivot = Offset.Zero)
        }) {
            for (part in parts) {
                val muscle = part.slug.muscle
                if (hideSubGroups && muscle != null && muscle.isSubGroup && !muscle.isAlwaysVisibleSubGroup) {
                    continue
                }

                val highlight = muscle?.let { highlights[it] }
                val isSelected = muscle != null && (
                    selectedMuscles.contains(muscle) ||
                        (hideSubGroups && muscle.isAlwaysVisibleSubGroup &&
                            muscle.parentGroup?.let { selectedMuscles.contains(it) } == true)
                    )

                val fill = resolveFill(part.slug, highlight, isSelected, highlights, style)
                val opacity = highlight?.opacity ?: 1f

                for (svg in part.allPaths) {
                    val path = PathCache.path(svg)
                    when (fill) {
                        is MuscleFill.Solid -> drawPath(path, fill.color, alpha = opacity)
                        is MuscleFill.LinearGradient -> {
                            val b = path.getBounds()
                            val brush = Brush.linearGradient(
                                colors = fill.colors,
                                start = Offset(b.left + b.width * fill.start.x, b.top + b.height * fill.start.y),
                                end = Offset(b.left + b.width * fill.end.x, b.top + b.height * fill.end.y),
                            )
                            drawPath(path, brush, alpha = opacity)
                        }
                    }

                    if (style.strokeWidth > 0f) {
                        drawPath(path, style.strokeColor, style = Stroke(width = style.strokeWidth / geo.scale))
                    }
                    if (isSelected) {
                        drawPath(
                            path,
                            style.selectionStrokeColor,
                            style = Stroke(width = style.selectionStrokeWidth / geo.scale),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Convenience wrapper that renders a heatmap of muscle [intensities] using the
 * given [colorScale].
 */
@Composable
fun BodyHeatmapView(
    side: BodySide,
    intensities: List<MuscleIntensity>,
    modifier: Modifier = Modifier,
    gender: BodyGender = BodyGender.Male,
    selectedMuscles: Set<Muscle> = emptySet(),
    colorScale: HeatmapColorScale = HeatmapColorScale.AroDarkWorkout,
    style: BodyViewStyle = BodyViewStyle.AroDark,
    hideSubGroups: Boolean = true,
    enableSensoryFeedback: Boolean = true,
    isSoundMuted: Boolean = false,
    scale: Float = 1f,
    panOffset: Offset = Offset.Zero,
    onTransformChanged: ((scale: Float, pan: Offset) -> Unit)? = null,
    onMuscleSelected: ((Muscle, MuscleSide) -> Unit)? = null,
) {
    val highlights = remember(intensities, colorScale) { heatmapHighlights(intensities, colorScale) }
    BodyView(
        side = side,
        modifier = modifier,
        gender = gender,
        highlights = highlights,
        selectedMuscles = selectedMuscles,
        style = style,
        hideSubGroups = hideSubGroups,
        enableSensoryFeedback = enableSensoryFeedback,
        isSoundMuted = isSoundMuted,
        scale = scale,
        panOffset = panOffset,
        onTransformChanged = onTransformChanged,
        onMuscleSelected = onMuscleSelected,
    )
}

/** Mirrors `BodyRenderer.resolveFill`, including sub-group fill inheritance. */
private fun resolveFill(
    slug: BodySlug,
    highlight: MuscleHighlight?,
    isSelected: Boolean,
    highlights: Map<Muscle, MuscleHighlight>,
    style: BodyViewStyle,
): MuscleFill {
    if (slug == BodySlug.Hair) return MuscleFill.Solid(style.hairColor)
    if (slug == BodySlug.Head) return MuscleFill.Solid(style.headColor)
    if (isSelected) return MuscleFill.Solid(style.selectionColor)
    if (highlight != null) return highlight.fill
    slug.muscle?.parentGroup?.let { parent -> highlights[parent]?.let { return it.fill } }
    return MuscleFill.Solid(style.defaultFillColor)
}

/**
 * Finds which muscle was tapped, in screen coordinates. Sub-groups are tested
 * before their parents so they take priority. Mirrors `BodyRenderer.hitTest`.
 */
internal fun hitTest(
    point: Offset,
    size: Size,
    parts: List<BodyPartPathData>,
    viewBox: BodyViewBox,
    hideSubGroups: Boolean,
): Pair<Muscle, MuscleSide>? {
    if (size.minDimension <= 0f) return null
    val geo = BodyGeometry.of(viewBox, size)
    val rawX = ((point.x - geo.offsetX) / geo.scale)
    val rawY = ((point.y - geo.offsetY) / geo.scale)
    val px = rawX.toInt()
    val py = rawY.toInt()

    val sorted = parts.sortedByDescending { it.slug.muscle?.isSubGroup == true }
    for (part in sorted) {
        val muscle = part.slug.muscle ?: continue
        if (hideSubGroups && muscle.isSubGroup && !muscle.isAlwaysVisibleSubGroup) continue

        val resolved = if (hideSubGroups && muscle.isAlwaysVisibleSubGroup) {
            muscle.parentGroup ?: muscle
        } else {
            muscle
        }

        for (svg in part.left) if (PathCache.region(svg).contains(px, py)) return resolved to MuscleSide.Left
        for (svg in part.right) if (PathCache.region(svg).contains(px, py)) return resolved to MuscleSide.Right
        for (svg in part.common) if (PathCache.region(svg).contains(px, py)) return resolved to MuscleSide.Both
    }
    return null
}
