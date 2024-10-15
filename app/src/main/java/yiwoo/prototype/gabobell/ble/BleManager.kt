package yiwoo.prototype.gabobell.ble

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import yiwoo.prototype.gabobell.model.DeviceData
import java.lang.IllegalArgumentException
import java.util.UUID

class BleManager: Service() {

    private var connectionState = STATE_DISCONNECTED
    var bleGatt: BluetoothGatt? = null

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    private var scanning = false
    private val handler = Handler(Looper.getMainLooper())
    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    fun initialize(): Boolean {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Log.e("BLE!@!@", "Unable to obtain a BluetoothAdapter.")
            return false
        }
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        return true
    }
    companion object{
        const val ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "EXTRA_DATA"

        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2

        const val BLE_SCAN_RESULT = "BLE_SCAN_RESULT"
    }

//    private val bluetoothManager =
//        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//    private val bluetoothAdapter = bluetoothManager.adapter
//
//    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
//    private var scanList: MutableList<DeviceData>? = mutableListOf()
//    private var connectedStateObserver: BleInterface? = null
//    var bleGatt: BluetoothGatt? = null
//    private var foundedDevice: BleInterface? = null

    private val scanCallback: ScanCallback =
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                // 스캔이 중단된 상태라면 추가적인 스캔 결과는 무시
                if (!scanning) return
                Log.d("BLE!@!@", "Scanning...")
                //스캔 결과값 받아올 콜백 메소드
                //어뎁터에 연결하여 디바이스 정보 뿌려주는 로직(우선 리스트에 담아서 로그로 확인작업)
                //result 를 브로드캐스트로 액티비티 전달
                val device = result?.device
                val deviceName = device?.name
                val deviceAddress = device?.address
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(
                            this@BleManager,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        scanning = false
                        bluetoothLeScanner?.stopScan(this)
                        handler.removeCallbacksAndMessages(null)
                        Log.d("BLE!@!@", "Scan stopped")

                        val intent = Intent(BLE_SCAN_RESULT) //action 값
                        intent.putExtra("device_name", deviceName)
                        intent.putExtra("device_address", deviceAddress)
                        intent.putExtra("result", result)
                        sendBroadcast(intent)
                    } else return
                } else {
                    scanning = false
                    bluetoothLeScanner?.stopScan(this)
                    handler.removeCallbacksAndMessages(null)
                    Log.d("BLE!@!@", "Scan stopped")

                    val intent = Intent(BLE_SCAN_RESULT) //action 값
                    intent.putExtra("device_name", deviceName)
                    intent.putExtra("device_address", deviceAddress)
                    intent.putExtra("result", result)
                    sendBroadcast(intent)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                println("onScanFailed  $errorCode")
            }

        }


    /**
     * GATT 콜백 선언
     * 활동이 서비스에 연결할 기기와 서비스를 알려준 후 장치에 연결되면 서비스는 장치의 GATT 서버에 접속해야
     * BLE 기기 이 연결에서는 BluetoothGattCallback이(가) 있어야 연결 상태, 서비스 검색, 특성에 대한 알림 특성 알림을 제공
     */

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            var intentAction = ""
            when(newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    // successfully connected to the GATT Server
                    intentAction = ACTION_GATT_CONNECTED
                    broadcastUpdate(intentAction)
                    connectionState = STATE_CONNECTED
                    Log.d("BLE!@!@", "successfully connected to the GATT Server")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(
                                this@BleManager,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            //BLE 기기에서 정보를 쿼리
                            bleGatt?.discoverServices()
                            Log.d("BLE!@!@", "discoverServices_qurey")
                        } else return
                    } else {
                        //버전 11 이하
                        bleGatt?.discoverServices()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    // disconnected from the GATT Server
                    intentAction = ACTION_GATT_DISCONNECTED
                    broadcastUpdate(intentAction)
                    connectionState = STATE_DISCONNECTED
                    Log.d("BLE!@!@", "disconnected from the GATT Server")
                }
            }

