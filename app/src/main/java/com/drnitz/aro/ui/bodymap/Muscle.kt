//
// Muscle.kt
//
// Kotlin port of the MIT-licensed MuscleMap Swift package
// (github.com/melihcolpan/MuscleMap, Copyright (c) 2026 Melih Colpan)
// enriched with Aro exercise database domain mapping.
//

package com.drnitz.aro.ui.bodymap

/** Which face of the body to display. */
enum class BodySide { Front, Back }

/** The body gender model. Both Male and Female anatomical vector datasets are supported. */
enum class BodyGender { Male, Female }

/** Which side of the body a muscle belongs to. */
enum class MuscleSide { Left, Right, Both }

/** All muscle groups that can be highlighted on the body. */
enum class Muscle(val raw: String, val displayName: String) {
    Abs("abs", "Abs"),
    Biceps("biceps", "Biceps"),
    Calves("calves", "Calves"),
    Chest("chest", "Chest"),
    Deltoids("deltoids", "Shoulders"),
    Feet("feet", "Feet"),
    Forearm("forearm", "Forearms"),
    Gluteal("gluteal", "Glutes"),
    Hamstring("hamstring", "Hamstrings"),
    Hands("hands", "Hands"),
    Head("head", "Head"),
    Knees("knees", "Knees"),
    LowerBack("lower-back", "Lower Back"),
    Obliques("obliques", "Obliques"),
    Quadriceps("quadriceps", "Quadriceps"),
    Tibialis("tibialis", "Tibialis"),
    Trapezius("trapezius", "Traps"),
    Triceps("triceps", "Triceps"),
    UpperBack("upper-back", "Upper Back / Lats"),

    // Specialized muscle groups
    RotatorCuff("rotator-cuff", "Rotator Cuff"),
    Serratus("serratus", "Serratus"),
    Rhomboids("rhomboids", "Rhomboids"),

    // Sub-groups
    Ankles("ankles", "Ankles"),
    Adductors("adductors", "Adductors"),
    Neck("neck", "Neck"),
    HipFlexors("hip-flexors", "Hip Flexors"),
    UpperChest("upper-chest", "Upper Chest"),
    LowerChest("lower-chest", "Lower Chest"),
    InnerQuad("inner-quad", "Inner Quad"),
    OuterQuad("outer-quad", "Outer Quad"),
    UpperAbs("upper-abs", "Upper Abs"),
    LowerAbs("lower-abs", "Lower Abs"),
    FrontDeltoid("front-deltoid", "Front Deltoid"),
    RearDeltoid("rear-deltoid", "Rear Deltoid"),
    UpperTrapezius("upper-trapezius", "Upper Traps"),
    LowerTrapezius("lower-trapezius", "Lower Traps");

    /** Whether this is a cosmetic part (head/hair) rather than a muscle. */
    val isCosmeticPart: Boolean get() = this == Head

    /** The parent muscle group, if this muscle is a sub-group. */
    val parentGroup: Muscle?
        get() = when (this) {
            UpperChest, LowerChest -> Chest
            InnerQuad, OuterQuad, HipFlexors -> Quadriceps
            UpperAbs, LowerAbs -> Abs
            FrontDeltoid, RearDeltoid -> Deltoids
            UpperTrapezius, LowerTrapezius -> Trapezius
            Serratus -> Obliques
            Ankles -> Feet
            Adductors -> Hamstring
            Neck -> Head
            else -> null
        }

    /** Whether this muscle is a sub-group of another muscle. */
    val isSubGroup: Boolean get() = parentGroup != null

    /**
     * Whether this sub-group is always rendered even when sub-groups are hidden.
     * When tapped in default mode, the parent muscle is returned instead.
     */
    val isAlwaysVisibleSubGroup: Boolean
        get() = when (this) {
            Ankles, Adductors, Neck -> true
            else -> false
        }

