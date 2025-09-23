package com.talauncher.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.AppSession
import com.talauncher.data.model.LauncherSettings

@Database(
    entities = [AppInfo::class, LauncherSettings::class, AppSession::class],
    version = 11,
    exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun settingsDao(): SettingsDao
    abstract fun appSessionDao(): AppSessionDao

    companion object {
        @Volatile
        private var INSTANCE: LauncherDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN isOnboardingCompleted INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Update app_info table structure
                database.execSQL("""
                    CREATE TABLE app_info_new (
                        packageName TEXT PRIMARY KEY NOT NULL,
                        appName TEXT NOT NULL,
                        isPinned INTEGER NOT NULL DEFAULT 0,
                        isHidden INTEGER NOT NULL DEFAULT 0,
                        pinnedOrder INTEGER NOT NULL DEFAULT 0,
                        isDistracting INTEGER NOT NULL DEFAULT 0
                    )
                """)

                // Copy existing data, mapping isEssential to isPinned
                database.execSQL("""
                    INSERT INTO app_info_new (packageName, appName, isPinned, isDistracting)
                    SELECT packageName, appName, isEssential, isDistracting FROM app_info
                """)

                // Drop old table and rename new one
                database.execSQL("DROP TABLE app_info")
                database.execSQL("ALTER TABLE app_info_new RENAME TO app_info")

                // Update launcher_settings table
                database.execSQL("ALTER TABLE launcher_settings ADD COLUMN backgroundColor TEXT NOT NULL DEFAULT 'system'")
                database.execSQL("ALTER TABLE launcher_settings ADD COLUMN enableHapticFeedback INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add showWallpaper field to launcher_settings
                database.execSQL("ALTER TABLE launcher_settings ADD COLUMN showWallpaper INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new fields to launcher_settings
                database.execSQL("ALTER TABLE launcher_settings ADD COLUMN enableTimeLimitPrompt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE launcher_settings ADD COLUMN enableMathChallenge INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE launcher_settings ADD COLUMN mathDifficulty TEXT NOT NULL DEFAULT 'easy'")

                // Create app_sessions table
                database.execSQL("""
                    CREATE TABLE app_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        packageName TEXT NOT NULL,
                        plannedDurationMinutes INTEGER NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER,
                        isActive INTEGER NOT NULL DEFAULT 1
                    )
                """)
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN sessionExpiryCountdownSeconds INTEGER NOT NULL DEFAULT 5"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN recentAppsLimit INTEGER NOT NULL DEFAULT 10"
                )
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN showPhoneAction INTEGER NOT NULL DEFAULT 1"
                )
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN showMessageAction INTEGER NOT NULL DEFAULT 1"
                )
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN showWhatsAppAction INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN weatherDisplay TEXT NOT NULL DEFAULT 'off'"
                )
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN weatherLocationLat REAL"
                )
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN weatherLocationLon REAL"
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN weatherTemperatureUnit TEXT NOT NULL DEFAULT 'celsius'"
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN buildCommitHash TEXT"
                )
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN buildCommitMessage TEXT"
                )
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN buildCommitDate TEXT"
                )
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN buildBranch TEXT"
                )
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN buildTime TEXT"
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN defaultTimeLimitMinutes INTEGER NOT NULL DEFAULT 30"
                )
                database.execSQL(
                    "ALTER TABLE app_info ADD COLUMN timeLimitMinutes INTEGER"
                )
            }
        }

        fun getDatabase(context: Context): LauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LauncherDatabase::class.java,
                    "launcher_database"
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
