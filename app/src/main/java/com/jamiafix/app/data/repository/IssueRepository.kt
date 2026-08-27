package com.jamiafix.app.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jamiafix.app.data.local.dao.IssueDao
import com.jamiafix.app.data.local.entity.IssueEntity
import com.jamiafix.app.data.model.CommentCreateRequest
import com.jamiafix.app.data.model.CommentDto
import com.jamiafix.app.data.model.ImageAttachmentDto
import com.jamiafix.app.data.model.IssueAssignRequest
import com.jamiafix.app.data.model.IssueCreateRequest
import com.jamiafix.app.data.model.IssueDetailDto
import com.jamiafix.app.data.model.IssueDto
import com.jamiafix.app.data.model.IssueStatusUpdateRequest
import com.jamiafix.app.data.remote.JamiaFixApiService
import com.jamiafix.app.data.worker.IssueSyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IssueRepository(
    private val context: Context,
    private val apiService: JamiaFixApiService,
    private val issueDao: IssueDao
) {
    val allIssuesFlow: Flow<List<IssueDto>> = issueDao.getAllIssuesFlow().map { list ->
        list.map { it.toDto() }
    }

    fun getMyReportedIssuesFlow(userId: Int): Flow<List<IssueDto>> {
        return issueDao.getMyReportedIssuesFlow(userId).map { list ->
            list.map { it.toDto() }
        }
    }

    fun getAssignedIssuesFlow(staffId: Int): Flow<List<IssueDto>> {
        return issueDao.getAssignedIssuesFlow(staffId).map { list ->
            list.map { it.toDto() }
        }
    }

    suspend fun refreshIssues(
        status: String? = null,
        categoryId: Int? = null,
        locationId: Int? = null,
        search: String? = null
    ): Result<List<IssueDto>> {
        return try {
            val response = apiService.getIssues(
                status = status,
                categoryId = categoryId,
                locationId = locationId,
                search = search
            )
            if (response.isSuccessful && response.body() != null) {
                val issues = response.body()!!
                val entities = issues.map { IssueEntity.fromDto(it, syncStatus = "SYNCED") }
                issueDao.insertIssues(entities)
                Result.success(issues)
            } else {
                Result.failure(Exception("Failed to fetch issues (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getIssueDetail(issueId: Int): Result<IssueDetailDto> {
        return try {
            val response = apiService.getIssueDetail(issueId)
            if (response.isSuccessful && response.body() != null) {
                val detail = response.body()!!
                // Update local cache
                issueDao.insertIssue(IssueEntity.fromDto(detail.toSummaryDto(), syncStatus = "SYNCED"))
                Result.success(detail)
            } else {
                Result.failure(Exception("Failed to load issue details"))
            }
        } catch (e: Exception) {
            // Fallback to local cache if offline
            val local = issueDao.getIssueById(issueId)
            if (local != null) {
                Result.success(
                    IssueDetailDto(
                        id = local.id,
                        title = local.title,
                        description = local.description,
                        categoryId = local.categoryId,
                        categoryName = local.categoryName,
                        locationId = local.locationId,
                        locationName = local.locationName,
                        status = local.status,
                        priority = local.priority,
                        reportedBy = local.reportedBy,
                        reporterName = local.reporterName,
                        assignedTo = local.assignedTo,
                        assigneeName = local.assigneeName,
                        resolutionNotes = local.resolutionNotes,
                        commentsCount = local.commentsCount,
                        imagesCount = local.imagesCount,
                        createdAt = local.createdAt,
                        updatedAt = local.updatedAt,
                        comments = emptyList(),
                        images = emptyList(),
                        allowedNextStatuses = emptyList()
                    )
                )
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun createIssue(
        title: String,
        description: String,
        categoryId: Int,
        categoryName: String,
        locationId: Int,
        locationName: String,
        priority: String,
        reporterId: Int,
        reporterName: String
    ): Result<IssueDto> {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        return try {
            val response = apiService.createIssue(
                IssueCreateRequest(
                    title = title,
                    description = description,
                    categoryId = categoryId,
                    locationId = locationId,
                    priority = priority
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val created = response.body()!!
                issueDao.insertIssue(IssueEntity.fromDto(created, syncStatus = "SYNCED"))
                Result.success(created)
            } else {
                // Save offline
                savePendingOfflineIssue(
                    title, description, categoryId, categoryName,
                    locationId, locationName, priority, reporterId, reporterName, now
                )
            }
        } catch (e: Exception) {
            // Network error -> Save offline and schedule WorkManager sync
            savePendingOfflineIssue(
                title, description, categoryId, categoryName,
                locationId, locationName, priority, reporterId, reporterName, now
            )
        }
    }

    private suspend fun savePendingOfflineIssue(
        title: String,
        description: String,
        categoryId: Int,
        categoryName: String,
        locationId: Int,
        locationName: String,
        priority: String,
        reporterId: Int,
        reporterName: String,
        timestamp: String
    ): Result<IssueDto> {
        val tempId = -(System.currentTimeMillis().toInt() and 0x7FFFFFFF)
        val pendingEntity = IssueEntity(
            id = tempId,
            title = title,
            description = description,
            categoryId = categoryId,
            categoryName = categoryName,
            locationId = locationId,
            locationName = locationName,
            status = "SUBMITTED",
            priority = priority,
            reportedBy = reporterId,
            reporterName = reporterName,
            createdAt = timestamp,
            updatedAt = timestamp,
            syncStatus = "PENDING_SYNC"
        )

        issueDao.insertIssue(pendingEntity)
        scheduleOfflineSync()

        return Result.success(pendingEntity.toDto())
    }

    fun scheduleOfflineSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<IssueSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
    }

    suspend fun syncPendingIssues(): Int {
        val pendingList = issueDao.getPendingSyncIssues()
        var syncedCount = 0

        for (pending in pendingList) {
            try {
                val res = apiService.createIssue(
                    IssueCreateRequest(
                        title = pending.title,
                        description = pending.description,
                        categoryId = pending.categoryId,
                        locationId = pending.locationId,
                        priority = pending.priority
                    )
                )
                if (res.isSuccessful && res.body() != null) {
                    val serverIssue = res.body()!!
                    // Remove temporary local entry and insert real server entry
                    issueDao.deleteById(pending.id)
                    issueDao.insertIssue(IssueEntity.fromDto(serverIssue, syncStatus = "SYNCED"))
                    syncedCount++
                }
            } catch (_: Exception) {
                // Will retry on next sync
            }
        }
        return syncedCount
    }

    suspend fun updateIssueStatus(issueId: Int, newStatus: String, notes: String? = null): Result<IssueDetailDto> {
        return try {
            val res = apiService.updateIssueStatus(
                issueId,
                IssueStatusUpdateRequest(status = newStatus, resolutionNotes = notes)
            )
            if (res.isSuccessful && res.body() != null) {
                val detail = res.body()!!
                issueDao.insertIssue(IssueEntity.fromDto(detail.toSummaryDto(), syncStatus = "SYNCED"))
                Result.success(detail)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to update status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assignStaff(issueId: Int, staffId: Int): Result<IssueDetailDto> {
        return try {
            val res = apiService.assignStaff(issueId, IssueAssignRequest(staffId = staffId))
            if (res.isSuccessful && res.body() != null) {
                val detail = res.body()!!
                issueDao.insertIssue(IssueEntity.fromDto(detail.toSummaryDto(), syncStatus = "SYNCED"))
                Result.success(detail)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to assign staff"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addComment(issueId: Int, comment: String): Result<CommentDto> {
        return try {
            val res = apiService.addComment(issueId, CommentCreateRequest(comment = comment.trim()))
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Failed to post comment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadImage(issueId: Int, imageFile: File): Result<ImageAttachmentDto> {
        return try {
            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)
            val res = apiService.uploadImage(issueId, body)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Failed to upload image"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun IssueDetailDto.toSummaryDto(): IssueDto {
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
            commentsCount = comments.size,
            imagesCount = images.size,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
