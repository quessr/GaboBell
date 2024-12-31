package yiwoo.prototype.gabobell.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment.DIRECTORY_MOVIES
import android.os.Environment.DIRECTORY_PICTURES
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import yiwoo.prototype.gabobell.data.network.FileUploadClient
import yiwoo.prototype.gabobell.databinding.ActivityMediaCaptureBinding
import yiwoo.prototype.gabobell.helper.UserSettingsManager
import yiwoo.prototype.gabobell.workers.UploadWorker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class MediaCaptureActivity :
    BaseActivity<ActivityMediaCaptureBinding>(ActivityMediaCaptureBinding::inflate) {

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var captureFormat: UserSettingsManager.EmergencyFormatType =
        UserSettingsManager.EmergencyFormatType.NONE

    private var mediaFormat: Int = 0
    private var mediaEventId: Long = 0

    private val fileUploadClient = FileUploadClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        captureFormat = UserSettingsManager.getEmergencyFormat(this)
        startCamera()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // no code (뒤로가기 방지)
            }
        })
    }

    private fun startCamera() {
        Log.d("MediaCaptureActivity", "startCamera")

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                mediaFormat = intent.getIntExtra("mediaFormat", 0)
                mediaEventId = intent.getLongExtra("eventId", 0)

                Log.d("MediaCaptureActivity", "mediaFormat: $mediaFormat")
                Log.d("MediaCaptureActivity", "mediaEventId: $mediaEventId")

                // 촬영 형식에 따라 호출
                when (mediaFormat) {
                    UserSettingsManager.EmergencyFormatType.PHOTO.value -> {
                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .build()

                        cameraProvider.bindToLifecycle(
                            this, cameraSelector, preview, imageCapture
                        )

                        takePhoto()
                    }

                    UserSettingsManager.EmergencyFormatType.VIDEO.value -> {
                        val recorder = Recorder.Builder()
                            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                            .build()
                        videoCapture = VideoCapture.withOutput(recorder)

                        cameraProvider.bindToLifecycle(
                            this, cameraSelector, preview, videoCapture
                        )

                        captureVideo()
                    }

                    else -> Log.d("MediaCaptureActivity", "No valid capture format")
                }
            } catch (exc: Exception) {
                Log.e("CameraXApp", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {

        Log.d("MediaCaptureActivity", "takePhoto")

        val photoFile = File(
            getExternalFilesDir(DIRECTORY_PICTURES),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.KOREA)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraXApp", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${photoFile.absolutePath}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d("CameraXApp", msg)

                    CoroutineScope(Dispatchers.IO).launch {
                        uploadFiles(
                            eventId = mediaEventId,
                            imageFiles = listOf(photoFile),
                            videoFile = null
                        )
                    }
                }
            }
        )
    }

    private fun captureVideo() {
        val videoCapture = videoCapture ?: return

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.KOREA)
            .format(System.currentTimeMillis())
        val videoFile = File(getExternalFilesDir(DIRECTORY_MOVIES), "$name.mp4")
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        if (recording != null) {
            recording?.stop()
            recording = null
            return
        }

        recording = videoCapture.output
            .prepareRecording(this, outputOptions)
            .apply {
                if (ActivityCompat.checkSelfPermission(
                        this@MediaCaptureActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        Toast.makeText(
                            this@MediaCaptureActivity,
                            "Recording started",
                            Toast.LENGTH_SHORT
                        ).show()

                        object : CountDownTimer(5_000, 1_000) {
                            override fun onTick(millisUntilFinished: Long) {
                            }

                            override fun onFinish() {
                                recording?.stop()
                                recording = null
                            }
                        }.start()
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: ${videoFile.absolutePath}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                            Log.d("CameraXApp", msg)
                            Log.d(
                                "MediaCaptureActivity videoFileSize",
                                "${videoFile.length() / (1024 * 1024)} MB"
                            )

                            val inputData = Data.Builder()
                                .putLong("eventId", mediaEventId)
                                .putString("videoFilePath", videoFile.absolutePath)
                                .build()

                            val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadWorker>()
                                .setInputData(inputData)
                                .build()

                            WorkManager.getInstance(this).enqueue(uploadWorkRequest)

//                            // WorkManager 동작 확인용 코드
//                            WorkManager.getInstance(this)
//                                .getWorkInfoByIdLiveData(uploadWorkRequest.id)
//                                .observeForever { workInfo ->
//                                    workInfo?.let {
//                                        Log.d("WorkManagerTest", "현재 상태: ${it.state}")
//                                    }
//                                }
//
//                            // fileSize 확인용 코드
//                            val fileSizeMB = videoFile.length() / (1024 * 1024) // MB 단위로 변환
//                            Log.d("CameraXApp", "Video file size: ${fileSizeMB} MB")


                            if (!isFinishing) finish()

                        } else {
                            Log.e("CameraXApp", "Video capture failed: ${recordEvent.error}")
                        }
                    }
                }
            }
    }

    // TODO 추후 takePhoto시 uploadFiles로직 UploadWorker로 옮기기
    private suspend fun uploadFiles(eventId: Long, videoFile: File?, imageFiles: List<File>?) {
        fileUploadClient.uploadFiles(
            context = this,
            eventId = eventId,
            videoFile = videoFile,
            imageFiles = imageFiles,
            onSuccess = {
                yiwoo.prototype.gabobell.helper.Logger.d("파일 업로드 성공 $eventId")
                val intent = Intent().apply {
                    putExtra("onSuccess", "파일 업로드 성공 $eventId")
                }
                setResult(RESULT_OK, intent)
                if (!isFinishing) finish()
            },
            onFailure = { errorMessage ->
                yiwoo.prototype.gabobell.helper.Logger.d("파일 업로드 실패: $errorMessage, $eventId")
                val intent = Intent().apply {
                    putExtra("onFailure", "파일 업로드 실패 $errorMessage")
                }
                setResult(RESULT_CANCELED, intent)
                if (!isFinishing) finish()
            })
    }
}
