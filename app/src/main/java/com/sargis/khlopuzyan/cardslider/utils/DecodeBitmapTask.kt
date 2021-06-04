package com.sargis.khlopuzyan.cardslider.utils

import android.content.res.Resources
import android.graphics.*
import android.os.AsyncTask
import android.os.Build
import androidx.annotation.DrawableRes
import com.sargis.khlopuzyan.cardslider.R
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import kotlin.math.max


class DecodeBitmapTask constructor(
    private val resources: Resources,
    @DrawableRes private val bitmapResId: Int,
    private val reqWidth: Int,
    private val reqHeight: Int,
    listener: Listener
) : AsyncTask<Void, Void, Bitmap>() {

    companion object {
        fun getRoundedCornerBitmap(
            bitmap: Bitmap,
            pixels: Float,
            width: Int,
            height: Int
        ): Bitmap {
            val output: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas: Canvas = Canvas(output)

            val sourceWidth = bitmap.width
            val sourceHeight = bitmap.height

            val xScale = (width / bitmap.width).toFloat()
            val yScale = (height / bitmap.height).toFloat()
            val scale = max(xScale, yScale)

            val scaledWidth = scale * sourceWidth
            val scaledHeight = scale * sourceHeight

            val left = (width - scaledWidth) / 2
            val top = (height - scaledHeight) / 2

            val color = 0xff424242
            val paint: Paint = Paint()
            val rect: Rect = Rect(0, 0, width, height)
            val rectF: RectF = RectF(rect)

            val targetRect: RectF = RectF(left, top, left + scaledWidth, top + scaledHeight)

            paint.isAntiAlias = true
            canvas.drawARGB(0, 0, 0, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                paint.setColor(color)
            }
            canvas.drawRoundRect(rectF, pixels, pixels, paint)

            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, null, targetRect, paint)

            return output
        }
    }

    private val cache: BackgroundBitmapCache = BackgroundBitmapCache.getInstance()

    private val refListener: Reference<Listener>

    interface Listener {
        fun onPostExecuted(bitmap: Bitmap)
    }

    init {
        this.refListener = WeakReference(listener)
    }

    override fun doInBackground(vararg voids: Void): Bitmap? {
        val cachedBitmap: Bitmap? = cache.getBitmapFromBgMemCache(bitmapResId)
        if (cachedBitmap != null) {
            return cachedBitmap
        }

        val options: BitmapFactory.Options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(resources, bitmapResId, options)

        val width: Int = options.outWidth
        val height: Int = options.outHeight

        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfWidth = width / 2
            val halfHeight = height / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth
                && !isCancelled
            ) {
                inSampleSize *= 2
            }
        }

        if (isCancelled) {
            return null
        }

        options.inSampleSize = inSampleSize
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        val decodedBitmap: Bitmap = BitmapFactory.decodeResource(resources, bitmapResId, options)

        val result: Bitmap
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            result = getRoundedCornerBitmap(
                decodedBitmap,
                resources.getDimension(R.dimen.card_corner_radius), reqWidth, reqHeight
            )
            decodedBitmap.recycle()
        } else {
            result = decodedBitmap
        }

        cache.addBitmapToBgMemoryCache(bitmapResId, result)
        return result
    }

    override fun onPostExecute(bitmap: Bitmap) {
        val listener: Listener? = this.refListener.get()
        listener?.onPostExecuted(bitmap)
    }

}