package yiwoo.prototype.gabobell.helper

import android.content.Context
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FlashUtil private constructor(private val context: Context) {
    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    private var flashJob: Job? = null

    fun startEmergencySignal(scope: CoroutineScope) {
        flashJob?.cancel()
        flashJob = scope.launch {
            while (isActive) {
                toggleFlash(true)
                delay(300)
                toggleFlash(false)
                delay(300)
            }
        }
    }

    fun stopEmergencySignal() {
        flashJob?.cancel()
        toggleFlash(false)
    }

    private fun toggleFlash(isOn: Boolean) {
        try {
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, isOn)
        } catch (e: Exception) {
        }
    }

    companion object {
        @Volatile
        private var instance: FlashUtil? = null

        fun getInstance(context: Context): FlashUtil {
            return instance ?: synchronized(this) {
                instance ?: FlashUtil(context).also { instance = it }
            }
        }
    }
}