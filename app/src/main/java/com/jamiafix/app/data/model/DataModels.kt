package com.jamiafix.app.data.model

import com.google.gson.annotations.SerializedName

// --- User & Auth Models ---

enum class UserRole {
    @SerializedName("STUDENT") STUDENT,
    @SerializedName("STAFF") STAFF,
    @SerializedName("ADMIN") ADMIN;

    companion object {
        fun fromString(role: String?): UserRole {
            return when (role?.uppercase()) {
                "STAFF" -> STAFF
                "ADMIN" -> ADMIN
                else -> STUDENT
            }
        }
    }
}

data class UserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("created_at") val createdAt: String? = null
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("role") val role: String = "STUDENT"
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("user") val user: UserDto
)

// --- Metadata Models ---

data class CategoryDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null
)

data class CategoryCreateRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null
)

data class LocationDto(
    @SerializedName("id") val id: Int,
    @SerializedName("building") val building: String,
    @SerializedName("room") val room: String? = null,
    @SerializedName("description") val description: String? = null
) {
    val displayName: String
        get() = if (!room.isNullOrBlank()) "$building ($room)" else building
}

data class LocationCreateRequest(
    @SerializedName("building") val building: String,
    @SerializedName("room") val room: String? = null,
    @SerializedName("description") val description: String? = null
)

// --- Issue Models & Lifecycle ---

enum class IssueStatus(val label: String) {
    @SerializedName("SUBMITTED") SUBMITTED("Submitted"),
    @SerializedName("ACKNOWLEDGED") ACKNOWLEDGED("Acknowledged"),
    @SerializedName("ASSIGNED") ASSIGNED("Assigned"),
    @SerializedName("IN_PROGRESS") IN_PROGRESS("In Progress"),
    @SerializedName("RESOLVED") RESOLVED("Resolved"),
    @SerializedName("CLOSED") CLOSED("Closed");

    companion object {
        fun fromString(status: String?): IssueStatus {
            return when (status?.uppercase()) {
                "ACKNOWLEDGED" -> ACKNOWLEDGED
                "ASSIGNED" -> ASSIGNED
                "IN_PROGRESS" -> IN_PROGRESS
                "RESOLVED" -> RESOLVED
                "CLOSED" -> CLOSED
                else -> SUBMITTED
            }
        }
    }
}

enum class IssuePriority(val label: String) {
    @SerializedName("LOW") LOW("Low"),
    @SerializedName("MEDIUM") MEDIUM("Medium"),
    @SerializedName("HIGH") HIGH("High"),
    @SerializedName("URGENT") URGENT("Urgent");

    companion object {
        fun fromString(priority: String?): IssuePriority {
            return when (priority?.uppercase()) {
                "LOW" -> LOW
                "HIGH" -> HIGH
                "URGENT" -> URGENT
                else -> MEDIUM
            }
        }
    }
}

data class IssueDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("location_id") val locationId: Int,
    @SerializedName("location_name") val locationName: String,
    @SerializedName("status") val status: String,
    @SerializedName("priority") val priority: String,
    @SerializedName("reported_by") val reportedBy: Int,
    @SerializedName("reporter_name") val reporterName: String,
    @SerializedName("assigned_to") val assignedTo: Int? = null,
    @SerializedName("assignee_name") val assigneeName: String? = null,
    @SerializedName("resolution_notes") val resolutionNotes: String? = null,
    @SerializedName("comments_count") val commentsCount: Int = 0,
    @SerializedName("images_count") val imagesCount: Int = 0,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class IssueDetailDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("location_id") val locationId: Int,
    @SerializedName("location_name") val locationName: String,
    @SerializedName("status") val status: String,
    @SerializedName("priority") val priority: String,
    @SerializedName("reported_by") val reportedBy: Int,
    @SerializedName("reporter_name") val reporterName: String,
    @SerializedName("assigned_to") val assignedTo: Int? = null,
    @SerializedName("assignee_name") val assigneeName: String? = null,
    @SerializedName("resolution_notes") val resolutionNotes: String? = null,
    @SerializedName("comments_count") val commentsCount: Int = 0,
    @SerializedName("images_count") val imagesCount: Int = 0,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("comments") val comments: List<CommentDto> = emptyList(),
    @SerializedName("images") val images: List<ImageAttachmentDto> = emptyList(),
    @SerializedName("allowed_next_statuses") val allowedNextStatuses: List<String> = emptyList()
)

data class CommentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("issue_id") val issueId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("user_name") val userName: String,
    @SerializedName("user_role") val userRole: String,
    @SerializedName("comment") val comment: String,
    @SerializedName("created_at") val createdAt: String
)

data class ImageAttachmentDto(
    @SerializedName("id") val id: Int,
    @SerializedName("issue_id") val issueId: Int,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("created_at") val createdAt: String
)

data class IssueCreateRequest(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("location_id") val locationId: Int,
    @SerializedName("priority") val priority: String = "MEDIUM"
)

data class IssueStatusUpdateRequest(
    @SerializedName("status") val status: String,
    @SerializedName("resolution_notes") val resolutionNotes: String? = null
)

data class IssueAssignRequest(
    @SerializedName("staff_id") val staffId: Int
)

data class CommentCreateRequest(
    @SerializedName("comment") val comment: String
)
