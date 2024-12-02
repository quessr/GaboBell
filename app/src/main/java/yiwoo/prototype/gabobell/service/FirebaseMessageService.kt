package yiwoo.prototype.gabobell.service

import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import yiwoo.prototype.gabobell.GaboApplication
import yiwoo.prototype.gabobell.api.dto.request.ModifyUserDetails
import yiwoo.prototype.gabobell.api.dto.request.ModifyUserRequest
import yiwoo.prototype.gabobell.ble.BleManager
import yiwoo.prototype.gabobell.ble.BleManager.Companion.BLE_CANCEL_REPORTE_EMERGENCY
import yiwoo.prototype.gabobell.helper.ApiProvider
import yiwoo.prototype.gabobell.helper.UserDataStore

class FirebaseMessageService: FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // 획득한 토큰을 저장한다.
        Log.d("@!@", "FirebaseMessagingService onNewToken : $token")

        // 기존 저장된 푸시 토큰
        val storedToken = UserDataStore.getPushToken(applicationContext)
        Log.d("@!@", "FirebaseMessagingService storedToken : $token")
        if (storedToken.isEmpty()) {
            // 최초 등록
            UserDataStore.savePushToken(applicationContext, token)
        } else {
            // 변경 발생?
            if (storedToken != token) {
                UserDataStore.savePushToken(applicationContext, token)
                // PUSH 토큰 갱신
                val uuid = UserDataStore.getUUID(applicationContext)
                if (uuid.isEmpty()) {
                    return
                }
                CoroutineScope(Dispatchers.IO).launch {
                    val gaboApi = ApiProvider.provideGaboApi(applicationContext)
                    gaboApi.modifyUser(userId = uuid, ModifyUserRequest(ModifyUserDetails(pushToken = token)))
                }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // 푸시 페이로드 규격 정의 (24.11.29)
        /*
        // #eventStatus
        "data": {
            "type": "eventStatus",
            "customData": {
            "id": 23,
            "serviceState": "종료",
            "closureType": "자동 종료"
            }
        }
        // #notification
        "data": {
            "type": "notification",
            "customData": {
            "title": "Emergency Alert",
            "body": "This is an emergency message."
            }
        }
        // #action
        "data": {
            "type": "action",
            "customData": {
            "action": "OPEN_DETAILS"
        }
        */

        // 정의된 동작이 신고 상황 해제 밖에 없어서...
        // 일단 eventStatus 타입으로 푸시가 들어오면 신고 취소로 보자.
        if (message.data["type"] == "eventStatus") {
            // 긴급신고 상황해제
            val app = (application as GaboApplication)
            app.isEmergency = false
            app.eventId = -1
            if (app.isConnected) {
                BleManager.instance?.cmdEmergency(false)
            } else {
                sendBroadcast(Intent(BLE_CANCEL_REPORTE_EMERGENCY))
            }
        }

        // 테스트용 캠페인
        /*
        val body = message.notification?.body
        if (body != null && body.contains("eventStatus")) {
            val app = (application as GaboApplication)
            app.isEmergency = false
            app.eventId = -1
            if (app.isConnected) {
                BleManager.instance?.cmdEmergency(false)
            } else {
                sendBroadcast(Intent(BLE_CANCEL_REPORTE_EMERGENCY))
            }
        }
        */
    }
}