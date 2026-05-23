package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteLocationDao {
    @Query("SELECT * FROM favorite_locations ORDER BY addedTime DESC")
    fun getAllFavorites(): Flow<List<FavoriteLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(location: FavoriteLocation)

    @Delete
    suspend fun deleteFavorite(location: FavoriteLocation)

    @Query("DELETE FROM favorite_locations WHERE name = :name")
    suspend fun deleteFavoriteByName(name: String)
}

@Dao
interface PlannerEventDao {
    @Query("SELECT * FROM planner_events ORDER BY dayOffset ASC, hour ASC, minute ASC")
    fun getAllEvents(): Flow<List<PlannerEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: PlannerEvent): Long

    @Delete
    suspend fun deleteEvent(event: PlannerEvent)

    @Query("UPDATE planner_events SET isCompleted = :completed WHERE id = :id")
    suspend fun updateEventStatus(id: Int, completed: Boolean)

    @Query("SELECT * FROM planner_events WHERE dayOffset = 0 AND isCompleted = 0 ORDER BY hour ASC, minute ASC")
    suspend fun getTodayEventsSync(): List<PlannerEvent>
}

@Database(entities = [FavoriteLocation::class, PlannerEvent::class], version = 2, exportSchema = false)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun favoriteLocationDao(): FavoriteLocationDao
    abstract fun plannerEventDao(): PlannerEventDao

    companion object {
        @Volatile
        private var INSTANCE: WeatherDatabase? = null

        fun getDatabase(context: Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "skyline_weather_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
