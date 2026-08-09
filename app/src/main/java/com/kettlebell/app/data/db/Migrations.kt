package com.kettlebell.app.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migrations. Every version bump MUST add a migration here and register it in
 * [KettlebellDatabase]; the database no longer falls back to a destructive rebuild on upgrade,
 * so a forgotten migration fails loudly instead of silently deleting the user's workouts.
 *
 * The SQL mirrors exactly what Room generates for each entity, so the post-migration schema
 * validation passes.
 */

/** v1 → v2: exercises gained a [BodyPart] list column (stored as a comma-joined string). */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `exercises` ADD COLUMN `bodyParts` TEXT NOT NULL DEFAULT ''")
    }
}

/** v2 → v3: added user-created routines and their ordered exercise membership. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `routines` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `routine_exercises` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`routineId` INTEGER NOT NULL, " +
                "`exerciseId` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`routineId`) REFERENCES `routines`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_routine_exercises_routineId` " +
                "ON `routine_exercises` (`routineId`)",
        )
    }
}

/** v3 → v4: sets gained optional RPE and notes columns. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `sets` ADD COLUMN `rpe` INTEGER")
        db.execSQL("ALTER TABLE `sets` ADD COLUMN `notes` TEXT")
    }
}

/** All migrations, in order. Register new ones here. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
