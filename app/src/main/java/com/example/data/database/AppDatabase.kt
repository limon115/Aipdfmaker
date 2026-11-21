package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber

@Database(entities = [ProjectEntity::class, DocumentSnippetEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun documentSnippetDao(): DocumentSnippetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration placeholder - add actual schema changes when needed
        // shortcut: Current migration is a no-op for version 5. Add proper ALTER TABLE statements when schema changes.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from version 4 to 5")
                // No schema changes in this version - migration preserved for safety
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "docmorph_database"
                )
                .addMigrations(MIGRATION_4_5)
                // Fallback only for development - remove in production
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

