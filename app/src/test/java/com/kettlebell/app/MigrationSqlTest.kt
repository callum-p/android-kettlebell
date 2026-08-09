package com.kettlebell.app

import com.kettlebell.app.data.db.MIGRATION_1_2_SQL
import com.kettlebell.app.data.db.MIGRATION_2_3_SQL
import com.kettlebell.app.data.db.MIGRATION_3_4_SQL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * Exercises the migration SQL directly against an in-memory SQLite database (no Android/emulator).
 * Guards the data-loss regression: proves the migrations are purely additive — existing rows
 * survive and the new columns/tables appear — which the old destructive fallback was not.
 */
class MigrationSqlTest {

    @Test
    fun migrationsArePurelyAdditiveAndPreserveData() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { conn ->
            // A v1-era base schema — enough of the real tables for the migration SQL to apply.
            exec(conn, "CREATE TABLE `exercises` (`id` TEXT PRIMARY KEY NOT NULL, `name` TEXT NOT NULL)")
            exec(conn, "CREATE TABLE `sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL)")
            exec(
                conn,
                "CREATE TABLE `sets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`sessionExerciseId` INTEGER NOT NULL, `reps` INTEGER NOT NULL)",
            )

            // Seed a workout that must survive every migration.
            exec(conn, "INSERT INTO exercises (id, name) VALUES ('swing', 'Swing')")
            exec(conn, "INSERT INTO sessions (title) VALUES ('Leg Day')")
            exec(conn, "INSERT INTO sets (sessionExerciseId, reps) VALUES (1, 10)")

            // Apply migrations in order, exactly as Room would.
            (MIGRATION_1_2_SQL + MIGRATION_2_3_SQL + MIGRATION_3_4_SQL).forEach { exec(conn, it) }

            // Data survived.
            assertEquals(1, count(conn, "SELECT COUNT(*) FROM sets"))
            assertEquals(1, count(conn, "SELECT COUNT(*) FROM sessions"))
            assertEquals(1, count(conn, "SELECT COUNT(*) FROM exercises"))

            // New columns exist.
            assertTrue("sets.rpe", "rpe" in columns(conn, "sets"))
            assertTrue("sets.notes", "notes" in columns(conn, "sets"))
            assertTrue("exercises.bodyParts", "bodyParts" in columns(conn, "exercises"))

            // New tables exist.
            assertTrue("routines table", tableExists(conn, "routines"))
            assertTrue("routine_exercises table", tableExists(conn, "routine_exercises"))
        }
    }

    private fun exec(conn: Connection, sql: String) = conn.createStatement().use { it.execute(sql) }

    private fun count(conn: Connection, sql: String): Int =
        conn.createStatement().use { st ->
            st.executeQuery(sql).use { rs -> if (rs.next()) rs.getInt(1) else -1 }
        }

    private fun columns(conn: Connection, table: String): Set<String> {
        val cols = mutableSetOf<String>()
        conn.createStatement().use { st ->
            st.executeQuery("PRAGMA table_info($table)").use { rs ->
                while (rs.next()) cols += rs.getString("name")
            }
        }
        return cols
    }

    private fun tableExists(conn: Connection, name: String): Boolean =
        conn.createStatement().use { st ->
            st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='$name'")
                .use { rs -> rs.next() }
        }
}
