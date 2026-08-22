package com.clue2crew.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.clue2crew.app.data.local.dao.FamilyDao
import com.clue2crew.app.data.local.dao.FamilyMemberDao
import com.clue2crew.app.data.local.entities.FamilyEntity
import com.clue2crew.app.data.local.entities.FamilyMemberEntity

@Database(entities = [FamilyEntity::class, FamilyMemberEntity::class], version = 3, exportSchema = false)
abstract class Clue2CrewDatabase : RoomDatabase() {
    abstract fun familyDao(): FamilyDao
    abstract fun familyMemberDao(): FamilyMemberDao

    companion object {
        @Volatile
        private var INSTANCE: Clue2CrewDatabase? = null

        fun getDatabase(context: Context): Clue2CrewDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Clue2CrewDatabase::class.java,
                    "clue2crew_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
