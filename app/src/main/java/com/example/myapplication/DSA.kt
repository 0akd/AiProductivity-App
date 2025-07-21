// All necessary imports
package com.example.myapplication


// Required imports

import android.provider.Settings

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object QuestionArrays {
    val array1 = listOf(
        "find-the-duplicate-number",
        "sort-colors",
        "remove-duplicates-from-sorted-array",
        "set-matrix-zeroes",
        "move-zeroes",
        "best-time-to-buy-and-sell-stock",
        "maximum-subarray",
        "merge-intervals",
        "next-permutation",
        "pascals-triangle"
    )

    val array2 = listOf(
        "set-matrix-zeroes",
        "merge-sorted-array",
        "majority-element",
        "reverse-pairs",
        "print-all-possible-combinations-of-r-elements-in-a-given-array-of-size-n",
        "game-of-life",
        "max-value-of-equation",
        "insert-delete-getrandom-o1-duplicates-allowed",
        "rotate-image",
        "spiral-matrix"
    )

    val array3 = listOf(
        "two-sum",
        "three-sum",
        "four-sum",
        "container-with-most-water",
        "trapping-rain-water",
        "longest-substring-without-repeating-characters",
        "minimum-window-substring",
        "sliding-window-maximum",
        "find-all-anagrams-in-a-string",
        "longest-palindromic-substring"
    )

    val array4 = listOf(
        "binary-tree-inorder-traversal",
        "binary-tree-preorder-traversal",
        "binary-tree-postorder-traversal",
        "maximum-depth-of-binary-tree",
        "minimum-depth-of-binary-tree",
        "balanced-binary-tree",
        "path-sum",
        "binary-tree-level-order-traversal",
        "construct-binary-tree-from-preorder-and-inorder-traversal",
        "lowest-common-ancestor-of-a-binary-tree"
    )

    val array5 = listOf(
        "climbing-stairs",
        "house-robber",
        "coin-change",
        "longest-increasing-subsequence",
        "longest-common-subsequence",
        "edit-distance",
        "unique-paths",
        "minimum-path-sum",
        "decode-ways",
        "word-break"
    )

    val array6 = listOf(
        "valid-parentheses",
        "generate-parentheses",
        "minimum-remove-to-make-valid-parentheses",
        "longest-valid-parentheses",
        "remove-invalid-parentheses",
        "different-ways-to-add-parentheses",
        "score-of-parentheses",
        "valid-parenthesis-string",
        "minimum-add-to-make-parentheses-valid",
        "check-if-a-parentheses-string-can-be-valid"
    )

    val array7 = listOf(
        "reverse-linked-list",
        "linked-list-cycle",
        "merge-two-sorted-lists",
        "remove-nth-node-from-end-of-list",
        "add-two-numbers",
        "intersection-of-two-linked-lists",
        "palindrome-linked-list",
        "reverse-nodes-in-k-group",
        "copy-list-with-random-pointer",
        "lru-cache"
    )

    // Get all arrays with their indices
    fun getAllArrays(): List<Pair<Int, List<String>>> {
        return listOf(
            1 to array1,
            2 to array2,
            3 to array3,
            4 to array4,
            5 to array5,
            6 to array6,
            7 to array7
        )
    }
}

// Enhanced ProblemStat with priority and match info
@Serializable
data class EnhancedProblemStat(
    val stat: Stat,
    val status: String? = null,
    val difficulty: Difficulty,
    val paid_only: Boolean = false,
    val is_favor: Boolean = false,
    val frequency: Int = 0,
    val progress: Int = 0,
    val priority: Int = 0, // 0 means no match, 1-7 based on array match
    val matchPercentage: Double = 0.0,
    val matchedArray: Int = 0 // Which array it matched with
)

// Data classes for LeetCode API response
@Serializable
data class LeetCodeResponse(
    val user_name: String = "",
    val num_solved: Int = 0,
    val num_total: Int = 0,
    val ac_easy: Int = 0,
    val ac_medium: Int = 0,
    val ac_hard: Int = 0,
    val stat_status_pairs: List<ProblemStat>
)

