
package com.arjundubey.app
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale




import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import com.google.gson.Gson
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.CloudDownload
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import com.google.gson.reflect.TypeToken
import coil.compose.LocalImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
class SharedPreferencesManager(private val sharedPreferences: SharedPreferences) {
    private val gson = Gson()

    // Profile management
    fun saveProfile(profile: ProfileInfo): Result<String> {
        return try {
            val profiles = getProfiles().toMutableList()
            // Remove if exists (update case)
            profiles.removeAll { it.id == profile.id }
            profiles.add(profile)

            val profilesJson = gson.toJson(profiles)
            sharedPreferences.edit().putString("profiles", profilesJson).apply()
            Result.success("Profile saved locally")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getProfiles(): List<ProfileInfo> {
        val profilesJson = sharedPreferences.getString("profiles", "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<ProfileInfo>>() {}.type
            gson.fromJson(profilesJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteProfile(profileId: String): Result<String> {
        return try {
            val profiles = getProfiles().toMutableList()
            profiles.removeAll { it.id == profileId }

            val profilesJson = gson.toJson(profiles)
            sharedPreferences.edit().putString("profiles", profilesJson).apply()
            Result.success("Profile deleted locally")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Resume data management
    fun saveResumeData(profileId: String, resumeData: ResumeData): Result<String> {
        return try {
            val resumeDataJson = gson.toJson(convertToSerializableResumeData(resumeData))
            sharedPreferences.edit().putString("resume_$profileId", resumeDataJson).apply()
            Result.success("Resume saved locally")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun loadResumeData(profileId: String): Result<ResumeData?> {
        return try {
            val resumeDataJson = sharedPreferences.getString("resume_$profileId", null)
            if (resumeDataJson == null) {
                Result.success(null)
            } else {
                val serializableData = gson.fromJson(resumeDataJson, SerializableResumeData::class.java)
                Result.success(convertFromSerializableResumeData(serializableData))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper data classes for serialization
    private data class SerializableResumeData(
        val personal: Map<String, String> = mapOf(),
        val education: List<Map<String, String>> = listOf(),
        val experience: List<Map<String, Any>> = listOf(),
        val projects: List<Map<String, Any>> = listOf(),
        val skills: Map<String, List<String>> = mapOf()
    )

    // In SharedPreferencesManager, update the conversion methods
    private fun convertToSerializableResumeData(resumeData: ResumeData): SerializableResumeData {
        return SerializableResumeData(
            personal = mapOf(
                "name" to resumeData.personal.name,
                "title" to resumeData.personal.title,
                "email" to resumeData.personal.email,
                "phone" to resumeData.personal.phone,
                "location" to resumeData.personal.location,
                "linkedin" to resumeData.personal.linkedin,
                "github" to resumeData.personal.github,
                "website" to resumeData.personal.website
            ),
            education = resumeData.education.map { edu ->
                mapOf(
                    "institution" to edu.institution,
                    "degree" to edu.degree,
                    "location" to edu.location,
                    "startDate" to edu.startDate,
                    "endDate" to edu.endDate
                )
            },
            experience = resumeData.experience.map { exp ->
                mapOf(
                    "title" to exp.title,
                    "company" to exp.company,
                    "location" to exp.location,
                    "startDate" to exp.startDate,
                    "endDate" to exp.endDate,
                    "responsibilities" to exp.responsibilities.toList()
                )
            },
            projects = resumeData.projects.map { proj ->
                mapOf(
                    "name" to proj.name,
                    "technologies" to proj.technologies,
                    "startDate" to proj.startDate,
                    "endDate" to proj.endDate,
                    "description" to proj.description.toList(),
                    "link" to proj.link
                )
            },
            skills = mapOf(
                "languages" to resumeData.skills.languages.toList(),
                "frameworks" to resumeData.skills.frameworks.toList(),
                "tools" to resumeData.skills.tools.toList(),
                "libraries" to resumeData.skills.libraries.toList()
            )
        )
    }

    private fun convertFromSerializableResumeData(serializableData: SerializableResumeData): ResumeData {
        val resumeData = ResumeData()

        // Personal info
        resumeData.personal.name = serializableData.personal["name"] ?: ""
        resumeData.personal.title = serializableData.personal["title"] ?: ""
        resumeData.personal.email = serializableData.personal["email"] ?: ""
        resumeData.personal.phone = serializableData.personal["phone"] ?: ""
        resumeData.personal.location = serializableData.personal["location"] ?: ""
        resumeData.personal.linkedin = serializableData.personal["linkedin"] ?: ""
        resumeData.personal.github = serializableData.personal["github"] ?: ""
        resumeData.personal.website = serializableData.personal["website"] ?: ""

        // Education
        serializableData.education.forEach { eduMap ->
            resumeData.education.add(Education().apply {
                institution = eduMap["institution"] ?: ""
                degree = eduMap["degree"] ?: ""
                location = eduMap["location"] ?: ""
                startDate = eduMap["startDate"] ?: ""
                endDate = eduMap["endDate"] ?: ""
            })
        }

        // Experience
        serializableData.experience.forEach { expMap ->
            resumeData.experience.add(Experience().apply {
                title = expMap["title"] as? String ?: ""
                company = expMap["company"] as? String ?: ""
                location = expMap["location"] as? String ?: ""
                startDate = expMap["startDate"] as? String ?: ""
                endDate = expMap["endDate"] as? String ?: ""
                responsibilities.clear()
                (expMap["responsibilities"] as? List<*>)?.forEach { resp ->
                    responsibilities.add(resp as? String ?: "")
                }
            })
        }

        // Projects
        serializableData.projects.forEach { projMap ->
            resumeData.projects.add(Project().apply {
                name = projMap["name"] as? String ?: ""
                technologies = projMap["technologies"] as? String ?: ""
                startDate = projMap["startDate"] as? String ?: ""
                endDate = projMap["endDate"] as? String ?: ""
                link = projMap["link"] as? String ?: ""
                description.clear()
                (projMap["description"] as? List<*>)?.forEach { desc ->
                    description.add(desc as? String ?: "")
                }
            })
        }

        // Skills
        (serializableData.skills["languages"] ?: emptyList()).forEach { skill ->
            resumeData.skills.languages.add(skill)
        }
        (serializableData.skills["frameworks"] ?: emptyList()).forEach { skill ->
            resumeData.skills.frameworks.add(skill)
        }
        (serializableData.skills["tools"] ?: emptyList()).forEach { skill ->
            resumeData.skills.tools.add(skill)
        }
        (serializableData.skills["libraries"] ?: emptyList()).forEach { skill ->
            resumeData.skills.libraries.add(skill)
        }

        return resumeData
    }

}
// Data classes for resume structure
class PersonalInfo {
    var name by mutableStateOf("")
    var title by mutableStateOf("")
    var email by mutableStateOf("")
    var phone by mutableStateOf("")
    var location by mutableStateOf("")
    var linkedin by mutableStateOf("")
    var github by mutableStateOf("")
    var website by mutableStateOf("")
}
data class FirestoreResumeData(
    val personal: Map<String, String> = mapOf(),
    val education: List<Map<String, String>> = listOf(),
    val experience: List<Map<String, Any>> = listOf(),
    val projects: List<Map<String, Any>> = listOf(),
    val skills: Map<String, List<String>> = mapOf(),
    val userEmail: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)
class Education {
    var institution by mutableStateOf("")
    var degree by mutableStateOf("")
    var location by mutableStateOf("")
    var startDate by mutableStateOf("")
    var endDate by mutableStateOf("")
}

class Experience {
    var title by mutableStateOf("")
    var company by mutableStateOf("")
    var location by mutableStateOf("")
    var startDate by mutableStateOf("")
    var endDate by mutableStateOf("")
    var responsibilities = mutableStateListOf<String>()
}

class Project {
    var name by mutableStateOf("")
    var technologies by mutableStateOf("")
    var startDate by mutableStateOf("")
    var endDate by mutableStateOf("")
    var description = mutableStateListOf<String>()
    var link by mutableStateOf("")
}

class Skills {
    var languages = mutableStateListOf<String>()
    var frameworks = mutableStateListOf<String>()
    var tools = mutableStateListOf<String>()
    var libraries = mutableStateListOf<String>()
}

// Replace your ResumeData class with this implementation
@Stable
class ResumeData {
    val personal = PersonalInfo()
    val education = mutableStateListOf<Education>()
    val experience = mutableStateListOf<Experience>()
    val projects = mutableStateListOf<Project>()
    val skills = Skills()

    // Helper method to create a deep copy
    fun copyFrom(other: ResumeData) {
        // Copy personal info
        personal.name = other.personal.name
        personal.title = other.personal.title
        personal.email = other.personal.email
        personal.phone = other.personal.phone
        personal.location = other.personal.location
        personal.linkedin = other.personal.linkedin
        personal.github = other.personal.github
        personal.website = other.personal.website

        // Copy education
        education.clear()
        education.addAll(other.education.map { edu ->
            Education().apply {
                institution = edu.institution
                degree = edu.degree
                location = edu.location
                startDate = edu.startDate
                endDate = edu.endDate
            }
        })

        // Copy experience
        experience.clear()
        experience.addAll(other.experience.map { exp ->
            Experience().apply {
                title = exp.title
                company = exp.company
                location = exp.location
                startDate = exp.startDate
                endDate = exp.endDate
                responsibilities.clear()
                responsibilities.addAll(exp.responsibilities)
            }
        })

        // Copy projects
        projects.clear()
        projects.addAll(other.projects.map { proj ->
            Project().apply {
                name = proj.name
                technologies = proj.technologies
                startDate = proj.startDate
                endDate = proj.endDate
                link = proj.link
                description.clear()
                description.addAll(proj.description)
            }
        })

        // Copy skills
        skills.languages.clear()
        skills.languages.addAll(other.skills.languages)

        skills.frameworks.clear()
        skills.frameworks.addAll(other.skills.frameworks)

        skills.tools.clear()
        skills.tools.addAll(other.skills.tools)

        skills.libraries.clear()
        skills.libraries.addAll(other.skills.libraries)
    }
}

enum class ResumeTab(val title: String, val icon: ImageVector) {
    PERSONAL("Personal", Icons.Default.Person),
    EDUCATION("Education", Icons.Default.School),
    EXPERIENCE("Experience", Icons.Default.Work),
    PROJECTS("Projects", Icons.Default.Code),
    SKILLS("Skills", Icons.Default.Star)
}


// First, add these data classes for profile management
data class ProfileInfo(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

data class FirestoreProfileData(
    val profileInfo: Map<String, Any> = emptyMap(),
    val resumeData: Map<String, Any> = emptyMap(),
    val userEmail: String = ""
)

// Add these enums for dialog states
enum class ProfileDialogState {
    NONE, CREATE, EDIT, DELETE
}

// Modified ResumeBuilderApp with profile management
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeBuilderApp() {
    // Change this from mutableStateOf(ResumeData()) to just remember { ResumeData() }
    val resumeData = remember { ResumeData() }
    val selectedTab = remember { mutableStateOf(ResumeTab.PERSONAL) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { ResumeRepository(context) }
    val imageLoader = LocalImageLoader.current

    // Preload template images when the app starts
    LaunchedEffect(Unit) {
        val templateUrls = listOf(
            "https://www.getsetresumes.com/storage/resume-examples/December2021/enSlu3qMB9l9VWDyFHgP.jpg",
            // Add other template URLs here
        )

        templateUrls.forEach { url ->
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(100, 100) // Preload at the size you'll display
                    .build()
                imageLoader.enqueue(request)
            } catch (e: Exception) {
                // Handle preloading errors silently
                println("Failed to preload image: $url - ${e.message}")
            }
        }
    }

    // Profile management states
    var profiles by remember { mutableStateOf<List<ProfileInfo>>(emptyList()) }
    var selectedProfile by remember { mutableStateOf<ProfileInfo?>(null) }
    var showProfileDialog by remember { mutableStateOf(ProfileDialogState.NONE) }
    var showProfileDropdown by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Load profiles on app start
    LaunchedEffect(Unit) {
        isLoading = true
        repository.loadProfiles().fold(
            onSuccess = { loadedProfiles ->
                profiles = loadedProfiles
                println("Loaded ${loadedProfiles.size} profiles: ${loadedProfiles.map { it.name }}")

                // Auto-select first profile or create default if none exist
                if (loadedProfiles.isNotEmpty()) {
                    selectedProfile = loadedProfiles.first()
                    println("Selected profile: ${selectedProfile?.name}")

                    // Load resume data for selected profile
                    repository.loadResumeData(selectedProfile!!.id).fold(
                        onSuccess = { loadedData ->
                            loadedData?.let {
                                // Use copyFrom to update the existing resumeData
                                resumeData.copyFrom(it)
                            }
                            println("Resume data loaded for profile: ${selectedProfile?.name}")
                        },
                        onFailure = { error ->
                            println("Failed to load resume data: ${error.message}")
                        }
                    )
                } else {
                    println("No profiles found, user needs to create one")
                }
                isLoading = false
            },
            onFailure = { error ->
                isLoading = false
                println("Failed to load profiles: ${error.message}")
                Toast.makeText(context, "Failed to load profiles: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Save data when profile changes
    LaunchedEffect(selectedProfile) {
        selectedProfile?.let { profile ->
            isLoading = true
            repository.loadResumeData(profile.id).fold(
                onSuccess = { loadedData ->
                    loadedData?.let {
                        resumeData.copyFrom(it)
                    }
                    isLoading = false
                },
                onFailure = {
                    // Reset to empty resume data if loading fails
                    resumeData.copyFrom(ResumeData())
                    isLoading = false
                }
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar with Profile selector and actions
// Compact Top Bar with Profile selector and actions
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Box() {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showProfileDropdown = true }
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = selectedProfile?.name ?: "No Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Profile",
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(20.dp)
                        )
                    }

                    // Actions section
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile management button
                        IconButton(
                            onClick = { showProfileDialog = ProfileDialogState.CREATE },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Profile",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Refresh profiles button (for debugging)
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    repository.loadProfiles().fold(
                                        onSuccess = { loadedProfiles ->
                                            profiles = loadedProfiles
                                            Toast.makeText(
                                                context,
                                                "Loaded ${loadedProfiles.size} profiles",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            isLoading = false
                                        },
                                        onFailure = { error ->
                                            Toast.makeText(
                                                context,
                                                "Refresh failed: ${error.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            isLoading = false
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh Profiles",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Edit profile button
                        selectedProfile?.let {
                            IconButton(
                                onClick = { showProfileDialog = ProfileDialogState.EDIT },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Delete profile button
                        if (profiles.size > 1) {
                            selectedProfile?.let {
                                IconButton(
                                    onClick = { showProfileDialog = ProfileDialogState.DELETE },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Profile",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Save button
                        if (isLoading) {
                            Box(
                                modifier = Modifier.size(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        } else {
                            selectedProfile?.let { profile ->
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            isLoading = true
                                            repository.saveResumeData(profile.id, resumeData).fold(
                                                onSuccess = { message ->
                                                    Toast.makeText(
                                                        context,
                                                        message,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    isLoading = false
                                                },
                                                onFailure = { error ->
                                                    Toast.makeText(
                                                        context,
                                                        "Save failed: ${error.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    isLoading = false
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Save,
                                        contentDescription = "Save Resume",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Profile dropdown menu
            DropdownMenu(
                expanded = showProfileDropdown,
                onDismissRequest = { showProfileDropdown = false }
            ) {
                profiles.forEach { profile ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = profile.name,
                                    fontWeight = if (profile.id == selectedProfile?.id) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = profile.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            selectedProfile = profile
                            showProfileDropdown = false
                            // Load resume data for selected profile
                            coroutineScope.launch {
                                isLoading = true
                                repository.loadResumeData(profile.id).fold(
                                    onSuccess = { loadedData ->
                                        loadedData?.let {
                                            resumeData.copyFrom(it)
                                        }
                                        isLoading = false
                                    },
                                    onFailure = {
                                        // Reset to empty resume data if loading fails
                                        resumeData.copyFrom(ResumeData())
                                        isLoading = false
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab.value.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            ResumeTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab.value == tab,
                    onClick = { selectedTab.value = tab },
                    text = { Text(tab.title) },
                    icon = { Icon(tab.icon, contentDescription = tab.title) }
                )
            }
        }
// Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()

                .background(MaterialTheme.colorScheme.background) // Add this line
        ) {
            if (selectedProfile != null) {
                when (selectedTab.value) {
                    ResumeTab.PERSONAL -> PersonalInfoTab(resumeData.personal)
                    ResumeTab.EDUCATION -> EducationTab(resumeData.education)
                    ResumeTab.EXPERIENCE -> ExperienceTab(resumeData.experience)
                    ResumeTab.PROJECTS -> ProjectsTab(resumeData.projects)
                    ResumeTab.SKILLS -> SkillsTab(resumeData.skills)
                }
                selectedProfile?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxSize() // Changed from padding only
                            .padding(WindowInsets.navigationBars.asPaddingValues()),
                        contentAlignment = Alignment.BottomEnd // Changed from TopCenter
                    ) {
                        DownloadResumeScreen(context, resumeData)
                    }
                }
            } else {
                // Show message when no profile is selected
                Column(
                    modifier = Modifier.fillMaxSize() .background(MaterialTheme.colorScheme.background),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No Profile Selected",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Create a new profile to get started",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showProfileDialog = ProfileDialogState.CREATE
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Profile")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Create some default profiles
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val defaultProfiles = listOf(
                                    ProfileInfo(
                                        id = "${System.currentTimeMillis()}_web_dev",
                                        name = "Web Developer",
                                        description = "Full-stack web development profile"
                                    ),
                                    ProfileInfo(
                                        id = "${System.currentTimeMillis() + 1}_android_dev",
                                        name = "Android Developer",
                                        description = "Mobile Android development profile"
                                    ),
                                    ProfileInfo(
                                        id = "${System.currentTimeMillis() + 2}_ml_dev",
                                        name = "ML Engineer",
                                        description = "Machine Learning and AI profile"
                                    )
                                )

                                defaultProfiles.forEach { profile ->
                                    repository.saveProfile(profile).fold(
                                        onSuccess = {
                                            profiles = profiles + profile
                                        },
                                        onFailure = { error ->
                                            Toast.makeText(context, "Failed to create ${profile.name}: ${error.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }

                                if (profiles.isNotEmpty()) {
                                    selectedProfile = profiles.first()
                                    Toast.makeText(context, "Default profiles created!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Create Default Profiles")
                    }
                }
            }
        }


    }



    // Profile dialogs
    when (showProfileDialog) {
        ProfileDialogState.CREATE -> {
            ProfileCreateDialog(
                onDismiss = { showProfileDialog = ProfileDialogState.NONE },
                onConfirm = { name, description ->
                    coroutineScope.launch {
                        val newProfile = ProfileInfo(
                            id = "${System.currentTimeMillis()}_${name.replace(" ", "_")}",
                            name = name,
                            description = description
                        )
                        repository.saveProfile(newProfile).fold(
                            onSuccess = {
                                profiles = profiles + newProfile
                                selectedProfile = newProfile
                                // Use copyFrom instead of assignment
                                resumeData.copyFrom(ResumeData())
                                Toast.makeText(context, "Profile created successfully!", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { error ->
                                Toast.makeText(context, "Failed to create profile: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    showProfileDialog = ProfileDialogState.NONE
                }
            )
        }
        ProfileDialogState.EDIT -> {
            selectedProfile?.let { profile ->
                ProfileEditDialog(
                    profile = profile,
                    onDismiss = { showProfileDialog = ProfileDialogState.NONE },
                    onConfirm = { updatedProfile ->
                        coroutineScope.launch {
                            repository.updateProfile(updatedProfile).fold(
                                onSuccess = {
                                    profiles = profiles.map { if (it.id == updatedProfile.id) updatedProfile else it }
                                    selectedProfile = updatedProfile
                                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { error ->
                                    Toast.makeText(context, "Failed to update profile: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        showProfileDialog = ProfileDialogState.NONE
                    }
                )
            }
        }
        ProfileDialogState.DELETE -> {
            selectedProfile?.let { profile ->
                ProfileDeleteDialog(
                    profile = profile,
                    onDismiss = { showProfileDialog = ProfileDialogState.NONE },
                    onConfirm = {
                        coroutineScope.launch {
                            repository.deleteProfile(profile.id).fold(
                                onSuccess = {
                                    profiles = profiles.filter { it.id != profile.id }
                                    selectedProfile = profiles.firstOrNull()
                                    if (selectedProfile != null) {
                                        repository.loadResumeData(selectedProfile!!.id).fold(
                                            onSuccess = { loadedData ->
                                                // Use copyFrom instead of assignment
                                                loadedData?.let {
                                                    resumeData.copyFrom(it)
                                                } ?: run {
                                                    resumeData.copyFrom(ResumeData())
                                                }
                                            },
                                            onFailure = {
                                                // Reset to empty resume data if loading fails
                                                resumeData.copyFrom(ResumeData())
                                            }
                                        )
                                    } else {
                                        // Reset to empty resume data if no profile selected
                                        resumeData.copyFrom(ResumeData())
                                    }
                                    Toast.makeText(context, "Profile deleted successfully!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { error ->
                                    Toast.makeText(context, "Failed to delete profile: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        showProfileDialog = ProfileDialogState.NONE
                    }
                )
            }
        }
        ProfileDialogState.NONE -> { /* No dialog */ }
    }
}

// Profile Create Dialog
@Composable
fun ProfileCreateDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g., Web Developer, Android Developer") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Brief description of this profile") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Profile Edit Dialog
@Composable
fun ProfileEditDialog(
    profile: ProfileInfo,
    onDismiss: () -> Unit,
    onConfirm: (ProfileInfo) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var description by remember { mutableStateOf(profile.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        profile.copy(
                            name = name,
                            description = description,
                            lastModified = System.currentTimeMillis()
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Profile Delete Dialog
@Composable
fun ProfileDeleteDialog(
    profile: ProfileInfo,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Profile") },
        text = {
            Text("Are you sure you want to delete the profile \"${profile.name}\"? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Updated Repository class with profile management
class ResumeRepository(private val context: Context) {
    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private val sharedPrefsManager = SharedPreferencesManager(
        context.getSharedPreferences("resume_builder", Context.MODE_PRIVATE)
    )

    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // Profile management methods
    suspend fun loadProfiles(): Result<List<ProfileInfo>> {
        return try {
            if (isUserLoggedIn()) {
                // Load from Firestore if logged in
                val userEmail = getCurrentUserEmail()
                    ?: return Result.failure(Exception("User not logged in"))

                val querySnapshot = firestore.collection("user_profiles")
                    .whereEqualTo("userEmail", userEmail)
                    .get()
                    .await()

                val profiles = querySnapshot.documents.mapNotNull { document ->
                    val data = document.data
                    if (data != null) {
                        ProfileInfo(
                            id = document.id,
                            name = data["name"] as? String ?: "",
                            description = data["description"] as? String ?: "",
                            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                            lastModified = data["lastModified"] as? Long ?: System.currentTimeMillis()
                        )
                    } else null
                }

                // Also sync local profiles to Firestore
                val localProfiles = sharedPrefsManager.getProfiles()
                localProfiles.forEach { localProfile ->
                    // Upload local profiles to Firestore
                    saveProfileToFirestore(localProfile)
                }

                Result.success(profiles)
            } else {
                // Load from SharedPreferences if not logged in
                val localProfiles = sharedPrefsManager.getProfiles()
                Result.success(localProfiles)
            }
        } catch (e: Exception) {
            // Fallback to local storage if Firestore fails
            val localProfiles = sharedPrefsManager.getProfiles()
            Result.success(localProfiles)
        }
    }

    suspend fun saveProfile(profile: ProfileInfo): Result<String> {
        return try {
            // Always save locally first
            val localResult = sharedPrefsManager.saveProfile(profile)

            // Save to Firestore if logged in
            if (isUserLoggedIn()) {
                saveProfileToFirestore(profile).fold(
                    onSuccess = {
                        Result.success("Profile saved to cloud and locally")
                    },
                    onFailure = { error ->
                        Result.success("Profile saved locally only: ${error.message}")
                    }
                )
            } else {
                localResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveProfileToFirestore(profile: ProfileInfo): Result<String> {
        return try {
            val userEmail = getCurrentUserEmail()
                ?: return Result.failure(Exception("User not logged in"))

            val profileData = mapOf(
                "name" to profile.name,
                "description" to profile.description,
                "createdAt" to profile.createdAt,
                "lastModified" to profile.lastModified,
                "userEmail" to userEmail
            )

            firestore.collection("user_profiles")
                .document(profile.id)
                .set(profileData)
                .await()

            Result.success("Profile saved to Firestore")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(profile: ProfileInfo): Result<String> {
        return try {
            // Always update locally first
            val localResult = sharedPrefsManager.saveProfile(profile)

            // Update Firestore if logged in
            if (isUserLoggedIn()) {
                updateProfileInFirestore(profile).fold(
                    onSuccess = {
                        Result.success("Profile updated in cloud and locally")
                    },
                    onFailure = { error ->
                        Result.success("Profile updated locally only: ${error.message}")
                    }
                )
            } else {
                localResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateProfileInFirestore(profile: ProfileInfo): Result<String> {
        return try {
            val userEmail = getCurrentUserEmail()
                ?: return Result.failure(Exception("User not logged in"))

            val profileData = mapOf(
                "name" to profile.name,
                "description" to profile.description,
                "lastModified" to System.currentTimeMillis(),
                "userEmail" to userEmail
            )

            firestore.collection("user_profiles")
                .document(profile.id)
                .update(profileData)
                .await()

            Result.success("Profile updated in Firestore")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProfile(profileId: String): Result<String> {
        return try {
            // Always delete locally first
            val localResult = sharedPrefsManager.deleteProfile(profileId)

            // Delete from Firestore if logged in
            if (isUserLoggedIn()) {
                deleteProfileFromFirestore(profileId).fold(
                    onSuccess = {
                        Result.success("Profile deleted from cloud and locally")
                    },
                    onFailure = { error ->
                        Result.success("Profile deleted locally only: ${error.message}")
                    }
                )
            } else {
                localResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun deleteProfileFromFirestore(profileId: String): Result<String> {
        return try {
            val userEmail = getCurrentUserEmail()
                ?: return Result.failure(Exception("User not logged in"))

            // Delete the profile
            firestore.collection("user_profiles")
                .document(profileId)
                .delete()
                .await()

            // Delete associated resume data
            firestore.collection("resumes")
                .document("${userEmail}_${profileId}")
                .delete()
                .await()

            Result.success("Profile deleted from Firestore")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Resume data methods
    suspend fun saveResumeData(profileId: String, resumeData: ResumeData): Result<String> {
        return try {
            // Always save locally first
            val localResult = sharedPrefsManager.saveResumeData(profileId, resumeData)

            // Save to Firestore if logged in
            if (isUserLoggedIn()) {
                saveResumeDataToFirestore(profileId, resumeData).fold(
                    onSuccess = {
                        Result.success("Resume saved to cloud and locally")
                    },
                    onFailure = { error ->
                        Result.success("Resume saved locally only: ${error.message}")
                    }
                )
            } else {
                localResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveResumeDataToFirestore(profileId: String, resumeData: ResumeData): Result<String> {
        return try {
            val userEmail = getCurrentUserEmail()
                ?: return Result.failure(Exception("User not logged in"))

            val firestoreData = FirestoreResumeData(
                personal = mapOf(
                    "name" to resumeData.personal.name,
                    "title" to resumeData.personal.title,
                    "email" to resumeData.personal.email,
                    "phone" to resumeData.personal.phone,
                    "location" to resumeData.personal.location,
                    "linkedin" to resumeData.personal.linkedin,
                    "github" to resumeData.personal.github,
                    "website" to resumeData.personal.website
                ),
                education = resumeData.education.map { edu ->
                    mapOf(
                        "institution" to edu.institution,
                        "degree" to edu.degree,
                        "location" to edu.location,
                        "startDate" to edu.startDate,
                        "endDate" to edu.endDate
                    )
                },
                experience = resumeData.experience.map { exp ->
                    mapOf(
                        "title" to exp.title,
                        "company" to exp.company,
                        "location" to exp.location,
                        "startDate" to exp.startDate,
                        "endDate" to exp.endDate,
                        "responsibilities" to exp.responsibilities.toList()
                    )
                },
                projects = resumeData.projects.map { proj ->
                    mapOf(
                        "name" to proj.name,
                        "technologies" to proj.technologies,
                        "startDate" to proj.startDate,
                        "endDate" to proj.endDate,
                        "description" to proj.description.toList(),
                        "link" to proj.link
                    )
                },
                skills = mapOf(
                    "languages" to resumeData.skills.languages.toList(),
                    "frameworks" to resumeData.skills.frameworks.toList(),
                    "tools" to resumeData.skills.tools.toList(),
                    "libraries" to resumeData.skills.libraries.toList()
                ),
                userEmail = userEmail
            )

            val documentId = "${userEmail}_${profileId}"

            firestore.collection("resumes")
                .document(documentId)
                .set(firestoreData)
                .await()

            // Update profile's last modified time
            firestore.collection("user_profiles")
                .document(profileId)
                .update("lastModified", System.currentTimeMillis())
                .await()

            Result.success("Resume saved to Firestore")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadResumeData(profileId: String): Result<ResumeData?> {
        return try {
            // Try to load from Firestore first if logged in
            if (isUserLoggedIn()) {
                loadResumeDataFromFirestore(profileId).fold(
                    onSuccess = { firestoreData ->
                        if (firestoreData != null) {
                            // Also save locally for offline access
                            sharedPrefsManager.saveResumeData(profileId, firestoreData)
                            Result.success(firestoreData)
                        } else {
                            // Fallback to local storage
                            sharedPrefsManager.loadResumeData(profileId)
                        }
                    },
                    onFailure = {
                        // Fallback to local storage
                        sharedPrefsManager.loadResumeData(profileId)
                    }
                )
            } else {
                // Load from local storage only
                sharedPrefsManager.loadResumeData(profileId)
            }
        } catch (e: Exception) {
            // Final fallback to local storage
            sharedPrefsManager.loadResumeData(profileId)
        }
    }

    private suspend fun loadResumeDataFromFirestore(profileId: String): Result<ResumeData?> {
        return try {
            val userEmail = getCurrentUserEmail()
                ?: return Result.failure(Exception("User not logged in"))

            val documentId = "${userEmail}_${profileId}"

            val document = firestore.collection("resumes")
                .document(documentId)
                .get()
                .await()

            if (!document.exists()) {
                return Result.success(null)
            }

            val data = document.toObject(FirestoreResumeData::class.java)
                ?: return Result.success(null)

            val resumeData = ResumeData().apply {
                // Load personal info
                personal.name = data.personal["name"] as? String ?: ""
                personal.title = data.personal["title"] as? String ?: ""
                personal.email = data.personal["email"] as? String ?: ""
                personal.phone = data.personal["phone"] as? String ?: ""
                personal.location = data.personal["location"] as? String ?: ""
                personal.linkedin = data.personal["linkedin"] as? String ?: ""
                personal.github = data.personal["github"] as? String ?: ""
                personal.website = data.personal["website"] as? String ?: ""

                // Load education
                education.clear()
                data.education.forEach { eduMap ->
                    education.add(Education().apply {
                        institution = eduMap["institution"] as? String ?: ""
                        degree = eduMap["degree"] as? String ?: ""
                        location = eduMap["location"] as? String ?: ""
                        startDate = eduMap["startDate"] as? String ?: ""
                        endDate = eduMap["endDate"] as? String ?: ""
                    })
                }

                // Load experience
                experience.clear()
                data.experience.forEach { expMap ->
                    experience.add(Experience().apply {
                        title = expMap["title"] as? String ?: ""
                        company = expMap["company"] as? String ?: ""
                        location = expMap["location"] as? String ?: ""
                        startDate = expMap["startDate"] as? String ?: ""
                        endDate = expMap["endDate"] as? String ?: ""
                        responsibilities.clear()
                        (expMap["responsibilities"] as? List<*>)?.forEach { resp ->
                            responsibilities.add(resp as? String ?: "")
                        }
                    })
                }

                // Load projects
                projects.clear()
                data.projects.forEach { projMap ->
                    projects.add(Project().apply {
                        name = projMap["name"] as? String ?: ""
                        technologies = projMap["technologies"] as? String ?: ""
                        startDate = projMap["startDate"] as? String ?: ""
                        endDate = projMap["endDate"] as? String ?: ""
                        link = projMap["link"] as? String ?: ""
                        description.clear()
                        (projMap["description"] as? List<*>)?.forEach { desc ->
                            description.add(desc as? String ?: "")
                        }
                    })
                }

                // Load skills
                skills.languages.clear()
                skills.frameworks.clear()
                skills.tools.clear()
                skills.libraries.clear()

                (data.skills["languages"] as? List<*>)?.forEach { skill ->
                    skills.languages.add(skill as? String ?: "")
                }
                (data.skills["frameworks"] as? List<*>)?.forEach { skill ->
                    skills.frameworks.add(skill as? String ?: "")
                }
                (data.skills["tools"] as? List<*>)?.forEach { skill ->
                    skills.tools.add(skill as? String ?: "")
                }
                (data.skills["libraries"] as? List<*>)?.forEach { skill ->
                    skills.libraries.add(skill as? String ?: "")
                }
            }

            Result.success(resumeData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sync local data to cloud when user logs in
    suspend fun syncLocalDataToCloud(): Result<String> {
        return try {
            if (!isUserLoggedIn()) {
                return Result.failure(Exception("User not logged in"))
            }

            val localProfiles = sharedPrefsManager.getProfiles()
            var syncedCount = 0

            localProfiles.forEach { profile ->
                // Upload profile to Firestore
                saveProfileToFirestore(profile).fold(
                    onSuccess = {
                        // Upload resume data for this profile
                        sharedPrefsManager.loadResumeData(profile.id).fold(
                            onSuccess = { resumeData ->
                                resumeData?.let {
                                    saveResumeDataToFirestore(profile.id, it)
                                    syncedCount++
                                }
                            },
                            onFailure = { /* Ignore resume data sync failure */ }
                        )
                    },
                    onFailure = { /* Ignore profile sync failure */ }
                )
            }

            Result.success("Synced $syncedCount profiles to cloud")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


@Composable
fun PersonalInfoTab(personalInfo: PersonalInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Personal Information",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        OutlinedTextField(
            value = personalInfo.name,
            onValueChange = { personalInfo.name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = personalInfo.title,
            onValueChange = { personalInfo.title = it },
            label = { Text("Professional Title") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = personalInfo.email,
            onValueChange = { personalInfo.email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = personalInfo.phone,
            onValueChange = { personalInfo.phone = it },
            label = { Text("Phone") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = personalInfo.location,
            onValueChange = { personalInfo.location = it },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = personalInfo.linkedin,
            onValueChange = { personalInfo.linkedin = it },
            label = { Text("LinkedIn URL") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = personalInfo.github,
            onValueChange = { personalInfo.github = it },
            label = { Text("GitHub URL") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = personalInfo.website,
            onValueChange = { personalInfo.website = it },
            label = { Text("Website URL") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
@Composable
fun EducationTab(educationList: SnapshotStateList<Education>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Education",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Enhanced Add button with background
            Button(
                onClick = { educationList.add(Education()) },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Education",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Education")
            }
        }

        // Show message if no education entries
        if (educationList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No education entries yet",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Click the + button to add your education",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Display education items
        educationList.forEachIndexed { index, education ->
            EducationItem(
                education = education,
                onDelete = { educationList.removeAt(index) }
            )
        }
    }
}
@Composable
fun ExperienceTab(experienceList: SnapshotStateList<Experience>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Experience",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    experienceList.add(Experience().apply {
                        responsibilities.add("")
                    })
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Experience",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Experience")
            }
        }

        // Show message if no experience entries
        if (experienceList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No experience entries yet",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Click the + button to add your work experience",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Display experience items with reordering controls
        experienceList.forEachIndexed { index, experience ->
            ExperienceItem(
                experience = experience,
                onDelete = {
                    if (index < experienceList.size) {
                        experienceList.removeAt(index)
                    }
                },
                onMoveUp = if (index > 0) {
                    {
                        moveExperienceItem(experienceList, index, index - 1)
                    }
                } else null,
                onMoveDown = if (index < experienceList.size - 1) {
                    {
                        moveExperienceItem(experienceList, index, index + 1)
                    }
                } else null
            )
        }
    }
}
@Composable
fun EducationItem(education: Education, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Education Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }

            OutlinedTextField(
                value = education.institution,
                onValueChange = { education.institution = it },
                label = { Text("Institution") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = education.degree,
                onValueChange = { education.degree = it },
                label = { Text("Degree") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = education.location,
                onValueChange = { education.location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = education.startDate,
                    onValueChange = { education.startDate = it },
                    label = { Text("Start Date") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = education.endDate,
                    onValueChange = { education.endDate = it },
                    label = { Text("End Date") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}



// Safe function to move items in the list
private fun moveExperienceItem(
    list: SnapshotStateList<Experience>,
    fromIndex: Int,
    toIndex: Int
) {
    if (fromIndex >= 0 && fromIndex < list.size &&
        toIndex >= 0 && toIndex < list.size &&
        fromIndex != toIndex) {

        val item = list[fromIndex]
        list.removeAt(fromIndex)
        list.add(toIndex, item)
    }
}

@Composable
fun ExperienceItem(
    experience: Experience,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Experience Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    // Move up button
                    IconButton(
                        onClick = { onMoveUp?.invoke() },
                        enabled = onMoveUp != null
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            tint = if (onMoveUp != null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // Move down button
                    IconButton(
                        onClick = { onMoveDown?.invoke() },
                        enabled = onMoveDown != null
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            tint = if (onMoveDown != null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // Delete button
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            OutlinedTextField(
                value = experience.title,
                onValueChange = { experience.title = it },
                label = { Text("Job Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = experience.company,
                onValueChange = { experience.company = it },
                label = { Text("Company") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = experience.location,
                onValueChange = { experience.location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = experience.startDate,
                    onValueChange = { experience.startDate = it },
                    label = { Text("Start Date") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = experience.endDate,
                    onValueChange = { experience.endDate = it },
                    label = { Text("End Date") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Responsibilities section with reordering
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Responsibilities:", fontWeight = FontWeight.Medium)
                IconButton(
                    onClick = { experience.responsibilities.add("") }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Responsibility")
                }
            }

            experience.responsibilities.forEachIndexed { index, responsibility ->
                ResponsibilityItem(
                    responsibility = responsibility,
                    onValueChange = { experience.responsibilities[index] = it },
                    onDelete = {
                        if (index < experience.responsibilities.size) {
                            experience.responsibilities.removeAt(index)
                        }
                    },
                    onMoveUp = if (index > 0) {
                        {
                            moveResponsibilityItem(experience.responsibilities, index, index - 1)
                        }
                    } else null,
                    onMoveDown = if (index < experience.responsibilities.size - 1) {
                        {
                            moveResponsibilityItem(experience.responsibilities, index, index + 1)
                        }
                    } else null,
                    index = index
                )
            }
        }
    }
}

// Safe function to move responsibility items
private fun moveResponsibilityItem(
    list: SnapshotStateList<String>,
    fromIndex: Int,
    toIndex: Int
) {
    if (fromIndex >= 0 && fromIndex < list.size &&
        toIndex >= 0 && toIndex < list.size &&
        fromIndex != toIndex) {

        val item = list[fromIndex]
        list.removeAt(fromIndex)
        list.add(toIndex, item)
    }
}

@Composable
fun ResponsibilityItem(
    responsibility: String,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    index: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reorder controls
        Column {
            IconButton(
                onClick = { onMoveUp?.invoke() },
                enabled = onMoveUp != null,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move Up",
                    modifier = Modifier.size(16.dp),
                    tint = if (onMoveUp != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            IconButton(
                onClick = { onMoveDown?.invoke() },
                enabled = onMoveDown != null,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move Down",
                    modifier = Modifier.size(16.dp),
                    tint = if (onMoveDown != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }

        // Text field
        OutlinedTextField(
            value = responsibility,
            onValueChange = onValueChange,
            label = { Text("Responsibility ${index + 1}") },
            modifier = Modifier.weight(1f)
        )

        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun ProjectsTab(projectsList: SnapshotStateList<Project>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Projects",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    projectsList.add(Project().apply {
                        description.add("")
                    })
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Project",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Project")
            }
        }

        // Show message if no project entries
        if (projectsList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No project entries yet",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Click the + button to add your projects",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Display project items with reordering controls
        projectsList.forEachIndexed { index, project ->
            ProjectItem(
                project = project,
                onDelete = {
                    if (index < projectsList.size) {
                        projectsList.removeAt(index)
                    }
                },
                onMoveUp = if (index > 0) {
                    {
                        moveProjectItem(projectsList, index, index - 1)
                    }
                } else null,
                onMoveDown = if (index < projectsList.size - 1) {
                    {
                        moveProjectItem(projectsList, index, index + 1)
                    }
                } else null
            )
        }
    }
}

// Safe function to move project items
private fun moveProjectItem(
    list: SnapshotStateList<Project>,
    fromIndex: Int,
    toIndex: Int
) {
    if (fromIndex >= 0 && fromIndex < list.size &&
        toIndex >= 0 && toIndex < list.size &&
        fromIndex != toIndex) {

        val item = list[fromIndex]
        list.removeAt(fromIndex)
        list.add(toIndex, item)
    }
}

@Composable
fun ProjectItem(
    project: Project,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Project Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    // Move up button
                    IconButton(
                        onClick = { onMoveUp?.invoke() },
                        enabled = onMoveUp != null
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            tint = if (onMoveUp != null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // Move down button
                    IconButton(
                        onClick = { onMoveDown?.invoke() },
                        enabled = onMoveDown != null
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            tint = if (onMoveDown != null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    // Delete button
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            OutlinedTextField(
                value = project.name,
                onValueChange = { project.name = it },
                label = { Text("Project Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = project.technologies,
                onValueChange = { project.technologies = it },
                label = { Text("Technologies Used") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = project.link,
                onValueChange = { project.link = it },
                label = { Text("Project Link") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = project.startDate,
                    onValueChange = { project.startDate = it },
                    label = { Text("Start Date") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = project.endDate,
                    onValueChange = { project.endDate = it },
                    label = { Text("End Date") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Description section with reordering
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Description:", fontWeight = FontWeight.Medium)
                IconButton(
                    onClick = { project.description.add("") }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Description")
                }
            }

            project.description.forEachIndexed { index, desc ->
                DescriptionItem(
                    description = desc,
                    onValueChange = { project.description[index] = it },
                    onDelete = {
                        if (index < project.description.size) {
                            project.description.removeAt(index)
                        }
                    },
                    onMoveUp = if (index > 0) {
                        {
                            moveDescriptionItem(project.description, index, index - 1)
                        }
                    } else null,
                    onMoveDown = if (index < project.description.size - 1) {
                        {
                            moveDescriptionItem(project.description, index, index + 1)
                        }
                    } else null,
                    index = index
                )
            }
        }
    }
}

// Safe function to move description items
private fun moveDescriptionItem(
    list: SnapshotStateList<String>,
    fromIndex: Int,
    toIndex: Int
) {
    if (fromIndex >= 0 && fromIndex < list.size &&
        toIndex >= 0 && toIndex < list.size &&
        fromIndex != toIndex) {

        val item = list[fromIndex]
        list.removeAt(fromIndex)
        list.add(toIndex, item)
    }
}

@Composable
fun DescriptionItem(
    description: String,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    index: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reorder controls
        Column {
            IconButton(
                onClick = { onMoveUp?.invoke() },
                enabled = onMoveUp != null,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move Up",
                    modifier = Modifier.size(16.dp),
                    tint = if (onMoveUp != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            IconButton(
                onClick = { onMoveDown?.invoke() },
                enabled = onMoveDown != null,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move Down",
                    modifier = Modifier.size(16.dp),
                    tint = if (onMoveDown != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }

        // Text field
        OutlinedTextField(
            value = description,
            onValueChange = onValueChange,
            label = { Text("Description ${index + 1}") },
            modifier = Modifier.weight(1f)
        )

        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun SkillsTab(skills: Skills) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Skills",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        SkillSection("Programming Languages", skills.languages)
        SkillSection("Frameworks", skills.frameworks)
        SkillSection("Tools", skills.tools)
        SkillSection("Libraries", skills.libraries)
    }
}

@Composable
fun SkillSection(title: String, skillsList: SnapshotStateList<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { skillsList.add("") }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Skill")
                }
            }

            skillsList.forEachIndexed { index, skill ->
                SkillItem(
                    skill = skill,
                    onValueChange = { skillsList[index] = it },
                    onDelete = {
                        if (index < skillsList.size) {
                            skillsList.removeAt(index)
                        }
                    },
                    onMoveUp = if (index > 0) {
                        {
                            moveSkillItem(skillsList, index, index - 1)
                        }
                    } else null,
                    onMoveDown = if (index < skillsList.size - 1) {
                        {
                            moveSkillItem(skillsList, index, index + 1)
                        }
                    } else null,
                    index = index
                )
            }
        }
    }
}

// Safe function to move skill items
private fun moveSkillItem(
    list: SnapshotStateList<String>,
    fromIndex: Int,
    toIndex: Int
) {
    if (fromIndex >= 0 && fromIndex < list.size &&
        toIndex >= 0 && toIndex < list.size &&
        fromIndex != toIndex) {

        val item = list[fromIndex]
        list.removeAt(fromIndex)
        list.add(toIndex, item)
    }
}

@Composable
fun SkillItem(
    skill: String,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    index: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reorder controls
            Column {
                IconButton(
                    onClick = { onMoveUp?.invoke() },
                    enabled = onMoveUp != null,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move Up",
                        modifier = Modifier.size(16.dp),
                        tint = if (onMoveUp != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                IconButton(
                    onClick = { onMoveDown?.invoke() },
                    enabled = onMoveDown != null,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move Down",
                        modifier = Modifier.size(16.dp),
                        tint = if (onMoveDown != null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }

            // Text field
            OutlinedTextField(
                value = skill,
                onValueChange = onValueChange,
                label = { Text("Skill ${index + 1}") },
                modifier = Modifier.weight(1f)
            )

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
@Composable
fun TemplatePickerDialog(
    onTemplateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val templateImages = mapOf(
        "Developer" to "https://www.getsetresumes.com/storage/resume-examples/December2021/enSlu3qMB9l9VWDyFHgP.jpg",
//        "Template 2" to "https://yourcdn.com/template2.png",
//        "Template 3" to "https://yourcdn.com/template3.png"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a Resume Template") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                templateImages.forEach { (label, url) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable { onTemplateSelected(label) }
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text(label, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

private fun handleTemplateDownload(context: Context, resumeData: ResumeData, templateKey: String) {
    val templateUrls = mapOf(
        "Developer" to "http://arjundubey.com/resume/template1",
        "Template 2" to "http://arjundubey.com/resume/template2",
        "Template 3" to "http://arjundubey.com/resume/template3"
    )

    try {
        // Convert compose state objects to serializable data
        val serializableData = mapOf(
            "personal" to mapOf(
                "name" to resumeData.personal.name,
                "title" to resumeData.personal.title,
                "email" to resumeData.personal.email,
                "phone" to resumeData.personal.phone,
                "location" to resumeData.personal.location,
                "linkedin" to resumeData.personal.linkedin,
                "github" to resumeData.personal.github,
                "website" to resumeData.personal.website
            ),
            "education" to resumeData.education.map { edu ->
                mapOf(
                    "institution" to edu.institution,
                    "degree" to edu.degree,
                    "location" to edu.location,
                    "startDate" to edu.startDate,
                    "endDate" to edu.endDate
                )
            },
            "experience" to resumeData.experience.map { exp ->
                mapOf(
                    "title" to exp.title,
                    "company" to exp.company,
                    "location" to exp.location,
                    "startDate" to exp.startDate,
                    "endDate" to exp.endDate,
                    "responsibilities" to exp.responsibilities.toList()
                )
            },
            "projects" to resumeData.projects.map { proj ->
                mapOf(
                    "name" to proj.name,
                    "technologies" to proj.technologies,
                    "startDate" to proj.startDate,
                    "endDate" to proj.endDate,
                    "description" to proj.description.toList(),
                    "link" to proj.link
                )
            },
            "skills" to mapOf(
                "languages" to resumeData.skills.languages.toList(),
                "frameworks" to resumeData.skills.frameworks.toList(),
                "tools" to resumeData.skills.tools.toList(),
                "libraries" to resumeData.skills.libraries.toList()
            )
        )

        val gson = Gson()
        val jsonString = gson.toJson(serializableData)
        val encodedData = URLEncoder.encode(jsonString, StandardCharsets.UTF_8.toString())
        val url = "${templateUrls[templateKey]}?data=$encodedData"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error - show Toast message like in single function
        e.printStackTrace()
        Toast.makeText(context, "Error generating resume", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun DownloadResumeScreen(context: Context, resumeData: ResumeData) {
    var showDialog by remember { mutableStateOf(false) }

    // Remove the Box entirely - just use the FAB alone
    FloatingActionButton(
        onClick = { showDialog = true },
        modifier = Modifier
            .padding(16.dp),
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "Download Resume",
            tint = Color.White
        )
    }

    if (showDialog) {
        TemplatePickerDialog(
            onTemplateSelected = { selected ->
                handleTemplateDownload(context, resumeData, selected)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}