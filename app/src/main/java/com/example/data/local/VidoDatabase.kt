package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.CallHistoryDao
import com.example.data.local.dao.GhostCallDao
import com.example.data.local.entity.CallHistoryEntity
import com.example.data.local.entity.GhostCallEntity

@Database(
    entities = [GhostCallEntity::class, CallHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VidoDatabase : RoomDatabase() {

    abstract fun ghostCallDao(): GhostCallDao
    abstract fun callHistoryDao(): CallHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: VidoDatabase? = null

        fun getDatabase(context: Context): VidoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VidoDatabase::class.java,
                    "vido_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
