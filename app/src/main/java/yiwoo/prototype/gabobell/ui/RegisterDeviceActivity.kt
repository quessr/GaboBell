package yiwoo.prototype.gabobell.ui

import android.Manifest
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import yiwoo.prototype.gabobell.ble.BleManager
import yiwoo.prototype.gabobell.databinding.ActivityRegisterDeviceBinding
import yiwoo.prototype.gabobell.helper.Logger

class RegisterDeviceActivity :
    BaseActivity<ActivityRegisterDeviceBinding>(ActivityRegisterDeviceBinding::inflate) {

    private var bleManager: BleManager? = null
    private var deviceAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUi()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        registerReceiver(bleScanReceiver, bleScanIntentFilter())
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)
        unregisterReceiver(bleScanReceiver)
    }

    // region * UI
    private fun initUi() {
        binding.btnScan.setOnClickListener {
            startScan()
        }
        binding.btnConnection.setOnClickListener {
            startConnect()
        }
        binding.btnScanCancel.setOnClickListener {
            stopScan()
            binding.tvLoading.isVisible = false
        }
    }

    private fun showDevice(deviceName: String) {
        Logger.d("onDeviceFound : $deviceName")
        binding.tvLoading.isVisible = false
        binding.clDeviceItem.isVisible = true
        binding.tvDeviceName.text = deviceName
    }
    // endregion

    // region * BroadcastReceiver
    //ble_scanReceiver
    private val bleScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BleManager.BLE_SCAN_RESULT -> {
                    val result: ScanResult? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra("result", ScanResult::class.java)
                    } else {
                        intent.getParcelableExtra("result")
                    }
                    result?.let { onDeviceFounded(it) }
                }
            }
        }
    }

    private fun onDeviceFounded(result: ScanResult?) {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Logger.d("bleScanReceiver_PERMISSION_GRANTED")
            showDevice(result?.device!!.name)
            deviceAddress = result.device.address
        }
        Logger.d("bleScanReceiver_result : $result")
    }


    /**
     * BroadcastReceiver는 BluetoothLeService 로 부터 연결 상태와 데이터들을 받아오는 역할
     * 등록 후에 BluetoothService 에 정의되어 있는 connect 함수를 호출해 장치와 연결
     * 여기서 connect 함수는 BluetoothLeService의 ACTION_GATT_CONNECTED... 등등 변수 선언
     */
    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BleManager.ACTION_GATT_CONNECTED -> {
                    // GATT 서버 연결 성공
                    Logger.d("ACTION_GATT_CONNECTED")
                }
                BleManager.ACTION_GATT_DISCONNECTED -> {
                    // GATT 서버 연결 해제
                    Logger.d("ACTION_GATT_DISCONNECTED")
                }
                BleManager.ACTION_GATT_SERVICES_DISCOVERED -> {
                    Logger.d("BLE : GATT_SERVICES_DISCOVERED")
                    // 디바이스 연결 후 10초 이내 0xA1을 전송.
                    bleManager?.sayHello()
                }
            }
        }
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        return IntentFilter().apply {
            addAction(BleManager.ACTION_GATT_CONNECTED)
            addAction(BleManager.ACTION_GATT_DISCONNECTED)
            addAction(BleManager.ACTION_GATT_SERVICES_DISCOVERED)
            addAction(BleManager.ACTION_DATA_AVAILABLE)
        }
    }

    private fun bleScanIntentFilter(): IntentFilter? {
        return IntentFilter().apply {
            addAction(BleManager.BLE_SCAN_RESULT)
        }
    }
    // endregion

    // region * Bind service
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bleManager = (service as BleManager.LocalBinder).getService()
            bleManager?.let { service ->
                if (!service.initialize()) {
                    Logger.e("Unable to initialize Bluetooth")
                    finish()
                } else {
                    // 서비스 바인딩 성공 시 디바이스 스캔 시작
                    Logger.d("RegisterDeviceActivity onServiceConnected")
                    service.startBleScan()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bleManager = null
            Logger.d("onServiceDisconnected")
        }
    }
    // endregion

    // region * Scan & Connect
    private fun startScan() {
        // 서비스 생성 및 바인딩
        // TODO: 공통 - 서비스 지속 확인 (현재 엑티비티 종료 시에도 서비스가 유지되고 있는지 가시적으로 확인 할 것.)
        val intent = Intent(this, BleManager::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        binding.tvLoading.isVisible = true
        binding.btnScan.isVisible = false
        binding.btnScanCancel.isVisible = true

        Toast.makeText(this, "Scanning started", Toast.LENGTH_SHORT).show()
    }

    private fun stopScan() {
        binding.btnScan.isVisible = true
        binding.btnScanCancel.isVisible = false
        bleManager?.stopBleScan()
    }

    private fun startConnect() {
        bleManager?.connect(deviceAddress)
        Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show()
    }
    // endregion


}