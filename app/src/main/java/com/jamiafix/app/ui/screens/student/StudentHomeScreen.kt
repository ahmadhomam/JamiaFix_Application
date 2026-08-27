package com.jamiafix.app.ui.screens.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jamiafix.app.data.model.IssueDto
import com.jamiafix.app.data.model.IssueStatus
import com.jamiafix.app.data.repository.AuthRepository
import com.jamiafix.app.data.repository.IssueRepository
import com.jamiafix.app.ui.components.IssueCard
import com.jamiafix.app.ui.components.OfflineBanner
import com.jamiafix.app.ui.theme.JamiaGreenPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    authRepository: AuthRepository,
    issueRepository: IssueRepository,
    onNavigateToCreateIssue: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val studentName by authRepository.userName.collectAsState(initial = "Student")
    val studentId by authRepository.userId.collectAsState(initial = null)

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: My Issues, 1: Campus Feed
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    val allIssues by issueRepository.allIssuesFlow.collectAsState(initial = emptyList())

    // Filter issues based on tab & status chip
    val displayedIssues = remember(allIssues, selectedTabIndex, selectedStatusFilter, studentId) {
        val tabFiltered = if (selectedTabIndex == 0 && studentId != null) {
            allIssues.filter { it.reportedBy == studentId }
        } else {
            allIssues
        }

        if (selectedStatusFilter != null) {
            tabFiltered.filter { it.status.equals(selectedStatusFilter, ignoreCase = true) }
        } else {
            tabFiltered
        }
    }

    val pendingSyncCount = remember(allIssues) {
        allIssues.count { it.id < 0 } // Negative IDs indicate local pending offline issues
    }

    fun refresh() {
        coroutineScope.launch {
            isRefreshing = true
            issueRepository.refreshIssues()
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "JamiaFix",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Student: $studentName",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JamiaGreenPrimary)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreateIssue,
                containerColor = JamiaGreenPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = "Report Issue") },
                text = { Text("Report Issue", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Offline sync indicator
            OfflineBanner(pendingCount = pendingSyncCount)

            // Tabs: My Issues vs Campus Feed
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = JamiaGreenPrimary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("My Issues", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Campus Feed", fontWeight = FontWeight.Bold) }
                )
            }

            // Status Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("All") }
                    )
                }
                items(IssueStatus.values()) { status ->
                    FilterChip(
                        selected = selectedStatusFilter == status.name,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == status.name) null else status.name
                        },
                        label = { Text(status.label) }
                    )
                }
            }

            // Issues List
            if (isRefreshing && displayedIssues.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = JamiaGreenPrimary)
                }
            } else if (displayedIssues.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedTabIndex == 0) "No issues reported by you yet." else "No campus issues found.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap '+ Report Issue' to log a campus maintenance problem.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedIssues, key = { it.id }) { issue ->
                        IssueCard(
                            issue = issue,
                            onClick = { onNavigateToDetail(issue.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp)) // Space for FAB
                    }
                }
            }
        }
    }
}
