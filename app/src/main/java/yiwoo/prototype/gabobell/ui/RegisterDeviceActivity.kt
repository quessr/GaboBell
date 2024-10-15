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
    BaseActivity<ActivityRegisterDeviceBinding>(ActivityRegisterDeviceBinding::inflate) {


    private var bleManager: BleManager? = null
    private var isReceiverRegistered = false

    private var deviceAddress: String? = ""

    //bind서비스가 연결되어있을 경우 안되어있을경우
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bleManager = (service as BleManager.LocalBinder).getService()
            bleManager?.let { bluetooth ->
                //연결을 확인하고 장치에 연결하기 위해 서비스에서 기능을 호출
                if (!bluetooth.initialize()) {
                    Log.e("BLE!@!@", "Unable to initialize Bluetooth")
                    finish()
                } else {
//                    bluetooth.connect(deviceAddress)
                    Log.d("BLE!@!@", "onServiceConnected")
                    bluetooth.startBleScan()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bleManager = null
            Log.d("BLE!@!@", "onServiceDisconnected")
        }
    }

    //ble_scanReceiver
    private val bleScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BleManager.BLE_SCAN_RESULT -> {
                    //test용
                    val deviceName = intent.getStringExtra("device_name")
                     deviceAddress = intent.getStringExtra("device_address")
                    Log.d("BLE!@!@", "bleScanReceiver_deviceName : $deviceName")
                    Log.d("BLE!@!@", "bleScanReceiver_deviceAddress : $deviceAddress")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val result = intent.getParcelableExtra("result", ScanResult::class.java)
//                        leDeviceListAdapter.addDevice(result)
//                        leDeviceListAdapter.notifyDataSetChanged()
                        if (ContextCompat.checkSelfPermission(
                                applicationContext,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            Log.d("BLE!@!@", "bleScanReceiver_PERMISSION_GRANTED")
                            onDeviceFound(result?.device!!.name)

                            //기기주소 저장
                            deviceAddress = result.device.address
                        }
                        Log.d("BLE!@!@", "bleScanReceiver_result : $result")
                    } else {
                        val result: ScanResult? = intent.getParcelableExtra("result")
//                        leDeviceListAdapter.addDevice(result)
//                        leDeviceListAdapter.notifyDataSetChanged()
                        if (ContextCompat.checkSelfPermission(
                                applicationContext,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            Log.d("BLE!@!@", "bleScanReceiver_PERMISSION_GRANTED")
                            onDeviceFound(result?.device!!.name)

                            //기기주소 저장
                            deviceAddress = result.device.address
                        }
                        Log.d("BLE!@!@", "bleScanReceiver_result : $result")
                    }
                }
            }
        }
    }

    /**
     * BroadcastReceiver는 BluetoothLeService 로 부터 연결 상태와 데이터들을 받아오는 역할
     * 등록 후에 BluetoothService 에 정의되어 있는 connect 함수를 호출해 장치와 연결
     * 여기서 connect 함수는 BluetoothLeService의 ACTION_GATT_CONNECTED... 등등 변수 선언
     */
    var connected: Boolean = false
    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("BLE!@!@", "gattUpdateReceiver: ${intent?.action.toString()}")
            when (intent?.action) {
                BleManager.ACTION_GATT_CONNECTED -> {   //연결 성공
                    connected = true
                    Log.d("BLE!@!@", "BLE : Connected to device")
                }
                BleManager.ACTION_GATT_DISCONNECTED -> {    //연결 실패
                    connected = false
                    Log.d("BLE!@!@", "BLE : Disconnected to device")
                }
                BleManager.ACTION_GATT_SERVICES_DISCOVERED -> { //gatt service 발견
                    // Show all the supported services and characteristics on the user interface.
                    // BLE 제공되는 서비스(and 특성)들 가져오는 함수
//                    displayGattServices(bluetoothLeService?.getSupportedGattServices())
                    bleManager?.sayHello()
                    Log.d("BLE!@!@", "BLE : GATT_SERVICES_DISCOVERED")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        bleManager = BleManager(applicationContext)

//        bleManager?.setOnDeviceFoundListener(this)

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

    override fun onResume() {
        super.onResume()
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        registerReceiver(bleScanReceiver, bleScanIntentFilter())
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

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)

        if (isReceiverRegistered) {
            unregisterReceiver(bleScanReceiver)
            isReceiverRegistered = false
        }
    }

    private fun startScan() {
        binding.tvLoading.isVisible = true
        binding.btnScan.isVisible = false
        binding.btnScanCancel.isVisible = true
//        bleManager.startBleScan()

        // BLE 서비스를 시작 (startService 호출)
        val intent = Intent(this, BleManager::class.java)
//        startService(intent)  // 서비스 시작
        // 서비스와 바인딩하여 상호작용할 수 있도록 설정
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        Toast.makeText(this, "Scanning started", Toast.LENGTH_SHORT).show()
    }

    private fun stopScan() {
        binding.btnScan.isVisible = true
        binding.btnScanCancel.isVisible = false
        bleManager?.stopBleScan()
    }

    private fun startConnect() {
//        val deviceData = DeviceData("DeviceName", "UUID", "Address")
//        bleManager?.startBleConnectGatt(deviceData)

        bleManager?.connect(deviceAddress)
        Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show()
    }

    fun onDeviceFound(deviceName: String) {
        binding.tvLoading.isVisible = false
        binding.clDeviceItem.isVisible = true
        Log.d("BLE!@!@", "onDeviceFound : $deviceName")
        binding.tvDeviceName.text = deviceName
    }
}