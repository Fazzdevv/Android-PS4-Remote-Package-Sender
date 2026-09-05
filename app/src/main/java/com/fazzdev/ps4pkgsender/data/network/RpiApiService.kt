package com.fazzdev.ps4pkgsender.data.network

import com.fazzdev.ps4pkgsender.data.model.RpiInstallRequest
import com.fazzdev.ps4pkgsender.data.model.RpiInstallResponse
import com.fazzdev.ps4pkgsender.data.model.RpiIsExistsRequest
import com.fazzdev.ps4pkgsender.data.model.RpiIsExistsResponse
import com.fazzdev.ps4pkgsender.data.model.RpiProgressResponse
import com.fazzdev.ps4pkgsender.data.model.RpiTaskActionRequest
import com.fazzdev.ps4pkgsender.data.model.RpiTaskActionResponse
import com.fazzdev.ps4pkgsender.data.model.RpiUninstallRequest
import com.fazzdev.ps4pkgsender.data.model.RpiUninstallResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RpiApiService {

    @POST("/api/install")
    suspend fun installPackages(
        @Body request: RpiInstallRequest
    ): Response<RpiInstallResponse>

    @POST("/api/get_task_progress")
    suspend fun getTaskProgress(
        @Body request: RpiTaskActionRequest
    ): Response<RpiProgressResponse>

    @GET("/api/get_task_progress")
    suspend fun getTaskProgressGet(
        @Query("task_id") taskId: Int
    ): Response<RpiProgressResponse>

    @POST("/api/is_exists")
    suspend fun checkIsExists(
        @Body request: RpiIsExistsRequest
    ): Response<RpiIsExistsResponse>

    @GET("/api/is_exists")
    suspend fun checkIsExistsGet(
        @Query("title_id") titleId: String
    ): Response<RpiIsExistsResponse>

    @POST("/api/pause_task")
    suspend fun pauseTask(
        @Body request: RpiTaskActionRequest
    ): Response<RpiTaskActionResponse>

    @POST("/api/resume_task")
    suspend fun resumeTask(
        @Body request: RpiTaskActionRequest
    ): Response<RpiTaskActionResponse>

    @POST("/api/stop_task")
    suspend fun stopTask(
        @Body request: RpiTaskActionRequest
    ): Response<RpiTaskActionResponse>

    @POST("/api/uninstall_game")
    suspend fun uninstallGame(
        @Body request: RpiUninstallRequest
    ): Response<RpiUninstallResponse>

    @POST("/api/uninstall_patch")
    suspend fun uninstallPatch(
        @Body request: RpiUninstallRequest
    ): Response<RpiUninstallResponse>
}
