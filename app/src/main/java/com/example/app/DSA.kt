// All necessary imports
package com.arjundubey.app


// Required imports

import android.provider.Settings
import kotlinx.coroutines.yield
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.work.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object QuestionArrays {
    val array1 = listOf(
        // Arrays - EASY
        "two-sum",
        "best-time-to-buy-and-sell-stock",
        "plus-one",
        "move-zeroes",
        "best-time-to-buy-and-sell-stock-ii",
        "running-sum-of-1d-array",
        "find-pivot-index",
        "majority-element",
        "fibonacci-number",
        "squares-of-a-sorted-array",
        "pascals-triangle",
        "remove-duplicates-from-sorted-array",

        // Arrays - MEDIUM
        "merge-intervals",
        "3sum",
        "product-of-array-except-self",
        "insert-delete-getrandom-o1",
        "subarray-sum-equals-k",
        "next-permutation",
        "spiral-matrix",
        "container-with-most-water",
        "rotate-image",
        "word-search",
        "3sum-closest",
        "game-of-life",
        "pairs-of-songs-with-total-durations-divisible-by-60",
        "4sum",
        "find-the-duplicate-number",
        "combination-sum",
        "jump-game-ii",
        "maximum-points-you-can-obtain-from-cards",
        "maximum-area-of-a-piece-of-cake-after-horizontal-and-vertical-cuts",
        "max-area-of-island",
        "find-all-duplicates-in-an-array",
        "k-diff-pairs-in-an-array",
        "subsets",
        "invalid-transactions",
        "jump-game",
        "subarray-sums-divisible-by-k",

        // Arrays - HARD
        "first-missing-positive",
        "largest-rectangle-in-histogram",
        "insert-delete-getrandom-o1-duplicates-allowed",
        "best-time-to-buy-and-sell-stock-iii",
        "max-value-of-equation",

        // Recursion
        "powx-n",
        "valid-palindrome",
        "subsets",
        "permutations",
        "permutations-ii",
        "subsets-ii",
        "combinations",
        "combination-sum",
        "combination-sum-ii",
        "combination-sum-iii",
        "letter-combinations-of-a-phone-number",
        "partition-to-k-equal-sum-subsets",
        "maximum-length-of-a-concatenated-string-with-unique-characters",
        "flood-fill",
        "word-search",
        "n-queens",

        // Dynamic Programming - EASY
        "maximum-subarray",
        "climbing-stairs",
        "divisor-game",
        "counting-bits",

        // Dynamic Programming - MEDIUM
        "decode-ways",
        "word-break",
        "delete-and-earn",
        "maximal-square",
        "coin-change",
        "maximum-product-subarray",
        "maximum-length-of-repeated-subarray",
        "palindromic-substrings",
        "house-robber",
        "continuous-subarray-sum",
        "knight-dialer",
        "longest-increasing-subsequence",
        "unique-paths",
        "count-square-submatrices-with-all-ones",
        "range-sum-query-2d-immutable",
        "longest-arithmetic-subsequence",

        // Dynamic Programming - HARD
        "trapping-rain-water",
        "word-break-ii",
        "regular-expression-matching",
        "maximal-rectangle",
        "longest-valid-parentheses",
        "edit-distance",
        "minimum-difficulty-of-a-job-schedule",
        "frog-jump",
        "best-time-to-buy-and-sell-stock-iv",
        "burst-balloons",
        "minimum-cost-to-merge-stones",
        "minimum-insertion-steps-to-make-a-string-palindrome",
        "super-egg-drop",
        "count-different-palindromic-subsequences",
        "minimum-cost-to-cut-a-stick",

        // Strings - EASY
        "add-strings",
        "longest-common-prefix",
        "valid-palindrome-ii",
        "roman-to-integer",
        "implement-strstr",

        // Strings - MEDIUM
        "longest-substring-without-repeating-characters",
        "minimum-remove-to-make-valid-parentheses",
        "longest-palindromic-substring",
        "group-anagrams",
        "generate-parentheses",
        "basic-calculator-ii",
        "integer-to-roman",
        "reverse-words-in-a-string",
        "simplify-path",
        "zigzag-conversion",

        // Strings - HARD
        "text-justification",
        "integer-to-english-words",
        "minimum-window-substring",
        "valid-number",
        "distinct-subsequences",
        "smallest-range-covering-elements-from-k-lists",
        "substring-with-concatenation-of-all-words",

        // Maths - EASY
        "reverse-integer",
        "add-binary",
        "palindrome-number",
        "minimum-moves-to-equal-array-elements",
        "happy-number",
        "excel-sheet-column-title",
        "missing-number",
        "maximum-product-of-three-numbers",
        "power-of-two",

        // Maths - MEDIUM
        "encode-and-decode-tinyurl",
        "string-to-integer-atoi",
        "multiply-strings",
        "angle-between-hands-of-a-clock",
        "integer-break",
        "valid-square",
        "the-kth-factor-of-n",

        // Maths - HARD
        "basic-calculator",
        "max-points-on-a-line",
        "permutation-sequence",
        "number-of-digit-one",

        // Greedy - MEDIUM
        "task-scheduler",
        "gas-station",
        "minimum-deletion-cost-to-avoid-repeating-letters",
        "maximum-number-of-events-that-can-be-attended",
        "minimum-deletions-to-make-character-frequencies-unique",
        "remove-k-digits",
        "restore-the-array-from-adjacent-pairs",
        "non-overlapping-intervals",

        // Greedy - HARD
        "candy",
        "minimum-number-of-taps-to-open-to-water-a-garden",
        "create-maximum-number",

        // DFS - MEDIUM
        "letter-combinations-of-a-phone-number",
        "course-schedule-ii",
        "decode-string",
        "number-of-provinces",
        "clone-graph",
        "shortest-bridge",
        "all-paths-from-source-to-target",
        "surrounded-regions",
        "house-robber-iii",

        // DFS - HARD
        "critical-connections-in-a-network",
        "remove-invalid-parentheses",
        "longest-increasing-path-in-a-matrix",
        "concatenated-words",
        "making-a-large-island",
        "contain-virus",
        "24-game",
        "remove-boxes",

        // Tree - EASY
        "diameter-of-binary-tree",
        "invert-binary-tree",
        "subtree-of-another-tree",
        "range-sum-of-bst",
        "symmetric-tree",
        "convert-sorted-array-to-binary-search-tree",
        "merge-two-binary-trees",
        "maximum-depth-of-binary-tree",
        "binary-tree-paths",
        "same-tree",
        "lowest-common-ancestor-of-a-binary-search-tree",
        "path-sum",
        "minimum-absolute-difference-in-bst",
        "sum-of-left-leaves",
        "balanced-binary-tree",
        "binary-tree-inorder-traversal",

        // Tree - MEDIUM
        "count-good-nodes-in-binary-tree",
        "lowest-common-ancestor-of-a-binary-tree",
        "binary-tree-right-side-view",
        "all-nodes-distance-k-in-binary-tree",
        "validate-binary-search-tree",
        "binary-tree-zigzag-level-order-traversal",
        "binary-search-tree-iterator",
        "binary-tree-level-order-traversal",
        "path-sum-iii",
        "construct-binary-tree-from-preorder-and-postorder-traversal",
        "unique-binary-search-trees",
        "recover-binary-search-tree",
        "populating-next-right-pointers-in-each-node",
        "flatten-binary-tree-to-linked-list",
        "maximum-width-of-binary-tree",
        "unique-binary-search-trees-ii",
        "kth-smallest-element-in-a-bst",
        "redundant-connection",

        // Tree - HARD
        "serialize-and-deserialize-binary-tree",
        "binary-tree-maximum-path-sum",
        "vertical-order-traversal-of-a-binary-tree",
        "binary-tree-cameras",
        "sum-of-distances-in-tree",
        "number-of-ways-to-reconstruct-a-tree",
        "redundant-connection-ii",

        // Hash Table - EASY
        "verifying-an-alien-dictionary",
        "design-hashmap",

        // Hash Table - MEDIUM
        "top-k-frequent-elements",
        "design-twitter",

        // Binary Search - EASY
        "sqrtx",
        "binary-search",
        "count-negative-numbers-in-a-sorted-matrix",
        "peak-index-in-a-mountain-array",

        // Binary Search - MEDIUM
        "time-based-key-value-store",
        "search-in-rotated-sorted-array",
        "powx-n",
        "find-first-and-last-position-of-element-in-sorted-array",
        "find-peak-element",
        "search-a-2d-matrix",
        "divide-two-integers",
        "capacity-to-ship-packages-within-d-days",
        "minimum-limit-of-balls-in-a-bag",

        // Binary Search - HARD
        "median-of-two-sorted-arrays",
        "count-of-smaller-numbers-after-self",
        "max-sum-of-rectangle-no-larger-than-k",
        "split-array-largest-sum",
        "shortest-subarray-with-sum-at-least-k",

        // BFS - MEDIUM
        "number-of-islands",
        "rotting-oranges",
        "snakes-and-ladders",
        "is-graph-bipartite",
        "minimum-jumps-to-reach-home",

        // BFS - HARD
        "word-ladder",
        "word-ladder-ii",
        "cut-off-trees-for-golf-event",
        "reachable-nodes-in-subdivided-graph",

        // Two Pointer - MEDIUM/HARD
        "partition-labels",
        "sort-colors",
        "longest-repeating-character-replacement",
        "maximum-number-of-visible-points",
        "subarrays-with-k-different-integers",

        // Stack - EASY
        "min-stack",
        "next-greater-element-i",
        "backspace-string-compare",
        "implement-queue-using-stacks",
        "implement-stack-using-queues",

        // Stack - MEDIUM
        "remove-all-adjacent-duplicates-in-string-ii",
        "daily-temperatures",
        "flatten-nested-list-iterator",
        "online-stock-span",
        "minimum-cost-tree-from-leaf-values",
        "sum-of-subarray-minimums",
        "evaluate-reverse-polish-notation",

        // Design - MEDIUM/HARD
        "lru-cache",
        "find-median-from-data-stream",
        "design-underground-system",
        "lfu-cache",
        "tweet-counts-per-frequency",
        "all-oone-data-structure",
        "design-browser-history",

        // Graph - EASY
        "employee-importance",
        "find-the-town-judge",

        // Graph - MEDIUM
        "evaluate-division",
        "accounts-merge",
        "network-delay-time",
        "find-eventual-safe-states",
        "keys-and-rooms",
        "possible-bipartition",
        "most-stones-removed-with-same-row-or-column",
        "regions-cut-by-slashes",
        "satisfiability-of-equality-equations",
        "as-far-from-land-as-possible",
        "number-of-closed-islands",
        "number-of-operations-to-make-network-connected",
        "find-the-city-with-the-smallest-number-of-neighbors-at-a-threshold-distance",
        "time-needed-to-inform-all-employees",

        // Linked List
        "delete-node-in-a-linked-list",
        "middle-of-the-linked-list",
        "convert-binary-number-in-a-linked-list-to-integer",
        "design-hashset",
        "design-hashmap",
        "reverse-linked-list",
        "reverse-nodes-in-k-group",
        "merge-two-sorted-lists",
        "merge-k-sorted-lists",
        "remove-duplicates-from-sorted-list",
        "linked-list-cycle",
        "linked-list-cycle-ii",
        "intersection-of-two-linked-lists",
        "palindrome-linked-list",
        "remove-linked-list-elements",
        "design-browser-history",
        "lru-cache",
        "copy-list-with-random-pointer",

        // Heap - MEDIUM
        "k-closest-points-to-origin",
        "kth-largest-element-in-an-array",
        "reorganize-string",
        "furthest-building-you-can-reach",
        "kth-smallest-element-in-a-sorted-matrix",
        "cheapest-flights-within-k-stops",
        "find-the-most-competitive-subsequence",
        "ugly-number-ii",

        // Heap - HARD
        "merge-k-sorted-lists",
        "sliding-window-maximum",
        "the-skyline-problem",
        "trapping-rain-water-ii",
        "minimum-number-of-refueling-stops",
        "swim-in-rising-water",
        "shortest-path-to-get-all-keys",
        "minimum-cost-to-hire-k-workers",
        "k-th-smallest-prime-fraction",

        // Sliding Window - MEDIUM/HARD
        "longest-substring-with-at-least-k-repeating-characters",
        "max-consecutive-ones-iii",
        "grumpy-bookstore-owner",
        "sliding-window-median"
    )

//    val array2 = listOf(
//        "set-matrix-zeroes",
//        "merge-sorted-array",
//        "majority-element",
//        "reverse-pairs",
//        "print-all-possible-combinations-of-r-elements-in-a-given-array-of-size-n",
//        "game-of-life",
//        "max-value-of-equation",
//        "insert-delete-getrandom-o1-duplicates-allowed",
//        "rotate-image",
//        "spiral-matrix"
//    )
//
//    val array3 = listOf(
//        "two-sum",
//        "three-sum",
//        "four-sum",
//        "container-with-most-water",
//        "trapping-rain-water",
//        "longest-substring-without-repeating-characters",
//        "minimum-window-substring",
//        "sliding-window-maximum",
//        "find-all-anagrams-in-a-string",
//        "longest-palindromic-substring"
//    )
//
//    val array4 = listOf(
//        "binary-tree-inorder-traversal",
//        "binary-tree-preorder-traversal",
//        "binary-tree-postorder-traversal",
//        "maximum-depth-of-binary-tree",
//        "minimum-depth-of-binary-tree",
//        "balanced-binary-tree",
//        "path-sum",
//        "binary-tree-level-order-traversal",
//        "construct-binary-tree-from-preorder-and-inorder-traversal",
//        "lowest-common-ancestor-of-a-binary-tree"
//    )
//
//    val array5 = listOf(
//        "climbing-stairs",
//        "house-robber",
//        "coin-change",
//        "longest-increasing-subsequence",
//        "longest-common-subsequence",
//        "edit-distance",
//        "unique-paths",
//        "minimum-path-sum",
//        "decode-ways",
//        "word-break"
//    )
//
//    val array6 = listOf(
//        "valid-parentheses",
//        "generate-parentheses",
//        "minimum-remove-to-make-valid-parentheses",
//        "longest-valid-parentheses",
//        "remove-invalid-parentheses",
//        "different-ways-to-add-parentheses",
//        "score-of-parentheses",
//        "valid-parenthesis-string",
//        "minimum-add-to-make-parentheses-valid",
//        "check-if-a-parentheses-string-can-be-valid"
//    )
//
//    val array7 = listOf(
//        "reverse-linked-list",
//        "linked-list-cycle",
//        "merge-two-sorted-lists",
//        "remove-nth-node-from-end-of-list",
//        "add-two-numbers",
//        "intersection-of-two-linked-lists",
//        "palindrome-linked-list",
//        "reverse-nodes-in-k-group",
//        "copy-list-with-random-pointer",
//        "lru-cache"
//    )

    // Get all arrays with their indices
    fun getAllArrays(): List<Pair<Int, List<String>>> {
        return listOf(
            1 to array1,
//            2 to array2,
//            3 to array3,
//            4 to array4,
//            5 to array5,
//            6 to array6,
//            7 to array7
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


private const val PREFS_NAME = "LeetCodePrefs"
private const val KEY_SAVED_PROBLEMS = "SavedProblems"
private const val KEY_CACHED_PROBLEMS = "CachedProblems"
private const val KEY_CHECKED_PROBLEMS = "CheckedProblems"


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
class LeetCodeNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            // Get cached problems and send notification
            val cachedProblems = getCachedProblems(applicationContext)
            if (cachedProblems.isNotEmpty()) {
                sendLeetCodeNotification(applicationContext, cachedProblems)
            } else {
                // Send reminder to open app if no cached problems
                sendReminderNotification(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("LeetCodeWorker", "Error sending notification", e)
            Result.retry()
        }
    }
}
// Add this BootReceiver class to your code
// Add this BootReceiver class
class LeetCodeBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_PACKAGE_REPLACED) {

            Log.d("LeetCodeBootReceiver", "Device booted or app updated. Checking notification schedule...")

            // Check if notifications were previously scheduled
            val prefs = context.getSharedPreferences("leetcode_notifications", Context.MODE_PRIVATE)
            val wasScheduled = prefs.getBoolean("notifications_scheduled", false)

            if (wasScheduled) {
                // Get cached problems
                val cachedProblems = getCachedProblems(context)

                if (cachedProblems.isNotEmpty()) {
                    // Reschedule periodic notifications using WorkManager
                    scheduleHourlyNotifications(context, cachedProblems)
                    Log.d("LeetCodeBootReceiver", "LeetCode notifications restarted after boot with ${cachedProblems.size} problems")
                } else {
                    // Set a flag to reschedule when app is next opened and problems are loaded
                    prefs.edit().putBoolean("needs_reschedule_after_boot", true).apply()
                    Log.d("LeetCodeBootReceiver", "No cached problems found. Will reschedule when app is opened.")
                }
            } else {
                Log.d("LeetCodeBootReceiver", "Notifications were not previously scheduled. No action needed.")
            }

            // Also restart the CardNotificationManager if it was active
            val cardNotificationManager = CardNotificationManager(context)
            cardNotificationManager.startNotifications()
            Log.d("LeetCodeBootReceiver", "Card notifications also restarted")
        }
    }
}