@Serializable
data class ProblemStat(
    val stat: Stat,
    val status: String? = null,
    val difficulty: Difficulty,
    val paid_only: Boolean = false,
    val is_favor: Boolean = false,
    val frequency: Int = 0,
    val progress: Int = 0
)

@Serializable
data class Stat(
    val question_id: Int,
    val question__article__live: String? = null,
    val question__article__slug: String? = null,
    val question__article__has_video_solution: String? = null,
    val question__title: String,
    val question__title_slug: String,
    val question__hide: Boolean = false,
    val total_acs: Int = 0,
    val total_submitted: Int = 0,
    val frontend_question_id: Int,
    val is_new_question: Boolean = false
)

@Serializable
data class Difficulty(
    val level: Int
)

// String similarity calculation using Levenshtein distance
fun calculateSimilarity(str1: String, str2: String): Double {
    val longer = if (str1.length > str2.length) str1 else str2
    val shorter = if (str1.length > str2.length) str2 else str1

    if (longer.isEmpty()) return 1.0

    return (longer.length - levenshteinDistance(longer, shorter)) / longer.length.toDouble()
}

fun levenshteinDistance(str1: String, str2: String): Int {
    val dp = Array(str1.length + 1) { IntArray(str2.length + 1) }

    for (i in 0..str1.length) {
        for (j in 0..str2.length) {
            when {
                i == 0 -> dp[i][j] = j
                j == 0 -> dp[i][j] = i
                else -> {
                    val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                    dp[i][j] = minOf(
                        dp[i - 1][j] + 1,      // deletion
                        dp[i][j - 1] + 1,      // insertion
                        dp[i - 1][j - 1] + cost // substitution
                    )
                }
            }
        }
    }

    return dp[str1.length][str2.length]
}

// Function to match problems with arrays and assign priorities
fun matchProblemsWithArrays(problems: List<ProblemStat>): List<EnhancedProblemStat> {
    val enhancedProblems = mutableListOf<EnhancedProblemStat>()
    val arrays = QuestionArrays.getAllArrays()

    for (problem in problems) {
        val questionSlug = problem.stat.question__title_slug
        var bestMatch = 0.0
        var bestArrayIndex = 0
        var bestPriority = 0

        // Check against all arrays
        for ((arrayIndex, questionArray) in arrays) {
            for (arrayQuestion in questionArray) {
                val similarity = calculateSimilarity(questionSlug.lowercase(), arrayQuestion.lowercase())

                // If similarity is >= 70%, consider it a match
                if (similarity >= 0.7 && similarity > bestMatch) {
                    bestMatch = similarity
                    bestArrayIndex = arrayIndex
                    bestPriority = arrayIndex
                }
            }
        }

        val enhancedProblem = EnhancedProblemStat(
            stat = problem.stat,
            status = problem.status,
            difficulty = problem.difficulty,
            paid_only = problem.paid_only,
            is_favor = problem.is_favor,
            frequency = problem.frequency,
            progress = problem.progress,
            priority = bestPriority,
            matchPercentage = bestMatch * 100,
            matchedArray = bestArrayIndex
        )

        enhancedProblems.add(enhancedProblem)

        if (bestPriority > 0) {
            Log.d("ProblemMatcher", "Matched '${questionSlug}' with array $bestArrayIndex (${bestMatch * 100}% similarity)")
        }
    }

    // Sort by priority (higher first), then by match percentage, then by question ID
    return enhancedProblems.sortedWith(compareByDescending<EnhancedProblemStat> { it.priority }
        .thenByDescending { it.matchPercentage }
        .thenBy { it.stat.frontend_question_id })
}


