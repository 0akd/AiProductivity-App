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

    // Add shared URL state
    private var sharedYoutubeUrl by mutableStateOf<String?>(null)

    // Add these properties to store notification data
    private var notificationProblemSlug by mutableStateOf<String?>(null)
    private var notificationProblemUrl by mutableStateOf<String?>(null)

    private lateinit var cardNotificationManager: CardNotificationManager

    // Add this function to extract YouTube URL from intent
    private fun handleSharedIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                        // Extract YouTube URL from shared text
                        val youtubeUrl = extractYouTubeUrl(sharedText)
                        if (youtubeUrl != null) {
                            sharedYoutubeUrl = youtubeUrl
                            Log.d("MainActivity", "Shared YouTube URL: $youtubeUrl")
                        }
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                intent.data?.toString()?.let { url ->
                    sharedYoutubeUrl = url
                    Log.d("MainActivity", "Opened YouTube URL: $url")
                }
            }
        }
    }

    // Helper function to extract YouTube URL
    private fun extractYouTubeUrl(text: String): String? {
        val patterns = listOf(
            "(?:https?://)?(?:www\\.)?youtube\\.com/watch\\?v=([\\w-]+)".toRegex(),
            "(?:https?://)?(?:www\\.)?youtu\\.be/([\\w-]+)".toRegex(),
            "(?:https?://)?(?:www\\.)?youtube\\.com/embed/([\\w-]+)".toRegex(),
            "(?:https?://)?(?:www\\.)?youtube\\.com/v/([\\w-]+)".toRegex()
        )

        for (pattern in patterns) {
            val matchResult = pattern.find(text)
            if (matchResult != null) {
                return text.substring(matchResult.range)
            }
        }
        return null
    }

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
                    cardNotificationManager.startNotifications()
                } else {
                    Toast.makeText(this, "Notification permission is required", Toast.LENGTH_LONG).show()
                }
            }
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

        // Handle both notification and shared intents
        handleNotificationClick(intent)
        handleSharedIntent(intent)

        cardNotificationManager = CardNotificationManager(this)
        requestNotificationPermission()
        cardNotificationManager.startNotifications()

        Checkout.preload(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        createNotificationChannel(this)
        createLeetCodeNotificationChannel(this)

        setContent {
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("theme_prefs", MODE_PRIVATE)
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("is_dark", true)) }

            var isPremium by remember { mutableStateOf(false) }
            val user = remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

            var showTextFileScreen by remember { mutableStateOf(false) }
            var textFileParams by remember { mutableStateOf<TextFileScreenParams?>(null) }

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

            CompositionLocalProvider(LocalThemeToggle provides ThemeToggle(isDarkTheme) {
                isDarkTheme = it
                prefs.edit().putBoolean("is_dark", it).apply()
            }) {
                MyApplicationTheme(darkTheme = isDarkTheme) {
                    MainScreen(
                        isPremium = isPremium,
                        notificationProblemSlug = notificationProblemSlug,
                        notificationProblemUrl = notificationProblemUrl,
                        sharedYoutubeUrl = sharedYoutubeUrl,
                        onNotificationHandled = {
                            notificationProblemSlug = null
                            notificationProblemUrl = null
                        },
                        onSharedUrlHandled = {
                            sharedYoutubeUrl = null
                        },
                        onProblemsLoaded = { problems ->
                            handleLeetCodeBootRescheduling(problems)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationClick(intent)
        handleSharedIntent(intent)
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


