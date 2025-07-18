@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication



import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
data class ThemeToggle(val isDark: Boolean, val toggle: (Boolean) -> Unit)

val LocalThemeToggle = compositionLocalOf {
    ThemeToggle(false) {}
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            var isDarkTheme by remember {
                mutableStateOf(prefs.getBoolean("is_dark", false))
            }

            CompositionLocalProvider(LocalThemeToggle provides ThemeToggle(isDarkTheme) {
                isDarkTheme = it
                prefs.edit().putBoolean("is_dark", it).apply()
            }) {
                MyApplicationTheme(darkTheme = isDarkTheme) {
                    // Your app content
                    val user = remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

                    LaunchedEffect(Unit) {
                        FirebaseAuth.getInstance().addAuthStateListener { auth ->
                            user.value = auth.currentUser
                        }
                    }

                    if (user.value == null) {
                        AuthScreen()
                    } else {
                        MainScreen()
                    }
                }
            }
        }
    }
}
// Hard-coded website URLs array
val websiteUrls = arrayOf(
    "https://google.com",
    "https://github.com",
    "https://stackoverflow.com",
    "https://youtube.com",
    "https://facebook.com",
    "https://twitter.com",
    "https://instagram.com",
    "https://linkedin.com",
    "https://reddit.com",
    "https://wikipedia.org",
    "https://amazon.com",
    "https://netflix.com",
    "https://spotify.com",
    "https://medium.com",
    "https://unstop.com"
)

// Function to extract website name from URL
fun extractWebsiteName(url: String): String {
    return try {
        val cleanUrl = url.replace("https://", "").replace("http://", "").replace("www.", "")
        val domain = cleanUrl.split("/")[0]
        val name = domain.split(".")[0]
        name.replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        "Website"
    }
}

@Composable
fun WebsiteButton(url: String, name: String) {
    val context = LocalContext.current

    Button(
        onClick = {
            try {
                println("Button clicked for: $name with URL: $url")

                val intent = Intent(context, FullscreenWebViewActivity::class.java).apply {
                    putExtra("url", url)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(intent)
                println("Activity started successfully")

            } catch (e: Exception) {
                println("Error starting activity: ${e.message}")
                e.printStackTrace()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = "Open $name",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun WebsitesList() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choose a Website to Open",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(websiteUrls) { url ->
                    val websiteName = extractWebsiteName(url)
                    WebsiteButton(url = url, name = websiteName)
                }
            }
        }
    }
}
@Composable
fun ThemeToggleButton() {
    val themeToggle = LocalThemeToggle.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        FloatingActionButton(
            onClick = {
                themeToggle.toggle(!themeToggle.isDark)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .zIndex(10f)
                .size(60.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) {
            Icon(
                imageVector = if (themeToggle.isDark) Icons.Default.Close else Icons.Default.Menu,
                contentDescription = "Toggle Theme"
            )
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
            url = "https://unstop.com/hackathons",
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

                    val screens = listOf("Home", "Todo","Scrape","Full")//"Portfolio","About Dev","Full","Journal","Ai",

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
//                    "Portfolio" -> WebsiteCardsView(
//                        websites = websites,
//                        onPageLoaded = { url -> /* handle page load */ },
//                        onError = { error -> /* handle error */ }
//                    )
//                    "About Dev" -> AboutDevScreenroller(
//                        onReturn = { currentScreen = "Home" }
//                    )
////                    "Full" -> WebsitesList()
//                    "Full"->HackathonListScreen()
//                    "Journal"->JournalScreen()
//                    "Ai"->ChatScreen()
                    "Scrape"->ScraperScreen()
                    "Full"->Hack()
                }
            }
            ThemeToggleButton()
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
        "Full" -> "🌐"
        else -> "📄"
    }
}

@Composable
fun AboutDevScreenroller(onReturn: () -> Unit) {
    val context = LocalContext.current
    var browserOpened by remember { mutableStateOf(false) }

    // Open browser once
    LaunchedEffect(Unit) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://arjundubey.vercel.app"))
        context.startActivity(intent)
        browserOpened = true
    }

    // Detect when user returns to the app
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, browserOpened) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && browserOpened) {
                onReturn() // Go to Home
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // UI while waiting
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Opening browser...")
    }
}

