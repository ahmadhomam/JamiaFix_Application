package com.jamiafix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jamiafix.app.data.model.IssuePriority
import com.jamiafix.app.data.model.IssueStatus
import com.jamiafix.app.ui.theme.PriorityHigh
import com.jamiafix.app.ui.theme.PriorityLow
import com.jamiafix.app.ui.theme.PriorityMedium
import com.jamiafix.app.ui.theme.PriorityUrgent
import com.jamiafix.app.ui.theme.StatusAcknowledgedColor
import com.jamiafix.app.ui.theme.StatusAssignedColor
import com.jamiafix.app.ui.theme.StatusClosedColor
import com.jamiafix.app.ui.theme.StatusInProgressColor
import com.jamiafix.app.ui.theme.StatusResolvedColor
import com.jamiafix.app.ui.theme.StatusSubmittedColor

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val issueStatus = IssueStatus.fromString(status)
    val (bgColor, textColor) = when (issueStatus) {
        IssueStatus.SUBMITTED -> StatusSubmittedColor.copy(alpha = 0.15f) to StatusSubmittedColor
        IssueStatus.ACKNOWLEDGED -> StatusAcknowledgedColor.copy(alpha = 0.15f) to StatusAcknowledgedColor
        IssueStatus.ASSIGNED -> StatusAssignedColor.copy(alpha = 0.15f) to StatusAssignedColor
        IssueStatus.IN_PROGRESS -> StatusInProgressColor.copy(alpha = 0.15f) to StatusInProgressColor
        IssueStatus.RESOLVED -> StatusResolvedColor.copy(alpha = 0.15f) to StatusResolvedColor
        IssueStatus.CLOSED -> StatusClosedColor.copy(alpha = 0.15f) to StatusClosedColor
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = issueStatus.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PriorityBadge(
    priority: String,
    modifier: Modifier = Modifier
) {
    val issuePriority = IssuePriority.fromString(priority)
    val color = when (issuePriority) {
        IssuePriority.LOW -> PriorityLow
        IssuePriority.MEDIUM -> PriorityMedium
        IssuePriority.HIGH -> PriorityHigh
        IssuePriority.URGENT -> PriorityUrgent
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = issuePriority.label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
