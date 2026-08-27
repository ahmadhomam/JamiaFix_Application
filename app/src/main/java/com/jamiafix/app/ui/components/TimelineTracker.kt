package com.jamiafix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jamiafix.app.data.model.IssueStatus
import com.jamiafix.app.ui.theme.JamiaGreenPrimary

@Composable
fun TimelineTracker(
    currentStatus: String,
    modifier: Modifier = Modifier
) {
    val stages = listOf(
        IssueStatus.SUBMITTED to "Submitted",
        IssueStatus.ACKNOWLEDGED to "Acknowledged",
        IssueStatus.ASSIGNED to "Assigned",
        IssueStatus.IN_PROGRESS to "In Progress",
        IssueStatus.RESOLVED to "Resolved",
        IssueStatus.CLOSED to "Closed"
    )

    val currentEnum = IssueStatus.fromString(currentStatus)
    val currentIndex = stages.indexOfFirst { it.first == currentEnum }.let { if (it == -1) 0 else it }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Issue Lifecycle Progress",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        stages.forEachIndexed { index, (stageEnum, label) ->
            val isCompleted = index < currentIndex
            val isCurrent = index == currentIndex
            val isPending = index > currentIndex

            val dotColor = when {
                isCompleted -> JamiaGreenPrimary
                isCurrent -> Color(0xFFE65100)
                else -> Color.LightGray
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Step Indicator Node
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (index < stages.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(20.dp)
                                .background(if (index < currentIndex) JamiaGreenPrimary else Color.LightGray)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Label and status note
                Column(modifier = Modifier.padding(bottom = if (index < stages.size - 1) 16.dp else 0.dp)) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCurrent) Color(0xFFE65100) else if (isCompleted) Color(0xFF1B5E20) else Color.Gray
                    )
                    if (isCurrent) {
                        Text(
                            text = "Current stage",
                            fontSize = 11.sp,
                            color = Color(0xFFE65100).copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
