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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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

    val array2 = listOf(
        "find-the-duplicate-number",
        "sort-colors",
        "remove-duplicates-from-sorted-array",
        "set-matrix-zeroes",
        "move-zeroes",
        "best-time-to-buy-and-sell-stock",
        "chocolate-distribution-problem",
        "two-sum",
        "best-time-to-buy-and-sell-stock-ii",
        "subarray-sums-divisible-by-k",
        "find-all-duplicates-in-an-array",
        "container-with-most-water",
        "3sum",
        "4sum",
        "maximum-points-you-can-obtain-from-cards",
        "subarray-sum-equals-k",
        "spiral-matrix",
        "word-search",
        "jump-game",
        "merge-sorted-array",
        "majority-element",
        "reverse-pairs",
        "print-all-possible-combinations-of-r-elements-in-a-given-array-of-size-n",
        "game-of-life",
        "max-value-of-equation",
        "insert-delete-getrandom-o1-duplicates-allowed",
        "largest-rectangle-in-histogram",
        "valid-parentheses",
        "print-all-the-duplicates-in-the-input-string",
        "implement-strstr",
        "longest-common-prefix",
        "valid-palindrome-ii",
        "integer-to-roman",
        "generate-parentheses",
        "simplify-path",
        "smallest-window-in-a-string-containing-all-the-characters-of-another-string",
        "reverse-words-in-a-string",
        "rabin-karp-algorithm-for-pattern-searching",
        "group-anagrams",
        "word-wrap",
        "basic-calculator-ii",
        "valid-number",
        "integer-to-english-words",
        "minimum-window-substring",
        "text-justification",
        "boyer-moore-algorithm-for-pattern-searching",
        "distinct-subsequences",
        "maximum-size-rectangle-binary-sub-matrix-1s",
        "find-number-of-islands",
        "given-matrix-o-x-replace-o-x-surrounded-x",
        "rotate-image",
        "minimum-moves-to-equal-array-elements",
        "add-binary",
        "maximum-product-of-three-numbers",
        "excel-sheet-column-title",
        "happy-number",
        "palindrome-number",
        "missing-number",
        "reverse-integer",
        "power-of-two",
        "max-points-on-a-line",
        "valid-square",
        "the-kth-factor-of-n",
        "permute-two-arrays-sum-every-pair-greater-equal-k",
        "ceiling-in-a-sorted-array",
        "find-a-pair-with-the-given-difference",
        "check-reversing-sub-array-make-array-sorted",
        "radix-sort",
        "a-product-array-puzzle",
        "make-array-elements-equal-minimum-cost",
        "find-peak-element",
        "allocate-minimum-number-of-pages",
        "minimum-number-swaps-required-sort-array",
        "aggressive-cows",
        "search-in-rotated-sorted-array",
        "count-of-smaller-numbers-after-self",
        "split-array-largest-sum",
        "middle-of-the-linked-list",
        "linked-list-cycle",
        "convert-binary-number-in-a-linked-list-to-integer",
        "remove-duplicates-from-sorted-list",
        "sort-a-linked-list-of-0s-1s-or-2s",
        "remove-linked-list-elements",
        "merge-two-sorted-lists",
        "multiply-two-numbers-represented-linked-lists",
        "intersection-of-two-linked-lists",
        "delete-node-in-a-linked-list",
        "palindrome-linked-list",
        "reverse-linked-list",
        "add-two-numbers",
        "copy-list-with-random-pointer",
        "add-two-numbers-ii",
        "reverse-linked-list-ii",
        "reorder-list",
        "remove-nth-node-from-end-of-list",
        "flatten-a-multilevel-doubly-linked-list",
        "partition-list",
        "remove-duplicates-from-sorted-list-ii",
        "linked-list-in-zig-zag-fashion",
        "sort-list",
        "segregate-even-and-odd-elements-in-a-linked-list",
        "rearrange-a-given-linked-list-in-place",
        "merge-k-sorted-lists",
        "reverse-nodes-in-k-group",
        "merge-sort-for-linked-list",
        "flattening-a-linked-list",
        "subtract-two-numbers-represented-as-linked-lists",
        "implement-queue-using-stacks",
        "backspace-string-compare",
        "implement-stack-using-queues",
        "implement-stack-queue-using-deque",
        "next-greater-element-i",
        "evaluation-of-postfix-expression",
        "implement-two-stacks-in-an-array",
        "minimum-cost-tree-from-leaf-values",
        "daily-temperatures",
        "distance-of-nearest-cell-having-1",
        "online-stock-span",
        "rotten-oranges",
        "sum-of-subarray-minimums",
        "evaluate-reverse-polish-notation",
        "circular-tour",
        "remove-all-adjacent-duplicates-in-string-ii",
        "flatten-nested-list-iterator",
        "maximum-of-minimums-for-every-window-size",
        "lru-cache",
        "the-celebrity-problem",
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
        "find-predecessor-and-successor",
        "binary-tree-inorder-traversal",
        "check-whether-bst-contains-dead-end",
        "binary-search-tree-iterator",
        "lowest-common-ancestor-of-a-binary-tree",
        "unique-binary-search-trees-ii",
        "all-nodes-distance-k-in-binary-tree",
        "validate-binary-search-tree",
        "binary-tree-right-side-view",
        "redundant-connection",
        "binary-tree-level-order-traversal",
        "path-sum-iii",
        "construct-binary-tree-from-preorder-and-postorder-traversal",
        "unique-binary-search-trees",
        "recover-binary-search-tree",
        "populating-next-right-pointers-in-each-node",
        "flatten-binary-tree-to-linked-list",
        "maximum-width-of-binary-tree",
        "min-distance-between-two-given-nodes-of-a-binary-tree",
        "kth-smallest-element-in-a-bst",
        "binary-tree-zigzag-level-order-traversal",
        "count-bst-nodes-that-lie-in-a-given-range",
        "preorder-to-postorder",
        "binary-tree-to-dll",
        "binary-tree-maximum-path-sum",
        "sum-of-distances-in-tree",
        "binary-tree-cameras",
        "vertical-order-traversal-of-a-binary-tree",
        "print-k-sum-paths-binary-tree",
        "serialize-and-deserialize-binary-tree",
        "find-median-bst",
        "largest-bst-binary-tree",
        "construct-bst-from-given-preorder-traversal",
        "bfs-traversal-of-graph",
        "depth-first-search-or-dfs-for-a-graph",
        "number-of-islands",
        "flood-fill",
        "rat-in-a-maze-problem",
        "detect-cycle-in-an-undirected-graph",
        "detect-cycle-in-a-graph",
        "steps-by-knight",
        "decode-string",
        "shortest-bridge",
        "number-of-operations-to-make-network-connected",
        "find-eventual-safe-states",
        "strongly-connected-components-kosarajus-algo",
        "time-needed-to-inform-all-employees",
        "graph-coloring",
        "most-stones-removed-with-same-row-or-column",
        "as-far-from-land-as-possible",
        "find-the-city-with-the-smallest-number-of-neighbors-at-a-threshold-distance",
        "course-schedule",
        "prims-minimum-spanning-tree",
        "floyd-warshall",
        "evaluate-division",
        "snakes-and-ladders",
        "topological-sort",
        "cheapest-flights-within-k-stops",
        "detect-negative-cycle-graph-bellman-ford",
        "bipartite-graph",
        "longest-increasing-path-in-a-matrix",
        "making-a-large-island",
        "remove-boxes",
        "critical-connections-in-a-network",
        "alien-dictionary",
        "water-jug-problem-using-bfs",
        "travelling-salesman-problem",
        "total-number-spanning-trees-graph",
        "word-ladder",
        "minimize-cash-flow-among-given-set-friends-borrowed-money",
        "design-add-and-search-words-data-structure",
        "word-break-problem-trie-solution",
        "trie-insert-and-search",
        "k-anagrams-1",
        "palindrome-pairs",
        "phone-directory",
        "top-k-frequent-elements",
        "kth-largest-element-in-an-array",
        "ugly-number-ii",
        "furthest-building-you-can-reach",
        "kth-smallest-element-in-a-sorted-matrix",
        "reorganize-string",
        "find-the-most-competitive-subsequence",
        "smallest-positive-missing-number",
        "largest-subarray-with-0-sum",
        "k-closest-points-to-origin",
        "minimum-number-of-refueling-stops",
        "minimum-cost-to-hire-k-workers",
        "swim-in-rising-water",
        "sliding-window-maximum",
        "climbing-stairs",
        "maximum-product-subarray",
        "ones-and-zeroes",
        "counting-bits",
        "knight-dialer",
        "cutted-segments",
        "unique-paths",
        "minimum-path-sum",
        "coin-change",
        "decode-ways",
        "maximum-length-of-repeated-subarray",
        "longest-increasing-subsequence",
        "longest-common-substring",
        "count-square-submatrices-with-all-ones",
        "maximal-square",
        "mobile-numeric-keypad",
        "weighted-job-scheduling",
        "delete-and-earn",
        "range-sum-query-2d-immutable",
        "optimal-binary-search-tree",
        "frog-jump",
        "best-time-to-buy-and-sell-stock-iv",
        "minimum-insertion-steps-to-make-a-string-palindrome",
        "largest-area-rectangular-sub-matrix-equal-number-1s-0s",
        "trapping-rain-water",
        "minimum-cost-to-merge-stones",
        "count-different-palindromic-subsequences",
        "maximal-rectangle",
        "burst-balloons",
        "super-egg-drop",
        "longest-repeating-character-replacement",
        "maximum-number-of-visible-points",
        "gas-station",
        "minimum-cost-for-acquiring-all-coins-with-k-extra-coins-allowed-with-every-coin",
        "restore-the-array-from-adjacent-pairs",
        "task-scheduler",
        "minimum-deletions-to-make-character-frequencies-unique",
        "remove-k-digits",
        "chocola",
        "non-overlapping-intervals",
        "minimum-deletion-cost-to-avoid-repeating-letters",
        "minimum-sum-two-numbers-formed-digits-array",
        "combination-sum-ii",
        "subset-sum-problem",
        "combinations",
        "subsets-ii",
        "m-coloring-problem",
        "beautiful-arrangement",
        "palindrome-partitioning",
        "permutations-ii",
        "word-search-ii",
        "sudoku-solver",
        "n-queens",
        "unique-paths-iii",
        "find-maximum-number-possible-by-doing-at-most-k-swaps",
        "partition-set-k-subsets-equal-sum",
        "tug-of-war",
        "find-paths-from-corner-cell-to-middle-cell-in-maze",
        "solving-cryptarithmetic-puzzles",
        "print-palindromic-partitions-string",
        "range-sum-query-immutable",
        "range-sum-query-mutable",
        "count-of-range-sum"
    )
