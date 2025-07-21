package com.example.resumebuilder
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.layout.ContentScale




import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ResumeBuilderApp()
            }
        }
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

data class ResumeData(
    var personal: PersonalInfo = PersonalInfo(),
    var education: SnapshotStateList<Education> = mutableStateListOf(),
    var experience: SnapshotStateList<Experience> = mutableStateListOf(),
    var projects: SnapshotStateList<Project> = mutableStateListOf(),
    var skills: Skills = Skills()
)

enum class ResumeTab(val title: String, val icon: ImageVector) {
    PERSONAL("Personal", Icons.Default.Person),
    EDUCATION("Education", Icons.Default.School),
    EXPERIENCE("Experience", Icons.Default.Work),
    PROJECTS("Projects", Icons.Default.Code),
    SKILLS("Skills", Icons.Default.Star)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeBuilderApp() {
    val resumeData = remember { mutableStateOf(ResumeData()) }
    val selectedTab = remember { mutableStateOf(ResumeTab.PERSONAL) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Resume Builder",
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

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
                .padding(16.dp)
        ) {
            when (selectedTab.value) {
                ResumeTab.PERSONAL -> PersonalInfoTab(resumeData.value.personal)
                ResumeTab.EDUCATION -> EducationTab(resumeData.value.education)
                ResumeTab.EXPERIENCE -> ExperienceTab(resumeData.value.experience)
                ResumeTab.PROJECTS -> ProjectsTab(resumeData.value.projects)
                ResumeTab.SKILLS -> SkillsTab(resumeData.value.skills)
            }
        }
        DownloadResumeScreen(context, resumeData.value)

    }
}

@Composable
fun PersonalInfoTab(personalInfo: PersonalInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Personal Information",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
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
        modifier = Modifier.fillMaxSize()
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

            IconButton(
                onClick = { educationList.add(Education()) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Education")
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(educationList) { index, education ->
                EducationItem(
                    education = education,
                    onDelete = { educationList.removeAt(index) }
                )
            }
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

@Composable
fun ExperienceTab(experienceList: SnapshotStateList<Experience>) {
    Column(
        modifier = Modifier.fillMaxSize()
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

            IconButton(
                onClick = {
                    experienceList.add(Experience().apply {
                        responsibilities.add("")
                    })
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Experience")
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(experienceList) { index, experience ->
                ExperienceItem(
                    experience = experience,
                    onDelete = { experienceList.removeAt(index) }
                )
            }
        }
    }
}

@Composable
fun ExperienceItem(experience: Experience, onDelete: () -> Unit) {
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
                    text = "Experience Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
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

            // Responsibilities
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = responsibility,
                        onValueChange = { experience.responsibilities[index] = it },
                        label = { Text("Responsibility ${index + 1}") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { experience.responsibilities.removeAt(index) }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectsTab(projectsList: SnapshotStateList<Project>) {
    Column(
        modifier = Modifier.fillMaxSize()
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

            IconButton(
                onClick = {
                    projectsList.add(Project().apply {
                        description.add("")
                    })
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Project")
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(projectsList) { index, project ->
                ProjectItem(
                    project = project,
                    onDelete = { projectsList.removeAt(index) }
                )
            }
        }
    }
}

@Composable
fun ProjectItem(project: Project, onDelete: () -> Unit) {
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
                    text = "Project Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
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

            // Description
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { project.description[index] = it },
                        label = { Text("Description ${index + 1}") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { project.description.removeAt(index) }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = skill,
                        onValueChange = { skillsList[index] = it },
                        label = { Text("Skill ${index + 1}") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { skillsList.removeAt(index) }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
@Composable
fun TemplatePickerDialog(
    onTemplateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val templateImages = mapOf(
        "Template 1" to "https://images.unsplash.com/photo-1682686581740-2c5f76eb86d1?q=80&w=1171&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDF8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D",
        "Template 2" to "https://yourcdn.com/template2.png",
        "Template 3" to "https://yourcdn.com/template3.png"
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
        "Template 1" to "http://arjundubey.com/resume/template1",
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

    Button(onClick = { showDialog = true }) {
        Text("Download Resume")
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