private const val PREFS_NAME = "LeetCodePrefs"
private const val KEY_SAVED_PROBLEMS = "SavedProblems"
private const val KEY_CACHED_PROBLEMS = "CachedProblems"
private const val KEY_CHECKED_PROBLEMS = "CheckedProblems"
private const val NOTIFICATION_REQUEST_CODE = 1001

// Data classes (add these if they don't exist)




// Create notification channel

// SharedPreferences functions with explicit types
fun getSavedProblems(context: Context): Set<String> {
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = sharedPreferences.getString(KEY_SAVED_PROBLEMS, null)
    return if (json != null) {
        val type = object : TypeToken<Set<String>>() {}.type
        Gson().fromJson(json, type) ?: emptySet()
    } else {
        emptySet()
    }
}

fun saveProblems(context: Context, problemSlugs: Set<String>) {
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val json = Gson().toJson(problemSlugs)
    editor.putString(KEY_SAVED_PROBLEMS, json)
    editor.apply()
}

fun getCheckedMap(context: Context): MutableMap<String, Boolean> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_CHECKED_PROBLEMS, null) ?: return mutableMapOf()
    val type = object : TypeToken<MutableMap<String, Boolean>>() {}.type
    return Gson().fromJson(json, type) ?: mutableMapOf()
}

fun saveCheckedMap(context: Context, map: Map<String, Boolean>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val editor = prefs.edit()
    editor.putString(KEY_CHECKED_PROBLEMS, Gson().toJson(map))
    editor.apply()
}

// Function to cache problems for notifications
fun cacheProblemsForNotifications(context: Context, problems: List<EnhancedProblemStat>) {
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val json = Gson().toJson(problems)
    editor.putString(KEY_CACHED_PROBLEMS, json)
    editor.apply()
}

// Function to retrieve cached problems
fun getCachedProblems(context: Context): List<EnhancedProblemStat> {
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = sharedPreferences.getString(KEY_CACHED_PROBLEMS, null)
    return if (json != null) {
        try {
            val type = object : TypeToken<List<EnhancedProblemStat>>() {}.type
            Gson().fromJson<List<EnhancedProblemStat>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("CachedProblems", "Error parsing cached problems: ${e.message}")
            emptyList()
        }
    } else {
        emptyList()
    }
}

// Notification scheduling functions
fun scheduleHourlyNotifications(context: Context, problems: List<EnhancedProblemStat>) {
    // Cache problems FIRST - this is crucial
    cacheProblemsForNotifications(context, problems)


    val intent = Intent(context, LeetCodeNotificationReceiver::class.java).apply {
        action = "com.example.myapplication"
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        NOTIFICATION_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Send immediate notification with the cached problems
    sendLeetCodeNotification(context, problems)

    // Schedule to start 10 seconds from now and repeat every hour
    val startTime = System.currentTimeMillis() + 10_000


    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                startTime,
                pendingIntent
            )
        } else {
            Toast.makeText(context, "Exact alarm permission not granted. Please enable it in settings.", Toast.LENGTH_LONG).show()
            // Optionally guide the user to settings:
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intent)
        }
    } else {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            startTime,
            pendingIntent
        )
    }


    // Save scheduling state
    val prefs = context.getSharedPreferences("leetcode_notifications", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("notifications_scheduled", true).apply()

    Toast.makeText(context, "Hourly notifications scheduled! First in 10 seconds.", Toast.LENGTH_SHORT).show()
}

