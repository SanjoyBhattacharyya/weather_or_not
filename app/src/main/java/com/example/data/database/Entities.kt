package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_locations")
data class FavoriteLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val addedTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "planner_events")
data class PlannerEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val hour: Int,       // 0 - 23
    val minute: Int,     // 0 - 59
    val dayOffset: Int,   // 0 for today, 1 for tomorrow
    val notes: String = "",
    val isCompleted: Boolean = false,
    val isReminderEnabled: Boolean = false,
    val reminderHour: Int = -1,     // If -1, defaults to event hour
    val reminderMinute: Int = -1,   // If -1, defaults to event minute
    val notificationTune: String = "Default" // Default, Cosmic Pip, Echo Beep, High Alert, Wave Chime
)
