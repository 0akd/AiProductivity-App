package com.arjundubey.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// Data Models
data class JobSearchRequest(
    val keyword: String = "software engineer",
    val location: String = "United States",
    val dateSincePosted: String = "",
    val jobType: String = "",
    val remoteFilter: String = "",
    val salary: String = "",
    val experienceLevel: String = "",
    val limit: String = "20",
    val sortBy: String = "recent",
    val page: String = "0",
    val has_verification: Boolean = false,
    val under_10_applicants: Boolean = false
)

data class JobListing(
    val position: String?,
    val company: String?,
    val location: String?,
    val date: String?,
    val salary: String?,
    val jobUrl: String?,
    val companyLogo: String?,
    val companyUrl: String?,
    val applyUrl: String?,
    val agoTime: String?
)

data class JobResponse(
    val success: Boolean,
    val count: Int,
    val data: List<JobListing>,
    val timestamp: String
)

// Retrofit API Interface
interface LinkedInJobsApi {
    @POST("api/v1/jobs")
    suspend fun searchJobs(@Body request: JobSearchRequest): JobResponse

    // Update your Retrofit interface companion object
    companion object {
        fun create(baseUrl: String): LinkedInJobsApi {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS) // Increase connection timeout
                .readTimeout(60, TimeUnit.SECONDS)    // Increase read timeout
                .writeTimeout(30, TimeUnit.SECONDS)   // Increase write timeout
                .retryOnConnectionFailure(true)       // Retry on failure
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(LinkedInJobsApi::class.java)
        }
    }
}

// ViewModel
class JobSearchViewModel : ViewModel() {
    var jobs by mutableStateOf<List<JobListing>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val api = LinkedInJobsApi.create("https://jobserver-bfdk.onrender.com/")

    fun searchJobs(request: JobSearchRequest) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = api.searchJobs(request)
                jobs = response.data
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to fetch jobs"
            } finally {
                isLoading = false
            }
        }
    }
}

// Main Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobSearchScreen(
    viewModel: JobSearchViewModel = viewModel()
) {
    var keyword by remember { mutableStateOf("software engineer") }
    var location by remember { mutableStateOf("United States") }
    var showFilters by remember { mutableStateOf(false) }

    // Filter states
    var dateSincePosted by remember { mutableStateOf("") }
    var jobType by remember { mutableStateOf("") }
    var remoteFilter by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var experienceLevel by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("20") }
    var hasVerification by remember { mutableStateOf(false) }
    var under10Applicants by remember { mutableStateOf(false) }

    Scaffold(

    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()

        ) {
            // Compact Search Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                  ,
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // Compact keyword and location in a row for very small screens
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            label = { Text("Job Title", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Location", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Scrollable Filters Section
                    if (showFilters) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp) // Limit height for scrolling
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                "Filters",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            FilterDropdown(
                                label = "Date Posted",
                                value = dateSincePosted,
                                options = listOf("", "24hr", "past Week", "past Month"),
                                onValueChange = { dateSincePosted = it }
                            )

                            FilterDropdown(
                                label = "Job Type",
                                value = jobType,
                                options = listOf("", "full time", "part time", "contract", "temporary", "volunteer", "internship"),
                                onValueChange = { jobType = it }
                            )

                            FilterDropdown(
                                label = "Remote",
                                value = remoteFilter,
                                options = listOf("", "remote", "on site", "hybrid"),
                                onValueChange = { remoteFilter = it }
                            )

                            FilterDropdown(
                                label = "Experience Level",
                                value = experienceLevel,
                                options = listOf("", "internship", "entry level", "associate", "senior", "director", "executive"),
                                onValueChange = { experienceLevel = it }
                            )

                            FilterDropdown(
                                label = "Min. Salary",
                                value = salary,
                                options = listOf("", "40000", "60000", "80000", "100000", "120000"),
                                onValueChange = { salary = it }
                            )

                            FilterDropdown(
                                label = "Results",
                                value = limit,
                                options = listOf("10", "20", "50", "100"),
                                onValueChange = { limit = it }
                            )

                            // Compact checkboxes
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = hasVerification,
                                        onCheckedChange = { hasVerification = it },
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text("Verified only", style = MaterialTheme.typography.bodySmall)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = under10Applicants,
                                        onCheckedChange = { under10Applicants = it },
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text("Under 10 applicants", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search and Filter Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Filter Button on the left
                        OutlinedButton(
                            onClick = { showFilters = !showFilters },
                            modifier = Modifier
                                .weight(0.3f)
                                .height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (showFilters) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filters",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Filter", style = MaterialTheme.typography.labelMedium)
                        }

                        // Search Button on the right
                        Button(
                            onClick = {
                                viewModel.searchJobs(
                                    JobSearchRequest(
                                        keyword = keyword,
                                        location = location,
                                        dateSincePosted = dateSincePosted,
                                        jobType = jobType,
                                        remoteFilter = remoteFilter,
                                        salary = salary,
                                        experienceLevel = experienceLevel,
                                        limit = limit,
                                        has_verification = hasVerification,
                                        under_10_applicants = under10Applicants
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(0.7f)
                                .height(44.dp),
                            enabled = !viewModel.isLoading
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Search", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // Results Section
            when {
                viewModel.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                viewModel.errorMessage != null -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "Error: ${viewModel.errorMessage}",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                viewModel.jobs.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "${viewModel.jobs.size} jobs found",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        items(viewModel.jobs) { job ->
                            JobCard(jobListing = job)
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Enter search criteria and tap Search",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value.ifEmpty { "Any" },
            onValueChange = {},
            readOnly = true,
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.ifEmpty { "Any" }, style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
fun JobCard(jobListing: JobListing) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                jobListing.jobUrl?.let { url ->
                    uriHandler.openUri(url)
                }
            },
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Company Logo - Smaller for compact view
            if (!jobListing.companyLogo.isNullOrEmpty()) {
                AsyncImage(
                    model = jobListing.companyLogo,
                    contentDescription = "Company Logo",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = "Company",
                        modifier = Modifier.padding(10.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Job Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = jobListing.position ?: "Unknown Position",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = jobListing.company ?: "Unknown Company",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = jobListing.location ?: "Unknown Location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Salary and Time Posted Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (jobListing.salary != null && jobListing.salary != "Not specified") {
                        Text(
                            text = jobListing.salary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!jobListing.agoTime.isNullOrEmpty()) {
                        Text(
                            text = jobListing.agoTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}