fun stopHourlyNotifications(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, LeetCodeNotificationReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        NOTIFICATION_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager.cancel(pendingIntent)

    // Save scheduling state
    val prefs = context.getSharedPreferences("leetcode_notifications", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("notifications_scheduled", false).apply()

    Toast.makeText(context, "Hourly notifications stopped!", Toast.LENGTH_SHORT).show()
}

fun isNotificationScheduled(context: Context): Boolean {
    val prefs = context.getSharedPreferences("leetcode_notifications", Context.MODE_PRIVATE)
    return prefs.getBoolean("notifications_scheduled", false)
}
fun createLeetCodeNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "LeetCode Notifications"
        val descriptionText = "Notifications for LeetCode problem reminders"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("leetcode_channel_id", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

// Main notification function
fun sendLeetCodeNotification(context: Context, problems: List<EnhancedProblemStat>? = null) {
    // Ensure channel exists before sending notification
    createLeetCodeNotificationChannel(context)

    // Get problems if not provided (for use in broadcast receiver)
    val problemsList = problems ?: getCachedProblems(context)

    if (problemsList.isEmpty()) {
        Toast.makeText(context, "No problems available for notification", Toast.LENGTH_SHORT).show()
        return
    }

    // Get checked problems from SharedPreferences
    val checkedMap = getCheckedMap(context)

    // Find the first unchecked problem (topmost in the list)
    val nextProblem = problemsList.firstOrNull { problem ->
        val slug = problem.stat.question__title_slug ?: ""
        checkedMap[slug] != true // Not checked or doesn't exist in map
    }

    if (nextProblem == null) {
        // All problems are completed - send completion notification
        sendCompletionNotification(context)
        return
    }

    val title = "${nextProblem.stat.frontend_question_id}. ${nextProblem.stat.question__title}"
    val slug = nextProblem.stat.question__title_slug ?: ""
    val url = "https://leetcode.com/problems/$slug/"

    val priorityText = if (nextProblem.priority > 0) {
        " [Priority ${nextProblem.priority}]"
    } else ""

    val difficultyText = when (nextProblem.difficulty.level) {
        1 -> "Easy"
        2 -> "Medium"
        3 -> "Hard"
        else -> "Unknown"
    }

    // Create intent to open the problem detail screen directly
    val intent = Intent(context, context.javaClass).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra("problem_slug", slug)
        putExtra("problem_url", url)
        putExtra("from_notification", true)
        putExtra("open_problem_detail", true)
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        System.currentTimeMillis().toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, "leetcode_channel_id")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Next LeetCode Problem!$priorityText")
        .setContentText("$title • $difficultyText")
        .setStyle(NotificationCompat.BigTextStyle().bigText(
            "$title\n" +
                    "Difficulty: $difficultyText\n" +
                    (if (nextProblem.priority > 0) "Priority: ${nextProblem.priority}\n" else "") +
                    "\nTap to open problem and start solving!"
        ))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

    val notificationManager = NotificationManagerCompat.from(context)

    try {
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(
                System.currentTimeMillis().toInt(),
                builder.build()
            )
            Log.d("LeetCodeNotification", "Notification sent for problem: $title")
        } else {
            Log.w("LeetCodeNotification", "Notifications are disabled for this app")
        }
    } catch (e: SecurityException) {
        Log.e("LeetCodeNotification", "SecurityException: ${e.message}")
    }
}

// Function to send completion notification
private fun sendCompletionNotification(context: Context) {
    val intent = Intent(context, context.javaClass).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        System.currentTimeMillis().toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, "leetcode_channel_id")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("🎉 Congratulations!")
        .setContentText("You've completed all your LeetCode problems!")
        .setStyle(NotificationCompat.BigTextStyle().bigText(
            "Amazing work! You've solved all the problems in your list.\n\n" +
                    "Consider adding more problems or reviewing previous ones to keep your skills sharp!"
        ))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

    val notificationManager = NotificationManagerCompat.from(context)

    try {
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(
                System.currentTimeMillis().toInt(),
                builder.build()
            )
            Log.d("LeetCodeNotification", "Completion notification sent")
        }
    } catch (e: SecurityException) {
        Log.e("LeetCodeNotification", "SecurityException: ${e.message}")
    }
}

