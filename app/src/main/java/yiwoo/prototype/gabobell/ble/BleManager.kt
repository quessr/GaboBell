package yiwoo.prototype.gabobell.ble

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import yiwoo.prototype.gabobell.GaboApplication
import yiwoo.prototype.gabobell.R
import yiwoo.prototype.gabobell.helper.ApiSender
import yiwoo.prototype.gabobell.helper.FlashUtil
import yiwoo.prototype.gabobell.helper.LocationHelper
import yiwoo.prototype.gabobell.helper.Logger
import yiwoo.prototype.gabobell.helper.UserDeviceManager
import yiwoo.prototype.gabobell.`interface`.EventIdCallback
import java.util.UUID

class BleManager : Service() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bleGatt: BluetoothGatt? = null
    private var connectionState = STATE_DISCONNECTED

    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    private val handler = Handler(Looper.getMainLooper())
    private val binder = LocalBinder()

    private var scanning = false
    private var permissionGranted: Boolean = true

    private var byteArrayValue: ByteArray? = null

    private var eventIdCallback: EventIdCallback? = null

    private lateinit var bluetoothStateReceiver: CommonReceiver
    private var isReceiverRegistered = false
    private var isEmergencyViaApp = false

    private lateinit var audioManager: AudioManager
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var flashUtil: FlashUtil
    private val serviceScope = CoroutineScope(Dispatchers.Main) // Main 스레드에서 실행



    fun setEventIdCallback(callback: EventIdCallback?) {
        eventIdCallback = callback
    }
    inner class LocalBinder : Binder() {
        fun getService() = this@BleManager
    }

    override fun onBind(intent: Intent?): IBinder {
        Logger.d("onBind")
        return binder
    }

    override fun onDestroy() {
        Logger.d("BleManager_onDestroy")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d("onStartCommand")
        instance = this
        initialize()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        flashUtil = FlashUtil.getInstance(applicationContext)

        val notification = createNotification()
        startForeground(1, notification)

        if (!isReceiverRegistered) {
            bluetoothStateReceiver = CommonReceiver()
            val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(bluetoothStateReceiver, filter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(bluetoothStateReceiver, filter)
            }

            isReceiverRegistered = true
        }

        //reboot 후 재연결 시도
        if (bluetoothAdapter != null && bluetoothAdapter!!.isEnabled) {
            Logger.d("reboot_reconnect")
            reconnect()
        }

        return START_STICKY
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "BLE_SERVICE_CHANNEL"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "BLE Background Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("BLE 연결 서비스")
            .setContentText("BLE 기기와 연결 중입니다.")
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .build()
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
        val scanFilter = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(CLIENT_CHARACTERISTIC_CONFIG_UUID))
                .build()
        )

        permissionGranted =
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
                    val intent = Intent(BLE_SCAN_NOT_FOUND)
                    sendBroadcast(intent)
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
                        ContextCompat.checkSelfPermission(
                            this@BleManager,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED

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


    fun disconnect() {
        bleGatt?.let { gatt ->
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Logger.d("BluetoothGatt_DISCONNECT_PERMISSION_GRANTED")
            }
            gatt.disconnect()
            gatt.close()
            bleGatt = null
        }

        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Logger.d("StopForeground_Service")
            instance = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            if (isReceiverRegistered) {
                unregisterReceiver(bluetoothStateReceiver)
                isReceiverRegistered = false
            }
        } else {
            Logger.d("StopForeground_Service")
            instance = null
            stopForeground(true)
            if (isReceiverRegistered) {
                unregisterReceiver(bluetoothStateReceiver)
                isReceiverRegistered = false
            }
        }
        */
    }

    // region * GATT
    fun connect(address: String?): Boolean {
        return bluetoothAdapter?.let { adapter ->
            try {
                val permissionGranted =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED

                if (permissionGranted) {
                    val device = adapter.getRemoteDevice(address)
                    bleGatt = device.connectGatt(this, true, gattCallback)
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

    fun reconnect() {
        val address = UserDeviceManager.getAddress(this)
        address.let {
            Logger.d("Attempting to reconnect...")
            connect(it)
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
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Logger.d("successfully connected to the GATT Server")

                    (application as GaboApplication).isConnected = true

                    intentAction = ACTION_GATT_CONNECTED
                    broadcastUpdate(intentAction)
                    connectionState = STATE_CONNECTED

                    val permissionGranted =
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                                ContextCompat.checkSelfPermission(
                                    this@BleManager, Manifest.permission.BLUETOOTH_CONNECT
                                ) == PackageManager.PERMISSION_GRANTED
                    if (permissionGranted) {
                        bleGatt = gatt
                        gatt?.discoverServices()
//                        bleGatt?.discoverServices()
                        Logger.d("discoverServices_query")
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Logger.d("disconnected from the GATT Server")

                    (application as GaboApplication).isConnected = false
                    intentAction = ACTION_GATT_DISCONNECTED
                    broadcastUpdate(intentAction)
                    connectionState = STATE_DISCONNECTED

                    bleGatt?.close()
                    bleGatt = null
                    reconnect()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //ble 특성 읽기
                displayGattServices(getSupportedGattServices())
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                // 알림 설정을 위해 setCharacteristicNotification 호출
                setCharacteristicNotification(notifyCharacteristic!!, true)

                Logger.d("onServicesDiscovered_GATT_SUCCESS")
            } else {
                Logger.d("onServicesDiscovered_GATT_FAIL: $status")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (descriptor?.uuid == UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Logger.d("onDescriptorWrite: Descriptor write successful")
                    // writeSuccess가 true일 때 sayHello 호출
                    sayHello()
                } else {
                    Logger.e("onDescriptorWrite: Descriptor write failed with status: $status")
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)

            Logger.d(
                "BleManager onCharacteristicChanged: gatt//$gatt, characteristic//$characteristic, value//${
                    value.get(
                        1
                    )
                }"
            )
            handleCommand(value)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {

                val byteArray = characteristic?.value
                val valueHex = byteArray?.joinToString(" ") { byte ->
                    String.format("0x%02X", byte)
                }
                Logger.d(
                    "BleManager onCharacteristicChanged: gatt//$gatt, characteristic//$valueHex"
                )
                byteArray?.let {
                    handleCommand(it)
                }
            }
        }
    }

    private fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        enable: Boolean
    ) {
        permissionGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        if (permissionGranted) {

            // 알림 설정의 성공 여부를 확인
            val notificationSet =
                bleGatt?.setCharacteristicNotification(characteristic, enable) == true
            Logger.d("BleManager Set characteristic notification success: $notificationSet")


            // 특성이 알림을 지원하는지 확인
            if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                Logger.d("BleManager Characteristic supports notifications")
                bleGatt?.setCharacteristicNotification(characteristic, enable)

                val descriptor =
                    characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                    val writeSuccess = bleGatt?.writeDescriptor(descriptor)
                    val writeSuccess = bleGatt?.writeDescriptor(descriptor) == true
                    Logger.d("BleManager Write descriptor success: $writeSuccess")
                } else {
                    val writeSuccess = bleGatt?.writeDescriptor(
                        descriptor,
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    )
                    Logger.d("BleManager Write descriptor success (TIRAMISU): $writeSuccess")

                }
            } else {
                Logger.e("BleManager Characteristic does not support notifications")
            }
        } else {
            Logger.e("BleManager setCharacteristicNotification: Permission denied")
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

        for (gattService in gattServices) {
            val gattCharacteristics: List<BluetoothGattCharacteristic> =
                gattService!!.characteristics

            for (gattCharacteristic in gattCharacteristics) {
                when (gattCharacteristic.uuid) {
                    UUID_DATA_WRITE -> {
                        writeCharacteristic = gattCharacteristic
                        Logger.d("Write Characteristic set: ${gattCharacteristic.uuid}")
                    }

                    UUID_DATA_NOTIFY -> {
                        notifyCharacteristic = gattCharacteristic
                        Logger.d("Notify Characteristic set: ${gattCharacteristic.uuid}")
                    }
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
//        setCharacteristicNotification(notifyCharacteristic!!, true)
    }

    // cmdEmergency 는 앱에서만 발생하는 메소드
    fun cmdEmergency(isRequest: Boolean) {
        if (isRequest) {
            Logger.d("[A2B] 0xA2_EMERGENCY_ON")
            isEmergencyViaApp = true
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
        byteArrayValue = if (data == null) {
            byteArrayOf(len, cmd)
        } else {
            byteArrayOf(len, cmd, data)
        }

        Logger.d("byteArrayValue: $byteArrayValue")
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED &&
                    bleGatt?.writeCharacteristic(
                        writeCharacteristic!!,
                        byteArrayValue!!,
                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
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
    private fun handleCommand(receivedData: ByteArray) {

        // CMD 값 및 데이터 추출
        val cmd = receivedData[1]
        val data1 = receivedData.getOrNull(2) ?: 0
        val data2 = receivedData.getOrNull(3) ?: 0
        val data3 = receivedData.getOrNull(4) ?: 0
        val data4 = receivedData.getOrNull(5) ?: 0

        // CMD 값에 따라 처리
        when (cmd) {
            0xB2.toByte() -> handleEmergencyRequestFromBell()
            0xB3.toByte() -> handleEmergencyCancelFromBell()
            0xB4.toByte() -> handleBellSetting(data1)
            0xB5.toByte() -> handleStatusResponse(data1, data2, data3, data4)
            0xB8.toByte() -> handleLedSetting(data1)
            0xB9.toByte() -> handleEmergencyRequestFromApp()
            0xBA.toByte() -> handleEmergencyCancelFromApp()
            else -> Logger.d("알 수 없는 CMD: 0x${String.format("%02X", cmd)}")
        }
    }

    // B2, B9 vs B3, BA
    private fun handleEmergency(cmd: Byte, stateEmergency: String) {

        // 벨에서만 호출.
        val valueHex = String.format("0x%02X", cmd.toInt() and 0xFF)
        if (cmd == 0xB2.toByte() || cmd == 0xB9.toByte()) {

            // 이미 신고 중인 상태냐?
            if ((application as GaboApplication).isEmergency)
                return

            // 전역 상태 변경 및 신고 API 호출
            (application as GaboApplication).isEmergency = true

            // 신고 이펙트 발생
           val serviceType = if (isEmergencyViaApp) {
                ApiSender.Event.EMERGENCY.serviceType
            } else {
                ApiSender.Event.BELL_EMERGENCY.serviceType
            }
//            val serviceType = if(cmd == 0xB2.toByte() ) {
//                ApiSender.Event.BELL_EMERGENCY.serviceType
//            } else {
//                ApiSender.Event.EMERGENCY.serviceType
//            }

            emergencyEffect(true)

            isEmergencyViaApp = false

            LocationHelper.getCurrentLocation(this) { lat, lng ->
                val locationLat: Double = lat
                val locationLng: Double = lng
                Logger.d("handleEmergency_currentLocation: $locationLat | $locationLng")

                ApiSender.createEvent(
                    context = this@BleManager,
                    serviceType = serviceType,
                    latitude = lat,
                    longitude = lng
                ) { eventId ->
                    eventIdCallback?.onEventId(eventId)
                    Logger.d("serviceType : $serviceType")
                }
            }
        } else if (cmd == 0xB3.toByte() || cmd == 0xBA.toByte()) {

            // 이미 취소 상태냐?
            if (!(application as GaboApplication).isEmergency)
                return

            // 전역 상태 변경 및 신고 취소 API 호출
            (application as GaboApplication).isEmergency = false
            val eventId = (application as GaboApplication).eventId
            // 신고 취소(상황해제) 푸시가 들어오면 이미 eventId 는 초기화(-1) 되므로 API 호출이 안되는게 맞다.
            // ApiSender.cancelEmergency 에서 필터됨.
            ApiSender.cancelEvent(this@BleManager, eventId)
            emergencyEffect(false)
        }

        val intent = Intent(stateEmergency).apply {
            putExtra("cmd", valueHex)
        }
        sendBroadcast(intent)
    }

    // 긴급 구조 요청 처리 (0xB2)
    private fun handleEmergencyRequestFromBell() {
        // 0xB2 는
        // 앱에서 벨로 요청해서 수신되는 경우와
        // 벨에서 직접 요청하는 경우를 구분해야 한다.

        // 24.12.13
        // FW16 에서 B2는 벨에서 신고시에만 수신된다.


        Logger.d("긴급 구조 요청")
        handleEmergency(0xB2.toByte(), BLE_REPORTE_EMERGENCY)
    }

    private fun handleEmergencyRequestFromApp() {

        Logger.d("긴급 구조 요청")
        handleEmergency(0xB9.toByte(), BLE_REPORTE_EMERGENCY)
    }

    // 긴급 구조 취소 처리 (0xB3)
    private fun handleEmergencyCancelFromBell() {
        Logger.d("긴급 구조 취소")
        handleEmergency(0xB3.toByte(), BLE_CANCEL_REPORTE_EMERGENCY)
    }

    private fun handleEmergencyCancelFromApp() {
        Logger.d("긴급 구조 취소")
        handleEmergency(0xBA.toByte(), BLE_CANCEL_REPORTE_EMERGENCY)
    }

    // 벨 On/Off 설정 응답 처리 (0xB4)
    private fun handleBellSetting(data1: Byte) {
        Logger.d("벨 On/Off 설정 응답")
        val bellStatusData = when (data1) {
            0x01.toByte() -> "Off"
            0x02.toByte() -> "On"
            else -> "BELL 상태 알 수 없음"
        }

        val intent = Intent(BLE_BELL_SETTING_CHANGED).apply {
            putExtra("status_bell", bellStatusData)
        }
        sendBroadcast(intent)
    }

    // 상태 응답 처리 (0xB5)
    private fun handleStatusResponse(data1: Byte, data2: Byte, data3: Byte, data4: Byte) {
        Logger.d("상태 응답")

        // data1 처리
        val statusMessage1 = when (data1) {
            0x01.toByte() -> "충전중"
            0x02.toByte() -> "완충"
            0x03.toByte() -> "충전필요"
            0x04.toByte() -> "기타"
            else -> "알 수 없는 상태"
        }
        Logger.d("상태 응답 data1: $statusMessage1")

        // data2 처리
        val statusMessage2 = when (data2) {
            0x01.toByte() -> "벨 Off"
            0x02.toByte() -> "벨 On"
            else -> "벨 상태 알 수 없음"
        }
        Logger.d("상태 응답 data2: $statusMessage2")

        // data3 처리
        val version = convertHexToVersion(data3)
        Logger.d("상태 응답 data3: $version")

        // data4 처리
        val statusMessage4 = when (data4) {
            0x01.toByte() -> "LED Off"
            0x02.toByte() -> "LED On"
            else -> "LED 상태 알 수 없음"
        }
        Logger.d("상태 응답 data4: $statusMessage4")

        // 상태 업데이트 브로드캐스트 전송
        val intent = Intent(BLE_STATUS_UPDATE).apply {
            putExtra("status_charging", statusMessage1)
            putExtra("status_bell", statusMessage2)
            putExtra("status_version", version)
            putExtra("status_led", statusMessage4)
        }
        sendBroadcast(intent)
    }

    // LED On/Off 설정 응답 처리 (0xB8)
    private fun handleLedSetting(data1: Byte) {
        Logger.d("LED On/Off 설정 응답")
        val ledStatusData = when (data1) {
            0x01.toByte() -> "Off"
            0x02.toByte() -> "On"
            else -> "LED 상태 알 수 없음"
        }

        val intent = Intent(BLE_LED_SETTING_CHANGED).apply {
            putExtra("status_led", ledStatusData)
        }
        sendBroadcast(intent)
    }
// endregion

    private fun convertHexToVersion(hex: Byte): String {
        val intHex = hex.toInt() and 0xFF
        val major = (intHex shr 4) // 상위 4비트 추출
        val minor = (intHex and 0x0F) // 하위 4비트 추출
        return "$major.$minor"
    }

    fun stopEmergencyEffect() {
        emergencyEffect(false)
    }

    private fun emergencyEffect(isPlay: Boolean) {
        if (isPlay) {
            flashUtil.startEmergencySignal(serviceScope)
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, AudioManager.FLAG_PLAY_SOUND)
            mediaPlayer = MediaPlayer.create(this, R.raw.siren).apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setVolume(1.0f, 1.0f)
                isLooping = true
                start()
            }
        } else {
            flashUtil.stopEmergencySignal()
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        }
    }

    companion object {
        //외부에서 서비스 클래스 인스턴스 사용
        var instance: BleManager? = null

        const val ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "EXTRA_DATA"
        const val BLE_STATUS_UPDATE = "BLE_STATUS_UPDATE"
        const val BLE_LED_SETTING_CHANGED = "BLE_LED_SETTING_CHANGED"
        const val BLE_BELL_SETTING_CHANGED = "BLE_BELL_SETTING_CHANGED"
        const val BLE_REPORTE_EMERGENCY = "BLE_REPORTE_EMERGENCY"
        const val BLE_CANCEL_REPORTE_EMERGENCY = "BLE_CANCEL_REPORTE_EMERGENCY"

        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2

        const val BLE_SCAN_RESULT = "BLE_SCAN_RESULT"
        const val BLE_SCAN_NOT_FOUND = "BLE_SCAN_NOT_FOUND"

        val UUID_DATA_NOTIFY: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        val UUID_DATA_WRITE: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        private val CLIENT_CHARACTERISTIC_CONFIG_UUID =
            UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    }
}