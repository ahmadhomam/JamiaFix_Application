package com.jamiafix.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jamiafix.app.data.local.dao.CategoryDao
import com.jamiafix.app.data.local.dao.IssueDao
import com.jamiafix.app.data.local.dao.LocationDao
import com.jamiafix.app.data.local.entity.CategoryEntity
import com.jamiafix.app.data.local.entity.IssueEntity
import com.jamiafix.app.data.local.entity.LocationEntity

@Database(
    entities = [
        IssueEntity::class,
        CategoryEntity::class,
        LocationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun issueDao(): IssueDao
    abstract fun categoryDao(): CategoryDao
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jamiafix_local_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