// Constants for request codes
const val NOTIFICATION_REQUEST_CODE = 1001
const val LEETCODE_NOTIFICATION_REQUEST_CODE = 1002

// Update your existing scheduleHourlyNotifications function to handle the boot flag
fun scheduleHourlyNotifications(context: Context, problems: List<EnhancedProblemStat>) {
    // Cache problems FIRST - this is crucial
    cacheProblemsForNotifications(context, problems)

    // Send immediate notification with the cached problems (only if not from boot)
    val prefs = context.getSharedPreferences("leetcode_notifications", Context.MODE_PRIVATE)
    val isFromBoot = prefs.getBoolean("needs_reschedule_after_boot", false)

    if (!isFromBoot) {
        sendLeetCodeNotification(context, problems)
    }

    // Create constraints for the work
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .setRequiresBatteryNotLow(false)
        .build()

    // Create the periodic work request (minimum interval is 15 minutes)
    val notificationWorkRequest = PeriodicWorkRequestBuilder<LeetCodeNotificationWorker>(
        15, TimeUnit.MINUTES
    )
        .setConstraints(constraints)
        .setInitialDelay(15, TimeUnit.MINUTES) // First notification after 15 minutes
        .addTag("leetcode_notifications")
        .build()

    // Enqueue the work
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "leetcode_periodic_notifications",
        ExistingPeriodicWorkPolicy.REPLACE,
        notificationWorkRequest
    )

    // Save scheduling state and clear boot flag
    prefs.edit()
        .putBoolean("notifications_scheduled", true)
        .putBoolean("needs_reschedule_after_boot", false)
        .apply()

    val message = if (isFromBoot) {
        "Notifications rescheduled after boot/update!"
    } else {
        "Periodic notifications scheduled! Next in 15 minutes."
    }

    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

