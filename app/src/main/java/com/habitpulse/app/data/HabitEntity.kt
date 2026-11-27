package com.habitpulse.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val timeSlot: String, // "Morning", "Afternoon", "Evening", "Anytime"
    val targetCount: Int = 1,
    val unit: String = "session",
    val activeDaysCsv: String = "MON,TUE,WED,THU,FRI,SAT,SUN",
    val currentProgress: Int = 0,
    val isCompletedToday: Boolean = false
)
