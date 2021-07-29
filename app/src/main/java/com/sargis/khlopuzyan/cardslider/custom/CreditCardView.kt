package com.sargis.khlopuzyan.cardslider.custom

import android.content.Context
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.sargis.khlopuzyan.cardslider.R

/**
 * Created by Sargis Khlopuzyan, on 7/1/2021.
 *
 * @author Sargis Khlopuzyan (sargis.khlopuzyan@fastshift.com)
 */
class CreditCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private var skew = 360f
    private var position = 0f
    private var translate = 0f

    private val camera: Camera = Camera()

    private var textView: TextView? = null
    init {
        val v = inflate(context, R.layout.layout_slider_card, this)
        textView = v.findViewById(R.id.text_view)
        elevation = 0.0f
        setBackgroundColor(Color.TRANSPARENT)
//        setBackgroundColor(Color.GREEN)
    }

    fun setSkew(skew: Float, position: Float) {
        this.skew = skew
        this.position = position
        textView?.text = "$position"
        invalidate()
    }

    fun setTranslated(translate: Float) {
        this.translate = translate
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val matrix = canvas.matrix

        camera.save()
        camera.rotateX(-skew)
        camera.translate(0f, translate, 0f) // -
        camera.getMatrix(matrix)

        val centerX = width / 2
        val centerY = height / 2


        val dx = centerX.toFloat()
        val dy = if (skew >= 0) centerY.toFloat() else 2 * centerY.toFloat()

//        matrix.preTranslate(
////            -dx, // -
//            0f,
////            -dy
//            - height.toFloat(),
//        ) //For getting the correct viewing perspective
//
//        matrix.postTranslate(
////            dx, // +
//            0f,
////            dy
//            height.toFloat(),
//        )

        matrix.preTranslate(
            -centerX.toFloat(), // -
//            0f,
            -2*centerY.toFloat()
//            0f,
//            0f
        ) //For getting the correct viewing perspective

        matrix.postTranslate(
            centerX.toFloat(), // +
//            0f,
            2*centerY.toFloat()
//            0f,
//            0f
        )

        camera.restore()
        canvas.concat(matrix)

    }
}