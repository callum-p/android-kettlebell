package com.kettlebell.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Exercise::class,
        WorkoutSession::class,
        SessionExercise::class,
        WorkoutSet::class,
        Routine::class,
        RoutineExercise::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class KettlebellDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sessionDao(): SessionDao
    abstract fun sessionExerciseDao(): SessionExerciseDao
    abstract fun workoutSetDao(): WorkoutSetDao
    abstract fun routineDao(): RoutineDao
    abstract fun routineExerciseDao(): RoutineExerciseDao

    companion object {
        @Volatile
        private var instance: KettlebellDatabase? = null

        fun get(context: Context): KettlebellDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KettlebellDatabase::class.java,
                    "kettlebell.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }

        /** Close and forget the singleton so the underlying file can be safely replaced. */
        fun closeInstance() = synchronized(this) {
            instance?.close()
            instance = null
        }
    }
}
