package yiwoo.prototype.gabobell.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yiwoo.prototype.gabobell.data.network.FileUploadClient
import java.io.File

class UploadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    private val fileUploadClient = FileUploadClient()
    override suspend fun doWork(): Result {
        Log.d("UploadWorker", "작업 시작")

        val eventId = inputData.getLong("eventId", -1L)
        val videoFile = inputData.getString("videoFilePath")?.let { File(it) }

        if (eventId == -1L) {
            Log.d("UploadWorker", "유효하지 않은 입력 데이터")
            return Result.failure()
        }

        return try {
            uploadFiles(
                eventId = eventId,
                imageFiles = null,
                videoFile = videoFile
            )
            Log.d("UploadWorker", "작업 성공")
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("UploadWorker", "작업 실패: ${e.message}", e)
            Result.failure()
        }
    }

    private suspend fun uploadFiles(eventId: Long, videoFile: File?, imageFiles: List<File>?) {
        withContext(Dispatchers.IO) {
            fileUploadClient.uploadFiles(
                context = applicationContext,
                eventId = eventId,
                videoFile = videoFile,
                imageFiles = imageFiles,
                onSuccess = {
                    yiwoo.prototype.gabobell.helper.Logger.d("파일 업로드 성공 $eventId")
                },
                onFailure = { errorMessage ->
                    throw Exception("파일 업로드 실패: $errorMessage, $eventId")
                }
            )
        }
    }
}