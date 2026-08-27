package com.jamiafix.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jamiafix.app.data.model.CategoryDto
import com.jamiafix.app.data.model.IssueDto
import com.jamiafix.app.data.model.LocationDto

@Entity(tableName = "issues")
data class IssueEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String,
    val categoryId: Int,
    val categoryName: String,
    val locationId: Int,
    val locationName: String,
    val status: String,
    val priority: String,
    val reportedBy: Int,
    val reporterName: String,
    val assignedTo: Int? = null,
    val assigneeName: String? = null,
    val resolutionNotes: String? = null,
    val commentsCount: Int = 0,
    val imagesCount: Int = 0,
    val createdAt: String,
    val updatedAt: String,
    val syncStatus: String = "SYNCED" // "SYNCED" or "PENDING_SYNC"
) {
    fun toDto(): IssueDto {
        return IssueDto(
            id = id,
            title = title,
            description = description,
            categoryId = categoryId,
            categoryName = categoryName,
            locationId = locationId,
            locationName = locationName,
            status = status,
            priority = priority,
            reportedBy = reportedBy,
            reporterName = reporterName,
            assignedTo = assignedTo,
            assigneeName = assigneeName,
            resolutionNotes = resolutionNotes,
            commentsCount = commentsCount,
            imagesCount = imagesCount,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDto(dto: IssueDto, syncStatus: String = "SYNCED"): IssueEntity {
            return IssueEntity(
                id = dto.id,
                title = dto.title,
                description = dto.description,
                categoryId = dto.categoryId,
                categoryName = dto.categoryName,
                locationId = dto.locationId,
                locationName = dto.locationName,
                status = dto.status,
                priority = dto.priority,
                reportedBy = dto.reportedBy,
                reporterName = dto.reporterName,
                assignedTo = dto.assignedTo,
                assigneeName = dto.assigneeName,
                resolutionNotes = dto.resolutionNotes,
                commentsCount = dto.commentsCount,
                imagesCount = dto.imagesCount,
                createdAt = dto.createdAt,
                updatedAt = dto.updatedAt,
                syncStatus = syncStatus
            )
        }
    }
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String? = null
) {
    fun toDto(): CategoryDto = CategoryDto(id, name, description)

    companion object {
        fun fromDto(dto: CategoryDto): CategoryEntity = CategoryEntity(dto.id, dto.name, dto.description)
    }
}

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: Int,
    val building: String,
    val room: String? = null,
    val description: String? = null
) {
    fun toDto(): LocationDto = LocationDto(id, building, room, description)

    companion object {
        fun fromDto(dto: LocationDto): LocationEntity = LocationEntity(dto.id, dto.building, dto.room, dto.description)
    }
}
