package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CreationEntity::class], version = 4, exportSchema = false)
abstract class CreationDatabase : RoomDatabase() {
    abstract fun creationDao(): CreationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE creations ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE creations ADD COLUMN seed INTEGER"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE creations ADD COLUMN tags TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        @Volatile
        private var instance: CreationDatabase? = null

        fun getInstance(context: Context): CreationDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CreationDatabase::class.java,
                    "lumina_creations.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
