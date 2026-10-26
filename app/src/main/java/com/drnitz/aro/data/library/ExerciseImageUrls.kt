package com.drnitz.aro.data.library

import com.drnitz.aro.domain.model.Exercise

/**
 * Exercise images:
 * - Free Exercise DB images are relative to its `exercises/` folder on GitHub.
 * - RepDB images are prefixed with `repdb/` and streamed from the RepDB GitHub repository.
 * - Absolute http/https URLs are returned verbatim.
 */
private const val FREE_EXERCISE_DB_IMAGE_BASE_URL =
    "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

private const val REPDB_IMAGE_BASE_URL =
    "https://raw.githubusercontent.com/RepDB/exercise-dataset/main/"

/** Absolute URL for one demonstration posture, or null when the entry has no image. */
fun Exercise.imageUrlAt(index: Int): String? =
    images.getOrNull(index)
        ?.takeIf { it.isNotBlank() }
        ?.let { path ->
            when {
                path.startsWith("http://") || path.startsWith("https://") -> path
                path.startsWith("repdb/") -> REPDB_IMAGE_BASE_URL + path.removePrefix("repdb/")
                else -> FREE_EXERCISE_DB_IMAGE_BASE_URL + path
            }
        }

/**
 * Absolute URLs for the available postures, capped at [limit].
 * Blank paths are ignored so a malformed record can never produce a broken request.
 */
fun Exercise.imageUrls(limit: Int = Int.MAX_VALUE): List<String> {
    require(limit >= 0) { "limit must not be negative" }
    return images.asSequence()
        .filter { it.isNotBlank() }
        .take(limit)
        .map { path ->
            when {
                path.startsWith("http://") || path.startsWith("https://") -> path
                path.startsWith("repdb/") -> REPDB_IMAGE_BASE_URL + path.removePrefix("repdb/")
                else -> FREE_EXERCISE_DB_IMAGE_BASE_URL + path
            }
        }
        .toList()
}