//
val array3 = listOf(
    "maximum-and-minimum-element-in-an-array",
    "reverse-the-array",
    "maximum-subarray",
    "contains-duplicate",
    "chocolate-distribution-problem",
    "search-in-rotated-sorted-array",
    "next-permutation",
    "best-time-to-buy-and-sell-stock",
    "repeat-and-missing-number-array",
    "kth-largest-element-in-an-array",
    "trapping-rain-water",
    "product-of-array-except-self",
    "maximum-product-subarray",
    "find-minimum-in-rotated-sorted-array",
    "find-pair-with-sum-in-sorted-and-rotated-array",
    "3sum",
    "container-with-most-water",
    "given-sum-pair",
    "kth-smallest-element",
    "merge-overlapping-intervals",
    "find-minimum-number-of-merge-operations-to-make-an-array-palindrome",
    "given-an-array-of-numbers-arrange-the-numbers-to-form-the-biggest-number",
    "space-optimization-using-bit-manipulations",
    "subarray-sum-divisible-k",
    "print-all-possible-combinations-of-r-elements-in-a-given-array-of-size-n",
    "mos-algorithm",
    "valid-palindrome",
    "valid-anagram",
    "valid-parentheses",
    "remove-consecutive-characters",
    "longest-common-prefix",
    "convert-a-sentence-into-its-equivalent-mobile-numeric-keypad-sequence",
    "print-all-the-duplicates-in-the-input-string",
    "longest-substring-without-repeating-characters",
    "longest-repeating-character-replacement",
    "group-anagrams",
    "longest-palindromic-substring",
    "palindromic-substrings",
    "count-palindromic-subsequences",
    "smallest-window-in-a-string-containing-all-the-characters-of-another-string",
    "wildcard-string-matching",
    "longest-prefix-suffix",
    "rabin-karp-algorithm-for-pattern-searching",
    "transform-one-string-to-another-using-minimum-number-of-given-operation",
    "minimum-window-substring",
    "boyer-moore-algorithm-for-pattern-searching",
    "word-wrap",
    "zigzag-or-diagonal-traversal-of-matrix",
    "set-matrix-zeroes",
    "spiral-matrix",
    "rotate-image",
    "word-search",
    "find-the-number-of-islands-using-dfs",
    "given-a-matrix-of-o-and-x-replace-o-with-x-if-surrounded-by-x",
    "find-a-common-element-in-all-rows-of-a-given-row-wise-sorted-matrix",
    "create-a-matrix-with-alternating-rectangles-of-o-and-x",
    "maximum-size-rectangle-of-all-1s",
    "permute-two-arrays-such-that-sum-of-every-pair-is-greater-or-equal-to-k",
    "counting-sort",
    "find-common-elements-three-sorted-arrays",
    "searching-in-an-array-where-adjacent-differ-by-at-most-k",
    "ceiling-in-a-sorted-array",
    "pair-with-given-difference",
    "majority-element",
    "count-triplets-with-sum-smaller-that-a-given-value",
    "maximum-sum-subsequence-with-no-adjacent-elements",
    "merge-sorted-arrays-using-o1-space",
    "inversion-of-array",
    "find-duplicates-in-on-time-and-o1-extra-space",
    "radix-sort",
    "product-of-array-except-itself",
    "make-all-array-elements-equal",
    "check-if-reversing-a-sub-array-make-the-array-sorted",
    "find-four-elements-that-sum-to-a-given-value",
    "median-of-two-sorted-array-with-different-size",
    "median-of-stream-of-integers-running-integers",
    "print-subarrays-with-0-sum",
    "aggressive-cows",
    "allocate-minimum-number-of-pages",
    "minimum-swaps-to-sort",
    "rat-in-a-maze",
    "combinational-sum",
    "crossword-puzzle",
    "longest-possible-route-in-a-matrix-with-hurdles",
    "n-queen-problem",
    "sudoku-solver",
    "partition-equal-subset-sum",
    "m-coloring-problem",
    "knight-tour",
    "remove-invalid-parentheses",
    "word-break-problem-using-backtracking",
    "print-all-palindromic-partitions-of-a-string",
    "find-shortest-safe-route-in-a-path-with-landmines",
    "partition-of-set-into-k-subsets-with-equal-sum",
    "hamiltonian-cycle",
    "tug-of-war",
    "maximum-possible-number-by-doing-at-most-k-swaps",
    "solving-cryptarithmetic-puzzles",
    "find-paths-from-corner-cell-to-middle-cell-in-maze",
    "arithmetic-expressions",
    "reverse-linked-list",
    "linked-list-cycle",
    "merge-two-sorted-lists",
    "delete-without-head-node",
    "remove-duplicates-from-an-unsorted-linked-list",
    "sort-a-linked-list-of-0s-1s-or-2s",
    "multiply-two-numbers-represented-linked-lists",
    "remove-nth-node-from-end-of-list",
    "reorder-list",
    "detect-and-remove-loop-in-a-linked-list",
    "intersection-point-of-two-linked-lists",
    "flatten-a-linked-list-with-next-and-child-pointers",
    "linked-list-in-zig-zag-fashion",
    "reverse-a-doubly-linked-list",
    "delete-nodes-which-have-a-greater-value-on-right-side",
    "segregate-even-and-odd-elements-in-a-linked-list",
    "point-to-next-higher-value-node-in-a-linked-list-with-an-arbitrary-pointer",
    "rearrange-a-given-linked-list-in-place",
    "sort-biotonic-doubly-linked-lists",
    "merge-k-sorted-lists",
    "merge-sort-for-linked-list",
    "quicksort-on-singly-linked-list",
    "sum-of-two-linked-lists",
    "flattening-a-linked-list",
    "clone-a-linked-list-with-next-and-random-pointer",
    "subtract-two-numbers-represented-as-linked-lists",
    "implement-two-stacks-in-an-array",
    "evaluation-of-postfix-expression",
    "implement-stack-using-queues",
    "queue-reversal",
    "implement-stack-queue-using-deque",
    "reverse-first-k-elements-of-queue",
    "design-stack-with-middle-operation",
    "infix-to-postfix",
    "design-and-implement-special-stack",
    "longest-valid-string",
    "duplicate-parenthesis",
    "stack-permutations-check-if-an-array-is-stack-permutation-of-other",
    "count-natural-numbers-whose-permutation-greater-number",
    "sort-a-stack-using-recursion",
    "first-non-repeating-character-in-a-stream",
    "the-celebrity-problem",
    "next-larger-element",
    "distance-of-nearest-cell",
    "rotten-oranges",
    "next-smaller-element",
    "circular-tour",
    "efficiently-implement-k-stacks-single-array",
    "iterative-tower-of-hanoi",
    "maximum-of-minimums-for-every-window-size-in-a-given-array",
    "lru-cache-implementation",
    "find-a-tour-that-visits-all-stations",
    "activity-selection-problem",
    "minimum-number-of-coins",
    "minimum-sum-two-numbers-formed-digits-array",
    "minimum-sum-absolute-difference-pairs-two-arrays",
    "find-maximum-height-pyramid-from-the-given-array-of-objects",
    "minimum-cost-for-acquiring-all-coins-with-k-extra-coins-allowed-with-every-coin",
    "find-maximum-equal-sum-of-every-three-stacks",
    "job-sequencing-problem",
    "egyptian-fraction",
    "fractional-knapsack-problem",
    "maximum-length-chain-of-pairs",
    "find-smallest-number-with-given-number-of-digits-and-digit-sum",
    "maximize-sum-of-consecutive-differences-circular-array",
    "paper-cut-minimum-number-squares",
    "lexicographically-smallest-array-k-consecutive-swaps",
    "chocola-problem",
    "find-minimum-time-to-finish-all-jobs-with-given-constraints",
    "job-sequencing-using-disjoint-set-union",
    "rearrange-characters-string-such-that-no-two-adjacent-are-same",
    "minimum-edges-to-reverse-to-make-path-from-a-source-to-a-destination",
    "minimize-cash-flow-among-given-set-of-friends-who-have-borrowed-money-from-each-other",
    "minimum-cost-to-cut-a-board-into-squares",
    "maximum-depth-of-binary-tree",
    "reverse-level-order-traversal",
    "subtree-of-another-tree",
    "invert-binary-tree",
    "binary-tree-level-order-traversal",
    "left-view-of-binary-tree",
    "right-view-of-binary-tree",
    "zigzag-tree-traversal",
    "mirror-tree",
    "leaf-at-same-level",
    "check-for-balanced-tree",
    "transform-to-sum-tree",
    "check-if-tree-is-isomorphic",
    "same-tree",
    "construct-binary-tree-from-preorder-and-inorder-traversal",
    "height-of-binary-tree",
    "diameter-of-a-binary-tree",
    "top-view-of-binary-tree",
    "bottom-view-of-binary-tree",
    "diagonal-traversal-of-binary-tree",
    "boundary-traversal-of-binary-tree",
    "construct-binary-tree-from-string-with-brackets",
    "minimum-swap-required-to-convert-binary-tree-to-binary-search-tree",
    "duplicate-subtree-in-binary-tree",
    "check-if-a-given-graph-is-tree-or-not",
    "lowest-common-ancestor-in-a-binary-tree",
    "min-distance-between-two-given-nodes-of-a-binary-tree",
    "duplicate-subtrees",
    "kth-ancestor-of-a-node-in-binary-tree",
    "binary-tree-maximum-path-sum",
    "serialize-and-deserialize-binary-tree",
    "binary-tree-to-dll",
    "print-all-k-sum-paths-in-a-binary-tree",
    "lowest-common-ancestor-of-a-binary-search-tree",
    "binary-search-tree-search-and-insertion",
    "minimum-element-in-bst",
    "predecessor-and-successor",
    "check-whether-bst-contains-dead-end",
    "binary-tree-to-bst",
    "kth-largest-element-in-bst",
    "validate-binary-search-tree",
    "kth-smallest-element-in-a-bst",
    "delete-node-in-a-bst",
    "flatten-bst-to-sorted-list",
    "preorder-to-postorder",
    "count-bst-nodes-that-lie-in-a-given-range",
    "populate-inorder-successor-for-all-nodes",
    "convert-normal-bst-to-balanced-bst",
    "merge-two-bsts",
    "find-conflicting-appointments",
    "replace-every-element",
    "construct-bst-from-given-preorder-traversal",
    "find-median-of-bst-in-on-time-and-o1-space",
    "largest-bst-in-a-binary-tree",
    "choose-k-array-elements-such-that-difference-of-maximum-and-minimum-is-minimized",
    "heap-sort",
    "top-k-frequent-elements",
    "k-largest-elements-in-an-array",
    "next-greater-element",
    "kth-smallest-largest-element-in-unsorted-array",
    "find-the-maximum-repeating-number-in-on-time-and-o1-extra-space",
    "kth-smallest-element-after-removing-some-integers-from-natural-numbers",
    "find-k-closest-elements-to-a-given-value",
    "kth-largest-element-in-a-stream",
    "connect-ropes",
    "cuckoo-hashing",
    "itinerary-from-a-list-of-tickets",
    "largest-subarray-with-0-sum",
    "count-distinct-elements-in-every-window-of-size-k",
    "group-shifted-strings",
    "find-median-from-data-stream",
    "sliding-window-maximum",
    "find-the-smallest-positive-number",
    "find-surpasser-count-of-each-element-in-array",
    "tournament-tree-and-binary-heap",
    "check-for-palindrome",
    "length-of-the-largest-subarray-with-contiguous-elements",
    "palindrome-substring-queries",
    "subarray-distinct-elements",
    "find-the-recurring-function",
    "k-maximum-sum-combinations-from-two-arrays",
    "bfs",
    "dfs",
    "flood-fill-algorithm",
    "number-of-triangles",
    "detect-cycle-in-a-graph",
    "detect-cycle-in-an-undirected-graph",
    "rat-in-a-maze-problem",
    "steps-by-knight",
    "clone-graph",
    "number-of-operations-to-make-network-connected",
    "dijkstras-shortest-path-algorithm",
    "topological-sort",
    "oliver-and-the-game",
    "minimum-time-taken-by-each-job-to-be-completed-given-by-a-directed-acyclic-graph",
    "find-whether-it-is-possible-to-finish-all-tasks-or-not-from-given-dependencies",
    "find-the-number-of-islands",
    "prims-algo",
    "negative-weighted-cycle",
    "floyd-warshall",
    "graph-coloring",
    "snakes-and-ladders",
    "kosarajus-theorem",
    "journey-to-moon",
    "vertex-cover",
    "m-coloring-problem",
    "cheapest-flights-within-k-stops",
    "find-if-there-is-a-path-of-more-than-k-length-from-a-source",
    "bellman-ford",
    "bipartite-graph",
    "word-ladder",
    "alien-dictionary",
    "kruskals-mst",
    "total-number-spanning-trees-graph",
    "travelling-salesman",
    "find-longest-path-directed-acyclic-graph",
    "two-clique-problem",
    "minimise-the-cash-flow",
    "chinese-postman",
    "water-jug",
    "water-jug-2",
    "construct-a-trie-from-scratch",
    "print-unique-rows-in-a-given-boolean-matrix",
    "word-break-problem-trie-solution",
    "print-all-anagrams-together",
    "find-shortest-unique-prefix-for-every-word-in-a-given-list",
    "implement-a-phone-directory",
    "knapsack-with-duplicate-items",
    "bbt-counter",
    "reach-a-given-score",
    "maximum-difference-of-zeros-and-ones-in-binary-string",
    "climbing-stairs",
    "permutation-coefficient",
    "longest-repeating-subsequence",
    "pairs-with-specific-difference",
    "longest-subsequence-1",
    "coin-change",
    "longest-increasing-subsequence",
    "longest-common-subsequence",
    "word-break",
    "combination-sum-iv",
    "house-robber",
    "house-robber-2",
    "decode-ways",
    "unique-paths",
    "jump-game",
    "knapsack-problem",
    "ncr",
    "catalan-number",
    "edit-distance",
    "subset-sum",
    "gold-mine",
    "assembly-line-scheduling",
    "maximize-the-cut-segments",
    "maximum-sum-increasing-subsequence",
    "count-all-subsequences-having-product-less-than-k",
    "egg-dropping-puzzle",
    "max-length-chain",
    "largest-square-in-matrix",
    "maximum-path-sum",
    "minimum-number-of-jumps",
    "minimum-removals-from-array-to-make-max-min-less-equal-k",
    "longest-common-substring",
    "partition-equal-subset-sum",
    "longest-palindromic-subsequence",
    "count-palindromic-subsequences",
    "longest-palindromic-substring",
    "longest-alternating-sequence",
    "weighted-job-scheduling",
    "coin-game",
    "coin-game-winner",
    "optimal-strategy-for-a-game",
    "word-wrap",
    "mobile-numeric-keypad",
    "maximum-length-of-pair-chain",
    "matrix-chain-multiplication",
    "maximum-profit-by-buying-and-selling-a-share-at-most-twice",
    "optimal-bst",
    "largest-submatrix-with-sum-0",
    "largest-area-rectangular-sub-matrix-with-equal-number-of-1s-and-0s",
    "count-set-bits-in-an-integer",
    "find-the-two-non-repeating-elements-in-an-array-of-repeating-elements",
    "program-to-find-whether-a-no-is-power-of-two",
    "find-position-of-the-only-set-bit",
    "count-number-of-bits-to-be-flipped-to-convert-a-to-b",
    "count-total-set-bits-in-all-numbers-from-1-to-n",
    "copy-set-bits-in-a-range",
    "calculate-square-of-a-number-without-using-and-pow",
    "divide-two-integers-without-using-multiplication-division-and-mod-operator",
    "power-set",
    "range-sum-query-immutable",
    "range-minimum-query",
    "range-sum-query-mutable",
    "create-sorted-array-through-instructions",
    "count-of-range-sum",
    "count-of-smaller-numbers-after-self"
)
    val array4 = listOf(
        "squares-of-a-sorted-array",
        "rotate-array",
        "relatively-prime",
        "napoleon-cake",
        "next-greater-element-iii",
        "majority-element",
        "majority-element-ii",
        "majority-element-general",
        "max-chunks-to-make-sorted",
        "max-chunks-to-make-sorted-ii",
        "maximum-product-of-three-numbers",
        "largest-number-at-least-twice-of-others",
        "product-of-array-except-self",
        "number-of-subarrays-with-bounded-maximum",
        "maximum-subarray",
        "k-con",
        "best-meeting-point",
        "segregate-0-and-1",
        "segregate-0-1-2",
        "sort-array-by-parity",
        "partition-labels",
        "check-whether-one-string-is-a-rotation-of-another",
        "meximization",
        "k-lcm-1",
        "k-lcm-2",
        "consecutive-number-sum",
        "fast-exponentiation",
        "fibonacci-number",
        "sieve-of-eratosthenes",
        "segmented-sieve",
        "wiggle-sort",
        "min-jump-required-with-plus-i-or-minus-i-allowed",
        "maximum-swap",
        "minimum-domino-rotation-for-equal-row",
        "multiply-strings",
        "two-sum",
        "two-difference",
        "boats-to-save-people",
        "smallest-range-from-k-lists",
        "maximum-product-subarray",
        "min-no-of-platform",
        "reverse-vowels-of-a-string",
        "first-missing-positive",
        "rotate-image",
        "minimum-swaps-required-bring-elements-less-equal-k-together",
        "push-dominoes",
        "valid-palindrome-ii",
        "sum-of-subsequence-width",
        "find-the-kth-max-and-min-element-of-an-array",
        "maximize-distance-to-closest-person",
        "buddy-nim",
        "bulb-switcher-iii",
        "interesting-subarrays",
        "sort-the-array",
        "max-sum-of-two-non-overlapping-subarrays",
        "minimise-the-maximum-difference-between-heights",
        "range-related-question",
        "long-pressed-name",
        "range-addition",
        "max-range-query",
        "orderly-queue",
        "container-with-most-water",
        "spiral-traversal-on-a-matrix",
        "search-an-element-in-a-matrix",
        "find-median-in-a-row-wise-sorted-matrix",
        "find-row-with-maximum-no-of-1s",
        "print-elements-in-sorted-order-using-row-column-wise-sorted-matrix",
        "find-a-specific-pair-in-matrix",
        "rotate-matrix-by-90-degrees",
        "kth-smallest-element-in-a-row-column-wise-sorted-matrix",
        "common-elements-in-all-rows-of-a-given-matrix",
        "binary-search",
        "median-of-two-sorted-array",
        "capacity-to-ship-within-d-days",
        "koko-eating-bananas",
        "smallest-divisor-given-a-threshold",
        "painters-partition-problem",
        "split-array-largest-sum",
        "median-from-data-stream",
        "kth-smallest-prime-fraction",
        "search-in-rotated-sorted-array",
        "search-in-rotated-sorted-array-ii",
        "find-minimum-in-rotated-sorted-array",
        "find-min-in-rotated-sorted-array-ii",
        "counting-sort",
        "merge-sort",
        "count-inversions",
        "reverse-linkedlist",
        "reverse-a-linked-list-in-group-of-given-size",
        "find-the-middle-element",
        "floyd-cycle",
        "clone-a-linkedlist",
        "split-circular-linkedlist",
        "intersection-point-of-2-linked-list",
        "lru-cache",
        "merge-sort-for-linked-lists",
        "quicksort-for-linked-lists",
        "check-whether-the-singly-linked-list-is-a-palindrome",
        "sort-a-k-sorted-doubly-linked-list",
        "rotate-a-doubly-linked-list-in-group-of-given-size",
        "flatten-a-linked-list",
        "sort-a-ll-of-0s-1s-and-2s",
        "delete-nodes-which-have-a-greater-value-on-right-side",
        "remove-duplicates-in-a-sorted-linked-list",
        "add-two-numbers-represented-by-linked-lists",
        "next-greater-element-on-right",
        "next-greater-element-2",
        "daily-temperatures",
        "stock-span-problem",
        "maximum-difference-between-left-and-right-smaller",
        "largest-rectangular-area-histogram",
        "maximum-size-binary-matrix-containing-1",
        "asteroid-collision",
        "valid-parentheses",
        "length-of-longest-valid-substring",
        "count-of-duplicate-parentheses",
        "minimum-number-of-bracket-reversal",
        "minimum-add-to-make-parentheses-valid",
        "remove-k-digits-from-number",
        "longest-unbalanced-subsequence",
        "first-negative-integer-in-k-sized-window",
        "maximum-sum-of-smallest-and-second-smallest",
        "k-reverse-in-a-queue",
        "k-stacks-in-a-single-array",
        "k-queue-in-a-single-array",
        "gas-station",
        "print-binary-number",
        "remove-duplicate-letters",
        "backspace-string-compare",
        "car-fleet",
        "validate-stack",
        "max-frequency-stack",
        "min-stack",
        "adapters",
        "min-cost-tree-from-leaf-values",
        "inorder-traversal",
        "preorder-traversal",
        "postorder-traversal",
        "binary-tree-level-order",
        "binary-search-tree-to-greater-sum",
        "next-right-pointer-in-each-node",
        "sum-of-distances-in-tree",
        "right-side-view",
        "left-view",
        "top-view",
        "bottom-view",
        "reverse-level-order",
        "vertical-order",
        "diagonal-traversal",
        "boundary-traversal",
        "binary-tree-coloring-game",
        "image-multiplication",
        "inorder-successor",
        "max-product-splitted-binary-tree",
        "lowest-common-ancestor-in-bst",
        "lowest-common-ancestor",
        "distribute-coins-in-a-binary-tree",
        "binary-tree-cameras",
        "max-path-sum",
        "recover-binary-search-tree",
        "flatten-binary-tree-to-linked-list",
        "convert-a-binary-tree-to-circular-doubly-linked-list",
        "conversion-of-sorted-dll-to-bst",
        "merge-two-bst",
        "clone-a-binary-tree-with-random-pointer",
        "delete-node-in-bst",
        "construct-from-inorder-and-preorder",
        "construct-from-inorder-and-postorder",
        "inorder-and-level-order",
        "is-bst-preorder",
        "bst-from-postorder",
        "bst-from-preorder",
        "construct-from-pre-and-post",
        "serialize-and-deserialize",
        "fenwick-tree",
        "longest-zigzag-path-in-binary-tree",
        "count-complete-tree-node",
        "closest-binary-search-tree",
        "closest-binary-search-tree-value-2",
        "sum-root-to-leaf",
        "lca-in-o-root-h",
        "segment-tree",
        "bfs-of-graph",
        "bipartite-graph",
        "bus-routes",
        "prims-algo",
        "connecting-cities-with-minimum-cost",
        "dijkstra-algo",
        "chef-and-reversing",
        "optimize-water-distribution-in-village",
        "dfs",
        "evaluate-division",
        "strongly-connected-components-kosarajus-algo",
        "mother-vertex",
        "number-of-enclaves",
        "0-1-matrix",
        "number-of-islands",
        "number-of-distinct-islands",
        "word-ladder",
        "shortest-bridge",
        "as-far-from-land-as-possible",
        "sliding-puzzle",
        "bellman-ford",
        "coloring-a-border",
        "rotten-oranges",
        "topological-sorting",
        "kahns-algo",
        "course-schedule-2",
        "articulation-point",
        "doctor-strange",
        "eulerian-path-in-an-undirected-graph",
        "euler-circuit-in-a-directed-graph",
        "dsu",
        "dfs-vs-dsu",
        "number-of-islands-ii",
        "regions-cut-by-slashes",
        "sentence-similarity-ii",
        "satisfiability-of-equality-equations",
        "redundant-connection",
        "redundant-connection-2",
        "castle-run",
        "minimize-malware-spread",
        "kruskals-algo",
        "job-sequencing",
        "reconstruct-itinerary",
        "sort-item-by-group-accord-to-dependencies",
        "most-stones-removed-with-same-row-or-column",
        "find-the-maximum-flow",
        "maximum-bipartite-matching",
        "min-swaps-required-to-sort-array",
        "min-swaps-to-make-2-array-identical",
        "bricks-falling-when-hit",
        "alien-dictionary",
        "largest-color-value-in-a-directed-graph",
        "swim-in-rising-water",
        "shortest-distance-from-all-buildings",
        "remove-max-number-of-edges-to-keep-graph-traversal",
        "graph-connectivity-with-threshold",
        "smallest-strings-with-swaps",
        "cheapest-flight-within-k-stops",
        "cracking-the-safe",
        "find-eventual-safe-state",
        "shortest-cycle-in-an-undirected-graph",
        "floyd-warshall",
        "johnsons-algorithm",
        "number-of-subarrays-sum-exactly-k",
        "subarray-sum-divisible-by-k",
        "same-differences",
        "subarray-with-equal-number-of-0-and-1",
        "substring-with-equal-0-1-and-2",
        "k-closest-point-from-origin",
        "longest-consecutive-1s",
        "minimum-number-of-refueling-spots",
        "potions-hard-version",
        "x-of-a-kind-in-a-deck",
        "check-ap-sequence",
        "array-of-doubled-pair",
        "rabbits-in-forest",
        "longest-consecutive-sequence",
        "the-skyline-problem",
        "morning-assembly",
        "brick-wall",
        "grid-illumination",
        "island-perimeter",
        "bulb-switcher",
        "isomorphic-string",
        "pairs-of-coinciding-points",
        "trapping-rain-water",
        "trapping-rain-water-ii",
        "count-pair-whose-sum-is-divisible-by-k",
        "length-of-largest-subarray-with-continuous-element",
        "length-of-largest-subarray-with-cont-element-2",
        "smallest-number-whose-digit-mult-to-given-no",
        "same-frequency-after-one-removal",
        "insert-delete-getrandom-o1",
        "insert-delete-get-random-duplicates-allowed",
        "find-all-anagrams-in-a-string",
        "anagram-palindrome",
        "find-smallest-size-of-string-containing-all-char-of-other",
        "group-anagram",
        "longest-substring-with-unique-character",
        "smallest-subarray-with-all-the-occurence-of-mfe",
        "find-anagram-mapping",
        "k-anagram",
        "rearrange-character-string-such-that-no-two-are-same",
        "mode-of-frequencies",
        "line-reflection",
        "kth-smallest-element-in-sorted-2d-matrix",
        "kth-smallest-prime-fraction",
        "employee-free-time",
        "activity-selection-problem",
        "job-sequencing-problem",
        "huffman-coding",
        "water-connection-problem",
        "fractional-knapsack-problem",
        "greedy-algorithm-to-find-minimum-number-of-coins",
        "maximum-trains-for-which-stoppage-can-be-provided",
        "minimum-platforms-problem",
        "buy-maximum-stocks-if-i-stocks-can-be-bought-on-i-th-day",
        "find-the-minimum-and-maximum-amount-to-buy-all-n-candies",
        "minimize-cash-flow-among-given-set-of-friends-who-have-borrowed-money-from-each-other",
        "minimum-cost-to-cut-a-board-into-squares",
        "check-if-it-is-possible-to-survive-on-island",
        "find-maximum-meetings-in-one-room",
        "maximum-product-subset-of-an-array",
        "maximize-array-sum-after-k-negations",
        "maximize-the-sum-of-arri-times-i",
        "maximum-sum-of-absolute-difference-of-an-array",
        "maximize-sum-of-consecutive-differences-in-a-circular-array",
        "minimum-sum-of-absolute-difference-of-pairs-of-two-arrays",
        "program-for-shortest-job-first-cpu-scheduling",
        "program-for-least-recently-used-page-replacement-algorithm",
        "smallest-subset-with-sum-greater-than-all-other-elements",
        "chocolate-distribution-problem",
        "defkin-defense-of-a-kingdom",
        "diehard-die-hard",
        "gergovia-wine-trading-in-gergovia",
        "picking-up-chicks",
        "chocola-chocolate",
        "arrange-arranging-amplifiers",
        "k-centers-problem",
        "minimum-cost-of-ropes",
        "find-smallest-number-with-given-number-of-digits-and-sum-of-digits",
        "rearrange-characters-in-a-string-such-that-no-two-adjacent-are-same",
        "find-maximum-sum-possible-equal-sum-of-three-stacks",
        "rat-in-a-maze-problem",
        "printing-all-solutions-in-n-queen-problem",
        "word-break-problem-using-backtracking",
        "remove-invalid-parentheses",
        "sudoku-solver",
        "m-coloring-problem",
        "print-all-palindromic-partitions-of-a-string",
        "subset-sum-problem",
        "the-knights-tour-problem",
        "tug-of-war",
        "find-shortest-safe-route-in-a-path-with-landmines",
        "combinational-sum",
        "find-maximum-number-possible-by-doing-at-most-k-swaps",
        "print-all-permutations-of-a-string",
        "find-if-there-is-a-path-of-more-than-k-length-from-a-source",
        "longest-possible-route-in-a-matrix-with-hurdles",
        "print-all-possible-paths-from-top-left-to-bottom-right-of-a-mxn-matrix",
        "partition-of-a-set-into-k-subsets-with-equal-sum",
        "find-the-k-th-permutation-sequence-of-first-n-natural-numbers",
        "longest-increasing-subsequence-n2",
        "longest-increasing-subsequence-nlogn",
        "building-bridges",
        "russian-doll-envelopes",
        "box-stacking",
        "weighted-job-scheduling",
        "minimum-number-of-increasing-subsequence",
        "paint-fence",
        "paint-house",
        "paint-house-2",
        "no-of-binary-string-without-consecutive-1",
        "possible-ways-to-construct-the-building",
        "catalan-number",
        "total-no-of-bst",
        "applications-of-catalan-numbers",
        "min-cost-path",
        "cherry-pickup",
        "cherry-pickup-2",
        "best-time-to-buy-and-sell-stock",
        "best-time-to-buy-and-sell-stock-2",
        "buy-and-sell-with-transaction-fee",
        "best-time-to-buy-and-sell-with-cool-down",
        "best-time-to-buy-and-sell-stock-3",
        "best-time-to-buy-and-sell-stock-4",
        "highway-billboard-problem",
        "burst-balloons",
        "matrix-chain-multiplication",
        "boolean-parenthesization",
        "min-and-max-val-of-expression",
        "minimum-score-triangulation",
        "binomial-coefficient",
        "longest-common-subsequence",
        "lcs-triplet",
        "longest-palindromic-subsequence",
        "count-all-palindromic-subsequence",
        "count-distinct-palindromic-subsequence",
        "palindromic-substrings",
        "no-of-sequence-of-type-ai-bj-ck",
        "count-of-distinct-subsequences",
        "edit-distance",
        "2-egg-100-floor",
        "egg-drop",
        "optimal-strategy-for-a-game",
        "ugly-number",
        "super-ugly-number",
        "max-size-subsquare-with-all-1",
        "wildcard-pattern-matching",
        "regular-expression-matching",
        "palindrome-partitioning",
        "longest-bitonic-subsequence",
        "knights-probability-in-chessboard",
        "word-break-problem",
        "longest-repeating-subsequence",
        "optimal-bst",
        "max-sum-subarray-with-atleast-k-elements",
        "say-no-to-palindrome",
        "number-of-digit-1",
        "scramble-string",
        "distinct-subsequences",
        "shortest-common-superseq",
        "longest-common-substring",
        "frog-jump",
        "jump-game-2",
        "friends-pairing-problem",
        "dungeon-game",
        "kmp",
        "rabin-karp",
        "shortest-palindrome",
        "z-algo",
        "chef-and-secret-password",
        "manachers-algo",
        "tri-tiling",
        "coin-change",
        "coin-change-2",
        "unbounded-knapsack",
        "count-set-bits-in-an-integer",
        "reverse-bits",
        "find-the-two-non-repeating-elements-in-an-array-of-repeating-elements",
        "count-number-of-bits-to-be-flipped-to-convert-a-to-b",
        "program-to-find-whether-a-no-is-power-of-two",
        "copy-set-bits-in-a-range",
        "calculate-square-of-a-number-without-using-and-pow",
        "subsequence-of-string-using-bit-manipulation",
        "single-number-ii",
        "hamming-distance",
        "bitwise-ors-of-subarrays",
        "divide-integers",
        "minimum-xor-value",
        "max-xor-in-a-range-l-r",
        "implement-trie-prefix-tree",
        "maximum-xor-of-two-numbers-in-an-array",
        "search-suggestions-system",
        "kth-smallest-in-lexicographical-order",
        "no-prefix-set",
        "shortest-unique-prefix"
    )
