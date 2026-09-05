package com.fazzdev.ps4pkgsender.data.model

import com.google.gson.annotations.SerializedName

data class RpiInstallRequest(
    @SerializedName("type") val type: String = "direct",
    @SerializedName("packages") val packages: List<String>
)

data class RpiInstallResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("task_id") val taskId: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("error") val error: String? = null
)

data class RpiProgressResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("length") val length: Long? = null,
    @SerializedName("transferred") val transferred: Long? = null,
    @SerializedName("length_total") val lengthTotal: Long? = null,
    @SerializedName("transferred_total") val transferredTotal: Long? = null,
    @SerializedName("num_index") val numIndex: Int? = null,
    @SerializedName("num_total") val numTotal: Int? = null,
    @SerializedName("error") val error: Long? = null
)

data class RpiIsExistsResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("exists") val exists: String? = null,
    @SerializedName("size") val size: Long? = null
)

data class RpiIsExistsRequest(
    @SerializedName("title_id") val titleId: String
)

data class RpiTaskActionRequest(
    @SerializedName("task_id") val taskId: Int
)

data class RpiTaskActionResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("error") val error: String? = null
)

data class RpiUninstallRequest(
    @SerializedName("title_id") val titleId: String
)

data class RpiUninstallResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("error") val error: String? = null
)