//            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                gatt?.discoverServices()
//                connectedStateObserver?.onConnectedStateObserve(
//                    true,
//                    "onConnectionStateChange: STATE_CONNECTED" + "\n" + "---"
//                )
//            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                connectedStateObserver?.onConnectedStateObserve(
//                    false,
//                    "onConnectionStateChange: STATE_CONNECTED" + "\n" + "---"
//                )
//            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //ble 특성 읽기
                displayGattServices(getSupportedGattServices())
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                Log.d("BLE!@!@", "onServicesDiscovered_GATT_SUCCESS")
            } else {
                Log.d("BLE!@!@", "onServicesDiscovered_GATT_FAIL: $status")
            }

//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                MainScope().launch {
//                    bleGatt = gatt
//                    Toast.makeText(context, " ${gatt?.device?.name} 연결 성공", Toast.LENGTH_SHORT)
//                        .show()
//                    var sendText = "onServicesDiscovered:  GATT_SUCCESS" + "\n" + "↓" + "\n"
//
//                    for (service in gatt?.services!!) {
//                        sendText += "- " + service.uuid.toString() + "\n"
//                        for (characteristics in service.characteristics) {
//                            sendText += "    "
//                        }
//                    }
//                    sendText += "---"
//                    connectedStateObserver?.onConnectedStateObserve(true, sendText)
//                }.cancel()
//            }
        }
    }

    /**
     * 서비스가 검색되면 서비스는 getServices()(으)로 보고된 데이터를 가져옵니다.
     * BLE 장치에서 제공되는 서비스들을 받아올 수 있도록 해주는 메소드
     */
    fun getSupportedGattServices(): List<BluetoothGattService?>? {
        return bleGatt?.services
    }

    /**
     * BLE 특성읽기
     * BluetoothGattService의 리스트를 받아와서 그 서비스와 해당하는 특성 들을 화면에 표시하기 위한 작업 수행
     * 지원되는 GATT를 반복하는 방법을 보여줍니다.
     */
    private fun displayGattServices(gattServices: List<BluetoothGattService?>?) {
        if (gattServices == null) return
        var uuid: String?

        Log.d("BLE!@!@", "displayGattServices")
        //사용 가능한 GATT 서비스를 반복
        gattServices.forEach { gattService ->
            uuid = gattService?.uuid.toString()
            val gattCharacteristics = gattService?.characteristics

            //사용 가능한 특성을 반복
            gattCharacteristics?.forEach { gattCharacteristic ->
                uuid = gattCharacteristic.uuid.toString()
                //tx 특성만 뽑을경우
                if (uuid.equals("6E400002-B5A3-F393-E0A9-E50E24DCCA9E".lowercase())) {
                    writeCharacteristic = gattCharacteristic
                }
            }
        }
    }

    /**
     * 서버가 GATT 서버에 연결하거나 연결을 끊을 때 새로운 상태의 활동 전달
     */
    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        Log.d("BLE!@!@", "broadcastUpdate")
        sendBroadcast(intent)
    }


    fun startBleScan() {
//        scanList?.clear()

        // ScanSettings 설정
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        // ScanFilter 설정
        val scanFilter = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")))
                .build()
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED) {
                if (!scanning) {
                    handler.postDelayed({
                        scanning = false
                        bluetoothLeScanner?.stopScan(scanCallback)
                        Log.d("BLE!@!@", "No_Device_Scan stopped")
                    }, SCAN_PERIOD)
                    scanning = true
                    Log.d("BLE!@!@", "Start_Scan_v12-------->")
                    bluetoothLeScanner?.startScan(scanFilter, scanSettings, scanCallback)
                } else {
                    scanning = false
                    bluetoothLeScanner?.stopScan(scanCallback)
                    Log.d("BLE!@!@", "Scan stopped")
                }
            } else return
        } else {
            if (!scanning) {
                handler.postDelayed({
                    scanning = false
                    bluetoothLeScanner?.stopScan(scanCallback)
                    Log.d("BLE!@!@", "No_Device_Scan stopped")
                }, SCAN_PERIOD)
                scanning = true
                Log.d("BLE!@!@", "Start_Scan")
                bluetoothLeScanner?.startScan(scanCallback)
            } else {
                scanning = false
                bluetoothLeScanner?.stopScan(scanCallback)
                Log.d("BLE!@!@", "Scan stopped")
            }
        }
