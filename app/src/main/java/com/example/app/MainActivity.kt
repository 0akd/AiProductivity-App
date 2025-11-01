package com.arjundubey.app

import android.app.AlarmManager


import android.provider.Settings
import android.util.Log

import android.Manifest

import com.razorpay.Checkout
import com.razorpay.PaymentResultListener

import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode

import android.app.Activity
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import org.json.JSONObject

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Close
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
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
import com.arjundubey.app.ui.theme.MyApplicationTheme
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager

import android.os.Build

import androidx.compose.foundation.background
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import io.ktor.http.ContentType

data class ThemeToggle(val isDark: Boolean, val toggle: (Boolean) -> Unit)

val LocalThemeToggle = compositionLocalOf {
    ThemeToggle(false) {}
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "LeetCode Problems"
        val descriptionText = "Notifications for random LeetCode problems"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("leetcode_channel_id", name, importance).apply {
            description = descriptionText
        }

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
class MainActivity : ComponentActivity(), PaymentResultListener {
    var isPremiumPurchase = false

    // Add these properties to store notification data
    private var notificationProblemSlug by mutableStateOf<String?>(null)
    private var notificationProblemUrl by mutableStateOf<String?>(null)

    private lateinit var cardNotificationManager: CardNotificationManager

    private fun handleNotificationClick(intent: Intent?) {
        if (intent?.getBooleanExtra("from_notification", false) == true) {
            val problemSlug = intent.getStringExtra("problem_slug") ?: ""
            val problemUrl = intent.getStringExtra("problem_url") ?: ""
            val openProblemDetail = intent.getBooleanExtra("open_problem_detail", false)

            if (openProblemDetail && problemSlug.isNotEmpty()) {
                notificationProblemSlug = problemSlug
                notificationProblemUrl = problemUrl
            }
        }
    }

    // Add this helper function to handle boot rescheduling for LeetCode notifications
    private fun handleLeetCodeBootRescheduling(problems: List<EnhancedProblemStat>) {
        val prefs = getSharedPreferences("leetcode_notifications", MODE_PRIVATE)
        val needsReschedule = prefs.getBoolean("needs_reschedule_after_boot", false)

        if (needsReschedule && problems.isNotEmpty()) {
            scheduleHourlyNotifications(this, problems)
            Log.d("MainActivity", "Rescheduled LeetCode notifications after boot with fresh problem data")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, start notifications
                    cardNotificationManager.startNotifications()
                } else {
                    // Handle permission denial
                    Toast.makeText(this, "Notification permission is required", Toast.LENGTH_LONG).show()
                }
            }
            // Add handling for exact alarm permission (Android 12+)
            2 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = getSystemService(ALARM_SERVICE) as? AlarmManager
                    if (alarmManager?.canScheduleExactAlarms() == false) {
                        Toast.makeText(this, "Exact alarm permission is required for precise notifications", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

        // Also request exact alarm permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as? AlarmManager
            if (alarmManager?.canScheduleExactAlarms() == false) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Could not open exact alarm settings", e)
                    Toast.makeText(this, "Please enable exact alarm permission in settings", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        // Handle notification click BEFORE setContent
        handleNotificationClick(intent)

        cardNotificationManager = CardNotificationManager(this)

        // Request notification permission for Android 13+
        requestNotificationPermission()

        // Start notifications
        cardNotificationManager.startNotifications()

        Checkout.preload(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        createNotificationChannel(this)
//        createNotificationChanne(this)

        // Create LeetCode notification channel
        createLeetCodeNotificationChannel(this)

        // In your MainActivity onCreate() method, replace the setContent block:

        setContent {
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("theme_prefs", MODE_PRIVATE)
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("is_dark", true)) }

            var isPremium by remember { mutableStateOf(false) }
            val user = remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

            // Keep your existing state variables
            var showTextFileScreen by remember { mutableStateOf(false) }
            var textFileParams by remember { mutableStateOf<TextFileScreenParams?>(null) }

            // Your existing LaunchedEffects...
            LaunchedEffect(Unit) {
                FirebaseAuth.getInstance().addAuthStateListener { auth ->
                    user.value = auth.currentUser
                }
            }

            LaunchedEffect(user.value) {
                user.value?.email?.let { email ->
                    Premium.checkIfPremium(email) { isPremium = it }
                }
            }

            LaunchedEffect(user.value) {
                user.value?.email?.let { email ->
                    Premium.checkIfPremium(email) { result ->
                        isPremium = result
                    }
                }
            }

            CompositionLocalProvider(LocalThemeToggle provides ThemeToggle(isDarkTheme) {
                isDarkTheme = it
                prefs.edit().putBoolean("is_dark", it).apply()
            }) {
                MyApplicationTheme(darkTheme = isDarkTheme) {
                    // Use the navigation-enabled MainScreen
                    MainScreen(  // or MainScreen with navigation state
                        isPremium = isPremium,
                        notificationProblemSlug = notificationProblemSlug,
                        notificationProblemUrl = notificationProblemUrl,
                        onNotificationHandled = {
                            notificationProblemSlug = null
                            notificationProblemUrl = null
                        },
                        onProblemsLoaded = { problems ->
                            handleLeetCodeBootRescheduling(problems)
                        }
                    )
                }
            }
        }

// Don't forget to add these imports if using Navigation Compose:
// import androidx.navigation.compose.*
// import androidx.navigation.NavType
// import java.net.URLEncoder
// import java.net.URLDecoder
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationClick(intent)
    }

    override fun onPaymentSuccess(razorpayPaymentID: String) {
        Toast.makeText(this, "✅ Payment Successful", Toast.LENGTH_SHORT).show()

        val email = FirebaseAuth.getInstance().currentUser?.email ?: return

        if (isPremiumPurchase) {
            Premium.storePremiumUser(email) { success ->
                if (success) {
                    Toast.makeText(this, "🎉 Premium Activated!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Payment OK, but failed to activate premium", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "🙏 Thank you for your donation!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "❌ Payment Failed: $response", Toast.LENGTH_LONG).show()
    }
}
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isPremium: Boolean,
    notificationProblemSlug: String?,
    notificationProblemUrl: String?,
    onNotificationHandled: () -> Unit,
    onProblemsLoaded: (List<EnhancedProblemStat>) -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showTextFileScreen by remember { mutableStateOf(false) }
    var textFileParams by remember { mutableStateOf<TextFileScreenParams?>(null) }
    var fileSystemNavigationPath by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentScreen by remember { mutableStateOf("Problems") }

    // Viewer states
    var showYoutubePlayer by rememberSaveable { mutableStateOf(false) }
    var showPdfViewer by rememberSaveable { mutableStateOf(false) }
    var selectedYoutubeUrl by rememberSaveable { mutableStateOf("") }
    var selectedPdfUrl by rememberSaveable { mutableStateOf("") }
    var selectedPdfTitle by rememberSaveable { mutableStateOf("") }

    // NEW: Problem detail and search states
    var showProblemDetail by remember { mutableStateOf(false) }
    var selectedProblemSlug by remember { mutableStateOf("") }
    var selectedProblemUrl by remember { mutableStateOf("") }
    var showProblemSearch by remember { mutableStateOf(false) }
    var problemSearchQuery by remember { mutableStateOf("") }

    // Search states
    var youtubeSearchQuery by remember { mutableStateOf("") }
    var youtubeResults by remember { mutableStateOf(emptyList<SearchResult>()) }
    var youtubeIsLoading by remember { mutableStateOf(false) }
    var youtubeErrorMessage by remember { mutableStateOf<String?>(null) }
    var pdfSearchQuery by remember { mutableStateOf("") }
    var pdfResults by remember { mutableStateOf(emptyList<SearchResult>()) }
    var pdfIsLoading by remember { mutableStateOf(false) }
    var pdfErrorMessage by remember { mutableStateOf<String?>(null) }

    // NEW: Problem search specific states
    var problemYoutubeResults by remember { mutableStateOf(emptyList<SearchResult>()) }
    var problemYoutubeIsLoading by remember { mutableStateOf(false) }
    var problemYoutubeErrorMessage by remember { mutableStateOf<String?>(null) }
    var problemWebResults by remember { mutableStateOf(emptyList<SearchResult>()) }
    var problemWebIsLoading by remember { mutableStateOf(false) }
    var problemWebErrorMessage by remember { mutableStateOf<String?>(null) }

    // Existing LaunchedEffects
    LaunchedEffect(notificationProblemSlug) {
        if (notificationProblemSlug != null) {
            currentScreen = "Problems"
            ScreenPrefs.saveScreen(context, "Problems")
        }
    }

    LaunchedEffect(Unit) {
        currentScreen = ScreenPrefs.getSavedScreen(context)
    }

    LaunchedEffect(Unit) {
        val cachedProblems = getCachedProblems(context)
        val savedProblemIds = getSavedProblems(context)
        val filteredProblems = if (savedProblemIds.isNotEmpty()) {
            cachedProblems.filter { problem ->
                savedProblemIds.contains(problem.stat.question__title_slug) ||
                        savedProblemIds.contains(problem.stat.question_id.toString())
            }
        } else {
            cachedProblems
        }
        onProblemsLoaded(filteredProblems)
        Log.d("MainScreen", "Loaded ${filteredProblems.size} problems (${savedProblemIds.size} saved)")
    }

    val auth = remember { FirebaseAuth.getInstance() }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

    LaunchedEffect(Unit) {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            isLoggedIn = firebaseAuth.currentUser != null
        }
        auth.addAuthStateListener(authStateListener)
    }

    fun changeScreen(screen: String) {
        currentScreen = screen
        ScreenPrefs.saveScreen(context, screen)
        scope.launch { drawerState.close() }
    }

    // Handle back button logic
    fun handleBackPress() {
        when {
            showTextFileScreen -> {
                showTextFileScreen = false
                textFileParams = null
            }
            showYoutubePlayer && showProblemSearch -> {
                // Coming back from YouTube in problem search
                showYoutubePlayer = false
            }
            showProblemSearch -> {
                // Coming back from problem search to problem detail
                showProblemSearch = false
                problemSearchQuery = ""
                problemYoutubeResults = emptyList()
                problemWebResults = emptyList()
            }
            showProblemDetail -> {
                // Coming back from problem detail to DSA list
                showProblemDetail = false
                selectedProblemSlug = ""
                selectedProblemUrl = ""
            }
            showYoutubePlayer -> showYoutubePlayer = false
            showPdfViewer -> showPdfViewer = false
            else -> {
                // Default back behavior if needed
            }
        }
    }

    // Determine if back button should be shown
    val shouldShowBackButton = showTextFileScreen || showYoutubePlayer ||
            showPdfViewer || showProblemDetail || showProblemSearch

    // Handle TextFileScreen display
    if (showTextFileScreen && textFileParams != null) {
        TextFileScreen(
            courseName = textFileParams!!.courseName,
            subjectName = textFileParams!!.subjectName,
            chapterName = textFileParams!!.chapterName,
            fileName = textFileParams!!.fileName,
            onBackPressed = {
                showTextFileScreen = false
                textFileParams = null
            },
            onNavigateToFileSystem = { pathSegments ->
                showTextFileScreen = false
                textFileParams = null
                fileSystemNavigationPath = pathSegments
                currentScreen = "Courses"
                ScreenPrefs.saveScreen(context, "Courses")
            }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
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
                    HorizontalDivider()

                    val screens = if (isPremium) {
                        listOf("Home", "Tasks","Login/Signup")
                    } else {
                        listOf(
"ex","Music",
                            "Resume",
                            "DSA Problems",
                            "Search Engine",
                            "Hackathons",
                            "Jobs",
                            "Internships",
                            "Courses",
                            "Login/Signup",

                        )
                    }
                    screens.forEach { screen ->
                        DrawerButton(screen) {
                            changeScreen(screen)
                        }
                    }
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar with unified back button
            val screenTitles = mapOf(
                "Hackathons" to "Hackathons",
                "Courses" to "Content",
                "Internships" to "Internships",
                "Login/Signup" to "Login/Signup",
                "Home" to "Welcome Home",
                "DSA Problems" to "DSA Problems",
                "Resume" to "Resume",
                "Research" to "Know",
                "Jobs" to "Find Jobs",
                "Search Engine" to "Resource Extracter"
            )

            // Determine title based on what's showing
            val displayTitle = when {
                showProblemSearch && showYoutubePlayer -> "YouTube Solution"
                showProblemSearch -> "Problem Solutions"
                showProblemDetail -> "Problem Details"
                showYoutubePlayer -> "YouTube Player"
                showPdfViewer -> selectedPdfTitle.ifEmpty { "PDF Viewer" }
                else -> screenTitles[currentScreen] ?: "deArKs"
            }

            TopAppBar(
                title = { Text(displayTitle) },
                navigationIcon = {
                    // Show back button when viewer is active
                    if (shouldShowBackButton) {
                        IconButton(onClick = { handleBackPress() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                actions = {
                    val themeToggle = LocalThemeToggle.current
                    IconButton(
                        onClick = {
                            themeToggle.toggle(!themeToggle.isDark)
                        }
                    ) {
                        Icon(
                            imageVector = if (themeToggle.isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (themeToggle.isDark) "Switch to Light Mode" else "Switch to Dark Mode",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier.zIndex(1f)
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                modifier = Modifier
                    .height(100.dp)
                    .zIndex(999f),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 0.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            if (dragAmount > 100) {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open()
                                }
                            }
                        }
                    }
            ) {
                when (currentScreen) {

                    "ex"->YouTubeDownloaderScreen()
                    "Search Engine" -> {
                        when {
                            showYoutubePlayer -> {
                                YouTubeFullscreenScreen(
                                    videoUrl = selectedYoutubeUrl
                                )
                            }
                            showPdfViewer -> {
                                PdfViewerScreen(
                                    pdfUrl = selectedPdfUrl
                                )
                            }
                            else -> {
                                DualSearchScreen(
                                    youtubeSearchQuery = youtubeSearchQuery,
                                    onYoutubeSearchQueryChange = { youtubeSearchQuery = it },
                                    youtubeResults = youtubeResults,
                                    onYoutubeResultsChange = { youtubeResults = it },
                                    youtubeIsLoading = youtubeIsLoading,
                                    onYoutubeLoadingChange = { youtubeIsLoading = it },
                                    youtubeErrorMessage = youtubeErrorMessage,
                                    onYoutubeErrorMessageChange = { youtubeErrorMessage = it },
                                    onYoutubeClick = { url ->
                                        selectedYoutubeUrl = url
                                        showYoutubePlayer = true
                                    },
                                    pdfSearchQuery = pdfSearchQuery,
                                    onPdfSearchQueryChange = { pdfSearchQuery = it },
                                    pdfResults = pdfResults,
                                    onPdfResultsChange = { pdfResults = it },
                                    pdfIsLoading = pdfIsLoading,
                                    onPdfLoadingChange = { pdfIsLoading = it },
                                    pdfErrorMessage = pdfErrorMessage,
                                    onPdfErrorMessageChange = { pdfErrorMessage = it },
                                    onPdfClick = { url, title ->
                                        selectedPdfUrl = url
                                        selectedPdfTitle = title
                                        showPdfViewer = true
                                    }
                                )
                            }
                        }
                    }

                    "DSA Problems" -> {
                        when {
                            // Show YouTube player for problem search
                            showProblemSearch && showYoutubePlayer -> {
                                YouTubeFullscreenScreen(
                                    videoUrl = selectedYoutubeUrl
                                )
                            }
                            // Show problem search screen
                            showProblemSearch -> {
                                ProblemSearchScreen(
                                    initialQuery = problemSearchQuery,
                                    onBackClick = { handleBackPress() },
                                    onYoutubeClick = { url ->
                                        selectedYoutubeUrl = url
                                        showYoutubePlayer = true
                                    },
                                    onWebResultClick = { url ->
                                        // Open in external browser or internal webview
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    }
                                )
                            }
                            // Show problem detail screen
                            showProblemDetail -> {
                                ProblemDetailScreen(
                                    slug = selectedProblemSlug,
                                    url = selectedProblemUrl,

                                    onSearchClick = { query ->
                                        problemSearchQuery = query
                                        showProblemSearch = true
                                    }
                                )
                            }
                            // Show LeetCode list screen
                            else -> {
                                LeetCodeScreen(
                                    notificationProblemSlug = notificationProblemSlug,
                                    onNotificationHandled = onNotificationHandled,
                                    onProblemClick = { slug, url ->
                                        selectedProblemSlug = slug
                                        selectedProblemUrl = url
                                        showProblemDetail = true
                                    }
                                )
                            }
                        }
                    }

                    "Video" -> YouTubeFullscreenScreen(videoUrl = "https://youtu.be/gKRSIpPMTew?si=1uASzopqv3BfEDjL")

                    "Home" -> HomeScreen()
                    "Music" -> MP3PlayerScreen()
                    "Internships" -> ScraperJobScreen()
                    "Tasks" -> CardListManager()
                    "Jobs" -> JobSearchScreen()
                    "Hackathons" -> ScraperScreen()
                    "Resume" -> ResumeBuilderApp()
                    "Login/Signup" -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    ) {
                        AuthScreen(
                            onLoginSuccess = {
                                isLoggedIn = true
                            }
                        )
                    }
                    "admin" -> AdminScreen()
                    "course" -> CardDisplayScreen()
//                    "Ex" -> ExerciseTimerScreen()
                    "Steps" -> TileScreen()
                    "Courses" -> FileSystemScreen(
                        navigationCallback = object : FileNavigationCallback {
                            override fun navigateToTextFile(
                                courseName: String,
                                subjectName: String,
                                chapterName: String,
                                fileName: String
                            ) {
                                textFileParams = TextFileScreenParams(
                                    courseName = courseName,
                                    subjectName = subjectName,
                                    chapterName = chapterName,
                                    fileName = fileName
                                )
                                showTextFileScreen = true
                                fileSystemNavigationPath = listOf(courseName, subjectName, chapterName)
                                    .filter { it != "default_course" && it != "default_subject" && it != "default_chapter" }
                            }

                            override fun navigateBack() {
                                currentScreen = "Home"
                                ScreenPrefs.saveScreen(context, "Home")
                            }
                        },
                        initialPath = fileSystemNavigationPath
                    )
                    "Research" -> SearchLauncherScreen()
                }
            }
        }
    }
}

// Data class to hold text file parameters
data class TextFileScreenParams(
    val courseName: String,
    val subjectName: String,
    val chapterName: String,
    val fileName: String
)


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
        "Tasks" -> "📝"
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

@Composable
fun DonationScreen() {
    val context = LocalContext.current
    var amount by remember { mutableStateOf("1") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Donate To Arjun", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹)") })
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact") })

        Button(
            onClick = {
                val amt = amount.toIntOrNull()
                if (amt != null && amt > 0 && email.isNotBlank()) {
                    // Mark this as a donation
                    (context as? MainActivity)?.isPremiumPurchase = false
                    startPayment(
                        context = context,
                        amount = amt * 100, // Razorpay expects amount in paise
                        name = name,
                        email = email,
                        contact = contact
                    )
                } else {
                    Toast.makeText(context, "Please enter valid details", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors= ButtonDefaults.buttonColors(MaterialTheme.colorScheme.onSecondaryContainer)
        ) {
            Text("Donate ₹$amount")

        }

    }
}

fun startPayment(
    context: Context,
    amount: Int,
    name: String,
    email: String,
    contact: String
) {
    val activity = context as? Activity ?: return
    val checkout = Checkout()
    checkout.setKeyID("rzp_live_QulPR0m620bxEF") // Same as your frontend key

    val client = OkHttpClient()
    val json = """
        {
            "amount": $amount
        }
    """.trimIndent()

    val requestBody = json.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url("https://www.arjundubey.com/api/create-razorpay-order") // ✅ your working Next.js API
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            activity.runOnUiThread {
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        override fun onResponse(call: Call, response: Response) {
            val responseBody = response.body?.string()
            if (!response.isSuccessful || responseBody == null) {
                activity.runOnUiThread {
                    Toast.makeText(context, "Error: $responseBody", Toast.LENGTH_LONG).show()
                }
                return
            }

            val order = JSONObject(responseBody)
            val options = JSONObject().apply {
                put("name", "$name to Arjun Dubey")
                put("description", "Donation")
                put("currency", "INR")
                put("amount", order.getInt("amount"))
                put("order_id", order.getString("id"))
                put("prefill", JSONObject().apply {
                    put("email", email)
                    put("contact", contact)
                    put("name", name)
                })
            }

            activity.runOnUiThread {
                checkout.open(activity, options)
            }
        }
    })
}
@Composable
fun PremiumScreen(onPremiumActivated: () -> Unit = {}) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var contact by remember { mutableStateOf("") }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.email?.let {
            email = it
        }
    }

    Button(
        onClick = {
            val amount = 1
            if (email.isNotBlank()) {
                (context as? MainActivity)?.isPremiumPurchase = true
                startPayment(
                    context = context,
                    amount = amount * 100,
                    name = name,
                    email = email,
                    contact = contact
                )
                Premium.storePremiumUser(email) { success ->
                    if (success) {
                        Toast.makeText(context, "Premium Activated!", Toast.LENGTH_SHORT).show()
                        onPremiumActivated()
                    }
                }
            } else {
                Toast.makeText(context, "Email required", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Buy Premium ₹199")
    }

}