// Function to send reminder notification when no cached problems
private fun sendReminderNotification(context: Context) {
    val intent = Intent(context, context.javaClass).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        System.currentTimeMillis().toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, "leetcode_channel_id")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("LeetCode Reminder")
        .setContentText("Open the app to load your problem list")
        .setStyle(NotificationCompat.BigTextStyle().bigText(
            "Your problem list needs to be refreshed.\n\n" +
                    "Open the app to load your problems and continue your coding journey!"
        ))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

    val notificationManager = NotificationManagerCompat.from(context)

    try {
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(
                System.currentTimeMillis().toInt(),
                builder.build()
            )
        }
    } catch (e: SecurityException) {
        Log.e("LeetCodeNotification", "SecurityException: ${e.message}")
    }
}

// Broadcast Receiver for handling scheduled notifications
class LeetCodeNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("LeetCodeNotification", "Hourly notification triggered")

        // Get cached problems
        val problems = getCachedProblems(context)

        if (problems.isNotEmpty()) {
            sendLeetCodeNotification(context, problems)
        } else {
            Log.w("LeetCodeNotification", "No cached problems available for notification")
            sendReminderNotification(context)
        }

        // 🔁 Reschedule the next alarm for 1 hour later
        scheduleNextAlarm(context)
    }

    private fun scheduleNextAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextTriggerTime = System.currentTimeMillis() + 60 * 60 * 1000 // 1 hour

        val intent = Intent(context, LeetCodeNotificationReceiver::class.java).apply {
            action = "com.yourapp.LEETCODE_NOTIFICATION"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerTime,
                    pendingIntent
                )
            } else {
                Log.w("LeetCodeNotification", "Cannot schedule exact alarm. Permission not granted.")
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTriggerTime,
                pendingIntent
            )
        }

        Log.d("LeetCodeNotification", "Next alarm scheduled in 1 hour")
    }
}


// Helper function to update problems cache
fun updateProblemsCache(context: Context, problems: List<EnhancedProblemStat>) {
    cacheProblemsForNotifications(context, problems)
    Log.d("LeetCodeNotification", "Cached ${problems.size} problems for notifications")
}

// Modified MainActivity handler for opening problem detail directly
fun handleNotificationClick(intent: Intent?, onOpenProblemDetail: (String, String) -> Unit) {
    if (intent?.getBooleanExtra("from_notification", false) == true) {
        val problemSlug = intent.getStringExtra("problem_slug") ?: ""
        val problemUrl = intent.getStringExtra("problem_url") ?: ""
        val openProblemDetail = intent.getBooleanExtra("open_problem_detail", false)

        if (openProblemDetail && problemSlug.isNotEmpty()) {
            onOpenProblemDetail(problemSlug, problemUrl)
        }
    }
}


// Data class to hold category information
data class ProblemCategory(
    val categoryTitle: String = "N/A",
    val isLoading: Boolean = false,
    val error: String? = null
)

