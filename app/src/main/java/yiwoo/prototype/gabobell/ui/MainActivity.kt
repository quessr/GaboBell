package yiwoo.prototype.gabobell.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import yiwoo.prototype.gabobell.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {
    private var bluetoothAdapter: BluetoothAdapter? = null

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value } // 모든 권한이 승인되었는지 확인
        if (allGranted) {
            Log.d("BLE!@!@", "모든 권한이 허용됨")
        } else {
            Log.d("BLE!@!@", "일부 권한이 거부됨")
            finish() // 권한 거부 시 앱 종료 (임시 조치)
        }
    }

    //블루투스 활성화 요청 콜백
    private var requestBluetooth = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK){
            //granted
            Log.d("BLE!@!@", "Bluetooth가 활성화되었습니다")
            val intent = Intent(this, RegisterDeviceActivity::class.java)
            startActivity(intent)
        } else {
            //deny
            Log.d("BLE!@!@", "Bluetooth 활성화 거부")
            finish()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()

        binding.btnEmergencyReport.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            startActivity(intent)
        }

        binding.btnCheckReportDetails.setOnClickListener {
            val intent = Intent(this, ReportDetailActivity::class.java)
            startActivity(intent)
        }

        binding.btnDeviceRegistration.setOnClickListener {
            //BLE 어뎁터 설정
            enableBleAdapter()
        }

        binding.btnDeviceSettings.setOnClickListener {
            val intent = Intent(this, DeviceSettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun enableBleAdapter() {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        Log.d("BLE!@!@", "bluetoothAdapter_info : $bluetoothAdapter")
        Log.d("BLE!@!@", "bluetoothAdapter_boolean : ${bluetoothAdapter?.isEnabled}")

        // 블루투스 활성화 상태 체크
        if (bluetoothAdapter?.isEnabled == false) {
            // 활성화가 안 되어 있을 경우
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)

        } else { // 활성화가 되어 있을 경우
            val intent = Intent(this, RegisterDeviceActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {   //android 12 이상
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {    //android 11 이하
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }
}