// Add this helper function to check and handle boot rescheduling in your main activity
fun handleBootRescheduling(context: Context, problems: List<EnhancedProblemStat>) {
    val prefs = context.getSharedPreferences("leetcode_notifications", Context.MODE_PRIVATE)
    val needsReschedule = prefs.getBoolean("needs_reschedule_after_boot", false)

    if (needsReschedule && problems.isNotEmpty()) {
        scheduleHourlyNotifications(context, problems)
        Log.d("LeetCodeApp", "Rescheduled notifications after boot with fresh problem data")
    }
}

// 3. Replace the stopHourlyNotifications function
fun stopHourlyNotifications(context: Context) {
    // Cancel the periodic work
    WorkManager.getInstance(context).cancelUniqueWork("leetcode_periodic_notifications")

    // Also cancel by tag as a backup
    WorkManager.getInstance(context).cancelAllWorkByTag("leetcode_notifications")

    // Save scheduling state
    val prefs = context.getSharedPreferences("leetcode_notifications", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("notifications_scheduled", false).apply()

    Toast.makeText(context, "Periodic notifications stopped!", Toast.LENGTH_SHORT).show()
}



fun isNotificationScheduled(context: Context): Boolean {
    val prefs = context.getSharedPreferences("leetcode_notifications", Context.MODE_PRIVATE)
    val prefScheduled = prefs.getBoolean("notifications_scheduled", false)

    // Also check WorkManager state
    val workManager = WorkManager.getInstance(context)
    val workInfos = workManager.getWorkInfosForUniqueWork("leetcode_periodic_notifications").get()
    val workScheduled = workInfos.isNotEmpty() &&
            workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }

    return prefScheduled && workScheduled
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
                    scheduleHourlyNotifications(context, problems)
                    isScheduled = true
                }
            } else {
                Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    FloatingActionButton(
        onClick = {
            if (problems.isEmpty()) {
                Toast.makeText(context, "Load problems first to enable notifications", Toast.LENGTH_SHORT).show()
                return@FloatingActionButton
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (granted) {
                    if (isScheduled) {
                        stopHourlyNotifications(context)
                        isScheduled = false
                        Toast.makeText(context, "Notifications stopped", Toast.LENGTH_SHORT).show()
                    } else {
                        scheduleHourlyNotifications(context, problems)
                        isScheduled = true
                        Toast.makeText(context, "Notifications started", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                if (isScheduled) {
                    stopHourlyNotifications(context)
                    isScheduled = false
                    Toast.makeText(context, "Notifications stopped", Toast.LENGTH_SHORT).show()
                } else {
                    scheduleHourlyNotifications(context, problems)
                    isScheduled = true
                    Toast.makeText(context, "Notifications started", Toast.LENGTH_SHORT).show()
                }
            }
        },
        modifier = Modifier.size(48.dp), // Smaller size for better fit
        containerColor = if (isScheduled) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
        contentColor = if (isScheduled) {
            MaterialTheme.colorScheme.onError
        } else {
            MaterialTheme.colorScheme.onPrimary
        }
    ) {
        Icon(
            imageVector = if (isScheduled) Icons.Default.NotificationsOff else Icons.Default.Notifications,
            contentDescription = if (isScheduled) "Stop Notifications" else "Start Notifications",
            modifier = Modifier.size(24.dp)
        )
    }
}
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




private const val KEY_LAST_FETCH_TIME = "LastFetchTime" // NEW
private const val KEY_CACHED_ENHANCED_PROBLEMS = "CachedEnhancedProblems" // NEW
private const val CACHE_DURATION = 24 * 60 * 60 * 1000 // 24 hours in milliseconds
// Save enhanced problems to cache
fun saveEnhancedProblemsToCache(context: Context, problems: List<EnhancedProblemStat>) {
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val json = Gson().toJson(problems)
    editor.putString(KEY_CACHED_ENHANCED_PROBLEMS, json)
    editor.putLong(KEY_LAST_FETCH_TIME, System.currentTimeMillis())
    editor.apply()
    Log.d("Cache", "Saved ${problems.size} enhanced problems to cache")
}
private suspend fun fetchLeetCodeProblemsWithRetry(
    maxRetries: Int = 3,
    initialDelay: Long = 1000
): List<ProblemStat> {
    var lastException: Exception? = null

    for (attempt in 1..maxRetries) {
        try {
            Log.d("Network", "Attempt $attempt to fetch problems")
            return fetchLeetCodeProblems()
        } catch (e: Exception) {
            lastException = e
            Log.w("Network", "Attempt $attempt failed: ${e.message}")

            if (attempt < maxRetries) {
                val delayTime = initialDelay * attempt
                Log.d("Network", "Retrying in ${delayTime}ms...")
                delay(delayTime)
            }
        }
    }

    throw lastException ?: Exception("Failed to fetch problems after $maxRetries attempts")
}
// Get cached enhanced problems
fun getCachedEnhancedProblems(context: Context): List<EnhancedProblemStat> {
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = sharedPreferences.getString(KEY_CACHED_ENHANCED_PROBLEMS, null)
    return if (json != null) {
        try {
            val type = object : TypeToken<List<EnhancedProblemStat>>() {}.type
            Gson().fromJson<List<EnhancedProblemStat>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e("Cache", "Error parsing cached enhanced problems: ${e.message}")
            emptyList()
        }
    } else {
        emptyList()
    }
}

// Check if cache is valid (less than 24 hours old)
fun isCacheValid(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val lastFetchTime = sharedPreferences.getLong(KEY_LAST_FETCH_TIME, 0)
    return System.currentTimeMillis() - lastFetchTime < CACHE_DURATION
}

// Clear cache (for force refresh)
fun clearCache(context: Context) {
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.remove(KEY_CACHED_ENHANCED_PROBLEMS)
    editor.remove(KEY_LAST_FETCH_TIME)
    editor.apply()
    Log.d("Cache", "Cache cleared")
}

// OPTIMIZED VERSION - Replace your existing functions with these
// Add these imports at the top of your file:
// import kotlinx.coroutines.yield
// import kotlinx.coroutines.launch
// import androidx.compose.runtime.rememberCoroutineScope

// 1. Optimized matching with HashSet lookup instead of nested loops
fun matchProblemsWithArrays(problems: List<ProblemStat>): List<EnhancedProblemStat> {
    val enhancedProblems = mutableListOf<EnhancedProblemStat>()
    val arrays = QuestionArrays.getAllArrays()

    // Create HashMaps for O(1) lookup instead of O(n) search
    val arrayMaps = arrays.map { (index, questions) ->
        index to questions.associateWith { it }
    }.toMap()

    for (problem in problems) {
        val questionSlug = problem.stat.question__title_slug?.lowercase() ?: ""
        var bestMatch = 0.0
        var bestArrayIndex = 0
        var bestPriority = 0

        // Check exact matches first (much faster)
        for ((arrayIndex, questionMap) in arrayMaps) {
            if (questionMap.containsKey(questionSlug)) {
                bestMatch = 100.0
                bestArrayIndex = arrayIndex
                bestPriority = arrayIndex
                break
            }
        }

        // Only do expensive similarity calculation if no exact match found
        if (bestMatch == 0.0) {
            for ((arrayIndex, questionArray) in arrays) {
                for (arrayQuestion in questionArray) {
                    // Quick pre-check: if lengths are too different, skip
                    if (kotlin.math.abs(questionSlug.length - arrayQuestion.length) > 10) {
                        continue
                    }

                    val similarity = calculateSimilarity(questionSlug, arrayQuestion.lowercase())

                    if (similarity >= 0.7 && similarity > bestMatch) {
                        bestMatch = similarity
                        bestArrayIndex = arrayIndex
                        bestPriority = arrayIndex
                    }
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
            matchPercentage = bestMatch,
            matchedArray = bestArrayIndex
        )

        enhancedProblems.add(enhancedProblem)
    }

    // Sort by priority
    return enhancedProblems.sortedWith(
        compareByDescending<EnhancedProblemStat> { it.priority }
            .thenByDescending { it.matchPercentage }
            .thenBy { it.stat.frontend_question_id }
    )
}

// 2. Process problems in chunks to avoid blocking
suspend fun processProblemsInChunks(
    problems: List<ProblemStat>,
    chunkSize: Int = 500
): List<EnhancedProblemStat> = withContext(Dispatchers.Default) {
    val results = mutableListOf<EnhancedProblemStat>()

    problems.chunked(chunkSize).forEach { chunk ->
        val processed = matchProblemsWithArrays(chunk)
        results.addAll(processed)
        // Allow other coroutines to run
        yield()
    }

    // Final sort
    results.sortedWith(
        compareByDescending<EnhancedProblemStat> { it.priority }
            .thenByDescending { it.matchPercentage }
            .thenBy { it.stat.frontend_question_id }
    )
}

// 3. Optimized fetch with progress callback
suspend fun fetchAndProcessProblems(
    context: Context,
    onProgress: (String) -> Unit = {}
): List<EnhancedProblemStat> = withContext(Dispatchers.IO) {
    onProgress("Fetching problems from LeetCode...")

    val fetchedProblems = fetchLeetCodeProblemsWithRetry()

    onProgress("Processing ${fetchedProblems.size} problems...")

    // Switch to Default dispatcher for CPU-intensive work
    val enhancedProblems = withContext(Dispatchers.Default) {
        processProblemsInChunks(fetchedProblems)
    }

    onProgress("Saving to cache...")

    // Save in background
    withContext(Dispatchers.IO) {
        saveEnhancedProblemsToCache(context, enhancedProblems)
        updateProblemsCache(context, enhancedProblems)
    }

    onProgress("Done!")

    enhancedProblems
}

// 4. COMPLETELY REWRITTEN LeetCodeScreen with proper state management
@Composable
fun LeetCodeScreen(
    notificationProblemSlug: String? = null,
    onNotificationHandled: () -> Unit = {},
    onProblemClick: (slug: String, url: String) -> Unit = { _, _ -> } // NEW PARAMETER
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        var problems by remember { mutableStateOf<List<EnhancedProblemStat>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var selectedProblem by remember { mutableStateOf<EnhancedProblemStat?>(null) }
        var isUsingCachedData by remember { mutableStateOf(false) }
        var showRefreshButton by remember { mutableStateOf(false) }
        var loadingProgress by remember { mutableStateOf("") }
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        var checkedMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

        // Single function to load data
        suspend fun loadDataFromCache() {
            isLoading = true
            errorMessage = null
            loadingProgress = "Checking cache..."

            try {
                val cachedProblems = withContext(Dispatchers.IO) {
                    getCachedEnhancedProblems(context)
                }

                if (cachedProblems.isNotEmpty() && isCacheValid(context)) {
                    loadingProgress = "Loading from cache..."
                    problems = cachedProblems
                    isUsingCachedData = true
                    showRefreshButton = true
                    isLoading = false

                    withContext(Dispatchers.IO) {
                        updateProblemsCache(context, cachedProblems)
                    }

                } else {
                    // Fetch fresh data
                    val fresh = fetchAndProcessProblems(context) { progress ->
                        loadingProgress = progress
                    }

                    problems = fresh
                    isUsingCachedData = false
                    showRefreshButton = true
                    errorMessage = null
                }
            } catch (e: Exception) {
                errorMessage = "Failed: ${e.message}"
                Log.e("LoadData", "Error", e)

                val cachedProblems = withContext(Dispatchers.IO) {
                    getCachedEnhancedProblems(context)
                }
                if (cachedProblems.isNotEmpty()) {
                    problems = cachedProblems
                    isUsingCachedData = true
                    errorMessage = "Using cached data (network error)"
                }
            } finally {
                isLoading = false
                loadingProgress = ""
            }
        }

        // Function for background refresh
        fun startBackgroundRefresh() {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val fresh = fetchAndProcessProblems(context) { }
                    withContext(Dispatchers.Main) {
                        problems = fresh
                        isUsingCachedData = false
                    }
                } catch (e: Exception) {
                    Log.d("BackgroundRefresh", "Failed: ${e.message}")
                }
            }
        }

        // Load function wrapper
        val loadData = remember {
            suspend {
                loadDataFromCache()
                // Start background refresh if we loaded from cache
                if (isUsingCachedData && problems.isNotEmpty()) {
                    startBackgroundRefresh()
                }
            }
        }

        // Initial load - SINGLE LaunchedEffect
        LaunchedEffect(Unit) {
            // Load checked map first (fast)
            checkedMap = withContext(Dispatchers.IO) {
                getCheckedMap(context)
            }

            // Then load problems
            loadData()
        }

        // Handle notifications
        LaunchedEffect(notificationProblemSlug, problems) {
            if (notificationProblemSlug != null && problems.isNotEmpty()) {
                val problemToOpen = problems.find {
                    it.stat.question__title_slug == notificationProblemSlug
                }
                if (problemToOpen != null) {
                    // Use the callback instead of setting selectedProblem
                    onProblemClick(
                        problemToOpen.stat.question__title_slug ?: "",
                        "https://leetcode.com/problems/${problemToOpen.stat.question__title_slug}/"
                    )
                    onNotificationHandled()
                }
            }
        }

        // Main UI
        Column(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = loadingProgress.ifEmpty { "Loading problems..." },
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            if (problems.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${problems.size} problems loaded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                errorMessage != null && problems.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        loadData()
                                    }
                                }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        item {
                            ProblemsHeader(
                                problemsCount = problems.size,
                                matchedCount = problems.count { it.priority > 0 },
                                isUsingCachedData = isUsingCachedData,
                                errorMessage = errorMessage,
                                showRefreshButton = showRefreshButton,
                                onRefresh = {
                                    coroutineScope.launch {
                                        clearCache(context)
                                        loadData()
                                    }
                                },
                                problems = problems,
                                coroutineScope = coroutineScope
                            )
                        }

                        items(
                            items = problems,
                            key = { it.stat.frontend_question_id }
                        ) { problem ->
                            ProblemCard(
                                problem = problem,
                                isChecked = checkedMap[problem.stat.question__title_slug] == true,
                                onCheckedChange = { isChecked ->
                                    val updated = checkedMap.toMutableMap()
                                    updated[problem.stat.question__title_slug ?: ""] = isChecked
                                    checkedMap = updated

                                    coroutineScope.launch(Dispatchers.IO) {
                                        saveCheckedMap(context, updated)
                                    }
                                },
                                onClick = {
                                    // Use the callback instead of setting selectedProblem
                                    onProblemClick(
                                        problem.stat.question__title_slug ?: "",
                                        "https://leetcode.com/problems/${problem.stat.question__title_slug}/"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// 5. Extract header to separate composable
@Composable
fun ProblemsHeader(
    problemsCount: Int,
    matchedCount: Int,
    isUsingCachedData: Boolean,
    errorMessage: String?,
    showRefreshButton: Boolean,
    onRefresh: () -> Unit,
    problems: List<EnhancedProblemStat>,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$problemsCount Questions",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Question matched with sheet: $matchedCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (isUsingCachedData) {
                    Text(
                        text = "Using cached data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                if (errorMessage != null && problemsCount > 0) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row {
                if (showRefreshButton) {
                    Button(
                        onClick = onRefresh,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Refresh")
                    }
                }
                LeetCodeNotificationButton(problems)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// 6. Extract problem card to separate composable
@Composable
fun ProblemCard(
    problem: EnhancedProblemStat,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = if (problem.priority > 0) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${problem.stat.frontend_question_id}. ${problem.stat.question__title}",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when (problem.difficulty.level) {
                            1 -> "Easy"
                            2 -> "Medium"
                            3 -> "Hard"
                            else -> "Unknown"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (problem.difficulty.level) {
                            1 -> Color.Green
                            2 -> Color(0xFFFF9800)
                            3 -> Color.Red
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (problem.priority > 0) {
                        Text(
                            text = "⭐ P${problem.priority}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
suspend fun fetchLeetCodeProblems(): List<ProblemStat> = withContext(Dispatchers.IO) {
    // Your existing implementation, but ensure it's properly on IO dispatcher
    val problems = mutableListOf<ProblemStat>()

    try {
        Log.d("FetchProblems", "Making HTTP request to LeetCode API...")

        val url = URL("https://leetcode.com/api/problems/all/")
        val connection = url.openConnection() as HttpURLConnection

        connection.apply {
            requestMethod = "GET"
            connectTimeout = 15000  // Increased timeout
            readTimeout = 15000
            setRequestProperty("User-Agent", "Mozilla/5.0")
            setRequestProperty("Accept", "application/json")
        }

        val responseCode = connection.responseCode
        Log.d("FetchProblems", "Response code: $responseCode")

        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }

            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

            val leetCodeResponse = json.decodeFromString<LeetCodeResponse>(response)
            problems.addAll(leetCodeResponse.stat_status_pairs)

            Log.d("FetchProblems", "Successfully fetched ${problems.size} problems")
        } else {
            throw Exception("HTTP $responseCode: Failed to fetch from LeetCode API")
        }

    } catch (e: Exception) {
        Log.e("FetchProblems", "Network request failed", e)
        throw e
    }

    return@withContext problems
}
