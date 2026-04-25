package com.example.neonanime

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class ProcessResponse(
    val status: String?,
    val image_base64: String?,
    val caption: String?,
    val error: String?
)

interface NeonAnimeApi {
    @Multipart
    @POST("process-media")
    suspend fun processMedia(
        @Part file: MultipartBody.Part
    ): ProcessResponse
}
