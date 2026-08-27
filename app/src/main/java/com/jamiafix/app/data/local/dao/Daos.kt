package com.jamiafix.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jamiafix.app.data.local.entity.CategoryEntity
import com.jamiafix.app.data.local.entity.IssueEntity
import com.jamiafix.app.data.local.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IssueDao {

    @Query("SELECT * FROM issues ORDER BY createdAt DESC")
    fun getAllIssuesFlow(): Flow<List<IssueEntity>>

    @Query("SELECT * FROM issues WHERE reportedBy = :userId ORDER BY createdAt DESC")
    fun getMyReportedIssuesFlow(userId: Int): Flow<List<IssueEntity>>

    @Query("SELECT * FROM issues WHERE assignedTo = :staffId ORDER BY createdAt DESC")
    fun getAssignedIssuesFlow(staffId: Int): Flow<List<IssueEntity>>

    @Query("SELECT * FROM issues WHERE id = :issueId")
    suspend fun getIssueById(issueId: Int): IssueEntity?

    @Query("SELECT * FROM issues WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncIssues(): List<IssueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssues(issues: List<IssueEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssue(issue: IssueEntity)

    @Update
    suspend fun updateIssue(issue: IssueEntity)

    @Delete
    suspend fun deleteIssue(issue: IssueEntity)

    @Query("DELETE FROM issues WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM issues WHERE syncStatus = 'SYNCED'")
    suspend fun clearSyncedIssues()
}

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun clearCategories()
}

@Dao
interface LocationDao {

    @Query("SELECT * FROM locations ORDER BY building ASC")
    fun getAllLocationsFlow(): Flow<List<LocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationEntity>)

    @Query("DELETE FROM locations")
    suspend fun clearLocations()
}
