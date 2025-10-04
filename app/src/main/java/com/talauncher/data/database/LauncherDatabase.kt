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
import com.talauncher.data.model.SearchInteractionEntity

@Database(
    entities = [AppInfo::class, LauncherSettings::class, AppSession::class, SearchInteractionEntity::class, com.talauncher.data.model.NewsArticle::class],
    version = 23,
    exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun settingsDao(): SettingsDao
    abstract fun appSessionDao(): AppSessionDao
    abstract fun searchInteractionDao(): SearchInteractionDao
    abstract fun newsDao(): NewsDao

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
                    "ALTER TABLE launcher_settings ADD COLUMN weatherDisplay TEXT NOT NULL DEFAULT 'daily'"
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

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Remove isPinned and pinnedOrder columns from app_info table
                // SQLite doesn't support DROP COLUMN, so we need to recreate the table

                // Create new table without pinned columns
                database.execSQL("""
                    CREATE TABLE app_info_new (
                        packageName TEXT PRIMARY KEY NOT NULL,
                        appName TEXT NOT NULL,
                        isHidden INTEGER NOT NULL DEFAULT 0,
                        isDistracting INTEGER NOT NULL DEFAULT 0,
                        timeLimitMinutes INTEGER
                    )
                """)

                // Copy data from old table (excluding isPinned and pinnedOrder)
                database.execSQL("""
                    INSERT INTO app_info_new (packageName, appName, isHidden, isDistracting, timeLimitMinutes)
                    SELECT packageName, appName, isHidden, isDistracting, timeLimitMinutes FROM app_info
                """)

                // Drop old table and rename new one
                database.execSQL("DROP TABLE app_info")
                database.execSQL("ALTER TABLE app_info_new RENAME TO app_info")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "UPDATE launcher_settings SET mathDifficulty = UPPER(mathDifficulty) WHERE mathDifficulty IS NOT NULL"
                )
                database.execSQL(
                    "UPDATE launcher_settings SET weatherDisplay = UPPER(weatherDisplay) WHERE weatherDisplay IS NOT NULL"
                )
                database.execSQL(
                    "UPDATE launcher_settings SET weatherTemperatureUnit = UPPER(weatherTemperatureUnit) WHERE weatherTemperatureUnit IS NOT NULL"
                )
                database.execSQL(
                    "UPDATE launcher_settings SET colorPalette = UPPER(colorPalette) WHERE colorPalette IS NOT NULL"
                )
                database.execSQL(
                    "UPDATE launcher_settings SET uiDensity = UPPER(uiDensity) WHERE uiDensity IS NOT NULL"
                )
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'SYSTEM'"
                )
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN customColorOption TEXT"
                )
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS search_interactions (" +
                        "itemKey TEXT NOT NULL PRIMARY KEY," +
                        "lastUsedAt INTEGER NOT NULL" +
                        ")"
                )
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN appIconStyle TEXT NOT NULL DEFAULT 'BLACK_AND_WHITE'"
                )
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Update old MONOCHROME values to BLACK_AND_WHITE for both colorPalette and appIconStyle
                database.execSQL(
                    "UPDATE launcher_settings SET colorPalette = 'BLACK_AND_WHITE' WHERE colorPalette = 'MONOCHROME'"
                )
                database.execSQL(
                    "UPDATE launcher_settings SET appIconStyle = 'BLACK_AND_WHITE' WHERE appIconStyle = 'MONOCHROME'"
                )
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // From master: introduce pinned state on app_info
                database.execSQL(
                    "ALTER TABLE app_info ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0"
                )
                // From feature: sidebar customization columns
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN sidebarActiveScale REAL NOT NULL DEFAULT 1.4"
                )
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN sidebarPopOutDp INTEGER NOT NULL DEFAULT 16"
                )
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN sidebarWaveSpread REAL NOT NULL DEFAULT 1.5"
                )
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN fastScrollerActiveItemScale REAL NOT NULL DEFAULT 1.06"
                )
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN searchLayout TEXT NOT NULL DEFAULT 'LIST'"
                )
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN searchDisplayStyle TEXT NOT NULL DEFAULT 'ICON_AND_TEXT'"
                )
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN searchIconColor TEXT NOT NULL DEFAULT 'ORIGINAL'"
                )
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add news settings columns
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN newsRefreshInterval TEXT NOT NULL DEFAULT 'DAILY'"
                )
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN newsCategoriesCsv TEXT"
                )
                database.execSQL(
                    "ALTER TABLE launcher_settings ADD COLUMN newsLastFetchedAt INTEGER"
                )
                // Create news_articles table
                database.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS news_articles (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            title TEXT NOT NULL,
                            link TEXT NOT NULL,
                            category TEXT,
                            publishedAtMillis INTEGER NOT NULL
                        )
                    """.trimIndent()
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
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                    MIGRATION_20_21,
                    MIGRATION_21_22,
                    MIGRATION_22_23
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
