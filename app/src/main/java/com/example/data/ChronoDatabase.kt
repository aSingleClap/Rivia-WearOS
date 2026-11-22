package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [TimerPresetEntity::class, LapRecordEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class ChronoDatabase : RoomDatabase() {
  abstract fun chronoDao(): ChronoDao

  companion object {
    @Volatile private var instance: ChronoDatabase? = null

    fun getInstance(context: Context): ChronoDatabase {
      return instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                ChronoDatabase::class.java,
                "wrist_chrono_watch.db",
              )
              .fallbackToDestructiveMigration(true)
              .build()
              .also { instance = it }
        }
    }
  }
}
