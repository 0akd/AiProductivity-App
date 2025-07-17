@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.DrawerState
import androidx.compose.material.icons.filled.Close
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.zIndex

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var currentScreen by remember {
        mutableStateOf(ScreenPrefs.getSavedScreen(context))
    }
    var focusedTask by remember { mutableStateOf<Task?>(null) }


    fun changeScreen(screen: String) {
        currentScreen = screen
        ScreenPrefs.saveScreen(context, screen)
        scope.launch { drawerState.close() }
    }
    val websites = listOf(
        WebsiteInfo(
            title = "Google",
            url = "https://www.google.com",
            description = "Search the world's information"
        ),
        WebsiteInfo(
            title = "Google",
            url = "https://arjundubey.vercel.app/",
            description = "Search the world's information"
        ),
        // ... more websites
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Cross button to close the drawer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { scope.launch { drawerState.close() } }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Drawer",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        "Menu",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    Divider()

                    val screens = listOf("Home", "Todo","Portfolio","About Dev")
                    screens.forEach { screen ->
                        DrawerButton(screen) {
                            changeScreen(screen)

                        }
                    }
                }
            }
        }

    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Floating Menu Icon
            IconButton(
                onClick = { scope.launch { drawerState.open() } },
                modifier = Modifier

                    .size(90.dp)
                    .align(Alignment.TopEnd)
                    .zIndex(1f) // Ensure it's above other content

                    .shadow(6.dp, shape = CircleShape)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
            }


            // Screen content below
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 0.dp) // Add top padding if needed to avoid overlap
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            if (dragAmount > 100) { // 🔥 Require at least 30 pixels swipe right
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open()
                                }
                            }
                        }
                    }
            ) {
                when (currentScreen) {
                    "Home" -> HomeScreen()
                    "Todo" -> Renaem()
                    "Portfolio"->WebsiteCardsView(
                        websites = websites,
                        onPageLoaded = { url -> /* handle page load */ },
                        onError = { error -> /* handle error */ }
                    )
                    "About Dev" -> {
                        val context = LocalContext.current
                        LaunchedEffect(Unit) {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://arjundubey.vercel.app")
                            )
                            context.startActivity(intent)
                        }
                        // Show some UI while opening
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Opening browser...")
                        }
                    }
                    // Add other screens here
                }
            }
        }

    }
    }


object ScreenPrefs {
    private const val PREF_NAME = "screen_prefs"
    private const val KEY_CURRENT_SCREEN = "current_screen"

    fun saveScreen(context: Context, screen: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENT_SCREEN, screen).apply()
    }

    fun getSavedScreen(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENT_SCREEN, "Home") ?: "Home"
    }
}


@Composable
fun DrawerButton(text: String, onClick: () -> Unit) {
    Text(
        text = "${getEmojiForScreen(text)} $text",
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
            .border(1.dp, Color.Gray, shape = MaterialTheme.shapes.medium)
            .padding(12.dp), // inner padding inside border
        style = MaterialTheme.typography.bodyLarge
    )
}
fun getEmojiForScreen(screen: String): String {
    return when (screen) {
        "Home" -> "🏠"
        "Todo" -> "📝"
        "Settings" -> "⚙️"
        "Portfolio" -> "ℹ️"
        else -> "📄"
    }
}