// Function to fetch category title from LeetCode API
suspend fun fetchProblemCategory(slug: String): ProblemCategory {
    return try {
        val apiUrl = "https://leetcode-api-pied.vercel.app/problem/$slug"

        val response = withContext(Dispatchers.IO) {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}")
            }
        }

        val json = JSONObject(response)
        val categoryTitle = json.optString("categoryTitle", "N/A")

        ProblemCategory(categoryTitle = categoryTitle)

    } catch (e: Exception) {
        ProblemCategory(error = e.message)
    }
}
@Composable
fun LeetCodeNotificationButton(problems: List<EnhancedProblemStat>) {
    val context = LocalContext.current
    var isScheduled by remember { mutableStateOf(isNotificationScheduled(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                if (isScheduled) {
                    stopHourlyNotifications(context)
                    isScheduled = false
                } else {
                    // Pass the problems list here
                    scheduleHourlyNotifications(context, problems)
                    isScheduled = true
                }
            } else {
                Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Column {
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (granted) {
                        if (isScheduled) {
                            stopHourlyNotifications(context)
                            isScheduled = false
                        } else {
                            // Pass the problems list here
                            scheduleHourlyNotifications(context, problems)
                            isScheduled = true
                        }
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    if (isScheduled) {
                        stopHourlyNotifications(context)
                        isScheduled = false
                    } else {
                        // Pass the problems list here
                        scheduleHourlyNotifications(context, problems)
                        isScheduled = true
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = if (isScheduled) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            } else {
                ButtonDefaults.buttonColors()
            },
            enabled = problems.isNotEmpty() // Disable if no problems
        ) {
            Text(
                if (isScheduled) "Stop Hourly Notifications" else "Start Hourly Notifications"
            )
        }

        // Show status text
        if (problems.isEmpty()) {
            Text(
                text = "Load problems first to enable notifications",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}


// Modified LeetCodeScreen with category fetching
@Composable
fun LeetCodeScreen(
    notificationProblemSlug: String? = null,
    onNotificationHandled: () -> Unit = {}
) {
    var problems by remember { mutableStateOf<List<EnhancedProblemStat>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedProblem by remember { mutableStateOf<EnhancedProblemStat?>(null) }
    val context = LocalContext.current
    var checkedMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    LaunchedEffect(notificationProblemSlug, problems) {
        if (notificationProblemSlug != null && problems.isNotEmpty()) {
            val problemToOpen = problems.find {
                it.stat.question__title_slug == notificationProblemSlug
            }
            if (problemToOpen != null) {
                selectedProblem = problemToOpen
                onNotificationHandled() // Clear the notification slug
            }
        }
    }

    // Map to store category titles for each problem slug
    var categoryMap by remember { mutableStateOf<Map<String, ProblemCategory>>(emptyMap()) }

    LaunchedEffect(Unit) {
        try {
            isLoading = true
            errorMessage = null

            val fetchedProblems = fetchLeetCodeProblems()

            // Move context-dependent code here using the context we got earlier
            val savedSlugs = getSavedProblems(context).toMutableSet()
            val newProblems = fetchedProblems.filterNot { savedSlugs.contains(it.stat.question__title_slug) }

            if (newProblems.isNotEmpty()) {
                val newSlugs = newProblems.mapNotNull { it.stat.question__title_slug }
                savedSlugs.addAll(newSlugs)
                saveProblems(context, savedSlugs)
            }

            problems = matchProblemsWithArrays(fetchedProblems)
            updateProblemsCache(context, problems)
        } catch (e: Exception) {
            errorMessage = "Failed to load problems: ${e.message}"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    // Fetch categories for all problems when problems are loaded
    LaunchedEffect(problems) {
        if (problems.isNotEmpty()) {
            // Initialize loading state for all problems
            val loadingMap = problems.associate { problem ->
                problem.stat.question__title_slug!! to ProblemCategory(isLoading = true)
            }
            categoryMap = loadingMap

            // Fetch categories in parallel with limited concurrency
            val semaphore = Semaphore(5) // Limit to 5 concurrent requests

            coroutineScope {
                problems.mapNotNull { problem ->
                    problem.stat.question__title_slug?.let { slug ->
                        async {
                            semaphore.withPermit {
                                val category = fetchProblemCategory(slug)
                                categoryMap = categoryMap + (slug to category)
                            }
                        }
                    }
                }.awaitAll()
            }
        }
    }

    checkedMap = getCheckedMap(context)

    // Show detail screen if a problem is selected
    if (selectedProblem != null) {
        ProblemDetailScreen(
            slug = selectedProblem!!.stat.question__title_slug ?: "",
            url = "https://leetcode.com/problems/${selectedProblem!!.stat.question__title_slug}/",
            onBackClick = { selectedProblem = null }
        )
        return
    }

    // Show main screen
    Column(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading LeetCode problems...")
                        Text(
                            text = "Matching with priority arrays...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            text = "Check your internet connection and try again",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            problems.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No problems found")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    item {
                        val matchedCount = problems.count { it.priority > 0 }
                        Column {
                            Text(
                                text = "LeetCode Problems (${problems.size})",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Matched with arrays: $matchedCount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
LeetCodeNotificationButton(problems)
                        }
                    }

                    items(problems) { problem ->
                        val slug = problem.stat.question__title_slug ?: ""
                        val categoryInfo = categoryMap[slug] ?: ProblemCategory()

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedProblem = problem
                                },
                            shape = MaterialTheme.shapes.medium,
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = if (problem.priority > 0) {
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            } else {
                                CardDefaults.cardColors()
                            }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checkedMap[slug] == true,
                                        onCheckedChange = { isChecked ->
                                            val updated = checkedMap.toMutableMap()
                                            updated[slug] = isChecked
                                            checkedMap = updated
                                            saveCheckedMap(context, updated)
                                        }
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${problem.stat.frontend_question_id}. ${problem.stat.question__title}",
                                            style = MaterialTheme.typography.titleMedium
                                        )

                                        // Display category title
                                        when {
                                            categoryInfo.isLoading -> {
                                                Text(
                                                    text = "Loading category...",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            categoryInfo.error != null -> {
                                                Text(
                                                    text = "Category: Unable to load",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                            categoryInfo.categoryTitle != "N/A" -> {
                                                Text(
                                                    text = "Category: ${categoryInfo.categoryTitle}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.padding(vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (problem.priority > 0) {
                                            Text(
                                                text = "Priority ${problem.priority} • ${String.format("%.1f", problem.matchPercentage)}% match",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Row {
                                        if (problem.priority > 0) {
                                            Text(
                                                text = "⭐",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                        if (problem.paid_only) {
                                            Text(
                                                text = "🔒",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Difficulty: ${when (problem.difficulty.level) {
                                            1 -> "Easy"
                                            2 -> "Medium"
                                            3 -> "Hard"
                                            else -> "Unknown"
                                        }}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = when (problem.difficulty.level) {
                                            1 -> Color.Green
                                            2 -> Color(0xFFFF9800)
                                            3 -> Color.Red
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )

                                    Text(
                                        text = "AC: ${problem.stat.total_acs}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (problem.paid_only) {
                                        Text(
                                            text = "Premium Only",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFFFD700)
                                        )
                                    }

                                    if (problem.stat.is_new_question) {
                                        Text(
                                            text = "New Problem",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
suspend fun fetchLeetCodeProblems(): List<ProblemStat> = withContext(Dispatchers.IO) {
    val problems = mutableListOf<ProblemStat>()

    try {
        Log.d("FetchProblems", "Making HTTP request to LeetCode API...")

        val url = URL("https://leetcode.com/api/problems/all/")
        val connection = url.openConnection() as HttpURLConnection

        connection.apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            setRequestProperty("Cache-Control", "no-cache")
        }

        val responseCode = connection.responseCode
        Log.d("FetchProblems", "Response code: $responseCode")

        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d("FetchProblems", "Response received, length: ${response.length}")

            try {
                val json = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }

                val leetCodeResponse = json.decodeFromString<LeetCodeResponse>(response)
                Log.d("FetchProblems", "JSON parsed successfully. Found ${leetCodeResponse.stat_status_pairs.size} problems")

                val filteredProblems = leetCodeResponse.stat_status_pairs.take(1000)
                problems.addAll(filteredProblems)
                Log.d("FetchProblems", "Added ${filteredProblems.size} problems after filtering")

            } catch (jsonException: Exception) {
                Log.e("FetchProblems", "JSON parsing failed", jsonException)
                throw Exception("Failed to parse LeetCode response: ${jsonException.message}")
            }

        } else {
            val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
            Log.e("FetchProblems", "HTTP error $responseCode: $errorResponse")
            throw Exception("HTTP $responseCode: Failed to fetch from LeetCode API")
        }

        connection.disconnect()

    } catch (e: Exception) {
        Log.e("FetchProblems", "Network request failed", e)
        throw Exception("Network error: ${e.message}")
    }

    Log.d("FetchProblems", "Returning ${problems.size} problems")
    return@withContext problems
}

