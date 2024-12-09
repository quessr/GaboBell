package yiwoo.prototype.gabobell.ui.popup

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import yiwoo.prototype.gabobell.databinding.CustomPopupBinding

class CustomPopup(context: Context): Dialog(context) {
    private var binding: CustomPopupBinding = CustomPopupBinding.inflate(layoutInflater)

    private var isVisibleCloseButton: Boolean = false
    private var onConfirmClickListener: View.OnClickListener? = null
    private var onCancelClickListener: View.OnClickListener? = null
    private var onCloseClickListener: View.OnClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initView()
    }

    private fun initView() {
        // 뒤로가기 버튼, 빈 화면 터치를 통해 dialog가 사라지지 않도록
        setCancelable(false)

        // background를 투명하게 만듦
        // (중요) Dialog는 내부적으로 뒤에 흰 사각형 배경이 존재하므로, 배경을 투명하게 만들지 않으면
        // corner radius의 적용이 보이지 않는다.
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.btnConfirm.setOnClickListener {
            onConfirmClickListener?.onClick(it)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            onCancelClickListener?.onClick(it)
            dismiss()
        }

        binding.btnClose.setOnClickListener {
            onCloseClickListener?.onClick(it)
            dismiss()
        }
    }

    //Builder 클래스 정의
    class Builder(private val context: Context) {
        private var title: String? = null
        private var message: String? = null
        private var btnConfirmText: String? = null
        private var btnCancelText: String? = null
        private var needCloseButton: Boolean = false
        private var onConfirmClickListener: View.OnClickListener? = null
        private var onCancelClickListener: View.OnClickListener? = null

        //device popup
        private var deviceId: String? = null
        private var deviceMessage: String? = null

        //device Id 설정
        fun setDeviceId(deviceId: String): Builder {
            this.deviceId = deviceId
            return this
        }
        fun setDeviceMessage(deviceMessage: String): Builder{
         this.deviceMessage = deviceMessage
         return this
        }

        // 제목 설정
        fun setTitle(title: String): Builder {
            this.title = title
            return this
        }

        // 메시지 설정
        fun setMessage(message: String): Builder {
            this.message = message
            return this
        }

        // 버튼 클릭 리스너 설정
        fun setOnOkClickListener(btnText: String, listener: View.OnClickListener): Builder {
            this.btnConfirmText = btnText
            this.onConfirmClickListener = listener
            return this
        }

        fun setOnCancelClickListener(btnText: String, listener: View.OnClickListener): Builder {
            this.btnCancelText = btnText
            this.onCancelClickListener = listener
            return this
        }

        // 닫기 버튼 설정 (X 버튼)
        fun setOnCloseClickListener(isVisible: Boolean): Builder {
            this.needCloseButton = isVisible
            return this
        }

        fun build(): CustomPopup {
            val popup = CustomPopup(context)
            popup.binding.popupTitle.text = title
            popup.binding.alertDetail.text = message
            popup.isVisibleCloseButton = needCloseButton
            popup.onConfirmClickListener = onConfirmClickListener
            popup.onCancelClickListener = onCancelClickListener

            //device popup
            if (!deviceId.isNullOrEmpty()) {
                popup.binding.deviceIdArea.visibility = View.VISIBLE
                popup.binding.alertDetail.visibility = View.GONE
                popup.binding.deviceId.text = deviceId
            }
            if (!deviceMessage.isNullOrEmpty()) {
                popup.binding.deviceMessage.visibility = View.VISIBLE
                popup.binding.deviceMessage.text = deviceMessage
            }

            if (!btnConfirmText.isNullOrEmpty()) {
                popup.binding.btnConfirm.text = btnConfirmText
            }
            if (!btnCancelText.isNullOrEmpty()) {
                popup.binding.btnCancel.visibility = View.VISIBLE
                popup.binding.btnCancel.text = btnCancelText
            }
            popup.binding.btnClose.visibility = if(this.needCloseButton) View.VISIBLE else View.GONE
            return popup
        }
    }
}