package com.habitpulse.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.habitpulse.app.data.HabitDatabase
import com.habitpulse.app.data.HabitEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = HabitDatabase.getInstance(application).habitDao()

    val habits: StateFlow<List<HabitEntity>> = dao.getAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            if (dao.getCount() == 0) {
                dao.insertAll(
                    listOf(
                        HabitEntity(
                            title = "Morning Walk",
                            category = "Fitness",
                            timeSlot = "Morning",
                            targetCount = 11000,
                            unit = "steps",
                            activeDaysCsv = "MON,TUE,WED,THU,FRI,SAT",
                            currentProgress = 5500
                        ),
                        HabitEntity(
                            title = "Study Session",
                            category = "Study",
                            timeSlot = "Morning",
                            targetCount = 2,
                            unit = "hours",
                            activeDaysCsv = "MON,TUE,WED,THU,FRI,SAT,SUN",
                            currentProgress = 1
                        ),
                        HabitEntity(
                            title = "Drink Water",
                            category = "Health",
                            timeSlot = "Anytime",
                            targetCount = 8,
                            unit = "glasses",
                            activeDaysCsv = "MON,TUE,WED,THU,FRI,SAT,SUN",
                            currentProgress = 4
                        ),
                        HabitEntity(
                            title = "Read Book",
                            category = "Mindset",
                            timeSlot = "Evening",
                            targetCount = 1,
                            unit = "session",
                            activeDaysCsv = "MON,TUE,WED,THU,FRI,SAT,SUN",
                            currentProgress = 0
                        )
                    )
                )
            }
        }
    }

    fun toggleHabit(habit: HabitEntity) {
        viewModelScope.launch {
            val nextDone = !habit.isCompletedToday
            dao.updateHabit(
                habit.copy(
                    isCompletedToday = nextDone,
                    currentProgress = if (nextDone) habit.targetCount else 0
                )
            )
        }
    }

    fun adjustProgress(habit: HabitEntity, delta: Int) {
        viewModelScope.launch {
            val step = if (habit.targetCount >= 1000) 1000 else 1
            val updated = (habit.currentProgress + delta * step).coerceIn(0, habit.targetCount)
            dao.updateHabit(
                habit.copy(
                    currentProgress = updated,
                    isCompletedToday = updated >= habit.targetCount
                )
            )
        }
    }

    fun saveHabit(habit: HabitEntity) {
        viewModelScope.launch {
            if (habit.id == 0) dao.insertHabit(habit) else dao.updateHabit(habit)
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            dao.deleteHabit(habit)
        }
    }
}
