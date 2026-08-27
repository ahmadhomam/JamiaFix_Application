package com.jamiafix.app.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.jamiafix.app.data.model.IssueStatus
import com.jamiafix.app.data.repository.AuthRepository
import com.jamiafix.app.data.repository.IssueRepository
import com.jamiafix.app.ui.components.IssueCard
import com.jamiafix.app.ui.components.OfflineBanner
import com.jamiafix.app.ui.theme.JamiaGoldAccent
import com.jamiafix.app.ui.theme.JamiaGreenPrimary
import com.jamiafix.app.ui.theme.StatusSubmittedColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    authRepository: AuthRepository,
    issueRepository: IssueRepository,
    onNavigateToManageCampus: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val adminName by authRepository.userName.collectAsState(initial = "Administrator")

    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    val allIssues by issueRepository.allIssuesFlow.collectAsState(initial = emptyList())

    val displayedIssues = remember(allIssues, selectedStatusFilter) {
        if (selectedStatusFilter != null) {
            allIssues.filter { it.status.equals(selectedStatusFilter, ignoreCase = true) }
        } else {
            allIssues
        }
    }

    val unassignedCount = remember(allIssues) {
        allIssues.count { it.status == "SUBMITTED" || it.status == "ACKNOWLEDGED" }
    }
    val inProgressCount = remember(allIssues) {
        allIssues.count { it.status == "IN_PROGRESS" || it.status == "ASSIGNED" }
    }
    val resolvedCount = remember(allIssues) {
        allIssues.count { it.status == "RESOLVED" || it.status == "CLOSED" }
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
                            text = "JamiaFix Admin Hub",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Chief Admin: $adminName",
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
                onClick = onNavigateToManageCampus,
                containerColor = JamiaGreenPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Domain, contentDescription = "Manage Campus") },
                text = { Text("Campus Meta", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OfflineBanner()

            // Metrics Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Pending",
                    count = unassignedCount,
                    color = StatusSubmittedColor,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Active",
                    count = inProgressCount,
                    color = Color(0xFFF57F17),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Resolved",
                    count = resolvedCount,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
            }

            // Status Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("All (${allIssues.size})") }
                    )
                }
                items(IssueStatus.values()) { status ->
                    val count = allIssues.count { it.status.equals(status.name, ignoreCase = true) }
                    FilterChip(
                        selected = selectedStatusFilter == status.name,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == status.name) null else status.name
                        },
                        label = { Text("${status.label} ($count)") }
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
                    Text(
                        text = "No campus issues matching this filter.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
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
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
