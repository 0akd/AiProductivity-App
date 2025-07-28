

@file:OptIn(ExperimentalMaterial3Api::class)
package com.arjundubey.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
// Additional imports needed
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlin.math.*
    data class ExerciseProfile(
val id: String = "",
val name: String = "",
val iconName: String = "FitnessCenter",
val defaultMinutes: Int = 5,
val colorHex: String = "#4CAF50",
val userId: String = "",
val createdAt: Long = System.currentTimeMillis(),
val order: Int? = null // Add this field
) {
    fun getIcon(): ImageVector = when (iconName) {
        "FitnessCenter" -> Icons.Default.FitnessCenter
        "Timer" -> Icons.Default.Timer
        "DirectionsRun" -> Icons.Default.DirectionsRun
        "Sports" -> Icons.Default.Sports
        "Settings" -> Icons.Default.Settings
        "SportsGymnastics" -> Icons.Default.SportsGymnastics
        "DirectionsWalk" -> Icons.Default.DirectionsWalk
        "Pool" -> Icons.Default.Pool
        else -> Icons.Default.FitnessCenter
    }

    fun getColor(): Color = Color(android.graphics.Color.parseColor(colorHex))
}

enum class TimerState {
    STOPPED, RUNNING, PAUSED
}

class ExerciseTimerViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _profiles = MutableLiveData<List<ExerciseProfile>>(emptyList())
    val profiles: LiveData<List<ExerciseProfile>> = _profiles

    private val _selectedProfileIndex = MutableLiveData(0)
    val selectedProfileIndex: LiveData<Int> = _selectedProfileIndex

    private val _timerState = MutableLiveData(TimerState.STOPPED)
    val timerState: LiveData<TimerState> = _timerState

    private val _remainingSeconds = MutableLiveData(0)
    val remainingSeconds: LiveData<Int> = _remainingSeconds
    private var reorderJob: Job? = null
    private val reorderDelay = 500L // 500ms delay
    private val _totalSeconds = MutableLiveData(0)
    val totalSeconds: LiveData<Int> = _totalSeconds

    private val _showProfileDialog = MutableLiveData(false)
    val showProfileDialog: LiveData<Boolean> = _showProfileDialog

    private val _editingProfile = MutableLiveData<ExerciseProfile?>(null)
    val editingProfile: LiveData<ExerciseProfile?> = _editingProfile

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private var profilesListener: ListenerRegistration? = null
    private var hasCheckedForDefaultProfiles = false // Add this flag

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        val userId = auth.currentUser?.uid ?: return

        profilesListener = db.collection("exercise_profiles")
            .whereEqualTo("userId", userId)
            .orderBy("order")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _errorMessage.value = "Failed to load profiles: ${error.message}"
                    return@addSnapshotListener
                }

                val profilesList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ExerciseProfile::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                _profiles.value = profilesList

                // Only create default profiles once, when we first load and list is empty
                if (profilesList.isEmpty() &&
                    snapshot != null &&
                    !snapshot.metadata.isFromCache &&
                    !hasCheckedForDefaultProfiles) {
                    hasCheckedForDefaultProfiles = true
                    createDefaultProfiles()
                } else {
                    val currentIndex = _selectedProfileIndex.value ?: 0
                    if (currentIndex >= profilesList.size && profilesList.isNotEmpty()) {
                        _selectedProfileIndex.value = 0
                    }
                    updateTimerForSelectedProfile()
                }
            }
    }

    private fun createDefaultProfiles() {
        val userId = auth.currentUser?.uid ?: return
        val defaultProfiles = listOf(
            ExerciseProfile(
                name = "Push-ups",
                iconName = "FitnessCenter",
                defaultMinutes = 5,
                colorHex = "#4CAF50",
                userId = userId,
                order = 0
            ),
            ExerciseProfile(
                name = "Plank",
                iconName = "Timer",
                defaultMinutes = 3,
                colorHex = "#2196F3",
                userId = userId,
                order = 1
            ),
            ExerciseProfile(
                name = "Squats",
                iconName = "DirectionsRun",
                defaultMinutes = 4,
                colorHex = "#9C27B0",
                userId = userId,
                order = 2
            ),
            ExerciseProfile(
                name = "Burpees",
                iconName = "Sports",
                defaultMinutes = 6,
                colorHex = "#FF5722",
                userId = userId,
                order = 3
            ),
            ExerciseProfile(
                name = "Yoga",
                iconName = "SportsGymnastics",
                defaultMinutes = 15,
                colorHex = "#FF9800",
                userId = userId,
                order = 4
            )
        )

        viewModelScope.launch {
            try {
                defaultProfiles.forEach { profile ->
                    db.collection("exercise_profiles").add(profile).await()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create default profiles: ${e.message}"
            }
        }
    }

    fun selectProfile(index: Int) {
        _selectedProfileIndex.value = index
        updateTimerForSelectedProfile()
    }
    fun stopTimer(context: Context) {
        _timerState.value = TimerState.STOPPED
        cancelTimerWork(context)
        // Don't reset the time - keep current remaining seconds
    }

    fun seekToTime(seekSeconds: Int) {
        val totalSecs = _totalSeconds.value ?: 0
        if (totalSecs > 0 && seekSeconds in 0..totalSecs) {
            _remainingSeconds.value = seekSeconds

            // If timer is running, restart the WorkManager task with new time
            if (_timerState.value == TimerState.RUNNING) {
                // Get current context - you might need to pass context to this method
                // or store it as a class variable if needed for WorkManager operations
                // For now, we'll just update the local timer
                startLocalTimer()
            }
        }
    }
    private fun updateTimerForSelectedProfile() {
        if (_timerState.value == TimerState.STOPPED) {
            val profile = getCurrentProfile()
            if (profile != null) {
                val seconds = profile.defaultMinutes * 60
                _remainingSeconds.value = seconds
                _totalSeconds.value = seconds
            }
        }
    }

    fun getCurrentProfile(): ExerciseProfile? {
        val profiles = _profiles.value ?: return null
        val index = _selectedProfileIndex.value ?: return null
        return if (index < profiles.size) profiles[index] else null
    }

    fun startTimer(context: Context) {
        _timerState.value = TimerState.RUNNING
        startTimerWork(context)
        startLocalTimer()
    }

    fun pauseTimer(context: Context) {
        _timerState.value = TimerState.PAUSED
        cancelTimerWork(context)
    }

    fun resetTimer(context: Context) {
        _timerState.value = TimerState.STOPPED
        cancelTimerWork(context)
        updateTimerForSelectedProfile()
    }

    private fun startLocalTimer() {
        viewModelScope.launch {
            while (_timerState.value == TimerState.RUNNING && (_remainingSeconds.value ?: 0) > 0) {
                delay(1000L)
                val current = _remainingSeconds.value ?: 0
                if (current > 0) {
                    _remainingSeconds.value = current - 1
                } else {
                    _timerState.value = TimerState.STOPPED
                    break
                }
            }
        }
    }

    private fun startTimerWork(context: Context) {
        val profile = getCurrentProfile() ?: return
        val workData = workDataOf(
            "profileName" to profile.name,
            "colorHex" to profile.colorHex,
            "remainingSeconds" to (_remainingSeconds.value ?: 0)
        )

        val timerWork = OneTimeWorkRequestBuilder<TimerWorker>()
            .setInputData(workData)
            .addTag("exercise_timer")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "exercise_timer",
            ExistingWorkPolicy.REPLACE,
            timerWork
        )
    }

    private fun cancelTimerWork(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("exercise_timer")
    }

    fun showAddProfileDialog() {
        _editingProfile.value = null
        _showProfileDialog.value = true
    }

    fun showEditProfileDialog(profile: ExerciseProfile) {
        _editingProfile.value = profile
        _showProfileDialog.value = true
    }

    fun hideProfileDialog() {
        _showProfileDialog.value = false
        _editingProfile.value = null
    }

    fun addProfile(profile: ExerciseProfile) {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        val currentProfiles = _profiles.value?.toMutableList() ?: mutableListOf()
        val newOrder = currentProfiles.size

        // Create profile with temporary ID for optimistic update
        val profileWithTempId = profile.copy(
            userId = userId,
            order = newOrder,
            id = "temp_${System.currentTimeMillis()}" // Temporary ID
        )

        // Optimistic update - add to UI immediately
        currentProfiles.add(profileWithTempId)
        _profiles.value = currentProfiles

        viewModelScope.launch {
            try {
                // Add to Firestore
                val docRef = db.collection("exercise_profiles")
                    .add(profile.copy(userId = userId, order = newOrder))
                    .await()

                // Update the temporary profile with real Firestore ID
                val updatedProfiles = _profiles.value?.toMutableList() ?: mutableListOf()
                val tempIndex = updatedProfiles.indexOfFirst { it.id == profileWithTempId.id }
                if (tempIndex != -1) {
                    updatedProfiles[tempIndex] = profileWithTempId.copy(id = docRef.id)
                    _profiles.value = updatedProfiles
                }

            } catch (e: Exception) {
                // Revert optimistic update on failure
                val revertedProfiles = _profiles.value?.toMutableList() ?: mutableListOf()
                revertedProfiles.removeIf { it.id == profileWithTempId.id }
                _profiles.value = revertedProfiles

                _errorMessage.value = "Failed to add profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(profile: ExerciseProfile) {
        if (profile.id.isEmpty()) return
        _isLoading.value = true

        // Optimistic update - update UI immediately
        val currentProfiles = _profiles.value?.toMutableList() ?: mutableListOf()
        val profileIndex = currentProfiles.indexOfFirst { it.id == profile.id }
        val oldProfile = if (profileIndex != -1) currentProfiles[profileIndex] else null

        if (profileIndex != -1) {
            currentProfiles[profileIndex] = profile
            _profiles.value = currentProfiles
        }

        viewModelScope.launch {
            try {
                db.collection("exercise_profiles")
                    .document(profile.id)
                    .set(profile)
                    .await()
            } catch (e: Exception) {
                // Revert optimistic update on failure
                if (profileIndex != -1 && oldProfile != null) {
                    val revertedProfiles = _profiles.value?.toMutableList() ?: mutableListOf()
                    if (profileIndex < revertedProfiles.size) {
                        revertedProfiles[profileIndex] = oldProfile
                        _profiles.value = revertedProfiles
                    }
                }
                _errorMessage.value = "Failed to update profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun deleteProfile(profileId: String) {
        if (profileId.isEmpty()) return
        _isLoading.value = true

        // Store the current state for potential rollback
        val currentProfiles = _profiles.value?.toMutableList() ?: mutableListOf()
        val currentIndex = _selectedProfileIndex.value ?: 0
        val profileToDelete = currentProfiles.find { it.id == profileId }
        val profileIndex = currentProfiles.indexOfFirst { it.id == profileId }

        if (profileToDelete == null) {
            _isLoading.value = false
            return
        }

        // Optimistically remove from local list for immediate UI feedback
        currentProfiles.removeAt(profileIndex)
        _profiles.value = currentProfiles

        // Adjust selected index immediately
        val newSelectedIndex = when {
            currentProfiles.isEmpty() -> 0
            currentIndex >= currentProfiles.size -> maxOf(0, currentProfiles.size - 1)
            profileIndex <= currentIndex && currentIndex > 0 -> currentIndex - 1
            else -> currentIndex
        }
        _selectedProfileIndex.value = newSelectedIndex

        viewModelScope.launch {
            try {
                // Delete from Firestore
                db.collection("exercise_profiles")
                    .document(profileId)
                    .delete()
                    .await()

                // Real-time listener will confirm the deletion

            } catch (e: Exception) {
                // Revert optimistic update on failure
                val revertedProfiles = _profiles.value?.toMutableList() ?: mutableListOf()
                revertedProfiles.add(profileIndex, profileToDelete)
                _profiles.value = revertedProfiles
                _selectedProfileIndex.value = currentIndex

                _errorMessage.value = "Failed to delete profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Add these properties to your ViewModel class


    // Enhanced reorderProfiles method with debouncing
    fun reorderProfiles(fromIndex: Int, toIndex: Int) {
        val currentProfiles = _profiles.value?.toMutableList() ?: return
        if (fromIndex < 0 || fromIndex >= currentProfiles.size ||
            toIndex < 0 || toIndex >= currentProfiles.size) return

        val userId = auth.currentUser?.uid ?: return

        // Cancel previous reorder job
        reorderJob?.cancel()

        // Optimistic update - update UI immediately
        val tempList = currentProfiles.toMutableList()
        val item = tempList.removeAt(fromIndex)
        tempList.add(toIndex, item)

        // Update orders in the temp list
        tempList.forEachIndexed { index, profile ->
            tempList[index] = profile.copy(order = index)
        }

        _profiles.value = tempList

        // Adjust selected index immediately
        val currentSelectedIndex = _selectedProfileIndex.value ?: 0
        val newSelectedIndex = when {
            currentSelectedIndex == fromIndex -> toIndex
            currentSelectedIndex in (minOf(fromIndex, toIndex) + 1)..maxOf(fromIndex, toIndex) -> {
                if (fromIndex < toIndex) currentSelectedIndex - 1 else currentSelectedIndex + 1
            }
            else -> currentSelectedIndex
        }
        _selectedProfileIndex.value = newSelectedIndex

        // Debounced Firestore update
        reorderJob = viewModelScope.launch {
            delay(reorderDelay) // Wait for rapid changes to settle

            try {
                val finalProfiles = _profiles.value ?: return@launch
                val batch = db.batch()
                finalProfiles.forEachIndexed { index, profile ->
                    if (profile.id.isNotEmpty() && !profile.id.startsWith("temp_")) {
                        val docRef = db.collection("exercise_profiles").document(profile.id)
                        batch.update(docRef, "order", index)
                    }
                }
                batch.commit().await()

            } catch (e: Exception) {
                // On failure, the real-time listener will eventually correct the order
                _errorMessage.value = "Failed to save new order: ${e.message}"
            }
        }
    }

    // Also add this method to handle cleanup
    override fun onCleared() {
        super.onCleared()
        profilesListener?.remove()
        reorderJob?.cancel() // Cancel any pending reorder operations
    }
    // Function to delete all default profiles (call this once to clean up)
    fun deleteAllDefaultProfiles() {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val snapshot = db.collection("exercise_profiles")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()

                // Reset the flag so you can create new profiles if needed
                hasCheckedForDefaultProfiles = false

            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete profiles: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }


}

@Composable
fun ExerciseTimerScreen(viewModel: ExerciseTimerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val context = LocalContext.current
    val profiles by viewModel.profiles.observeAsState(emptyList())
    val selectedProfileIndex by viewModel.selectedProfileIndex.observeAsState(0)
    val timerState by viewModel.timerState.observeAsState(TimerState.STOPPED)
    val remainingSeconds by viewModel.remainingSeconds.observeAsState(0)
    val totalSeconds by viewModel.totalSeconds.observeAsState(0)
    val showProfileDialog by viewModel.showProfileDialog.observeAsState(false)
    val editingProfile by viewModel.editingProfile.observeAsState(null)
    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState(null)

    val currentProfile = if (selectedProfileIndex < profiles.size) profiles[selectedProfileIndex] else null

    // Create notification channel
    LaunchedEffect(Unit) {
        createNotificationChannel(context)
    }

    // Show error messages
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            // You can show a snackbar or toast here
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Exercise Timer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(onClick = { viewModel.showAddProfileDialog() }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Profile",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Profile Selection
        if (profiles.isNotEmpty()) {
            Text(
                text = "Choose Exercise Profile",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Replace the existing LazyRow in your ExerciseTimerScreen with this:
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(profiles.indices.toList()) { index ->
                    ProfileCard(
                        profile = profiles[index],
                        isSelected = selectedProfileIndex == index,
                        onClick = { viewModel.selectProfile(index) },
                        onEdit = { viewModel.showEditProfileDialog(profiles[index]) },
                        onDelete = { viewModel.deleteProfile(profiles[index].id) },
                        onMoveLeft = {
                            if (index > 0) {
                                viewModel.reorderProfiles(index, index - 1)
                            }
                        },
                        onMoveRight = {
                            if (index < profiles.size - 1) {
                                viewModel.reorderProfiles(index, index + 1)
                            }
                        },
                        canMoveLeft = index > 0,
                        canMoveRight = index < profiles.size - 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Timer Display with Circular Slider
            currentProfile?.let { profile ->
                CircularTimerWithSlider(
                    profile = profile,
                    remainingSeconds = remainingSeconds,
                    totalSeconds = totalSeconds,
                    timerState = timerState,
                    onSeek = { seekSeconds ->
                        viewModel.seekToTime(seekSeconds)
                    },
                    onStart = { viewModel.startTimer(context) },
                    onPause = { viewModel.pauseTimer(context) },
                    onStop = { viewModel.stopTimer(context) },
                    onReset = { viewModel.resetTimer(context) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status Text
                Text(
                    text = when (timerState) {
                        TimerState.STOPPED -> if (remainingSeconds == 0) "🎉 Time's up!" else "Ready to start"
                        TimerState.RUNNING -> "⏱️ Timer running..."
                        TimerState.PAUSED -> "⏸️ Timer paused"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Loading state
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading profiles...")
        }
    }

    // Add/Edit Profile Dialog
    if (showProfileDialog) {
        AddEditProfileDialog(
            profile = editingProfile,
            onDismiss = { viewModel.hideProfileDialog() },
            onSave = { profile ->
                if (editingProfile != null) {
                    viewModel.updateProfile(profile)
                } else {
                    viewModel.addProfile(profile)
                }
                viewModel.hideProfileDialog()
            }
        )
    }
}


@Composable
fun ProfileCard(
    profile: ExerciseProfile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    canMoveLeft: Boolean,
    canMoveRight: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(120.dp) // Slightly wider to accommodate arrows
            .height(140.dp), // Slightly taller
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) profile.getColor().copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(profile.getColor()),
            width = 2.dp
        ) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = profile.getIcon(),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isSelected) profile.getColor() else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) profile.getColor() else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                Text(
                    text = "${profile.defaultMinutes}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) profile.getColor() else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Menu button (top right)
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    modifier = Modifier.size(16.dp)
                )
            }

            // Left arrow button (bottom left)
            if (canMoveLeft) {
                IconButton(
                    onClick = onMoveLeft,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Move Left",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Right arrow button (bottom right)
            if (canMoveRight) {
                IconButton(
                    onClick = onMoveRight,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Move Right",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        showMenu = false
                        onEdit()
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, null) }
                )
            }
        }
    }
}

@Composable
fun AddEditProfileDialog(
    profile: ExerciseProfile? = null,
    onDismiss: () -> Unit,
    onSave: (ExerciseProfile) -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var minutes by remember { mutableStateOf(profile?.defaultMinutes?.toString() ?: "5") }
    var selectedIcon by remember { mutableStateOf(profile?.iconName ?: "FitnessCenter") }
    var selectedColor by remember { mutableStateOf(profile?.colorHex ?: "#4CAF50") }

    val iconOptions = listOf(
        "FitnessCenter", "Timer", "DirectionsRun", "Sports",
        "SportsGymnastics", "DirectionsWalk", "Pool"
    )

    val colorOptions = listOf(
        "#4CAF50", "#2196F3", "#9C27B0", "#FF5722",
        "#FF9800", "#795548", "#607D8B"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (profile == null) "Add Profile" else "Edit Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = minutes,
                    onValueChange = { if (it.all { char -> char.isDigit() }) minutes = it },
                    label = { Text("Duration (minutes)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Icon", style = MaterialTheme.typography.labelMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(iconOptions) { iconName ->
                        IconButton(
                            onClick = { selectedIcon = iconName },
                            modifier = Modifier
                                .background(
                                    if (selectedIcon == iconName) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                imageVector = ExerciseProfile(iconName = iconName).getIcon(),
                                contentDescription = null
                            )
                        }
                    }
                }

                Text("Color", style = MaterialTheme.typography.labelMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(colorOptions) { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Color(android.graphics.Color.parseColor(colorHex)),
                                    RoundedCornerShape(20.dp)
                                )
                                .then(
                                    if (selectedColor == colorHex)
                                        Modifier.padding(4.dp).background(
                                            MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(16.dp)
                                        )
                                    else Modifier
                                )
                        ) {
                            Button(
                                onClick = { selectedColor = colorHex },
                                modifier = Modifier.fillMaxSize(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                )
                            ) {}
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && minutes.isNotBlank()) {
                                onSave(
                                    ExerciseProfile(
                                        id = profile?.id ?: "",
                                        name = name,
                                        iconName = selectedIcon,
                                        defaultMinutes = minutes.toIntOrNull() ?: 5,
                                        colorHex = selectedColor,
                                        userId = profile?.userId ?: ""
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun CircularTimerWithSlider(
    profile: ExerciseProfile,
    remainingSeconds: Int,
    totalSeconds: Int,
    timerState: TimerState,
    onSeek: (Int) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }

    val progress = if (totalSeconds > 0) {
        if (isDragging) {
            dragProgress
        } else {
            ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        }
    } else 0f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(300.dp) // Slightly larger to accommodate buttons
            .pointerInput(totalSeconds) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Only allow dragging when timer is stopped or paused
                        if (timerState == TimerState.STOPPED || timerState == TimerState.PAUSED) {
                            isDragging = true
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val angle = calculateAngleFromOffset(offset, center)
                            dragProgress = (angle / 360f).coerceIn(0f, 1f)
                        }
                    },
                    onDrag = { change, _ ->
                        if (isDragging) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val angle = calculateAngleFromOffset(change.position, center)
                            dragProgress = (angle / 360f).coerceIn(0f, 1f)
                        }
                    },
                    onDragEnd = {
                        if (isDragging && totalSeconds > 0) {
                            val seekSeconds = (totalSeconds * (1f - dragProgress)).toInt()
                            onSeek(seekSeconds.coerceIn(0, totalSeconds))
                        }
                        isDragging = false
                    }
                )
            }
    ) {
        // Background circle
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f - 30.dp.toPx() // More space for buttons
            val center = Offset(size.width / 2f, size.height / 2f)

            // Draw background track
            drawCircle(
                color = profile.getColor().copy(alpha = 0.2f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Draw progress arc
            if (progress > 0f) {
                drawArc(
                    color = profile.getColor(),
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }

            // Draw draggable handle (only when dragging or when timer is not running)
            if (totalSeconds > 0 && (isDragging || timerState != TimerState.RUNNING)) {
                val handleAngle = (progress * 360f - 90f) * (PI / 180f)
                val handleX = center.x + cos(handleAngle.toFloat()) * radius
                val handleY = center.y + sin(handleAngle.toFloat()) * radius

                // Handle shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.2f),
                    radius = 12.dp.toPx(),
                    center = Offset(handleX + 2f, handleY + 2f)
                )

                // Handle
                drawCircle(
                    color = profile.getColor(),
                    radius = 10.dp.toPx(),
                    center = Offset(handleX, handleY)
                )

                // Handle inner circle
                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = Offset(handleX, handleY)
                )
            }
        }

        // Timer content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = profile.getIcon(),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = profile.getColor()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            val displaySeconds = if (isDragging && totalSeconds > 0) {
                (totalSeconds * (1f - dragProgress)).toInt()
            } else {
                remainingSeconds
            }

            Text(
                text = formatTime(displaySeconds),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (isDragging) {
                    profile.getColor().copy(alpha = 0.7f)
                } else {
                    profile.getColor()
                }
            )

            if (isDragging) {
                Text(
                    text = "Release to seek",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Control buttons row
            if (!isDragging) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset button
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }

                    // Start/Pause button (main action)
                    FloatingActionButton(
                        onClick = {
                            when (timerState) {
                                TimerState.STOPPED -> onStart()
                                TimerState.RUNNING -> onPause()
                                TimerState.PAUSED -> onStart()
                            }
                        },
                        containerColor = profile.getColor(),
                        modifier = Modifier.size(56.dp),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Icon(
                            imageVector = when (timerState) {
                                TimerState.STOPPED -> Icons.Default.PlayArrow
                                TimerState.RUNNING -> Icons.Default.Pause
                                TimerState.PAUSED -> Icons.Default.PlayArrow
                            },
                            contentDescription = when (timerState) {
                                TimerState.STOPPED -> "Start"
                                TimerState.RUNNING -> "Pause"
                                TimerState.PAUSED -> "Resume"
                            },
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
                        )
                    }

                    // Stop button (only when timer is active)
                    if (timerState != TimerState.STOPPED) {
                        IconButton(
                            onClick = onStop, // Now calls onStop instead of onReset
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        // Invisible spacer to maintain layout balance
                        Spacer(modifier = Modifier.size(44.dp))
                    }
                }
            }
        }
    }
}


private fun calculateAngleFromOffset(offset: Offset, center: Offset): Float {
    val deltaX = offset.x - center.x
    val deltaY = offset.y - center.y
    val angle = atan2(deltaY, deltaX) * (180f / PI.toFloat()) + 90f
    return if (angle < 0) angle + 360f else angle
}


// WorkManager class for background timer
class TimerWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "exercise_timer_channel"
    }

    override fun doWork(): Result {
        val profileName = inputData.getString("profileName") ?: "Exercise"
        val colorHex = inputData.getString("colorHex") ?: "#4CAF50"
        var remainingSeconds = inputData.getInt("remainingSeconds", 0)

        try {
            while (remainingSeconds > 0 && !isStopped) {
                updateNotification(profileName, remainingSeconds, colorHex)
                Thread.sleep(1000)
                remainingSeconds--
            }

            if (remainingSeconds <= 0 && !isStopped) {
                showCompletionNotification(profileName)
                playAlarmSound()
                vibrate()
            } else {
                // Clear notification if timer was stopped
                clearNotification()
            }

            return Result.success()
        } catch (e: Exception) {
            clearNotification()
            return Result.failure()
        }
    }

    private fun updateNotification(profileName: String, remainingSeconds: Int, colorHex: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop action intent
        val stopIntent = Intent(applicationContext, TimerStopReceiver::class.java)
        val stopPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("$profileName Timer")
            .setContentText("Time remaining: ${formatTime(remainingSeconds)}")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setColor(Color(android.graphics.Color.parseColor(colorHex)).toArgb())
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .setProgress(0, 0, false)
            .setSilent(true) // Prevent notification sound during countdown
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(profileName: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("$profileName Timer Complete!")
            .setContentText("Your exercise session is finished. Great job!")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun clearNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun playAlarmSound() {
        try {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notification)
            ringtone?.play()

            // Stop ringtone after 5 seconds to prevent indefinite ringing
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                ringtone?.stop()
            }, 5000)
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    private fun vibrate() {
        try {
            val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 500), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }
}

// Broadcast receiver to handle timer stop action from notification
class TimerStopReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            WorkManager.getInstance(it).cancelUniqueWork("exercise_timer")
            val notificationManager = it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(TimerWorker.NOTIFICATION_ID)
        }
    }
}

fun createNotificationChanne(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            TimerWorker.CHANNEL_ID,
            "Exercise Timer",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Shows exercise timer countdown and completion alerts"
            enableLights(true)
            enableVibration(true)
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}