package com.habitpulse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.habitpulse.app.data.HabitEntity
import com.habitpulse.app.ui.HabitViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Emerald = Color(0xFF10B981)
private val LightBg = Color(0xFFFFFFFF)
private val CardBg = Color(0xFFF4F6F8)
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF6B7280)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val lightScheme = lightColorScheme(
                primary = Emerald,
                onPrimary = Color.White,
                background = LightBg,
                surface = LightBg,
                surfaceVariant = CardBg,
                onBackground = TextPrimary,
                onSurface = TextPrimary
            )
            MaterialTheme(colorScheme = lightScheme) {
                HabitPulseApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitPulseApp(vm: HabitViewModel = viewModel()) {
    val allHabits by vm.habits.collectAsState()
    var filterTodayOnly by remember { mutableStateOf(true) }
    var editingHabit by remember { mutableStateOf<HabitEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val todayCode = remember {
        LocalDate.now().dayOfWeek.name.take(3) // MON, TUE, ...
    }
    val dateLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    }

    val visibleHabits = remember(allHabits, filterTodayOnly, todayCode) {
        if (filterTodayOnly) {
            allHabits.filter { it.activeDaysCsv.contains(todayCode) }
        } else {
            allHabits
        }
    }

    val completedCount = visibleHabits.count { it.isCompletedToday }
    val completionRatio = if (visibleHabits.isEmpty()) 0f else completedCount.toFloat() / visibleHabits.size

    val timeSlots = listOf("Morning", "Afternoon", "Evening", "Anytime")

    Scaffold(
        containerColor = LightBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingHabit = null
                    showDialog = true
                },
                containerColor = Emerald,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(LightBg)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 88.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "HabitPulse",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Daily Progress", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(
                                    "$completedCount / ${visibleHabits.size} done",
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { completionRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = Emerald,
                                trackColor = Color(0xFFE5E7EB)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = filterTodayOnly,
                            onClick = { filterTodayOnly = true },
                            label = { Text("Today") }
                        )
                        FilterChip(
                            selected = !filterTodayOnly,
                            onClick = { filterTodayOnly = false },
                            label = { Text("All Habits") }
                        )
                    }
                }
            }

            timeSlots.forEach { slot ->
                val slotHabits = visibleHabits.filter { it.timeSlot == slot }
                if (slotHabits.isNotEmpty()) {
                    item {
                        Text(
                            text = slot,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(slotHabits, key = { it.id }) { habit ->
                        HabitCard(
                            habit = habit,
                            onToggle = { vm.toggleHabit(habit) },
                            onIncrement = { vm.adjustProgress(habit, +1) },
                            onDecrement = { vm.adjustProgress(habit, -1) },
                            onEdit = {
                                editingHabit = habit
                                showDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        HabitEditorDialog(
            initial = editingHabit,
            onDismiss = { showDialog = false },
            onSave = { saved ->
                vm.saveHabit(saved)
                showDialog = false
            },
            onDelete = editingHabit?.let { target ->
                {
                    vm.deleteHabit(target)
                    showDialog = false
                }
            }
        )
    }
}

@Composable
private fun HabitCard(
    habit: HabitEntity,
    onToggle: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (habit.isCompletedToday) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = "Complete",
                        tint = if (habit.isCompletedToday) Emerald else TextSecondary
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${habit.category} • ${habit.activeDaysCsv}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                if (habit.targetCount > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = onDecrement,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(34.dp)
                        ) { Text("-") }
                        Spacer(Modifier.width(6.dp))
                        FilledTonalButton(
                            onClick = onIncrement,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(34.dp)
                        ) { Text("+") }
                    }
                }
            }

            if (habit.targetCount > 1) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${habit.currentProgress} / ${habit.targetCount} ${habit.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    val pct = ((habit.currentProgress.toFloat() / habit.targetCount) * 100).toInt().coerceIn(0, 100)
                    Text("$pct%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Emerald)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (habit.currentProgress.toFloat() / habit.targetCount).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Emerald,
                    trackColor = Color(0xFFE5E7EB)
                )
            }
        }
    }
}

@Composable
private fun HabitEditorDialog(
    initial: HabitEntity?,
    onDismiss: () -> Unit,
    onSave: (HabitEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Health") }
    var timeSlot by remember { mutableStateOf(initial?.timeSlot ?: "Morning") }
    var targetText by remember { mutableStateOf((initial?.targetCount ?: 1).toString()) }
    var unit by remember { mutableStateOf(initial?.unit ?: "session") }
    val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    var selectedDays by remember {
        mutableStateOf(
            initial?.activeDaysCsv?.split(",")?.filter { it.isNotBlank() }?.toSet()
                ?: allDays.toSet()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (initial == null) "New Habit" else "Edit Habit")
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Habit Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Health, Study, Fitness...)") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { c -> c.isDigit() } },
                        label = { Text("Target") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Time Slot", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Morning", "Afternoon", "Evening", "Anytime").forEach { slot ->
                        FilterChip(
                            selected = timeSlot == slot,
                            onClick = { timeSlot = slot },
                            label = { Text(slot.take(4)) }
                        )
                    }
                }
                Text("Days", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    allDays.forEach { day ->
                        val active = selectedDays.contains(day)
                        FilterChip(
                            selected = active,
                            onClick = {
                                selectedDays = if (active && selectedDays.size > 1) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                            },
                            label = { Text(day.take(1)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            HabitEntity(
                                id = initial?.id ?: 0,
                                title = title.trim(),
                                category = category.trim().ifEmpty { "General" },
                                timeSlot = timeSlot,
                                targetCount = (targetText.toIntOrNull() ?: 1).coerceAtLeast(1),
                                unit = unit.trim().ifEmpty { "session" },
                                activeDaysCsv = allDays.filter { selectedDays.contains(it) }.joinToString(","),
                                currentProgress = initial?.currentProgress ?: 0,
                                isCompletedToday = initial?.isCompletedToday ?: false
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