//        bluetoothLeScanner.startScan(scanFilter, scanSettings, scanCallback)
//        Toast.makeText(context, "Scanning started", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission", "NewApi")
    fun stopBleScan() {
        bluetoothLeScanner?.stopScan(scanCallback)
    }

//    @SuppressLint("MissingPermission")
//    fun startBleConnectGatt(deviceData: DeviceData) {
//        bluetoothAdapter?.getRemoteDevice(deviceData.address)
//            ?.connectGatt(context, false, gattCallback)
//    }

//    fun setScanList(pScanList: MutableList<DeviceData>) {
//        scanList = pScanList
//    }
//
//    fun onConnectedStateObserve(pConnectedStateObserver: BleInterface) {
//        connectedStateObserver = pConnectedStateObserver
//    }

//    fun setOnDeviceFoundListener(listener: BleInterface) {
//        foundedDevice = listener
//    }

    inner class LocalBinder: Binder() {
        fun getService() : BleManager {
            return this@BleManager
        }
    }

    private val binder = LocalBinder()
    override fun onBind(intent: Intent?): IBinder {
        Log.d("BLE!@!@", "onBind")
        return binder
    }

    //블루투스 디바이스 GATT 서버 연결
    fun connect(address: String?): Boolean {
        bluetoothAdapter?.let { adapter ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED) {
                        //권한 설정되어있는 경우 로직
                        val device = adapter.getRemoteDevice(address)
                        // connect to the GATT server on the device
                        bleGatt = device.connectGatt(this, false, gattCallback)
                    } else {
                        Log.d("BLE!@!@", "Gatt 서버 연결시 권한 거부")
                    }
                } else {
                    //11 이하 버전
                    val device = adapter.getRemoteDevice(address)
                    // connect to the GATT server on the device
                    bleGatt = device.connectGatt(this, false, gattCallback)
                }
                Log.d("BLE!@!@", "connect to the GATT server on the device_Success")
                return true
            } catch (e: IllegalArgumentException) {
                Log.d("BLE!@!@", "Device not found with provided address.")
                return false
            }
        } ?: run {
            Log.d("BLE!@!@", "BluetoothAdapter not initialized")
            return false
        }
    }

    fun sayHello() {
        writeCharacteristic?.let {
            if (it.properties or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0) {
                writeCharacteristic(it)
            }
        }
    }

    private fun writeCharacteristic(it: BluetoothGattCharacteristic) {
        val len: Byte = 0x01  // 데이터 길이
        val cmd: Byte = 0xA1.toByte()  // 전송할 CMD 값
        val byteArrayValue = byteArrayOf(len, cmd)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED) {
                val result = bleGatt?.writeCharacteristic(
                    it,
                    byteArrayValue,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                )
                if (result == BluetoothStatusCodes.SUCCESS) {
                    // 성공적으로 데이터 전송됨
                    Log.d("BLE!@!@", "Data written successfully")
                } else {
                    // 데이터 전송 실패
                    Log.e("BLE!@!@", "Failed to write data: $result")
                }
            }
        } else {
            it.value = byteArrayValue
            it.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            val result = bleGatt?.writeCharacteristic(it)

            if (result == true) {
                Log.d("BLE!@!@", "Command sent successfully: len = $len, cmd = $cmd")

                Thread.sleep(2000)

                //test -> Led on
                val ledLen: Byte = 0x02  // 데이터 길이
                val ledCmd: Byte = 0xA8.toByte()  // 전송할 CMD 값
                val ledData: Byte = 0x02
                val ledByteArrayValue = byteArrayOf(ledLen, ledCmd, ledData)

                it.value = ledByteArrayValue
                it.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                bleGatt?.writeCharacteristic(it)

            } else {
                Log.d("BLE!@!@", "Failed to send command: $result")
            }
        }
    }
}