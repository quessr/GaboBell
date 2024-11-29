package yiwoo.prototype.gabobell

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.kakao.vectormap.KakaoMapSdk

class GaboApplication: Application() {

    // 전역 관리 프로퍼티 (긴급신고)
    var eventId: Long = -1

    // 귀가 모니터링
    var monitorId: Long = -1

    // 단말 연결 여부
    var isConnected: Boolean = false

    // 위급 상황 신고 여부
    var isEmergency: Boolean = false

    override fun onCreate() {
        super.onCreate()

        KakaoSdk.init(this, "2ec0ef7ff1549353fb1a5a9d4ac4b0ea")
        KakaoMapSdk.init(this, "2ec0ef7ff1549353fb1a5a9d4ac4b0ea")
    }
}