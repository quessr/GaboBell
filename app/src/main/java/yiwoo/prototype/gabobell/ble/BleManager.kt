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
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import yiwoo.prototype.gabobell.helper.Logger
import java.util.UUID

class BleManager: Service() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bleGatt: BluetoothGatt? = null
    private var connectionState = STATE_DISCONNECTED

    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    private val handler = Handler(Looper.getMainLooper())
    private val binder = LocalBinder()

    private var scanning = false

    private var byteArrayValue: ByteArray? = null

    inner class LocalBinder : Binder() {
        fun getService() = this@BleManager
    }
    override fun onBind(intent: Intent?): IBinder {
        Logger.d("onBind")
        return binder
    }

    override fun onDestroy() {
        Logger.d("onDestroy")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d("onStartCommand")
        return START_STICKY
    }

    // region * 초기화
    fun initialize(): Boolean {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            Logger.e("Unable to obtain a BluetoothAdapter.")
            return false
        }
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        return true
    }
    // endregion

    // region * Scan 기능
    fun startBleScan() {
        // ScanSettings 설정
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        // ScanFilter 설정
        // TODO: 세라 - UUID const 로 define 하여 사용
        val scanFilter = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")))
                .build()
        )

        val permissionGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            if (!scanning) {
                handler.postDelayed({
                    scanning = false
                    bluetoothLeScanner?.stopScan(scanCallback)
                    Logger.d("10초 동안 찾지 못함")
                }, 10_000)
                scanning = true
                bluetoothLeScanner?.startScan(scanFilter, scanSettings, scanCallback)
            } else {
                scanning = false
                bluetoothLeScanner?.stopScan(scanCallback)
            }
        }
    }

    @SuppressLint("MissingPermission", "NewApi")
    fun stopBleScan() {
        bluetoothLeScanner?.stopScan(scanCallback)
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            // 스캔이 중단된 상태라면 추가적인 스캔 결과는 무시
            if (!scanning) {
                return
            }
            Logger.d("Scanning...")
            //스캔 결과값 받아올 콜백 메소드
            //어뎁터에 연결하여 디바이스 정보 뿌려주는 로직(우선 리스트에 담아서 로그로 확인작업)
            //result 를 브로드캐스트로 액티비티 전달
            val device = result?.device
            val deviceName = device?.name
            val deviceAddress = device?.address

            val permissionGranted =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        ContextCompat.checkSelfPermission(this@BleManager, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

            if (permissionGranted) {
                scanning = false
                bluetoothLeScanner?.stopScan(this)
                handler.removeCallbacksAndMessages(null)
                Logger.d("Scan stopped")

                val intent = Intent(BLE_SCAN_RESULT).apply {
                    putExtra("device_name", deviceName)
                    putExtra("device_address", deviceAddress)
                    putExtra("result", result)
                }
                sendBroadcast(intent)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            println("onScanFailed  $errorCode")
        }
    }
    // endregion

    // region * GATT
    fun connect(address: String?): Boolean {
        return bluetoothAdapter?.let { adapter ->
            try {
                val permissionGranted =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

                if (permissionGranted) {
                    val device = adapter.getRemoteDevice(address)
                    // Connect to the GATT server on the device
                    bleGatt = device.connectGatt(this, false, gattCallback)
                    Logger.d("Connect to the GATT server on the device_Success")
                    true
                } else {
                    Logger.d("Gatt 서버 연결시 권한 거부")
                    false
                }
            } catch (e: IllegalArgumentException) {
                Logger.d("Device not found with provided address.")
                false
            }
        } ?: run {
            Logger.d("BluetoothAdapter not initialized")
            false
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
                    Logger.d("successfully connected to the GATT Server")
                    intentAction = ACTION_GATT_CONNECTED
                    broadcastUpdate(intentAction)
                    connectionState = STATE_CONNECTED

                    val permissionGranted =
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                                ContextCompat.checkSelfPermission(
                                    this@BleManager, Manifest.permission.BLUETOOTH_CONNECT
                                ) == PackageManager.PERMISSION_GRANTED

                    if (permissionGranted) {
                        bleGatt?.discoverServices()
                        Logger.d("discoverServices_query")
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Logger.d("disconnected from the GATT Server")
                    intentAction = ACTION_GATT_DISCONNECTED
                    broadcastUpdate(intentAction)
                    connectionState = STATE_DISCONNECTED

                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //ble 특성 읽기
                displayGattServices(getSupportedGattServices())
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                Logger.d("onServicesDiscovered_GATT_SUCCESS")
            } else {
                Logger.d("onServicesDiscovered_GATT_FAIL: $status")
            }
        }

        // TODO: 세라 - Notify를 통해서 데이터 수신
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)

            handleCommand(value)
        }
    }

    /**
     * BLE 특성읽기
     * BluetoothGattService의 리스트를 받아와서 그 서비스와 해당하는 특성 들을 화면에 표시하기 위한 작업 수행
     * 지원되는 GATT를 반복하는 방법을 보여줍니다.
     */
    private fun displayGattServices(gattServices: List<BluetoothGattService?>?) {
        if (gattServices == null) return
        var uuid: String?

        Logger.d("displayGattServices")
        //사용 가능한 GATT 서비스를 반복
        gattServices.forEach { gattService ->
            uuid = gattService?.uuid.toString()
            val gattCharacteristics = gattService?.characteristics

            // TODO: 세라 - notify 특성을 가지고 있는 Characteristic 찾기 (notifyCharacteristic 에 할당)
            // TODO: 세라 - 벨에서 오는 데이터를 수신하기 위한 코드 필요

            //사용 가능한 특성을 반복
            gattCharacteristics?.forEach { gattCharacteristic ->
                uuid = gattCharacteristic.uuid.toString()
                //tx 특성만 뽑을경우
                // TODO: 세라 - UUID 로 찾는 방법 이외에는 없는가?
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
        Logger.d("broadcastUpdate")
        sendBroadcast(intent)
    }

    /**
     * 서비스가 검색되면 서비스는 getServices()(으)로 보고된 데이터를 가져옵니다.
     * BLE 장치에서 제공되는 서비스들을 받아올 수 있도록 해주는 메소드
     */
    private fun getSupportedGattServices(): List<BluetoothGattService?>? {
        return bleGatt?.services
    }

    // endregion

    // region * Command (APP to BELL)
    fun sayHello() {
        Logger.d("[A2B] 0xA1")
        sendCommand(0x01, 0xA1.toByte(), null)
    }

    fun cmdEmergency(isRequest: Boolean) {
        if (isRequest) {
            Logger.d("[A2B] 0xA2_EMERGENCY_ON")
            sendCommand(0x01, 0xA2.toByte(), null)
        } else {
            Logger.d("[A2B] 0xA3_EMERGENCY_OFF")
            sendCommand(0x01, 0xA3.toByte(), null)
        }
    }

    fun cmdBellSetting(cmd: BellCommand) {
        // param(data1) 을 enum 으로 정의 (on, off, call)
        Logger.d("[A2B] 0xA4")
        Logger.d("[A2B] 0xA4_BELL_ON/OFF : ${cmd.data}")
        sendCommand(0x02, 0xA4.toByte(), cmd.data)
    }

    enum class BellCommand(val data: Byte) {
        ON(0x02),
        OFF(0x01),
        SIREN(0x03)
    }

    fun cmdGetStatus() {
        Logger.d("[A2B] 0xA5")
        Logger.d("[A2B] 0xA5_STATE_CHECK")
        sendCommand(0x01, 0xA5.toByte(), null)
    }

    fun cmdDeviceFirmwareUpdate() {
        // 구현하지 말것.
        Logger.d("[A2B] 0xA7")
        // sendCommand(0x01, 0xA7.toByte(), null)
    }

    fun cmdLedSetting(isOn: Boolean) {
        Logger.d("[A2B] 0xA8")
        if (isOn) {
            Logger.d("[A2B] 0xA8_LED_ON")
            sendCommand(0x02, 0xA8.toByte(), 0x02)
        } else {
            Logger.d("[A2B] 0xA8_LED_OFF")
            sendCommand(0x02, 0xA8.toByte(), 0x01)
        }
    }

    private fun sendCommand(
        len: Byte,
        cmd: Byte,
        data: Byte?
    ) {
        if (writeCharacteristic == null)
            return
        byteArrayValue = if (data == null){
            byteArrayOf(len, cmd)
        } else {
            byteArrayOf(len, cmd, data)
        }
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED &&
                    bleGatt?.writeCharacteristic(
                        writeCharacteristic!!, byteArrayValue!!, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    ) == BluetoothStatusCodes.SUCCESS
        } else {
            writeCharacteristic!!.value = byteArrayValue
            writeCharacteristic!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            bleGatt?.writeCharacteristic(writeCharacteristic) == true
        }
        Logger.d("Command sent successfully: $result")
    }

    // endregion

    // region * Receive (BELL to APP)
    // TODO: 세라 - 일단은 값이 정상적으로 수신되는지만 로그로 남긴다. (이후 로직은 나중에 생각한다.)
    private fun handleCommand(receivedData: ByteArray) {
        // CMD 를 추출해서 동작 처리

        // 긴급구조 요청 (0xB2)
        // 긴급구조 취소 (0xB3)
        // 벨 On/Off 설정 응답 (0xB4) with data
        // 상태 응답 (0xB5) with data
        // LED On/Off 설정 응답 (0xB8) with data
    }
    // endregion

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
}