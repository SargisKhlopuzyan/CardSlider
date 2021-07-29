package com.sargis.khlopuzyan.cardslider.cards

import android.os.Build
import android.view.View
import androidx.annotation.NonNull
import androidx.core.view.ViewCompat
import com.sargis.khlopuzyan.cardslider.custom.CreditCardView
import com.sargis.khlopuzyan.cardslider.vertical.CardSliderLayoutManager
import com.sargis.khlopuzyan.cardslider.vertical.DefaultViewUpdater
import kotlin.math.max

class CardsUpdater : DefaultViewUpdater() {

    override fun updateView(@NonNull view: CreditCardView, position: Float) {
        super.updateView(view, position)

        //TODO UNCOMMENT

//        val card: CreditCardView = view
//        val alphaView: View = card.getChildAt(1)
//        val imageView: View = card.getChildAt(0)
//
//        if (position < 0) {
//            val alpha: Float = ViewCompat.getAlpha(view)
//            ViewCompat.setAlpha(view, 1f)
//            ViewCompat.setAlpha(alphaView, 0.9f - alpha)
//            ViewCompat.setAlpha(imageView, 0.3f + alpha)
//        } else {
//            ViewCompat.setAlpha(alphaView, 0f)
//            ViewCompat.setAlpha(imageView, 1f)
//        }
//
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
//            val lm: CardSliderLayoutManager = getLayoutManager()!!
//            val ratio: Float = (lm.getDecoratedTop(view) / lm.activeCardTop).toFloat()
//
//            val z: Float
//            if (position < 0) {
//                z = Z_CENTER_1 * ratio
//            } else if (position < 0.5f) {
//                z = Z_CENTER_1.toFloat()
//            } else if (position < 1f) {
//                z = Z_CENTER_2.toFloat()
//            } else {
//                z = Z_BOTTOM.toFloat()
//            }
//
//            card.cardElevation = max(0f, z)
//        }
    }

}