package com.sargis.khlopuzyan.cardslider.cards

import android.graphics.Bitmap
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.sargis.khlopuzyan.cardslider.R
import com.sargis.khlopuzyan.cardslider.utils.DecodeBitmapTask

class SliderCard constructor(
    itemView: View
) : RecyclerView.ViewHolder(itemView)/*, DecodeBitmapTask.Listener*/ {

    /**
    companion object {
        private var viewWidth = 0
        private var viewHeight = 0
    }

    private val imageView: ImageView = itemView.findViewById(R.id.image) as ImageView

    private lateinit var task: DecodeBitmapTask

    fun setContent(@DrawableRes resId: Int) {
        imageView.setImageResource(resId)

        if (viewWidth == 0) {
            itemView.viewTreeObserver
                .addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        itemView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                        viewWidth = itemView.width
                        viewHeight = itemView.height
                        loadBitmap(resId)
                    }
                })
        } else {
            loadBitmap(resId)
        }
    }

    fun clearContent() {
        task.cancel(true)
    }

    private fun loadBitmap(@DrawableRes resId: Int) {
        task = DecodeBitmapTask(itemView.resources, resId, viewWidth, viewHeight, this)
        task.execute()
    }

    override fun onPostExecuted(bitmap: Bitmap) {
        imageView.setImageBitmap(bitmap)
    }
    */

}