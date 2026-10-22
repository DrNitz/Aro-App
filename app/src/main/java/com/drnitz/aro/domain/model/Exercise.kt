package com.drnitz.aro.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Public-domain exercise representation from Free Exercise DB.
 * Pure Kotlin value object.
 */
@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val force: String? = null,
    val level: String = "beginner",
    val mechanic: String? = null,
    val equipment: String? = null,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val category: String = "strength",
    val images: List<String> = emptyList(),
) {
    val targetMuscleSummary: String
        get() = (primaryMuscles + secondaryMuscles).distinct().joinToString(", ").ifEmpty { "Full Body" }

    fun matchesQuery(query: String): Boolean {
        if (query.isBlank()) return true
        val clean = query.trim().lowercase()
        return name.lowercase().contains(clean) ||
            category.lowercase().contains(clean) ||
            equipment?.lowercase()?.contains(clean) == true ||
            primaryMuscles.any { it.lowercase().contains(clean) } ||
            secondaryMuscles.any { it.lowercase().contains(clean) }
    }
}
