package com.jamiafix.app.data.repository

import com.jamiafix.app.data.local.dao.CategoryDao
import com.jamiafix.app.data.local.dao.LocationDao
import com.jamiafix.app.data.local.entity.CategoryEntity
import com.jamiafix.app.data.local.entity.LocationEntity
import com.jamiafix.app.data.model.CategoryCreateRequest
import com.jamiafix.app.data.model.CategoryDto
import com.jamiafix.app.data.model.LocationCreateRequest
import com.jamiafix.app.data.model.LocationDto
import com.jamiafix.app.data.remote.JamiaFixApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MetadataRepository(
    private val apiService: JamiaFixApiService,
    private val categoryDao: CategoryDao,
    private val locationDao: LocationDao
) {
    val categoriesFlow: Flow<List<CategoryDto>> = categoryDao.getAllCategoriesFlow().map { list ->
        list.map { it.toDto() }
    }

    val locationsFlow: Flow<List<LocationDto>> = locationDao.getAllLocationsFlow().map { list ->
        list.map { it.toDto() }
    }

    suspend fun refreshMetadata(): Result<Unit> {
        return try {
            val catRes = apiService.getCategories()
            if (catRes.isSuccessful && catRes.body() != null) {
                categoryDao.insertCategories(catRes.body()!!.map { CategoryEntity.fromDto(it) })
            }

            val locRes = apiService.getLocations()
            if (locRes.isSuccessful && locRes.body() != null) {
                locationDao.insertLocations(locRes.body()!!.map { LocationEntity.fromDto(it) })
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCategory(name: String, description: String?): Result<CategoryDto> {
        return try {
            val response = apiService.createCategory(CategoryCreateRequest(name.trim(), description?.trim()))
            if (response.isSuccessful && response.body() != null) {
                val cat = response.body()!!
                categoryDao.insertCategories(listOf(CategoryEntity.fromDto(cat)))
                Result.success(cat)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to create category"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createLocation(building: String, room: String?, description: String?): Result<LocationDto> {
        return try {
            val response = apiService.createLocation(LocationCreateRequest(building.trim(), room?.trim(), description?.trim()))
            if (response.isSuccessful && response.body() != null) {
                val loc = response.body()!!
                locationDao.insertLocations(listOf(LocationEntity.fromDto(loc)))
                Result.success(loc)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to create location"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
