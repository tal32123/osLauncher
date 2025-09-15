package com.talauncher.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.talauncher.data.model.AppInfo
import com.talauncher.data.model.LauncherSettings

@Database(
    entities = [AppInfo::class, LauncherSettings::class],
    version = 4,
    exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun settingsDao(): SettingsDao

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

        fun getDatabase(context: Context): LauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LauncherDatabase::class.java,
                    "launcher_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
                INSTANCE = instance
                instance
            }
        }
    }
}