    /** Returns primary/secondary muscle filter strings used in Aro's Exercise library. */
    fun toExerciseFilterMuscles(): List<String> = when (this) {
        Chest, UpperChest, LowerChest -> listOf("chest")
        Abs, UpperAbs, LowerAbs, Obliques -> listOf("abdominals")
        Biceps -> listOf("biceps")
        Triceps -> listOf("triceps")
        Forearm -> listOf("forearms")
        Deltoids, FrontDeltoid, RearDeltoid -> listOf("shoulders", "supraspinatus")
        RotatorCuff -> listOf("supraspinatus", "shoulders")
        Quadriceps, InnerQuad, OuterQuad, HipFlexors -> listOf("quadriceps")
        Hamstring -> listOf("hamstrings")
        Gluteal -> listOf("glutes", "abductors")
        Calves, Tibialis, Feet, Ankles -> listOf("calves")
        UpperBack, Rhomboids -> listOf("middle back", "lats")
        LowerBack -> listOf("lower back")
        Trapezius, UpperTrapezius, LowerTrapezius -> listOf("traps")
        Adductors -> listOf("adductors")
        Neck -> listOf("neck")
        Serratus -> listOf("serratus anterior", "abdominals")
        Head, Hands, Knees -> emptyList()
    }

    companion object {
        private val byRaw = entries.associateBy { it.raw }
        fun fromRaw(raw: String): Muscle? = byRaw[raw]

        /** Maps an Aro exercise muscle string (from Exercise.primaryMuscles/secondaryMuscles) to a body map [Muscle]. */
        fun fromExerciseMuscle(muscleName: String): Muscle? = when (muscleName.lowercase().trim()) {
            "chest" -> Chest
            "abdominals", "abs" -> Abs
            "biceps" -> Biceps
            "triceps" -> Triceps
            "forearms", "forearm" -> Forearm
            "shoulders", "deltoids", "delts" -> Deltoids
            "supraspinatus", "rotator cuff" -> RotatorCuff
            "quadriceps", "quads" -> Quadriceps
            "hamstrings", "hamstring" -> Hamstring
            "glutes", "gluteal", "butt" -> Gluteal
            "abductors" -> Gluteal
            "calves", "calf" -> Calves
            "lats", "latissimus dorsi" -> UpperBack
            "middle back", "upper back" -> UpperBack
            "lower back" -> LowerBack
            "traps", "trapezius" -> Trapezius
            "adductors", "adductor" -> Adductors
            "neck" -> Neck
            "serratus anterior", "serratus" -> Serratus
            else -> null
        }
    }
}

/** Internal-only slug that includes hair for rendering purposes. */
internal enum class BodySlug(val raw: String) {
    Abs("abs"),
    Biceps("biceps"),
    Calves("calves"),
    Chest("chest"),
    Deltoids("deltoids"),
    Feet("feet"),
    Forearm("forearm"),
    Gluteal("gluteal"),
    Hamstring("hamstring"),
    Hands("hands"),
    Hair("hair"),
    Head("head"),
    Knees("knees"),
    LowerBack("lower-back"),
    Obliques("obliques"),
    Quadriceps("quadriceps"),
    Tibialis("tibialis"),
    Trapezius("trapezius"),
    Triceps("triceps"),
    UpperBack("upper-back"),

    RotatorCuff("rotator-cuff"),
    Serratus("serratus"),
    Rhomboids("rhomboids"),

    Ankles("ankles"),
    Adductors("adductors"),
    Neck("neck"),
    HipFlexors("hip-flexors"),
    UpperChest("upper-chest"),
    LowerChest("lower-chest"),
    InnerQuad("inner-quad"),
    OuterQuad("outer-quad"),
    UpperAbs("upper-abs"),
    LowerAbs("lower-abs"),
    FrontDeltoid("front-deltoid"),
    RearDeltoid("rear-deltoid"),
    UpperTrapezius("upper-trapezius"),
    LowerTrapezius("lower-trapezius");

    /** The muscle this slug maps to, or null for cosmetic-only slugs (hair). */
    val muscle: Muscle? get() = if (this == Hair) null else Muscle.fromRaw(raw)
}
