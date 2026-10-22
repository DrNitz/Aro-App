package com.drnitz.aro.data.library

import com.drnitz.aro.domain.model.Exercise

/**
 * Free Exercise DB stores image paths relative to its `exercises/` folder.
 * The Android app streams them from the same public GitHub source the website uses, so
 * the APK does not grow by roughly 65 MB of bundled photos.
 */
private const val EXERCISE_IMAGE_BASE_URL =
    "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

/** Absolute URL for one demonstration posture, or null when the entry has no image. */
fun Exercise.imageUrlAt(index: Int): String? =
    images.getOrNull(index)
        ?.takeIf { it.isNotBlank() }
        ?.let { EXERCISE_IMAGE_BASE_URL + it }

/**
 * Absolute URLs for the available postures, capped at [limit].
 * Blank paths are ignored so a malformed record can never produce a broken request.
 */
fun Exercise.imageUrls(limit: Int = Int.MAX_VALUE): List<String> {
    require(limit >= 0) { "limit must not be negative" }
    return images.asSequence()
        .filter { it.isNotBlank() }
        .take(limit)
        .map { EXERCISE_IMAGE_BASE_URL + it }
        .toList()
}
