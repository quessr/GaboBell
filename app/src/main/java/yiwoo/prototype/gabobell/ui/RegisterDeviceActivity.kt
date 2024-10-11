package yiwoo.prototype.gabobell.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import yiwoo.prototype.gabobell.ble.BleInterface
import yiwoo.prototype.gabobell.ble.BleManager
import yiwoo.prototype.gabobell.databinding.ActivityRegisterDeviceBinding
import yiwoo.prototype.gabobell.model.DeviceData
import yiwoo.prototype.gabobell.ui.BaseActivity

class RegisterDeviceActivity :
    BaseActivity<ActivityRegisterDeviceBinding>(ActivityRegisterDeviceBinding::inflate),
    BleInterface {

    private var permissionArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private lateinit var bleManager: BleManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleManager = BleManager(applicationContext)

        bleManager.setOnDeviceFoundListener(this)

        binding.btnScan.setOnClickListener {
            checkAndRequestPermissions()

            if (arePermissionsGranted()) {
                startScan()
            } else {
                Toast.makeText(this, "BLE 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                checkAndRequestPermissions()  // 권한이 없으면 다시 요청
            }
        }

        binding.btnConnection.setOnClickListener {
            if (arePermissionsGranted()) {
                startConnect()
            } else {
                Toast.makeText(this, "BLE 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                checkAndRequestPermissions()  // 권한이 없으면 다시 요청
            }
        }

        binding.btnScanCancel.setOnClickListener {
            stopScan()
            binding.tvLoading.isVisible = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (permissionArray.all {
                    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
                }) {
                Toast.makeText(this, "권한 확인", Toast.LENGTH_SHORT).show()
            } else {
                requestPermissionLauncher.launch(permissionArray)
            }
        }
    }

    // 권한을 체크하는 함수
    private fun arePermissionsGranted(): Boolean {
        return permissionArray.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 권한 체크 후, 필요한 경우 요청하는 함수
    private fun checkAndRequestPermissions() {
        if (!arePermissionsGranted()) {
            requestPermissionLauncher.launch(permissionArray)
        }
    }

    private fun startScan() {
        binding.tvLoading.isVisible = true
        binding.btnScan.isVisible = false
        binding.btnScanCancel.isVisible = true
        bleManager.startBleScan()
        Toast.makeText(this, "Scanning started", Toast.LENGTH_SHORT).show()
    }

    private fun stopScan() {
        binding.btnScan.isVisible = true
        binding.btnScanCancel.isVisible = false
        bleManager.stopBleScan()
    }

    private fun startConnect() {
        val deviceData = DeviceData("DeviceName", "UUID", "Address")
        bleManager.startBleConnectGatt(deviceData)
        Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            Log.d("DEBUG", "${it.key} = ${it.value}")
            if (!it.value) {
                Toast.makeText(this, "${it.key} 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        if (arePermissionsGranted()) {
            Toast.makeText(this, "모든 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "권한이 부족하여 BLE 기능을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectedStateObserve(isConnected: Boolean, data: String) {
        TODO("Not yet implemented")
    }

    override fun onDeviceFound(deviceName: String) {
        binding.tvLoading.isVisible = false
        binding.clDeviceItem.isVisible = true
        binding.tvDeviceName.text = deviceName
    }
}