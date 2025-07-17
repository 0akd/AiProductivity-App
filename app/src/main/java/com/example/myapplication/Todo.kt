@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication

import androidx.compose.material.icons.filled.*
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.combinedClickable
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.alpha

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

@OptIn(ExperimentalMaterial3Api::class)

enum class TodoType(val label: String, val icon: String) {
    CHECKLIST("Checklist", "✓"),
    COUNTER("Counter", "#"),
    TIMER("Timer", "⏱")
}

@Serializable
data class ChecklistItem(
    val text: String,
    val checked: Boolean = false
)

@Serializable
data class Task(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val description: String = "",
    val isDone: Boolean = false,
    val type: TodoType = TodoType.CHECKLIST,
    val counterValue: Int = 0,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val timerSeconds: Int = 0
)

class TaskStorage(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("todo_tasks", Context.MODE_PRIVATE)
    private val taskPrefs: SharedPreferences =
        context.getSharedPreferences("task_prefs", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    fun saveTasks(tasks: List<Task>) {
        val tasksJson = json.encodeToString(tasks)
        sharedPreferences.edit().putString("tasks_list", tasksJson).apply()
    }

    fun loadTasks(): List<Task> {
        val tasksJson = sharedPreferences.getString("tasks_list", null)
        return if (tasksJson != null) {
            try {
                json.decodeFromString<List<Task>>(tasksJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun saveTaskValue(key: String, value: Int) {
        taskPrefs.edit().putInt(key, value).apply()
    }

    fun getTaskValue(key: String): Int {
        return taskPrefs.getInt(key, 0)
    }

    fun saveChecklistState(taskId: Long, index: Int, checked: Boolean) {
        taskPrefs.edit().putBoolean("checklist_${taskId}_${index}", checked).apply()
    }

    fun getChecklistState(taskId: Long, index: Int): Boolean {
        return taskPrefs.getBoolean("checklist_${taskId}_${index}", false)
    }

    fun resetAllTaskData() {
        taskPrefs.edit().clear().apply()
    }
}

// Extension functions for cleaner code
fun Context.getTaskStorage() = TaskStorage(this)

@Composable
fun TaskItem(
    task: Task,
    onTaskToggle: (Task) -> Unit,
    onTaskDelete: (Task) -> Unit,
    onTaskEdit: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    setFocus: (Task) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (task.isDone) 0.6f else 1f,
        label = "Task Alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .alpha(alpha)
            .combinedClickable(
                onClick = { onTaskClick(task) },
                onLongClick = { setFocus(task) }
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDone)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Task header with type indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type indicator
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = task.type.icon,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = task.text,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                        color = if (task.isDone)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    IconButton(
                        onClick = { onTaskEdit(task) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { onTaskDelete(task) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Description section
            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (task.description.length > 50) {
                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded)
                                    Icons.Default.KeyboardArrowUp
                                else
                                    Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Description",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistDialog(
    task: Task,
    onClose: () -> Unit,
    onTaskUpdate: (Task) -> Unit,
    storage: TaskStorage
) {
    var currentItems by remember {
        mutableStateOf(
            task.checklistItems.mapIndexed { index, item ->
                item.copy(checked = storage.getChecklistState(task.id, index))
            }
        )
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text("${task.type.icon} ${task.text}")
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                items(currentItems.size) { index ->
                    val item = currentItems[index]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.checked)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.checked,
                                onCheckedChange = { isChecked ->
                                    currentItems = currentItems.toMutableList().apply {
                                        set(index, item.copy(checked = isChecked))
                                    }
                                    storage.saveChecklistState(task.id, index, isChecked)
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                                color = if (item.checked)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val updatedTask = task.copy(
                    checklistItems = currentItems,
                    isDone = currentItems.all { it.checked }
                )
                onTaskUpdate(updatedTask)
                onClose()
            }) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CounterDialog(
    task: Task,
    onClose: () -> Unit,
    storage: TaskStorage
) {
    var count by remember { mutableIntStateOf(storage.getTaskValue("counter_${task.id}")) }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text("${task.type.icon} ${task.text}")
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            count = maxOf(0, count - 1)
                            storage.saveTaskValue("counter_${task.id}", count)
                        }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    FilledTonalButton(
                        onClick = {
                            count = 0
                            storage.saveTaskValue("counter_${task.id}", count)
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }

                    FilledTonalButton(
                        onClick = {
                            count++
                            storage.saveTaskValue("counter_${task.id}", count)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Close")
            }
        }
    )
}

@Composable
fun TimerDialog(
    task: Task,
    onClose: () -> Unit,
    storage: TaskStorage
) {
    var seconds by remember { mutableIntStateOf(storage.getTaskValue("timer_${task.id}")) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000)
            seconds++
            storage.saveTaskValue("timer_${task.id}", seconds)
        }
    }

    AlertDialog(
        onDismissRequest = {
            isRunning = false
            onClose()
        },
        title = {
            Text("${task.type.icon} ${task.text}")
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = String.format("%02d:%02d", seconds / 60, seconds % 60),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { isRunning = !isRunning }
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Start"
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            isRunning = false
                            seconds = 0
                            storage.saveTaskValue("timer_${task.id}", seconds)
                        }
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                isRunning = false
                onClose()
            }) {
                Text("Close")
            }
        }
    )
}

@Composable
fun TaskTypeDropdown(
    selectedType: TodoType,
    onTypeSelected: (TodoType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = "${selectedType.icon} ${selectedType.label}",
            onValueChange = {},
            readOnly = true,
            label = { Text("Task Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TodoType.values().forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text("${type.icon} ${type.label}")
                    },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp(setFocus: (Task) -> Unit) {
    val context = LocalContext.current
    val storage = remember { context.getTaskStorage() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // State variables
    var tasks by remember { mutableStateOf(storage.loadTasks()) }
    var newTaskText by remember { mutableStateOf("") }
    var newTaskDescription by remember { mutableStateOf("") }
    var newTaskType by remember { mutableStateOf(TodoType.CHECKLIST) }
    var showInputFields by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var editedText by remember { mutableStateOf("") }
    var editedDescription by remember { mutableStateOf("") }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Dialog states
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var showChecklistDialog by remember { mutableStateOf(false) }
    var showCounterDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }

    // Save tasks whenever the tasks list changes
    LaunchedEffect(tasks) {
        storage.saveTasks(tasks)
    }

    // Helper function to create new task
    fun createNewTask(): Task {
        val newTask = Task(
            id = System.currentTimeMillis(),
            text = newTaskText.trim(),
            description = newTaskDescription.trim(),
            type = newTaskType
        )

        return when (newTaskType) {
            TodoType.CHECKLIST -> newTask.copy(
                checklistItems = listOf(
                    ChecklistItem("Task item 1", false),
                    ChecklistItem("Task item 2", false),
                    ChecklistItem("Task item 3", false)
                )
            )
            else -> newTask
        }
    }

    // Helper function to add new task
    fun addNewTask() {
        if (newTaskText.isNotBlank()) {
            val newTask = createNewTask()
            tasks = listOf(newTask) + tasks
            newTaskText = ""
            newTaskDescription = ""
            newTaskType = TodoType.CHECKLIST
            keyboardController?.hide()
            showInputFields = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "My Tasks",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${tasks.size} tasks • ${tasks.count { it.isDone }} completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Add Task Button
        FilledTonalButton(
            onClick = { showInputFields = !showInputFields },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (showInputFields) "Hide Task Form" else "Add New Task")
        }

        // Add Task Form
        AnimatedVisibility(visible = showInputFields) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = newTaskText,
                        onValueChange = { newTaskText = it },
                        label = { Text("Task Title") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newTaskDescription,
                        onValueChange = { newTaskDescription = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { addNewTask() }
                        ),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TaskTypeDropdown(
                        selectedType = newTaskType,
                        onTypeSelected = { newTaskType = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                newTaskText = ""
                                newTaskDescription = ""
                                newTaskType = TodoType.CHECKLIST
                                showInputFields = false
                            }
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { addNewTask() },
                            enabled = newTaskText.isNotBlank()
                        ) {
                            Text("Add Task")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Task List
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📝",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No tasks yet!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add your first task above",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tasks.size) { index ->
                    val task = tasks[index]

                    if (editingTask?.id == task.id) {
                        // Edit Task Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = editedText,
                                    onValueChange = { editedText = it },
                                    label = { Text("Task Title") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = editedDescription,
                                    onValueChange = { editedDescription = it },
                                    label = { Text("Description") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { editingTask = null }
                                    ) {
                                        Text("Cancel")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            tasks = tasks.map {
                                                if (it.id == task.id) {
                                                    it.copy(
                                                        text = editedText,
                                                        description = editedDescription
                                                    )
                                                } else it
                                            }
                                            editingTask = null
                                        }
                                    ) {
                                        Text("Save")
                                    }
                                }
                            }
                        }
                    } else {
                        TaskItem(
                            task = task,
                            onTaskToggle = { toggledTask ->
                                tasks = tasks.map {
                                    if (it.id == toggledTask.id) {
                                        it.copy(isDone = !it.isDone)
                                    } else it
                                }
                            },
                            onTaskDelete = { taskToDelete = it },
                            onTaskEdit = { taskToEdit ->
                                editingTask = taskToEdit
                                editedText = taskToEdit.text
                                editedDescription = taskToEdit.description
                            },
                            onTaskClick = { clickedTask ->
                                selectedTask = clickedTask
                                when (clickedTask.type) {
                                    TodoType.CHECKLIST -> showChecklistDialog = true
                                    TodoType.COUNTER -> showCounterDialog = true
                                    TodoType.TIMER -> showTimerDialog = true
                                }
                            },
                            setFocus = setFocus
                        )
                    }
                }
            }
        }

        // Reset Button
        if (tasks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All Tasks")
            }
        }
    }

    // Dialogs
    if (showChecklistDialog && selectedTask != null) {
        ChecklistDialog(
            task = selectedTask!!,
            onClose = {
                showChecklistDialog = false
                selectedTask = null
            },
            onTaskUpdate = { updatedTask ->
                tasks = tasks.map {
                    if (it.id == updatedTask.id) updatedTask else it
                }
            },
            storage = storage
        )
    }

    if (showCounterDialog && selectedTask != null) {
        CounterDialog(
            task = selectedTask!!,
            onClose = {
                showCounterDialog = false
                selectedTask = null
            },
            storage = storage
        )
    }

    if (showTimerDialog && selectedTask != null) {
        TimerDialog(
            task = selectedTask!!,
            onClose = {
                showTimerDialog = false
                selectedTask = null
            },
            storage = storage
        )
    }

    // Delete confirmation dialog
    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete '${taskToDelete!!.text}'?") },
            confirmButton = {
                TextButton(onClick = {
                    tasks = tasks.filter { it.id != taskToDelete!!.id }
                    taskToDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Tasks") },
            text = { Text("This will reset all counters, timers, and checklist items. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    // Reset all task states
                    tasks = tasks.map { it.copy(isDone = false) }
                    // Clear all stored values
                    storage.resetAllTaskData()
                    showResetDialog = false
                }) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}