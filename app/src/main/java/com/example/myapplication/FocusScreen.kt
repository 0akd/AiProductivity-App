package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import kotlinx.coroutines.delay

@Composable
fun FocusScreen(focusedTask: Task?) {
    val context = LocalContext.current
    val counterValue = context.getTaskValue("counter_${focusedTask?.id}")
    val timerValue = context.getTaskValue("timer_${focusedTask?.id}")
    var isChecked by remember { mutableStateOf(focusedTask?.isDone ?: false) }
    var timerRunning by remember { mutableStateOf(false) }

    if (focusedTask == null) {
        Text("No task selected.")
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(focusedTask.text, style = MaterialTheme.typography.headlineMedium)
            Text(focusedTask.description)

            when (focusedTask.type) {
                TodoType.CHECKLIST -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mark as done")
                    }
                }

                TodoType.COUNTER -> {
                    var counter by remember { mutableIntStateOf(counterValue) }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = {
                            counter--
                            context.saveTaskValue("counter_${focusedTask.id}", counter)
                        }) { Text("-") }

                        Spacer(Modifier.width(16.dp))
                        Text(counter.toString(), style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(16.dp))

                        Button(onClick = {
                            counter++
                            context.saveTaskValue("counter_${focusedTask.id}", counter)
                        }) { Text("+") }
                    }
                }

                TodoType.TIMER -> {
                    var seconds by remember { mutableIntStateOf(timerValue) }

                    LaunchedEffect(timerRunning) {
                        while (timerRunning) {
                            delay(1000)
                            seconds++
                            context.saveTaskValue("timer_${focusedTask.id}", seconds)
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%02d:%02d", seconds / 60, seconds % 60),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { timerRunning = !timerRunning }) {
                            Text(if (timerRunning) "Stop" else "Start")
                        }
                    }
                }
            }
        }
    }
}