//
val array5 = listOf(
    "weird-algorithm",
    "missing-number",
    "repetitions",
    "increasing-array",
    "permutations",
    "number-spiral",
    "two-knights",
    "two-sets",
    "bit-strings",
    "trailing-zeros",
    "coin-piles",
    "palindrome-reorder",
    "gray-code",
    "tower-of-hanoi",
    "creating-strings",
    "apple-division",
    "chessboard-and-queens",
    "raab-game-i",
    "mex-grid-construction",
    "knight-moves-grid",
    "grid-coloring-i",
    "digit-queries",
    "string-reorder",
    "grid-path-description",
    "distinct-numbers",
    "apartments",
    "ferris-wheel",
    "concert-tickets",
    "restaurant-customers",
    "movie-festival",
    "sum-of-two-values",
    "maximum-subarray-sum",
    "stick-lengths",
    "missing-coin-sum",
    "collecting-numbers",
    "collecting-numbers-ii",
    "playlist",
    "towers",
    "traffic-lights",
    "distinct-values-subarrays",
    "distinct-values-subsequences",
    "josephus-problem-i",
    "josephus-problem-ii",
    "nested-ranges-check",
    "nested-ranges-count",
    "room-allocation",
    "factory-machines",
    "tasks-and-deadlines",
    "reading-books",
    "sum-of-three-values",
    "sum-of-four-values",
    "nearest-smaller-values",
    "subarray-sums-i",
    "subarray-sums-ii",
    "subarray-divisibility",
    "distinct-values-subarrays-ii",
    "array-division",
    "movie-festival-ii",
    "maximum-subarray-sum-ii",
    "dice-combinations",
    "minimizing-coins",
    "coin-combinations-i",
    "coin-combinations-ii",
    "removing-digits",
    "grid-paths-i",
    "book-shop",
    "array-description",
    "counting-towers",
    "edit-distance",
    "longest-common-subsequence",
    "rectangle-cutting",
    "minimal-grid-path",
    "money-sums",
    "removal-game",
    "two-sets-ii",
    "mountain-range",
    "increasing-subsequence",
    "projects",
    "elevator-rides",
    "counting-tilings",
    "counting-numbers",
    "increasing-subsequence-ii",
    "counting-rooms",
    "labyrinth",
    "building-roads",
    "message-route",
    "building-teams",
    "round-trip",
    "monsters",
    "shortest-routes-i",
    "shortest-routes-ii",
    "high-score",
    "flight-discount",
    "cycle-finding",
    "flight-routes",
    "round-trip-ii",
    "course-schedule",
    "longest-flight-route",
    "game-routes",
    "investigation",
    "planets-queries-i",
    "planets-queries-ii",
    "planets-cycles",
    "road-reparation",
    "road-construction",
    "flight-routes-check",
    "planets-and-kingdoms",
    "giant-pizza",
    "coin-collector",
    "mail-delivery",
    "de-bruijn-sequence",
    "teleporters-path",
    "hamiltonian-flights",
    "knights-tour",
    "download-speed",
    "police-chase",
    "school-dance",
    "distinct-routes",
    "static-range-sum-queries",
    "static-range-minimum-queries",
    "dynamic-range-sum-queries",
    "dynamic-range-minimum-queries",
    "range-xor-queries",
    "range-update-queries",
    "forest-queries",
    "hotel-queries",
    "list-removals",
    "salary-queries",
    "prefix-sum-queries",
    "pizzeria-queries",
    "visible-buildings-queries",
    "range-interval-queries",
    "subarray-sum-queries",
    "subarray-sum-queries-ii",
    "distinct-values-queries",
    "distinct-values-queries-ii",
    "increasing-array-queries",
    "movie-festival-queries",
    "forest-queries-ii",
    "range-updates-and-sums",
    "polynomial-queries",
    "range-queries-and-copies",
    "missing-coin-sum-queries",
    "subordinates",
    "tree-matching",
    "tree-diameter",
    "tree-distances-i",
    "tree-distances-ii",
    "company-queries-i",
    "company-queries-ii",
    "distance-queries",
    "counting-paths",
    "subtree-queries",
    "path-queries",
    "path-queries-ii",
    "distinct-colors",
    "finding-a-centroid",
    "fixed-length-paths-i",
    "fixed-length-paths-ii",
    "josephus-queries",
    "exponentiation",
    "exponentiation-ii",
    "counting-divisors",
    "common-divisors",
    "sum-of-divisors",
    "divisor-analysis",
    "prime-multiples",
    "counting-coprime-pairs",
    "next-prime",
    "binomial-coefficients",
    "creating-strings-ii",
    "distributing-apples",
    "christmas-party",
    "permutation-order",
    "permutation-rounds",
    "bracket-sequences-i",
    "bracket-sequences-ii",
    "counting-necklaces",
    "counting-grids",
    "fibonacci-numbers",
    "throwing-dice",
    "graph-paths-i",
    "graph-paths-ii",
    "system-of-linear-equations",
    "sum-of-four-squares",
    "triangle-number-sums",
    "dice-probability",
    "moving-robots",
    "candy-lottery",
    "inversion-probability",
    "stick-game",
    "nim-game-i",
    "nim-game-ii",
    "stair-game",
    "grundys-game",
    "another-game",
    "word-combinations",
    "string-matching",
    "finding-borders",
    "finding-periods",
    "minimal-rotation",
    "longest-palindrome",
    "all-palindromes",
    "required-substring",
    "palindrome-queries",
    "finding-patterns",
    "counting-patterns",
    "pattern-positions",
    "distinct-substrings",
    "distinct-subsequences",
    "repeating-substring",
    "string-functions",
    "inverse-suffix-array",
    "string-transform",
    "substring-order-i",
    "substring-order-ii",
    "substring-distribution",
    "point-location-test",
    "line-segment-intersection",
    "polygon-area",
    "point-in-polygon",
    "polygon-lattice-points",
    "minimum-euclidean-distance",
    "convex-hull",
    "maximum-manhattan-distances",
    "all-manhattan-distances",
    "intersection-points",
    "line-segments-trace-i",
    "line-segments-trace-ii",
    "lines-and-queries-i",
    "lines-and-queries-ii",
    "area-of-rectangles",
    "robot-path",
    "meet-in-the-middle",
    "hamming-distance",
    "corner-subgrid-check",
    "corner-subgrid-count",
    "reachable-nodes",
    "reachability-queries",
    "cut-and-paste",
    "substring-reversals",
    "reversals-and-sums",
    "necessary-roads",
    "necessary-cities",
    "eulerian-subgraphs",
    "monster-game-i",
    "monster-game-ii",
    "subarray-squares",
    "houses-and-schools",
    "knuth-division",
    "apples-and-bananas",
    "one-bit-positions",
    "signal-processing",
    "new-roads-queries",
    "dynamic-connectivity",
    "parcel-delivery",
    "task-assignment",
    "distinct-routes-ii",
    "sliding-window-sum",
    "sliding-window-minimum",
    "sliding-window-xor",
    "sliding-window-or",
    "sliding-window-distinct-values",
    "sliding-window-mode",
    "sliding-window-mex",
    "sliding-window-median",
    "sliding-window-cost",
    "sliding-window-inversions",
    "sliding-window-advertisement",
    "hidden-integer",
    "hidden-permutation",
    "k-th-highest-score",
    "permuted-binary-strings",
    "colored-chairs",
    "inversion-sorting",
    "counting-bits",
    "maximum-xor-subarray",
    "maximum-xor-subset",
    "number-of-subset-xors",
    "k-subset-xors",
    "all-subarray-xors",
    "xor-pyramid-peak",
    "xor-pyramid-diagonal",
    "xor-pyramid-row",
    "sos-bit-problem",
    "and-subset-count",
    "inverse-inversions",
    "monotone-subsequences",
    "third-permutation",
    "permutation-prime-sums",
    "chess-tournament",
    "distinct-sums-grid",
    "filling-trominos",
    "grid-path-construction",
    "nearest-shops",
    "prufer-code",
    "tree-traversals",
    "course-schedule-ii",
    "acyclic-graph-edges",
    "strongly-connected-edges",
    "even-outdegree-edges",
    "graph-girth",
    "fixed-length-walk-queries",
    "transfer-speeds-sum",
    "mst-edge-check",
    "mst-edge-set-check",
    "mst-edge-cost",
    "network-breakdown",
    "tree-coin-collecting-i",
    "tree-coin-collecting-ii",
    "tree-isomorphism-i",
    "tree-isomorphism-ii",
    "flight-route-requests",
    "critical-cities",
    "visiting-cities",
    "graph-coloring",
    "bus-companies",
    "split-into-two-paths",
    "network-renovation",
    "forbidden-cities",
    "creating-offices",
    "new-flight-routes",
    "filled-subgrid-count-i",
    "filled-subgrid-count-ii",
    "all-letter-subgrid-count-i",
    "all-letter-subgrid-count-ii",
    "border-subgrid-count-i",
    "border-subgrid-count-ii",
    "raab-game-ii",
    "empty-string",
    "permutation-inversions",
    "counting-bishops",
    "counting-sequences",
    "grid-paths-ii",
    "counting-permutations",
    "grid-completion",
    "counting-reorders",
    "tournament-graph-distribution",
    "collecting-numbers-distribution",
    "functional-graph-distribution",
    "shortest-subsequence",
    "distinct-values-sum",
    "distinct-values-splits",
    "swap-game",
    "beautiful-permutation-ii",
    "multiplication-table",
    "bubble-sort-rounds-i",
    "bubble-sort-rounds-ii",
    "nearest-campsites-i",
    "nearest-campsites-ii",
    "advertisement",
    "special-substrings",
    "counting-lcm-arrays",
    "square-subsets",
    "subarray-sum-constraints",
    "water-containers-moves",
    "water-containers-queries",
    "stack-weights",
    "maximum-average-subarrays",
    "subsets-with-fixed-average",
    "two-array-average",
    "pyramid-array",
    "permutation-subsequence",
    "bit-inversions",
    "writing-numbers",
    "letter-pair-move-game",
    "maximum-building-i",
    "sorting-methods",
    "cyclic-array",
    "list-of-sums",
    "bouncing-ball-steps",
    "bouncing-ball-cycle",
    "knight-moves-queries",
    "k-subset-sums-i",
    "k-subset-sums-ii",
    "increasing-array-ii",
    "food-division",
    "swap-round-sorting",
    "binary-subsequences",
    "school-excursion",
    "coin-grid",
    "grid-coloring-ii",
    "programmers-and-artists",
    "removing-digits-ii",
    "coin-arrangement",
    "replace-with-difference",
    "grid-puzzle-i",
    "grid-puzzle-ii",
    "bit-substrings",
    "reversal-sorting",
    "book-shop-ii",
    "gcd-subsets",
    "minimum-cost-pairs",
    "same-sum-subsets",
    "mex-grid-queries",
    "maximum-building-ii",
    "stick-divisions",
    "stick-difference",
    "coding-company",
    "two-stacks-sorting"
)
    val array6 = listOf(
        "rotate-array",
        "squares-of-a-sorted-array",
        "kadanes-algo",
        "maximum-product-subarray",
        "majority-element",
        "majority-element-2",
        "next-greater-element-iii",
        "max-chunks-to-make-sorted",
        "max-chunks-to-make-sorted-ii",
        "number-of-subarrays-with-bounded-maximum",
        "first-missing-positive",
        "range-addition",
        "min-no-of-platform",
        "trapping-rain-water",
        "container-with-most-water",
        "two-sum",
        "two-difference",
        "permutations",
        "permutation-sequence",
        "combination-sum",
        "combination-sum-2",
        "letter-combination-of-phone-number",
        "n-queens",
        "rat-in-a-maze-path",
        "single-element",
        "single-element-2",
        "single-number-3",
        "divide-2-integers",
        "max-and-pair",
        "check-ap-sequence",
        "grid-illumination",
        "brick-wall",
        "count-of-subarray-with-sum-equals-k",
        "subarray-sum-divisible-by-k",
        "insert-delete-getrandom-o1",
        "insert-delete-get-random-duplicates-allowed",
        "longest-consecutive-sequence",
        "find-all-anagrams-in-a-string",
        "find-smallest-size-of-string-containing-all-char-of-other",
        "write-hashmap",
        "subarray-with-equal-number-of-0-and-1",
        "substring-with-equal-0-1-and-2",
        "kth-largest-element",
        "minimum-number-of-refueling-spots",
        "minimum-cost-to-connect-sticks",
        "employee-free-time",
        "find-median-from-data-stream",
        "capacity-to-ship-within-d-days",
        "painters-partition-problem",
        "search-in-rotated-sorted-array",
        "search-in-rotated-sorted-array-2",
        "allocate-books",
        "median-of-two-sorted-array",
        "reverse-linkedlist",
        "find-the-middle-element",
        "floyd-cycle",
        "clone-a-linkedlist",
        "intersection-point-of-2-linked-list",
        "lru-cache",
        "next-greater-element",
        "largest-rectangular-area-histogram",
        "maximum-size-binary-matrix-containing-1",
        "valid-parentheses",
        "min-stack",
        "k-stacks-in-a-single-array",
        "infix-evaluation",
        "k-reverse-in-a-queue",
        "k-queue",
        "preorder-traversal",
        "inorder-traversal",
        "postorder-traversal",
        "right-side-view",
        "left-view",
        "top-view",
        "bottom-view",
        "vertical-order",
        "diagonal-traversal",
        "boundary-traversal",
        "binary-tree-cameras",
        "max-path-sum",
        "delete-node-in-bst",
        "construct-from-inorder-and-preorder",
        "next-right-pointer-in-each-node",
        "convert-a-binary-tree-to-circular-doubly-linked-list",
        "conversion-of-sorted-dll-to-bst",
        "lowest-common-ancestor",
        "serialize-and-deserialize",
        "implement-trie",
        "max-xor-of-two-numbers-in-an-array",
        "maximum-xor-with-an-element-from-array",
        "longest-increasing-subsequence",
        "building-bridges",
        "russian-doll-envelopes",
        "box-stacking",
        "paint-house",
        "no-of-binary-string-without-consecutive-1",
        "possible-ways-to-construct-the-building",
        "total-no-of-bst",
        "no-of-balanced-parentheses-sequence",
        "min-cost-path",
        "cherry-pickup",
        "cherry-pickup-2",
        "best-time-to-buy-and-sell-stock",
        "best-time-to-buy-and-sell-stock-2",
        "buy-and-sell-with-transaction-fee",
        "best-time-to-buy-and-sell-with-cool-down",
        "best-time-to-buy-and-sell-stock-3",
        "best-time-to-buy-and-sell-stock-4",
        "burst-balloons",
        "optimal-bst",
        "matrix-chain-multiplication",
        "longest-common-subsequence",
        "count-all-palindromic-subsequence",
        "count-distinct-palindromic-subsequence",
        "no-of-sequence-of-type-ai-bj-ck",
        "2-egg-100-floor",
        "egg-drop",
        "regular-expression-matching",
        "palindrome-partitioning",
        "frog-jump",
        "edit-distance",
        "0-1-knapsack",
        "unbounded-knapsack",
        "fractional-knapsack",
        "coin-change-combination",
        "coin-change-permutation",
        "number-of-islands",
        "number-of-distinct-islands",
        "rotten-oranges",
        "bipartite-graph",
        "bus-routes",
        "prims-algo",
        "dijkstra-algo",
        "swim-in-rising-water",
        "0-1-matrix",
        "bellman-ford",
        "strongly-connected-components-kosarajus-algo",
        "mother-vertex",
        "kahns-algo",
        "alien-dictionary",
        "number-of-islands-ii",
        "regions-cut-by-slashes",
        "sentence-similarity-ii",
        "redundant-connection",
        "redundant-connection-2",
        "articulation-point",
        "min-swaps-required-to-sort-array",
        "sliding-puzzle",
        "floyd-warshall",
        "remove-max-number-of-edges-to-keep-graph-traversal"
    )
    val array7 = listOf(
        "two-sum",
        "best-time-to-buy-and-sell-stock",
        "plus-one",
        "move-zeroes",
        "squares-of-a-sorted-array",
        "best-time-to-buy-and-sell-stock-ii",
        "find-pivot-index",
        "majority-element",
        "pascals-triangle",
        "remove-duplicates-from-sorted-array",
        "merge-intervals",
        "3sum",
        "insert-delete-getrandom-o1",
        "subarray-sum-equals-k",
        "next-permutation",
        "container-with-most-water",
        "spiral-matrix",
        "rotate-image",
        "word-search",
        "3sum-closest",
        "game-of-life",
        "pairs-of-songs-with-total-durations-divisible-by-60",
        "4sum",
        "combination-sum",
        "jump-game-ii",
        "maximum-points-you-can-obtain-from-cards",
        "maximum-area-of-a-piece-of-cake-after-horizontal-and-vertical-cuts",
        "max-area-of-island",
        "find-all-duplicates-in-an-array",
        "k-diff-pairs-in-an-array",
        "invalid-transactions",
        "jump-game",
        "subarray-sums-divisible-by-k",
        "best-time-to-buy-and-sell-stock-iii",
        "first-missing-positive",
        "largest-rectangle-in-histogram",
        "insert-delete-getrandom-o1-duplicates-allowed",
        "max-value-of-equation",
        "partition-labels",
        "sort-colors",
        "longest-repeating-character-replacement",
        "subarrays-with-k-different-integers",
        "count-negative-numbers-in-a-sorted-matrix",
        "peak-index-in-a-mountain-array",
        "time-based-key-value-store",
        "search-in-rotated-sorted-array",
        "powx-n",
        "find-first-and-last-position-of-element-in-sorted-array",
        "find-peak-element",
        "search-a-2d-matrix",
        "divide-two-integers",
        "capacity-to-ship-packages-within-d-days",
        "minimum-limit-of-balls-in-a-bag",
        "median-of-two-sorted-arrays",
        "count-of-smaller-numbers-after-self",
        "split-array-largest-sum",
        "verifying-an-alien-dictionary",
        "design-hashmap",
        "top-k-frequent-elements",
        "design-twitter",
        "reverse-linked-list",
        "middle-of-the-linked-list",
        "merge-two-sorted-lists",
        "remove-nth-node-from-end-of-list",
        "delete-node-in-a-linked-list",
        "add-two-numbers-ii",
        "intersection-of-two-linked-lists",
        "palindrome-linked-list",
        "reverse-nodes-in-k-group",
        "linked-list-cycle-ii",
        "rotate-list",
        "flatten-a-multilevel-doubly-linked-list",
        "copy-list-with-random-pointer",
        "find-the-duplicate-number",
        "next-greater-element-i",
        "next-greater-element-ii",
        "implement-stack-using-queues",
        "implement-queue-using-stacks",
        "valid-parentheses",
        "min-stack",
        "lru-cache",
        "sliding-window-maximum",
        "lfu-cache",
        "diameter-of-binary-tree",
        "invert-binary-tree",
        "subtree-of-another-tree",
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
        "maximum-width-of-binary-tree",
        "count-good-nodes-in-binary-tree",
        "lowest-common-ancestor-of-a-binary-tree",
        "binary-tree-right-side-view",
        "all-nodes-distance-k-in-binary-tree",
        "serialize-and-deserialize-binary-tree",
        "binary-tree-zigzag-level-order-traversal",
        "binary-search-tree-iterator",
        "binary-tree-level-order-traversal",
        "path-sum-iii",
        "construct-binary-tree-from-preorder-and-postorder-traversal",
        "populating-next-right-pointers-in-each-node",
        "flatten-binary-tree-to-linked-list",
        "range-sum-of-bst",
        "validate-binary-search-tree",
        "unique-binary-search-trees-ii",
        "kth-smallest-element-in-a-bst",
        "unique-binary-search-trees",
        "recover-binary-search-tree",
        "find-median-from-data-stream",
        "kth-largest-element-in-an-array",
        "merge-k-sorted-lists",
        "the-skyline-problem",
        "task-scheduler",
        "network-delay-time",
        "last-stone-weight",
        "n-meetings-in-one-room",
        "activity-selection",
        "fractional-knapsack",
        "minimum-platforms",
        "job-sequencing-problem",
        "rat-in-a-maze",
        "the-knights-tour-problem",
        "subset-sum",
        "letter-combinations-of-a-phone-number",
        "generate-parentheses",
        "sudoku-solver",
        "permutations",
        "combinations",
        "combination-sum-ii",
        "n-queen-problem",
        "subsets",
        "gray-code",
        "maximum-product-subarray",
        "longest-increasing-subsequence",
        "longest-common-subsequence",
        "0-1-knapsack-problem",
        "edit-distance",
        "maximum-sum-increasing-subsequence",
        "matrix-chain-multiplication",
        "minimum-path-sum",
        "coin-change",
        "subset-sum-problem",
        "rod-cutting",
        "super-egg-drop",
        "word-break",
        "palindrome-partitioning-ii",
        "clone-graph",
        "course-schedule",
        "minimum-height-trees",
        "reconstruct-itinerary",
        "evaluate-division",
        "number-of-provinces",
        "redundant-connection",
        "is-graph-bipartite",
        "cheapest-flights-within-k-stops",
        "find-eventual-safe-states",
        "keys-and-rooms",
        "reachable-nodes-in-subdivided-graph",
        "possible-bipartition",
        "satisfiability-of-equality-equations",
        "find-the-town-judge",
        "critical-connections-in-a-network",
        "number-of-operations-to-make-network-connected",
        "frog-position-after-t-seconds",
        "parallel-courses-ii",
        "word-ladder",
        "word-ladder-ii",
        "regions-cut-by-slashes",
        "add-strings",
        "longest-common-prefix",
        "valid-palindrome-ii",
        "roman-to-integer",
        "implement-strstr",
        "longest-substring-without-repeating-characters",
        "minimum-remove-to-make-valid-parentheses",
        "longest-palindromic-substring",
        "group-anagrams",
        "longest-substring-with-at-least-k-repeating-characters",
        "sliding-window-median",
        "implement-trie-prefix-tree",
        "stream-of-characters",
        "remove-sub-folders-from-the-filesystem",
        "maximum-product-of-three-numbers",
        "encode-and-decode-tinyurl",
        "string-to-integer-atoi",
        "happy-number",
        "multiply-strings",
        "angle-between-hands-of-a-clock",
        "integer-break"
    )
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


