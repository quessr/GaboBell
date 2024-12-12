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
import yiwoo.prototype.gabobell.R
import yiwoo.prototype.gabobell.ble.BleManager
import yiwoo.prototype.gabobell.databinding.ActivityRegisterDeviceBinding
import yiwoo.prototype.gabobell.helper.Logger
import yiwoo.prototype.gabobell.helper.UserDeviceManager
import yiwoo.prototype.gabobell.ui.popup.CustomPopup

class RegisterDeviceActivity :
    BaseActivity<ActivityRegisterDeviceBinding>(ActivityRegisterDeviceBinding::inflate) {

    private var bleManager: BleManager? = null
    private var deviceAddress: String? = null
    private var deviceName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initUi()
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter(), RECEIVER_EXPORTED)
            registerReceiver(bleScanReceiver, bleScanIntentFilter(), RECEIVER_EXPORTED)
        } else {
            registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
            registerReceiver(bleScanReceiver, bleScanIntentFilter())
        }
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
        binding.btnScanCancel.setOnClickListener {
            stopScan()
        }

        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    private fun upDateUI() {
        binding.btnScan.isVisible = true
        binding.btnScanCancel.isVisible = false
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
                BleManager.BLE_SCAN_NOT_FOUND -> {
                    Logger.d("bleScanReceiver_BLE_SCAN_NOT_FOUND")
                    CustomPopup.Builder(this@RegisterDeviceActivity)
                        .setTitle(getString(R.string.pop_emergency_completed_title))
                        .setMessage(getString(R.string.register_device_not_found))
                        .setOnOkClickListener(getString(R.string.pop_btn_yes)) {
                            upDateUI()
                        }
                        .setOnCancelClickListener(getString(R.string.pop_btn_no)) {
                            finish()
                        }
                        .build()
                        .show()
                }
            }
        }
    }

    private fun onDeviceFounded(result: ScanResult?) {
        result?.device?.let { device ->
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Logger.d("bleScanReceiver_PERMISSION_GRANTED")
            }
            Logger.d("bleScanReceiver_onDeviceFounded")

            CustomPopup.Builder(this)
                .setTitle(getString(R.string.pop_emergency_completed_title))
                .setDeviceId(device.name)
                .setDeviceMessage(getString(R.string.pop_register_message))
                .setOnOkClickListener(getString(R.string.register_device_connection)) {
                    startConnect()
                }
                .build()
                .show()

            deviceAddress = device.address
            deviceName = device.name
            Logger.d("bleScanReceiver_result : $result")
        }
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
//                    bleManager?.sayHello()
                    // 연결된 디바이스 정보 저장
                    UserDeviceManager.registerDevice(applicationContext, deviceName!!, deviceAddress!!)
                    finish()
                }
            }
        }
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(BleManager.ACTION_GATT_CONNECTED)
            addAction(BleManager.ACTION_GATT_DISCONNECTED)
            addAction(BleManager.ACTION_GATT_SERVICES_DISCOVERED)
            addAction(BleManager.ACTION_DATA_AVAILABLE)
        }
    }

    private fun bleScanIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(BleManager.BLE_SCAN_RESULT)
            addAction(BleManager.BLE_SCAN_NOT_FOUND)
        }
    }
    // endregion

    // region * Bind service
    /**
     * BleManager 가 초기화 되는 시점은 startForeground 가 될때의 시점
     * 스캔을 시작하면서 초기화를 진행하므로 bind 함수를 사용하여 serviceConnection 콜백처리 진행
     */
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
//        // 서비스 생성 및 바인딩
//        // TODO: 공통 - 서비스 지속 확인 (현재 엑티비티 종료 시에도 서비스가 유지되고 있는지 가시적으로 확인 할 것.)
//        val intent = Intent(this, BleManager::class.java)
//        startService(intent)
//        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        if (bleManager == null) {
            val intent = Intent(this, BleManager::class.java)
            startService(intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            // 이미 서비스가 바인딩되어 있으면 바로 스캔 시작
            bleManager?.startBleScan()
        }

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