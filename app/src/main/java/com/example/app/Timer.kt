

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

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        val userId = auth.currentUser?.uid ?: return

        profilesListener = db.collection("exercise_profiles")
            .whereEqualTo("userId", userId)
            .orderBy("order") // Change from "createdAt" to "order"
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _errorMessage.value = "Failed to load profiles: ${error.message}"
                    return@addSnapshotListener
                }

                val profilesList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ExerciseProfile::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                _profiles.value = profilesList

                if (profilesList.isEmpty() && snapshot != null && !snapshot.metadata.isFromCache) {
                    createDefaultProfiles()
                } else {
                    val currentIndex = _selectedProfileIndex.value ?: 0
                    if (currentIndex >= profilesList.size && profilesList.isNotEmpty()) {
                        _selectedProfileIndex.value = 0
                    }
                    updateTimerForSelectedProfile()
                }
            }}

    private fun createDefaultProfiles() {
        val userId = auth.currentUser?.uid ?: return
        val defaultProfiles = listOf(
            ExerciseProfile(name = "Push-ups", iconName = "FitnessCenter", defaultMinutes = 5, colorHex = "#4CAF50", userId = userId),
            ExerciseProfile(name = "Plank", iconName = "Timer", defaultMinutes = 3, colorHex = "#2196F3", userId = userId),
            ExerciseProfile(name = "Squats", iconName = "DirectionsRun", defaultMinutes = 4, colorHex = "#9C27B0", userId = userId),
            ExerciseProfile(name = "Burpees", iconName = "Sports", defaultMinutes = 6, colorHex = "#FF5722", userId = userId),
            ExerciseProfile(name = "Yoga", iconName = "SportsGymnastics", defaultMinutes = 15, colorHex = "#FF9800", userId = userId)
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

        // Optimistically add to local list for instant UI feedback
        val currentProfiles = _profiles.value?.toMutableList() ?: mutableListOf()
        val tempProfile = profile.copy(id = "temp_${System.currentTimeMillis()}", userId = userId)
        currentProfiles.add(tempProfile)
        _profiles.value = currentProfiles

        viewModelScope.launch {
            try {
                val docRef = db.collection("exercise_profiles").add(profile.copy(userId = userId)).await()
                // Real-time listener will update with actual server data
            } catch (e: Exception) {
                // Revert optimistic update on failure
                val revertedProfiles = _profiles.value?.toMutableList() ?: mutableListOf()
                revertedProfiles.removeAll { it.id == tempProfile.id }
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

        // Optimistically update local list
        val currentProfiles = _profiles.value?.toMutableList() ?: mutableListOf()
        val index = currentProfiles.indexOfFirst { it.id == profile.id }
        if (index != -1) {
            currentProfiles[index] = profile
            _profiles.value = currentProfiles
        }

        viewModelScope.launch {
            try {
                db.collection("exercise_profiles")
                    .document(profile.id)
                    .set(profile)
                    .await()
                // Real-time listener will sync with server data
            } catch (e: Exception) {
                // Revert on failure - real-time listener will restore original data
                _errorMessage.value = "Failed to update profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteProfile(profileId: String) {
        if (profileId.isEmpty()) return
        _isLoading.value = true

        // Optimistically remove from local list
        val currentProfiles = _profiles.value?.toMutableList() ?: mutableListOf()
        val profileToDelete = currentProfiles.find { it.id == profileId }
        currentProfiles.removeAll { it.id == profileId }
        _profiles.value = currentProfiles

        // Adjust selected index if needed
        val currentIndex = _selectedProfileIndex.value ?: 0
        if (currentIndex >= currentProfiles.size && currentProfiles.isNotEmpty()) {
            _selectedProfileIndex.value = 0
        }

        viewModelScope.launch {
            try {
                db.collection("exercise_profiles")
                    .document(profileId)
                    .delete()
                    .await()
                // Real-time listener will confirm deletion
            } catch (e: Exception) {
                // Revert on failure
                if (profileToDelete != null) {
                    val revertedProfiles = _profiles.value?.toMutableList() ?: mutableListOf()
                    revertedProfiles.add(profileToDelete)
                    revertedProfiles.sortBy { it.createdAt }
                    _profiles.value = revertedProfiles
                }
                _errorMessage.value = "Failed to delete profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        profilesListener?.remove()
    }
    fun reorderProfiles(fromIndex: Int, toIndex: Int) {
        val currentProfiles = _profiles.value?.toMutableList() ?: return
        if (fromIndex < 0 || fromIndex >= currentProfiles.size ||
            toIndex < 0 || toIndex >= currentProfiles.size) return

        val userId = auth.currentUser?.uid ?: return

        // Optimistically update local list
        val item = currentProfiles.removeAt(fromIndex)
        currentProfiles.add(toIndex, item)
        _profiles.value = currentProfiles

        // Adjust selected index if needed
        val currentSelectedIndex = _selectedProfileIndex.value ?: 0
        val newSelectedIndex = when {
            currentSelectedIndex == fromIndex -> toIndex
            currentSelectedIndex in (minOf(fromIndex, toIndex) + 1)..maxOf(fromIndex, toIndex) -> {
                if (fromIndex < toIndex) currentSelectedIndex - 1 else currentSelectedIndex + 1
            }
            else -> currentSelectedIndex
        }
        _selectedProfileIndex.value = newSelectedIndex

        // Update order in Firestore
        viewModelScope.launch {
            try {
                val batch = db.batch()
                currentProfiles.forEachIndexed { index, profile ->
                    val docRef = db.collection("exercise_profiles").document(profile.id)
                    batch.update(docRef, "order", index)
                }
                batch.commit().await()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to reorder profiles: ${e.message}"
                // Note: Real-time listener will restore original order on failure
            }
        }
    }

    // Update loadProfiles to order by the order field

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
                        onDelete = { viewModel.deleteProfile(profiles[index].id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Timer Display
            currentProfile?.let { profile ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(280.dp)
                ) {
                    val progress = if (totalSeconds > 0) {
                        ((totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 8.dp,
                        color = profile.getColor(),
                        trackColor = profile.getColor().copy(alpha = 0.2f),
                        strokeCap = StrokeCap.Round
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = profile.getIcon(),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = profile.getColor()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = formatTime(remainingSeconds),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = profile.getColor()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Control Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Start/Pause Button
                    FloatingActionButton(
                        onClick = {
                            when (timerState) {
                                TimerState.STOPPED -> viewModel.startTimer(context)
                                TimerState.RUNNING -> viewModel.pauseTimer(context)
                                TimerState.PAUSED -> viewModel.startTimer(context)
                            }
                        },
                        containerColor = profile.getColor(),
                        modifier = Modifier.size(64.dp)
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
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Reset Button
                    FloatingActionButton(
                        onClick = { viewModel.resetTimer(context) },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Status Text
                Text(
                    text = when (timerState) {
                        TimerState.STOPPED -> if (remainingSeconds == 0) "Time's up!" else "Ready to start"
                        TimerState.RUNNING -> "Timer running..."
                        TimerState.PAUSED -> "Timer paused"
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
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(100.dp)
            .height(120.dp),
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

            // Menu button
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    modifier = Modifier.size(16.dp)
                )
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