private const val PREFS_NAME = "LeetCodePrefs"
private const val KEY_SAVED_PROBLEMS = "SavedProblems"
private const val KEY_CACHED_PROBLEMS = "CachedProblems"
private const val KEY_CHECKED_PROBLEMS = "CheckedProblems"
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
// FIXED VERSION - Letters only + Priority by lowest array index

fun String.lettersOnly(): String = filter { it.isLetter() }

fun matchProblemsWithArrays(problems: List<ProblemStat>): List<EnhancedProblemStat> {
    val arrays = QuestionArrays.getAllArrays()

    // Count occurrences in each array for exact matches
    val occurrenceCount = hashMapOf<String, Int>()
    val firstArrayIndex = hashMapOf<String, Int>()

    for ((index, questions) in arrays) {
        for (q in questions) {
            val processed = q.lowercase().lettersOnly()
            occurrenceCount[processed] = (occurrenceCount[processed] ?: 0) + 1

            // Store first array where it appears (for tie-breaking)
            if (!firstArrayIndex.containsKey(processed)) {
                firstArrayIndex[processed] = index
            }
        }
    }

    // Log for debugging
    val twoSumCount = occurrenceCount["twosum"]
    Log.d("Matching", "two-sum appears in $twoSumCount arrays (Priority will be $twoSumCount)")

    // Preprocess arrays for fuzzy matching
    val fuzzyArrays = arrays.map { (index, questions) ->
        index to questions.map { it.lowercase().lettersOnly() }
    }

    return problems.map { problem ->
        val slug = (problem.stat.question__title_slug ?: "").lowercase().lettersOnly()

        var priority = 0
        var matchPct = 0.0
        var arrayIdx = 0

        // Try exact match first - priority = occurrence count
        occurrenceCount[slug]?.let { count ->
            priority = count
            matchPct = 100.0
            arrayIdx = firstArrayIndex[slug] ?: 0
        }

        // Fuzzy match if no exact match
        if (priority == 0 && slug.length >= 3) {
            for ((idx, questions) in fuzzyArrays) {
                for (q in questions) {
                    if (q.length < 3) continue

                    val sim = lcs(slug, q).toDouble() / minOf(slug.length, q.length)

                    if (sim >= 0.5 && priority == 0) {
                        priority = 1 // Fuzzy matches get priority 1
                        matchPct = sim * 100
                        arrayIdx = idx
                        break
                    }
                }
                if (priority > 0) break
            }
        }

        EnhancedProblemStat(
            stat = problem.stat,
            status = problem.status,
            difficulty = problem.difficulty,
            paid_only = problem.paid_only,
            is_favor = problem.is_favor,
            frequency = problem.frequency,
            progress = problem.progress,
            priority = priority,
            matchPercentage = matchPct,
            matchedArray = arrayIdx
        )
    }.sortedWith(
        compareByDescending<EnhancedProblemStat> { it.priority }
            .thenByDescending { it.matchPercentage }
            .thenBy { it.stat.frontend_question_id }
    )}

