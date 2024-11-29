package yiwoo.prototype.gabobell.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import yiwoo.prototype.gabobell.GaboApplication
import yiwoo.prototype.gabobell.ble.BleManager
import yiwoo.prototype.gabobell.helper.UserDataStore

class FirebaseMessageService: FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // 획득한 토큰을 저장한다.

        Log.d("@!@", "FirebaseMessagingService onNewToken : $token")

        val storedToken = UserDataStore.getPushToken(applicationContext)
        if (storedToken.isEmpty()) {
            // 최초 등록
            UserDataStore.savePushToken(applicationContext, token)
        } else {
            // 변경 발생?
            if (storedToken != token) {
                UserDataStore.savePushToken(applicationContext, token)
                // TODO: 데이터 갱신 필요 (서버)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
        var body = message.notification?.body

        Log.d("@!@", "onMessageReceived : $title, $body")

        // 임시 처리
        if (body?.contains("Emergency_Cancel") == true) {
            // 긴급신고 상황해제 (신고취소)
            val app = (application as GaboApplication)
            app.isEmergency = false
            app.eventId = -1
            if (app.isConnected) {
                BleManager.instance?.cmdEmergency(false)
            }
        }
    }
}