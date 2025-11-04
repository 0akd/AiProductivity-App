package com.arjundubey.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlin.text.ifEmpty


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




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isPremium: Boolean,
    notificationProblemSlug: String?,
    notificationProblemUrl: String?,
    sharedYoutubeUrl: String?,
    onNotificationHandled: () -> Unit,
    onSharedUrlHandled: () -> Unit,
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

    // Problem detail and search states
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

    // Problem search specific states
    var problemYoutubeResults by remember { mutableStateOf(emptyList<SearchResult>()) }
    var problemYoutubeIsLoading by remember { mutableStateOf(false) }
    var problemYoutubeErrorMessage by remember { mutableStateOf<String?>(null) }
    var problemWebResults by remember { mutableStateOf(emptyList<SearchResult>()) }
    var problemWebIsLoading by remember { mutableStateOf(false) }
    var problemWebErrorMessage by remember { mutableStateOf<String?>(null) }

    // Handle shared YouTube URL
    LaunchedEffect(sharedYoutubeUrl) {
        if (sharedYoutubeUrl != null) {
            currentScreen = "ex"
            ScreenPrefs.saveScreen(context, "ex")
            onSharedUrlHandled()
        }
    }

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
                showYoutubePlayer = false
            }
            showProblemSearch -> {
                showProblemSearch = false
                problemSearchQuery = ""
                problemYoutubeResults = emptyList()
                problemWebResults = emptyList()
            }
            showProblemDetail -> {
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
            // Top App Bar
            val screenTitles = mapOf(
                "ex" to "YouTube Downloader",
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
                    "ex" -> YouTubeDownloaderScreen(
                        initialUrl = sharedYoutubeUrl
                    )

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
                    "Music" -> TopicsCRUDScreen()
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