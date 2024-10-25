package yiwoo.prototype.gabobell

import android.app.Application

class GaboApplication: Application() {

    // 전역 관리 프로퍼티
    var eventId: Long = -1

    // 단말 연결 여부
    var isConnected: Boolean = false

    // 위급 상황 신고 여부
    var isEmergency: Boolean = false

    override fun onCreate() {
        super.onCreate()
    }
}