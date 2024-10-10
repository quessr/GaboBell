package yiwoo.prototype.gabobell.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import yiwoo.prototype.gabobell.model.DeviceData
import java.util.UUID

class BleManager(private val context: Context) {

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter

    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private var scanList: MutableList<DeviceData>? = mutableListOf()
    private var connectedStateObserver: BleInterface? = null
    var bleGatt: BluetoothGatt? = null
    private var foundedDevice: BleInterface? = null

    private val scanCallback: ScanCallback =
        object : ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                if (result?.device?.name != null) {
                    var uuid = "null"

                    if (result.scanRecord?.serviceUuids != null) {
                        uuid = result.scanRecord!!.serviceUuids.toString()
                    }

                    val scanItem = DeviceData(
                        result.device.name ?: "null",
                        uuid,
                        result.device.address ?: "null"
                    )

                    foundedDevice?.onDeviceFound(scanItem.name)

                    Log.d(
                        "BLE_SCAN",
                        "Device found: Name=${scanItem.name}, Address=${scanItem.address}, UUID=$uuid"
                    )

                    // Ansimi 기기를 찾은 후 스캔 중지
                    if (uuid.contains("6e400001-b5a3-f393-e0a9-e50e24dcca9e")) {
                        stopBleScan()
                        Toast.makeText(context, "기기를 찾았으므로 스캔을 중지합니다.", Toast.LENGTH_SHORT).show()
                    }

                    if (scanList == null) {
                        scanList = mutableListOf() // scanList가 null일 때 초기화
                    }


                    if (!scanList!!.contains(scanItem)) {
                        scanList!!.add(scanItem)
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                println("onScanFailed  $errorCode")
            }

        }

    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt?.discoverServices()
                connectedStateObserver?.onConnectedStateObserve(
                    true,
                    "onConnectionStateChange: STATE_CONNECTED" + "\n" + "---"
                )
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectedStateObserver?.onConnectedStateObserve(
                    false,
                    "onConnectionStateChange: STATE_CONNECTED" + "\n" + "---"
                )
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                MainScope().launch {
                    bleGatt = gatt
                    Toast.makeText(context, " ${gatt?.device?.name} 연결 성공", Toast.LENGTH_SHORT)
                        .show()
                    var sendText = "onServicesDiscovered:  GATT_SUCCESS" + "\n" + "↓" + "\n"

                    for (service in gatt?.services!!) {
                        sendText += "- " + service.uuid.toString() + "\n"
                        for (characteristics in service.characteristics) {
                            sendText += "    "
                        }
                    }
                    sendText += "---"
                    connectedStateObserver?.onConnectedStateObserve(true, sendText)
                }.cancel()
            }
        }
    }

    @SuppressLint("NewApi", "MissingPermission")
    fun startBleScan() {
        scanList?.clear()

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
        bluetoothLeScanner.startScan(scanFilter, scanSettings, scanCallback)
        Toast.makeText(context, "Scanning started", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission", "NewApi")
    fun stopBleScan() {
        bluetoothLeScanner.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun startBleConnectGatt(deviceData: DeviceData) {
        bluetoothAdapter.getRemoteDevice(deviceData.address)
            .connectGatt(context, false, gattCallback)
    }

    fun setScanList(pScanList: MutableList<DeviceData>) {
        scanList = pScanList
    }

    fun onConnectedStateObserve(pConnectedStateObserver: BleInterface) {
        connectedStateObserver = pConnectedStateObserver
    }

    fun setOnDeviceFoundListener(listener: BleInterface) {
        foundedDevice = listener
    }
}