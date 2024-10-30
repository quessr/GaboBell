package yiwoo.prototype.gabobell.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Environment.DIRECTORY_MOVIES
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import yiwoo.prototype.gabobell.constants.MediaFormatConstants
import yiwoo.prototype.gabobell.databinding.ActivityMediaCaptureBinding
import yiwoo.prototype.gabobell.helper.UserSettingsManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.logging.Logger

class MediaCaptureActivity :
    BaseActivity<ActivityMediaCaptureBinding>(ActivityMediaCaptureBinding::inflate) {

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var captureFormat: UserSettingsManager.EmergencyFormatType =
        UserSettingsManager.EmergencyFormatType.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (allPermissionsGranted()) {
            captureFormat = UserSettingsManager.getEmergencyFormat(this)
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
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

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, videoCapture
                )

                val mediaFormat = intent.getIntExtra("mediaFormat", 0)
                val mediaEventId = intent.getLongExtra("eventId", 0)

                Log.d("MediaCaptureActivity", "mediaFormat: $mediaFormat")
                Log.d("MediaCaptureActivity", "mediaEventId: $mediaEventId")

                // 촬영 형식에 따라 호출
                when (mediaFormat) {
                    UserSettingsManager.EmergencyFormatType.PHOTO.value -> takePhoto()
                    UserSettingsManager.EmergencyFormatType.VIDEO.value -> captureVideo()
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

                    finish()
                }
            }
        )
    }

    private fun captureVideo() {
        val videoCapture = videoCapture ?: return

        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
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

                        object : CountDownTimer(10_000, 1_000) {
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

                            // TODO ResultActivity에 결과 넘어가는지 확인
                            finish()
                        } else {
                            Log.e("CameraXApp", "Video capture failed: ${recordEvent.error}")
                        }
                    }
                }
            }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}
