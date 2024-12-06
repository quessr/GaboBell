package yiwoo.prototype.gabobell.ui.searchAddress

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import yiwoo.prototype.gabobell.R

class SearchAddressItemDecoration : ItemDecoration() {
    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        val left = parent.paddingLeft.toFloat()
        val right = parent.width - parent.paddingRight.toFloat()
        val paint = Paint().apply {
            color = ContextCompat.getColor(parent.context, R.color.divider_search_address)
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
            pathEffect = DashPathEffect(floatArrayOf(10f, 20f), 0f)
        }

        parent.children.forEach {
            val adapterPosition = parent.getChildAdapterPosition(it)
            if (adapterPosition + 1 == parent.adapter?.itemCount) return
            val top = it.bottom.toFloat()
            c.drawRect(left, top, right, top, paint)
        }
    }
}