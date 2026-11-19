package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ClassScheduleEntity
import com.example.data.DateUtils
import com.example.data.JobApplicationEntity
import com.example.data.JobApplicationStatus
import com.example.domain.LifeOsAiAssistant
import com.example.ui.LifeOsUiState
import com.example.ui.LifeOsViewModel
import com.example.ui.components.AppleSegmentedControl
import com.example.ui.components.AppleSubScreenHeader
import com.example.ui.components.GlassCard
import com.example.ui.components.LifeOsBadgeType
import com.example.ui.theme.JetBrainsMonoFamily

private val weekDays = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")

// ---------------- CLASS TIMETABLE SCREEN ----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassTimetableScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var viewTab by remember { mutableIntStateOf(0) } // 0=Today, 1=This Week, 2=Next Week, 3=Full
    var showAddDialog by remember { mutableStateOf(false) }

    val todayDow = remember(state.selectedDateIso) { LifeOsAiAssistant.isoDayOfWeekMon1Sun7(state.selectedDateIso) }
    val filteredClasses = remember(state.classSchedules, viewTab, todayDow) {
        when (viewTab) {
            0 -> state.classSchedules.filter { it.dayOfWeek == todayDow }
            1 -> state.classSchedules.filter { it.dayOfWeek >= todayDow }
            else -> state.classSchedules
        }.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("class_timetable_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppleSubScreenHeader(
                    title = "Class Timetable",
                    subtitle = "${state.classSchedules.size} scheduled classes",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.CALENDAR
                )
            }

            item {
                AppleSegmentedControl(
                    options = listOf("Today", "Week", "Next", "All"),
                    selectedIndex = viewTab,
                    onSelect = { viewTab = it }
                )
            }

            item {
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_class_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Class to Timetable")
                }
            }

            if (filteredClasses.isEmpty()) {
                item {
                    GlassCard {
                        Text(
                            text = if (viewTab == 0) "No classes scheduled for today (${weekDays.first { it.first == todayDow }.second})." else "Your class timetable is empty. Add your weekly classes above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(filteredClasses, key = { it.id }) { item ->
                    val dayName = weekDays.firstOrNull { it.first == item.dayOfWeek }?.second ?: "Mon"
                    GlassCard(cornerRadius = 18.dp) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text(
                                        text = "$dayName • ${item.startTime} – ${item.endTime}",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (item.reminderEnabled) {
                                        Icon(Icons.Filled.Notifications, contentDescription = "Reminder on", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { viewModel.deleteClassSchedule(item.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete class", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            Text(item.className, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (item.subject.isNotBlank() || item.instructor.isNotBlank()) {
                                Text(
                                    text = listOfNotNull(item.subject.takeIf { it.isNotBlank() }, item.instructor.takeIf { it.isNotBlank() }?.let { "Instructor: $it" }).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (item.locationOrLink.isNotBlank()) {
                                if (item.locationOrLink.startsWith("http")) {
                                    FilledTonalButton(onClick = {
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.locationOrLink))) }
                                    }) {
                                        Text("Open Class Link")
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                } else {
                                    Text("Location: ${item.locationOrLink}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var className by remember { mutableStateOf("") }
        var subject by remember { mutableStateOf("") }
        var instructor by remember { mutableStateOf("") }
        var dayOfWeek by remember { mutableIntStateOf(todayDow) }
        var startTime by remember { mutableStateOf("09:00") }
        var endTime by remember { mutableStateOf("10:00") }
        var location by remember { mutableStateOf("") }
        var reminder by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Class Schedule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = className, onValueChange = { className = it }, label = { Text("Class / Course Name") }, singleLine = true)
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject (optional)") }, singleLine = true)
                    OutlinedTextField(value = instructor, onValueChange = { instructor = it }, label = { Text("Instructor (optional)") }, singleLine = true)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        weekDays.forEach { (d, label) ->
                            FilterChip(selected = dayOfWeek == d, onClick = { dayOfWeek = d }, label = { Text(label, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Start (HH:MM)") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("End (HH:MM)") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Room or Meeting URL") }, singleLine = true)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Class Reminder", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = reminder, onCheckedChange = { reminder = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = className.isNotBlank(),
                    onClick = {
                        viewModel.saveClassSchedule(
                            ClassScheduleEntity(
                                userId = state.userId,
                                className = className.trim(),
                                subject = subject.trim(),
                                instructor = instructor.trim(),
                                dayOfWeek = dayOfWeek,
                                startTime = startTime.trim(),
                                endTime = endTime.trim(),
                                locationOrLink = location.trim(),
                                reminderEnabled = reminder
                            )
                        )
                        showAddDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}

// ---------------- JOB APPLICATIONS & INTERVIEW TRACKER SCREEN ----------------

private data class OfficialJobBoard(val name: String, val focus: String, val url: String)

private val officialFresherBoards = listOf(
    OfficialJobBoard("LinkedIn Entry-Level Tech", "Software Engineer (0–1 yr)", "https://www.linkedin.com/jobs/search/?keywords=software%20engineer%20fresher&f_E=1%2C2"),
    OfficialJobBoard("Naukri Fresher IT Jobs", "Java / Python / Full Stack", "https://www.naukri.com/software-developer-fresher-jobs"),
    OfficialJobBoard("Internshala Fresher Jobs", "Internships & Entry Roles", "https://internshala.com/fresher-jobs/software-development-jobs/"),
    OfficialJobBoard("Wellfound Startup Roles", "Early-stage Engineering", "https://wellfound.com/jobs")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JobInterviewTrackerScreen(
    state: LifeOsUiState,
    viewModel: LifeOsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var statusFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    val openUrl: (String) -> Unit = { url ->
        if (url.startsWith("http")) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    val filteredApps = remember(state.jobApplications, statusFilter) {
        if (statusFilter == "ALL") state.jobApplications else state.jobApplications.filter { it.status == statusFilter }
    }
    val upcomingInterviews = remember(state.jobApplications, state.selectedDateIso) {
        state.jobApplications.filter { it.interviewDateIso.isNotBlank() && it.interviewDateIso >= state.selectedDateIso }
            .sortedBy { it.interviewDateIso }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("jobs_interviews_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppleSubScreenHeader(
                    title = "Jobs & Interviews",
                    subtitle = "${state.jobApplications.size} tracked • ${upcomingInterviews.size} upcoming",
                    onBack = onBack,
                    badgeType = LifeOsBadgeType.CAREER
                )
            }

            // Upcoming Interviews Highlight
            if (upcomingInterviews.isNotEmpty()) {
                item {
                    GlassCard(accentTint = MaterialTheme.colorScheme.tertiary) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Event, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Text("Upcoming Interviews (${upcomingInterviews.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            upcomingInterviews.take(3).forEach { iv ->
                                Text(
                                    text = "• ${iv.company} (${iv.role}) — ${iv.interviewDateIso} ${iv.interviewTime} (${iv.interviewType})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_job_application_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Track Job Application / Interview")
                }
            }

            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(selected = statusFilter == "ALL", onClick = { statusFilter = "ALL" }, label = { Text("All (${state.jobApplications.size})") })
                    JobApplicationStatus.entries.forEach { st ->
                        FilterChip(
                            selected = statusFilter == st.name,
                            onClick = { statusFilter = st.name },
                            label = { Text(st.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            if (filteredApps.isEmpty()) {
                item {
                    GlassCard {
                        Text(
                            text = "No job applications tracked in this filter yet. Add your real applications and interview schedules above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(filteredApps, key = { it.id }) { app ->
                    val statusEnum = JobApplicationStatus.fromNameOrDefault(app.status)
                    val checklist = app.prepChecklistCsv.split("|").map { it.trim() }.filter { it.isNotBlank() }
                    val doneSet = app.completedChecklistCsv.split("|").map { it.trim() }.filter { it.isNotBlank() }.toSet()

                    GlassCard(cornerRadius = 18.dp) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${app.company} — ${app.role}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${app.jobType} • Applied ${app.appliedDateIso}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.deleteJobApplication(app.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete application", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                JobApplicationStatus.entries.forEach { st ->
                                    FilterChip(
                                        selected = statusEnum == st,
                                        onClick = { viewModel.saveJobApplication(app.copy(status = st.name)) },
                                        label = { Text(st.label, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            if (app.interviewDateIso.isNotBlank()) {
                                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = "Interview: ${app.interviewDateIso} ${app.interviewTime} • ${app.interviewType}",
                                            fontFamily = JetBrainsMonoFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        if (app.meetingLink.startsWith("http")) {
                                            TextButton(onClick = { openUrl(app.meetingLink) }) {
                                                Text("Join Meeting Link")
                                                Spacer(Modifier.width(4.dp))
                                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            if (checklist.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Preparation Checklist", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    checklist.forEach { step ->
                                        val isDone = step in doneSet
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { viewModel.toggleJobChecklistItem(app, step) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isDone) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                                    contentDescription = "Toggle prep item",
                                                    tint = if (isDone) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(step, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Official Verified Fresher (0–1 yr) Job Portals (Never fake job listings!)
            item {
                Text("Official Fresher (0–1 yr) Job Portals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Browse verified live software engineering openings directly on official career platforms.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(officialFresherBoards, key = { it.url }) { board ->
                GlassCard(cornerRadius = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Work, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(board.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(board.focus, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        FilledTonalButton(onClick = { openUrl(board.url) }) {
                            Text("Browse")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var company by remember { mutableStateOf("") }
        var role by remember { mutableStateOf("") }
        var jobType by remember { mutableStateOf("Full-time") }
        var link by remember { mutableStateOf("") }
        var interviewDate by remember { mutableStateOf("") }
        var interviewTime by remember { mutableStateOf("") }
        var interviewType by remember { mutableStateOf("Technical") }
        var meetingLink by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Track Job Application") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company Name") }, singleLine = true)
                    OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role (e.g. Java Developer)") }, singleLine = true)
                    OutlinedTextField(value = jobType, onValueChange = { jobType = it }, label = { Text("Job Type (Full-time / Intern)") }, singleLine = true)
                    OutlinedTextField(value = link, onValueChange = { link = it }, label = { Text("Application Link (optional)") }, singleLine = true)
                    OutlinedTextField(value = interviewDate, onValueChange = { interviewDate = it }, label = { Text("Interview Date (YYYY-MM-DD, opt)") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = interviewTime, onValueChange = { interviewTime = it }, label = { Text("Time (HH:MM)") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = interviewType, onValueChange = { interviewType = it }, label = { Text("Round") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = meetingLink, onValueChange = { meetingLink = it }, label = { Text("Meeting Link (optional)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    enabled = company.isNotBlank() && role.isNotBlank(),
                    onClick = {
                        viewModel.saveJobApplication(
                            JobApplicationEntity(
                                userId = state.userId,
                                company = company.trim(),
                                role = role.trim(),
                                jobType = jobType.ifBlank { "Full-time" },
                                appliedDateIso = DateUtils.todayIso(),
                                applicationLink = link.trim(),
                                status = if (interviewDate.isNotBlank()) JobApplicationStatus.INTERVIEW_SCHEDULED.name else JobApplicationStatus.APPLIED.name,
                                interviewDateIso = interviewDate.trim(),
                                interviewTime = interviewTime.trim(),
                                interviewType = interviewType.ifBlank { "Technical" },
                                meetingLink = meetingLink.trim()
                            )
                        )
                        showAddDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}
