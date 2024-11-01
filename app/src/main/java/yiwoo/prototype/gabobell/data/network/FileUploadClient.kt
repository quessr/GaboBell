package yiwoo.prototype.gabobell.data.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import yiwoo.prototype.gabobell.api.GaboAPI
import yiwoo.prototype.gabobell.helper.Logger
import yiwoo.prototype.gabobell.module.RetrofitModule
import java.io.File

class FileUploadClient {
    suspend fun uploadFiles(
        context: Context,
        eventId: Long,
        videoFile: File?,
        imageFiles: List<File>?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val retrofit: Retrofit = RetrofitModule.provideRetrofit(context)
        val gaboApi = retrofit.create(GaboAPI::class.java)

        val eventIdBody = eventId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        val partVideo = videoFile?.let {
            MultipartBody.Part.createFormData(
                "videoFile",
                videoFile.name,
                videoFile.asRequestBody("video/mp4".toMediaTypeOrNull())
            )
        }

        val partImages = imageFiles?.map { imageFile ->
            MultipartBody.Part.createFormData(
                "imageFiles",
                imageFile.name,
                imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )
        }

        val emptyImage = MultipartBody.Part.createFormData(
            "imageFiles",
            "",
            "".toRequestBody()
        )

        try {
            val response = withContext(Dispatchers.IO) {
                when {
                    partVideo != null -> gaboApi.uploadFiles(
                        eventIdBody,
                        partVideo,
                        listOf(emptyImage)
                    )

                    partImages != null -> gaboApi.uploadFiles(eventIdBody, null, partImages)
                    else -> throw IllegalArgumentException("No files provided for upload")
                }
            }
            Logger.d("response: ${response.body()}")
            if (response.isSuccessful) {
                onSuccess()
            } else {
                onFailure("Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            onFailure("Network Error: ${e.localizedMessage}")
        }
    }
}