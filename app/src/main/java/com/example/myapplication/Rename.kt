package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import android.content.Context

sealed class Screen(val title: String, val icon: ImageVector) {
    object Tasks : Screen("Tasks", Icons.Default.List)
    object Focus : Screen("Focus", Icons.Default.Check)
}


fun Context.saveTaskValue(key: String, value: Int) {
    getSharedPreferences("task_prefs", Context.MODE_PRIVATE)
        .edit()
        .putInt(key, value)
        .apply()
}

fun Context.getTaskValue(key: String): Int {
    return getSharedPreferences("task_prefs", Context.MODE_PRIVATE)
        .getInt(key, 0)
}

@Composable
fun Renaem() {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Tasks) }
    var focusedTask by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(Screen.Tasks, Screen.Focus).forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen }
                    )
                }
            }
        }
    ) { innerPadding ->

        // ✅ Proper padding applied here
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedScreen) {
                is Screen.Tasks -> TodoApp(setFocus = { focusedTask = it })
                is Screen.Focus -> FocusScreen(focusedTask)
            }
        }
    }
}
