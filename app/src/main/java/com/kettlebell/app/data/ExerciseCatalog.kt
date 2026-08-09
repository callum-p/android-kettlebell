package com.kettlebell.app.data

import com.kettlebell.app.data.db.Exercise
import com.kettlebell.app.data.db.Level

/** A ready-made routine the user can start with one tap. */
data class WorkoutTemplate(
    val id: String,
    val name: String,
    val level: Level,
    val description: String,
    val exerciseIds: List<String>,
)

/**
 * The built-in library of kettlebell exercises and starter routines. This is the single source of
 * truth that gets seeded into the database on first launch.
 */
object ExerciseCatalog {

    /** Standard kettlebell sizes in kilograms, used for weight snapping and progression. */
    val BELLS = listOf(4.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0, 20.0, 24.0, 28.0, 32.0, 36.0, 40.0)

    /** A reliable YouTube search link for an exercise's demo (always resolves to relevant tutorials). */
    private fun ytSearch(name: String): String =
        "https://www.youtube.com/results?search_query=" +
            "kettlebell $name tutorial".replace(" ", "+")

    val exercises: List<Exercise> by lazy {
        rawExercises.map { it.copy(youtubeUrl = ytSearch(it.name)) }
    }

    private val rawExercises: List<Exercise> = listOf(
        // ---------------------------------------------------------------- Beginner
        Exercise(
            id = "two_hand_swing",
            name = "Two-Hand Swing",
            level = Level.BEGINNER,
            category = "Full Body",
            primaryMuscles = "Glutes, Hamstrings, Core",
            description = "The foundational kettlebell movement. A powerful hip hinge that builds " +
                "explosive posterior-chain strength and conditioning.",
            instructions = listOf(
                "Stand with feet shoulder-width apart, kettlebell an arm's length in front of you.",
                "Hinge at the hips and grip the handle with both hands, back flat.",
                "Hike the bell back between your legs, then snap your hips forward to swing it to chest height.",
                "Let the bell float, then guide it back down into the next hinge. Keep arms relaxed.",
            ),
            repRangeLow = 10,
            repRangeHigh = 15,
            defaultSets = 3,
            defaultRestSeconds = 60,
            startingWeightKg = 12.0,
        ),
        Exercise(
            id = "goblet_squat",
            name = "Goblet Squat",
            level = Level.BEGINNER,
            category = "Legs",
            primaryMuscles = "Quads, Glutes, Core",
            description = "A beginner-friendly squat that teaches an upright torso and deep, " +
                "controlled range of motion.",
            instructions = listOf(
                "Hold the kettlebell by the horns close to your chest.",
                "Stand tall with feet slightly wider than shoulder-width.",
                "Sit back and down, keeping your chest up and elbows inside your knees.",
                "Drive through your heels to stand back up, squeezing your glutes at the top.",
            ),
            repRangeLow = 8,
            repRangeHigh = 12,
            defaultSets = 3,
            defaultRestSeconds = 75,
            startingWeightKg = 12.0,
        ),
        Exercise(
            id = "kb_deadlift",
            name = "Kettlebell Deadlift",
            level = Level.BEGINNER,
            category = "Posterior Chain",
            primaryMuscles = "Glutes, Hamstrings, Back",
            description = "The safest place to groove the hip hinge and build a strong, resilient back.",
            instructions = listOf(
                "Stand over the kettlebell with feet hip-width apart.",
                "Hinge down and grip the handle, keeping your spine neutral.",
                "Push the floor away and stand tall, locking out your hips.",
                "Reverse under control to place the bell down softly.",
            ),
            repRangeLow = 8,
            repRangeHigh = 12,
            defaultSets = 3,
            defaultRestSeconds = 75,
            startingWeightKg = 16.0,
        ),
        Exercise(
            id = "bent_over_row",
            name = "Bent-Over Row",
            level = Level.BEGINNER,
            category = "Back",
            primaryMuscles = "Lats, Upper Back, Biceps",
            description = "Builds pulling strength and a strong upper back to balance out pressing work.",
            instructions = listOf(
                "Hinge forward with a flat back, kettlebell hanging from one hand.",
                "Brace your core and pull the bell toward your hip.",
                "Squeeze your shoulder blade at the top.",
                "Lower under control and repeat, then switch sides.",
            ),
            repRangeLow = 8,
            repRangeHigh = 12,
            defaultSets = 3,
            defaultRestSeconds = 60,
            startingWeightKg = 12.0,
        ),
        Exercise(
            id = "halo",
            name = "Kettlebell Halo",
            level = Level.BEGINNER,
            category = "Shoulders",
            primaryMuscles = "Shoulders, Core, Mobility",
            description = "A shoulder-mobility and core-stability drill that circles the bell around your head.",
            instructions = listOf(
                "Hold the kettlebell upside-down by the horns at chest height.",
                "Circle the bell around your head, keeping it close.",
                "Move slowly and keep your ribs down and core braced.",
                "Complete the reps in one direction, then reverse.",
            ),
            repRangeLow = 6,
            repRangeHigh = 10,
            defaultSets = 2,
            defaultRestSeconds = 45,
            startingWeightKg = 8.0,
        ),
        Exercise(
            id = "farmers_carry",
            name = "Farmer's Carry",
            level = Level.BEGINNER,
            category = "Core",
            primaryMuscles = "Grip, Core, Traps",
            description = "Loaded carries that build grip, a bulletproof core, and total-body tension. " +
                "Log reps as steps or seconds.",
            instructions = listOf(
                "Hold a kettlebell in each hand (or one for a suitcase carry).",
                "Stand tall with shoulders packed down and back.",
                "Walk with short, controlled steps, bracing your core.",
                "Keep breathing and maintain posture for the full distance.",
            ),
            repRangeLow = 20,
            repRangeHigh = 40,
            defaultSets = 3,
            defaultRestSeconds = 60,
            startingWeightKg = 16.0,
        ),

        // ------------------------------------------------------------ Intermediate
        Exercise(
            id = "one_hand_swing",
            name = "One-Hand Swing",
            level = Level.INTERMEDIATE,
            category = "Full Body",
            primaryMuscles = "Glutes, Hamstrings, Core, Grip",
            description = "Adds a rotational anti-twist challenge to the swing and builds serious grip.",
            instructions = listOf(
                "Set up as for a two-hand swing but grip with a single hand.",
                "Hike the bell back, then snap the hips to float it forward.",
                "Resist rotation by bracing your core hard.",
                "Complete the reps, then switch hands.",
            ),
            repRangeLow = 8,
            repRangeHigh = 12,
            defaultSets = 4,
            defaultRestSeconds = 60,
            startingWeightKg = 16.0,
        ),
        Exercise(
            id = "clean",
            name = "Kettlebell Clean",
            level = Level.INTERMEDIATE,
            category = "Full Body",
            primaryMuscles = "Glutes, Back, Shoulders",
            description = "Brings the bell from the floor to the rack position — the gateway to presses and squats.",
            instructions = listOf(
                "Start with the bell between your feet and hinge to grip it.",
                "Pull the bell up close to your body as your hips extend.",
                "Guide your hand 'through' the handle to catch it softly in the rack.",
                "Lower back down under control and repeat.",
            ),
            repRangeLow = 6,
            repRangeHigh = 10,
            defaultSets = 4,
            defaultRestSeconds = 75,
            startingWeightKg = 16.0,
        ),
        Exercise(
            id = "front_squat",
            name = "Front Squat",
            level = Level.INTERMEDIATE,
            category = "Legs",
            primaryMuscles = "Quads, Glutes, Core",
            description = "A racked squat that loads the front of the body and demands core stability.",
            instructions = listOf(
                "Clean the bell into the rack position against your forearm.",
                "Squat down keeping your torso tall and elbow tucked.",
                "Drive up through mid-foot to standing.",
                "Keep the bell glued to your body throughout.",
            ),
            repRangeLow = 6,
            repRangeHigh = 10,
            defaultSets = 4,
            defaultRestSeconds = 90,
            startingWeightKg = 16.0,
        ),
        Exercise(
            id = "overhead_press",
            name = "Overhead Press",
            level = Level.INTERMEDIATE,
            category = "Shoulders",
            primaryMuscles = "Shoulders, Triceps, Core",
            description = "A strict grind that builds pressing strength and shoulder stability.",
            instructions = listOf(
                "Clean the bell to the rack position.",
                "Brace your core and glutes, then press the bell straight overhead.",
                "Lock out with your bicep by your ear.",
                "Lower under control back to the rack.",
            ),
            repRangeLow = 5,
            repRangeHigh = 8,
            defaultSets = 4,
            defaultRestSeconds = 90,
            startingWeightKg = 12.0,
        ),
        Exercise(
            id = "push_press",
            name = "Push Press",
            level = Level.INTERMEDIATE,
            category = "Shoulders",
            primaryMuscles = "Shoulders, Legs, Core",
            description = "Uses a leg drive to move bigger bells overhead and build explosive power.",
            instructions = listOf(
                "Start in the rack position with a soft knee bend.",
                "Dip slightly, then drive through your legs.",
                "Use the momentum to punch the bell overhead to lockout.",
                "Lower to the rack and reset for the next rep.",
            ),
            repRangeLow = 5,
            repRangeHigh = 8,
            defaultSets = 4,
            defaultRestSeconds = 90,
            startingWeightKg = 16.0,
        ),
        Exercise(
            id = "reverse_lunge",
            name = "Racked Reverse Lunge",
            level = Level.INTERMEDIATE,
            category = "Legs",
            primaryMuscles = "Quads, Glutes, Core",
            description = "A single-leg strength builder loaded in the rack for extra core demand.",
            instructions = listOf(
                "Hold the bell in the rack on one side.",
                "Step back into a lunge, dropping the back knee toward the floor.",
                "Drive through the front heel to return to standing.",
                "Complete the reps, then switch sides.",
            ),
            repRangeLow = 6,
            repRangeHigh = 10,
            defaultSets = 3,
            defaultRestSeconds = 75,
            startingWeightKg = 12.0,
        ),

        // ---------------------------------------------------------------- Advanced
        Exercise(
            id = "turkish_get_up",
            name = "Turkish Get-Up",
            level = Level.ADVANCED,
            category = "Full Body",
            primaryMuscles = "Shoulders, Core, Total Body",
            description = "A slow, deliberate full-body grind that builds shoulder stability, mobility and control.",
            instructions = listOf(
                "Lie down and press the bell to a straight arm overhead.",
                "Roll to your elbow, then your hand, and sweep your leg to a kneeling lunge.",
                "Stand up while keeping the bell locked out overhead.",
                "Reverse every step precisely to return to the floor.",
            ),
            repRangeLow = 3,
            repRangeHigh = 5,
            defaultSets = 3,
            defaultRestSeconds = 90,
            startingWeightKg = 12.0,
        ),
        Exercise(
            id = "snatch",
            name = "Kettlebell Snatch",
            level = Level.ADVANCED,
            category = "Full Body",
            primaryMuscles = "Glutes, Back, Shoulders",
            description = "The 'tsar' of kettlebell lifts — one explosive movement from the floor to overhead.",
            instructions = listOf(
                "Hike the bell back like a one-hand swing.",
                "Explosively extend the hips and pull the bell up the centreline.",
                "Punch your hand through as the bell rotates to catch it overhead.",
                "Lower back into the next rep in one smooth arc.",
            ),
            repRangeLow = 5,
            repRangeHigh = 10,
            defaultSets = 4,
            defaultRestSeconds = 90,
            startingWeightKg = 16.0,
        ),
        Exercise(
            id = "double_front_squat",
            name = "Double Front Squat",
            level = Level.ADVANCED,
            category = "Legs",
            primaryMuscles = "Quads, Glutes, Core",
            description = "Two bells racked for a brutal lower-body and core strength challenge.",
            instructions = listOf(
                "Clean two kettlebells into a double rack position.",
                "Keep your elbows high and torso tall.",
                "Squat to depth, then drive powerfully out of the hole.",
                "Maintain tension on both bells throughout.",
            ),
            repRangeLow = 5,
            repRangeHigh = 8,
            defaultSets = 4,
            defaultRestSeconds = 120,
            startingWeightKg = 20.0,
        ),
        Exercise(
            id = "windmill",
            name = "Windmill",
            level = Level.ADVANCED,
            category = "Core",
            primaryMuscles = "Obliques, Shoulders, Hamstrings",
            description = "A loaded mobility movement that carves out strong obliques and shoulder stability.",
            instructions = listOf(
                "Press the bell overhead and turn your feet away from it.",
                "Push your hips toward the bell as you hinge sideways.",
                "Reach your free hand down your leg, eyes on the bell.",
                "Stand back up under control, then switch sides.",
            ),
            repRangeLow = 4,
            repRangeHigh = 6,
            defaultSets = 3,
            defaultRestSeconds = 75,
            startingWeightKg = 10.0,
        ),
        Exercise(
            id = "long_cycle",
            name = "Clean & Jerk (Long Cycle)",
            level = Level.ADVANCED,
            category = "Full Body",
            primaryMuscles = "Total Body, Conditioning",
            description = "A demanding conditioning lift chaining a clean into an overhead jerk for reps.",
            instructions = listOf(
                "Clean the bell to the rack position.",
                "Dip and drive, dropping under the bell to lock it out overhead.",
                "Lower to the rack, then back to the hang for the next clean.",
                "Keep a steady rhythm and breathe with the movement.",
            ),
            repRangeLow = 5,
            repRangeHigh = 10,
            defaultSets = 4,
            defaultRestSeconds = 120,
            startingWeightKg = 20.0,
        ),
    )

    val templates: List<WorkoutTemplate> = listOf(
        WorkoutTemplate(
            id = "beginner_foundations",
            name = "Beginner Foundations",
            level = Level.BEGINNER,
            description = "Master the essentials: hinge, squat and pull.",
            exerciseIds = listOf("two_hand_swing", "goblet_squat", "kb_deadlift", "bent_over_row"),
        ),
        WorkoutTemplate(
            id = "intermediate_strength",
            name = "Intermediate Strength",
            level = Level.INTERMEDIATE,
            description = "Clean, squat and press for full-body strength.",
            exerciseIds = listOf("clean", "front_squat", "overhead_press", "reverse_lunge"),
        ),
        WorkoutTemplate(
            id = "advanced_power",
            name = "Advanced Power",
            level = Level.ADVANCED,
            description = "Explosive ballistics and heavy grinds.",
            exerciseIds = listOf("snatch", "double_front_squat", "turkish_get_up", "long_cycle"),
        ),
    )

    fun byId(id: String): Exercise? = exercises.firstOrNull { it.id == id }
}