// LCS helper (unchanged)
fun lcs(a: String, b: String): Int {
    if (a.isEmpty() || b.isEmpty()) return 0

    val (s, l) = if (a.length <= b.length) a to b else b to a
    var prev = IntArray(s.length + 1)
    var curr = IntArray(s.length + 1)

    for (i in 1..l.length) {
        for (j in 1..s.length) {
            curr[j] = if (l[i-1] == s[j-1]) prev[j-1] + 1
            else maxOf(prev[j], curr[j-1])
        }
        val t = prev; prev = curr; curr = t
    }

    return prev[s.length]
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
    onProblemClick: (slug: String, url: String) -> Unit = { _, _ -> }
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

        // Search state variables
        var searchQuery by remember { mutableStateOf("") }
        var isSearchActive by remember { mutableStateOf(false) }

        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        var checkedMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

        // Filter problems based on search query
        val filteredProblems = remember(problems, searchQuery) {
            if (searchQuery.isBlank()) {
                problems
            } else {
                problems.filter { problem ->
                    problem.stat.question__title?.contains(searchQuery, ignoreCase = true) == true ||
                            problem.stat.question__title_slug?.contains(searchQuery, ignoreCase = true) == true ||
                            problem.stat.frontend_question_id.toString().contains(searchQuery, ignoreCase = true)
                }
            }
        }

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
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search Bar
                        SearchBar(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onSearchActiveChange = { isSearchActive = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        // Search results info
                        if (searchQuery.isNotBlank()) {
                            Text(
                                text = "Found ${filteredProblems.size} of ${problems.size} problems",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
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
                                items = filteredProblems, // Use filteredProblems instead of problems
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
}

// Separate SearchBar composable for better organization
@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = modifier,
        placeholder = {
            Text(
                "Search the question",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            if (searchQuery.isNotBlank()) {
                IconButton(
                    onClick = { onSearchQueryChange("") }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search"
                    )
                }
            }
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    )
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
