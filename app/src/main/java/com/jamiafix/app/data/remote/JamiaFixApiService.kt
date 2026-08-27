package com.jamiafix.app.data.remote

import com.jamiafix.app.data.model.CategoryCreateRequest
import com.jamiafix.app.data.model.CategoryDto
import com.jamiafix.app.data.model.CommentCreateRequest
import com.jamiafix.app.data.model.CommentDto
import com.jamiafix.app.data.model.ImageAttachmentDto
import com.jamiafix.app.data.model.IssueAssignRequest
import com.jamiafix.app.data.model.IssueCreateRequest
import com.jamiafix.app.data.model.IssueDetailDto
import com.jamiafix.app.data.model.IssueDto
import com.jamiafix.app.data.model.IssueStatusUpdateRequest
import com.jamiafix.app.data.model.LocationCreateRequest
import com.jamiafix.app.data.model.LocationDto
import com.jamiafix.app.data.model.LoginRequest
import com.jamiafix.app.data.model.RegisterRequest
import com.jamiafix.app.data.model.TokenResponse
import com.jamiafix.app.data.model.UserDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface JamiaFixApiService {

    // --- Auth Endpoints ---
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserDto>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<UserDto>

    @GET("auth/staff")
    suspend fun getStaffList(): Response<List<UserDto>>

    // --- Metadata Endpoints ---
    @GET("categories")
    suspend fun getCategories(): Response<List<CategoryDto>>

    @POST("categories")
    suspend fun createCategory(@Body request: CategoryCreateRequest): Response<CategoryDto>

    @GET("locations")
    suspend fun getLocations(): Response<List<LocationDto>>

    @POST("locations")
    suspend fun createLocation(@Body request: LocationCreateRequest): Response<LocationDto>

    // --- Issues Endpoints ---
    @POST("issues")
    suspend fun createIssue(@Body request: IssueCreateRequest): Response<IssueDto>

    @GET("issues")
    suspend fun getIssues(
        @Query("status") status: String? = null,
        @Query("category_id") categoryId: Int? = null,
        @Query("location_id") locationId: Int? = null,
        @Query("reported_by_me") reportedByMe: Boolean? = null,
        @Query("assigned_to_me") assignedToMe: Boolean? = null,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<IssueDto>>

    @GET("issues/{id}")
    suspend fun getIssueDetail(@Path("id") id: Int): Response<IssueDetailDto>

    @PATCH("issues/{id}/status")
    suspend fun updateIssueStatus(
        @Path("id") id: Int,
        @Body request: IssueStatusUpdateRequest
    ): Response<IssueDetailDto>

    @PATCH("issues/{id}/assign")
    suspend fun assignStaff(
        @Path("id") id: Int,
        @Body request: IssueAssignRequest
    ): Response<IssueDetailDto>

    @POST("issues/{id}/comments")
    suspend fun addComment(
        @Path("id") id: Int,
        @Body request: CommentCreateRequest
    ): Response<CommentDto>

    @Multipart
    @POST("issues/{id}/images")
    suspend fun uploadImage(
        @Path("id") id: Int,
        @Part file: MultipartBody.Part
    ): Response<ImageAttachmentDto>
}
