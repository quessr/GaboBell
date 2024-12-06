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

    // 제목 설정
    fun setTitle(title: String): CustomPopup {
//        this.title = title
        binding.popupTitle.text = title
        return this
    }

    // 메시지 설정
    fun setMessage(message: CharSequence): CustomPopup {
        binding.alertDetail.text = message
        return this
    }

    // 버튼 클릭 리스너 설정
    fun setOnOkClickListener(listener: View.OnClickListener): CustomPopup {
        this.onConfirmClickListener = listener
        return this
    }

    fun setOnCancelClickListener(listener: View.OnClickListener): CustomPopup {
        this.onCancelClickListener = listener
        binding.btnCancel.visibility = View.VISIBLE
        return this
    }

    // 닫기 버튼 설정 (X 버튼)
    fun setOnCloseClickListener(listener: View.OnClickListener): CustomPopup {
        this.onCloseClickListener = listener
        binding.btnClose.visibility = View.VISIBLE
        return this
    }

    // 확인 버튼 텍스트 변경
    fun setConfirmButtonText(text: String): CustomPopup {
        binding.btnConfirm.text = text
        return this
    }
}