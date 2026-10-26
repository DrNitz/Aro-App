package com.drnitz.aro.domain.model

import kotlinx.serialization.Serializable

/**
 * Exercise representation supporting both public-domain Free Exercise DB and RepDB.
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
    val aliases: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val met: Double? = null,
    val isCustom: Boolean = false,
) {
    val targetMuscleSummary: String
        get() = (primaryMuscles + secondaryMuscles).distinct().joinToString(", ").ifEmpty { "Full Body" }

    /**
     * Smart search matching against:
     * - Name
     * - Curated aliases (e.g. "RDL", "Flat Bench", "OHP")
     * - Fitness acronyms & synonyms (e.g. "db" -> "dumbbell", "rdl" -> "romanian deadlift")
     * - Target primary and secondary muscles
     * - Equipment
     * - Category
     *
     * Multi-token queries (e.g. "bench db" or "lat cable") match if every token matches.
     */
    fun matchesQuery(query: String): Boolean {
        if (query.isBlank()) return true
        val clean = query.trim().lowercase()
        // If single exact phrase matches directly
        if (name.lowercase().contains(clean) ||
            aliases.any { it.lowercase().contains(clean) } ||
            category.lowercase().contains(clean) ||
            equipment?.lowercase()?.contains(clean) == true ||
            primaryMuscles.any { it.lowercase().contains(clean) } ||
            secondaryMuscles.any { it.lowercase().contains(clean) }
        ) {
            return true
        }

        val terms = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (terms.isEmpty()) return true

        return terms.all { term ->
            name.lowercase().contains(term) ||
                aliases.any { it.lowercase().contains(term) } ||
                category.lowercase().contains(term) ||
                equipment?.lowercase()?.contains(term) == true ||
                primaryMuscles.any { it.lowercase().contains(term) } ||
                secondaryMuscles.any { it.lowercase().contains(term) } ||
                matchesAcronymOrSynonym(term)
        }
    }

    private fun matchesAcronymOrSynonym(term: String): Boolean {
        val synonyms = ACRONYM_SYNONYMS[term] ?: return false
        return synonyms.any { syn ->
            name.lowercase().contains(syn) ||
                aliases.any { it.lowercase().contains(syn) } ||
                equipment?.lowercase()?.contains(syn) == true ||
                primaryMuscles.any { it.lowercase().contains(syn) } ||
                secondaryMuscles.any { it.lowercase().contains(syn) }
        }
    }

    companion object {
        private val ACRONYM_SYNONYMS: Map<String, List<String>> = mapOf(
            "rdl" to listOf("romanian deadlift"),
            "ohp" to listOf("overhead press", "shoulder press", "military press"),
            "db" to listOf("dumbbell"),
            "bb" to listOf("barbell"),
            "kb" to listOf("kettlebell"),
            "ez" to listOf("e-z curl bar", "ez bar", "ez-bar"),
            "cg" to listOf("close-grip", "close grip"),
            "wg" to listOf("wide-grip", "wide grip"),
            "pr" to listOf("personal record"),
            "pullup" to listOf("pull-up", "pull up"),
            "pushup" to listOf("push-up", "push up"),
            "chinup" to listOf("chin-up", "chin up"),
            "situp" to listOf("sit-up", "sit up"),
            "dip" to listOf("chest dip", "tricep dip", "parallel bar"),
            "pec" to listOf("chest"),
            "pecs" to listOf("chest"),
            "glute" to listOf("glutes"),
            "quad" to listOf("quadriceps"),
            "quads" to listOf("quadriceps"),
            "ham" to listOf("hamstrings"),
            "hams" to listOf("hamstrings"),
            "calve" to listOf("calves"),
            "calf" to listOf("calves"),
            "ab" to listOf("abdominals"),
            "abs" to listOf("abdominals"),
            "lat" to listOf("lats"),
            "lats" to listOf("lats"),
            "trap" to listOf("traps"),
            "traps" to listOf("traps"),
            "delt" to listOf("shoulders"),
            "delts" to listOf("shoulders"),
            "bicep" to listOf("biceps"),
            "tricep" to listOf("triceps"),
        )
    }
}
