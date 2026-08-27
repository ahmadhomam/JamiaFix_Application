package com.jamiafix.app.ui.screens.issue

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jamiafix.app.data.local.UserPreferences
import com.jamiafix.app.data.model.UserDto
import com.jamiafix.app.data.model.UserRole
import com.jamiafix.app.ui.components.PriorityBadge
import com.jamiafix.app.ui.components.StatusBadge
import com.jamiafix.app.ui.components.TimelineTracker
import com.jamiafix.app.ui.theme.JamiaGreenPrimary
import com.jamiafix.app.ui.theme.StatusClosedColor
import com.jamiafix.app.ui.theme.StatusInProgressColor
import com.jamiafix.app.ui.theme.StatusResolvedColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(
    viewModel: IssueDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val issue = uiState.issue

    var commentText by remember { mutableStateOf("") }
    var showResolveDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ticket Details", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadIssueDetail() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JamiaGreenPrimary)
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && issue == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = JamiaGreenPrimary)
            }
        } else if (issue == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.error ?: "Issue not found", color = MaterialTheme.colorScheme.error)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card: Title, Status, Metadata
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = "Category",
                                    tint = JamiaGreenPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = issue.categoryName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = JamiaGreenPrimary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PriorityBadge(priority = issue.priority)
                                StatusBadge(status = issue.status)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = issue.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = Color.Gray, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = issue.locationName, fontSize = 13.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = "Reporter", tint = Color.Gray, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Reported by: ${issue.reporterName}", fontSize = 12.sp, color = Color.Gray)
                        }

                        if (!issue.assigneeName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AssignmentInd, contentDescription = "Assignee", tint = Color(0xFF6A1B9A), modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Assigned to: ${issue.assigneeName}", fontSize = 12.sp, color = Color(0xFF6A1B9A), fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color(0xFFEEEEEE))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Problem Description",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = issue.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2D3748)
                        )

                        if (!issue.resolutionNotes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "✅ Resolution Notes (Technician):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = issue.resolutionNotes!!,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                            }
                        }
                    }
                }

                // Photos Attachment Gallery
                if (issue.images.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Attached Photos", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(issue.images) { img ->
                                    val fullUrl = if (img.imageUrl.startsWith("http")) img.imageUrl else "${UserPreferences.DEFAULT_BASE_URL.removeSuffix("/")}${img.imageUrl}"
                                    AsyncImage(
                                        model = fullUrl,
                                        contentDescription = "Attachment",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                        }
                    }
                }

                // 6-Stage Timeline Tracker
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    TimelineTracker(
                        currentStatus = issue.status,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Role-Adaptive Action Buttons
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Available Actions", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        when (uiState.currentUserRole) {
                            UserRole.STUDENT -> {
                                when (issue.status) {
                                    "RESOLVED" -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.updateStatus("CLOSED") },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusResolvedColor),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Done, contentDescription = "Confirm")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Confirm Fix")
                                            }

                                            OutlinedButton(
                                                onClick = { viewModel.updateStatus("SUBMITTED") },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Replay, contentDescription = "Reopen")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reopen")
                                            }
                                        }
                                    }
                                    "SUBMITTED" -> {
                                        OutlinedButton(
                                            onClick = { viewModel.updateStatus("CLOSED") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Cancel Issue")
                                        }
                                    }
                                    else -> {
                                        Text(text = "Ticket is being handled by campus maintenance staff.", fontSize = 13.sp, color = Color.Gray)
                                    }
                                }
                            }

                            UserRole.STAFF -> {
                                when (issue.status) {
                                    "ASSIGNED" -> {
                                        Button(
                                            onClick = { viewModel.updateStatus("IN_PROGRESS") },
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusInProgressColor),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Start Work (In Progress)")
                                        }
                                    }
                                    "IN_PROGRESS" -> {
                                        Button(
                                            onClick = { showResolveDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusResolvedColor),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Resolve")
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Mark Resolved + Notes")
                                        }
                                    }
                                    else -> {
                                        Text(text = "No pending technician actions for status '${issue.status}'.", fontSize = 13.sp, color = Color.Gray)
                                    }
                                }
                            }

                            UserRole.ADMIN -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (issue.status == "SUBMITTED") {
                                        Button(
                                            onClick = { viewModel.updateStatus("ACKNOWLEDGED") },
                                            colors = ButtonDefaults.buttonColors(containerColor = JamiaGreenPrimary),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Acknowledge Complaint")
                                        }
                                    }

                                    Button(
                                        onClick = { showAssignDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.AssignmentInd, contentDescription = "Assign")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (issue.assignedTo != null) "Reassign Staff" else "Assign Maintenance Staff")
                                    }

                                    if (issue.status != "CLOSED") {
                                        OutlinedButton(
                                            onClick = { viewModel.updateStatus("CLOSED") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Close / Reject Ticket")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Discussion Comments Thread
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Discussion & Updates (${issue.comments.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (issue.comments.isEmpty()) {
                            Text(text = "No comments yet. Post an update below.", fontSize = 12.sp, color = Color.Gray)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                issue.comments.forEach { c ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF7FAFC))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = c.userName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = JamiaGreenPrimary
                                                )
                                                Text(
                                                    text = c.userRole,
                                                    fontSize = 10.sp,
                                                    color = Color.Gray,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = c.comment, fontSize = 13.sp, color = Color.DarkGray)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Add Comment Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholder = { Text("Write a comment or update...") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                maxLines = 3
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        viewModel.postComment(commentText)
                                        commentText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(JamiaGreenPrimary, CircleShape)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // Resolve Dialog with Notes Input
    if (showResolveDialog) {
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showResolveDialog = false },
            title = { Text("Complete Resolution") },
            text = {
                Column {
                    Text(text = "Please enter resolution notes detailing the repairs made:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Resolution Notes *") },
                        placeholder = { Text("e.g. Replaced faulty circuit and tested projector.") },
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (notes.isNotBlank()) {
                            viewModel.updateStatus("RESOLVED", notes)
                            showResolveDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusResolvedColor)
                ) {
                    Text("Resolve Ticket")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResolveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Admin Staff Assignment Dialog
    if (showAssignDialog) {
        var selectedStaff by remember { mutableStateOf<UserDto?>(null) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = { Text("Assign Maintenance Staff") },
            text = {
                Column {
                    Text(text = "Select a technician to assign this ticket to:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedStaff?.name ?: "Select Technician",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            uiState.staffList.forEach { staff ->
                                DropdownMenuItem(
                                    text = { Text("${staff.name} (${staff.email})") },
                                    onClick = {
                                        selectedStaff = staff
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedStaff?.let {
                            viewModel.assignStaff(it.id)
                            showAssignDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JamiaGreenPrimary)
                ) {
                    Text("Assign")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAssignDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
