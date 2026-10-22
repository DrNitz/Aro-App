package com.drnitz.aro.data.library

import com.drnitz.aro.domain.model.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseImageUrlTest {

    private val exercise = Exercise(
        id = "bench-press",
        name = "Barbell Bench Press",
        images = listOf("Barbell_Bench_Press/0.jpg", "Barbell_Bench_Press/1.jpg"),
    )

    @Test
    fun `relative database paths become absolute GitHub URLs`() {
        assertEquals(
            "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/Barbell_Bench_Press/0.jpg",
            exercise.imageUrlAt(0),
        )
        assertEquals(
            "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/Barbell_Bench_Press/1.jpg",
            exercise.imageUrlAt(1),
        )
    }

    @Test
    fun `out of range or missing images return null`() {
        assertNull(exercise.imageUrlAt(2))
        assertNull(exercise.imageUrlAt(-1))
        assertNull(exercise.copy(images = emptyList()).imageUrlAt(0))
    }

    @Test
    fun `imageUrls skips blanks and honours the limit`() {
        val mixed = exercise.copy(
            images = listOf("Barbell_Bench_Press/0.jpg", " ", "Barbell_Bench_Press/1.jpg"),
        )

        assertEquals(2, mixed.imageUrls().size)
        assertEquals(1, mixed.imageUrls(limit = 1).size)
        assertTrue(mixed.imageUrls().all { it.startsWith("https://raw.githubusercontent.com/") })
    }
}
