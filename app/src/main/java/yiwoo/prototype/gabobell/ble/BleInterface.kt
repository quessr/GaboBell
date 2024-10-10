package yiwoo.prototype.gabobell.ble

interface BleInterface {
    fun onConnectedStateObserve(isConnected: Boolean, data: String)
    fun onDeviceFound(deviceName: String)
}