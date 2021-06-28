package com.sargis.khlopuzyan.cardslider.cards

import android.os.Build
import android.view.View
import androidx.annotation.NonNull
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import com.sargis.khlopuzyan.cardslider.vertical.CardSliderLayoutManager
import com.sargis.khlopuzyan.cardslider.vertical.DefaultViewUpdater
import kotlin.math.max

class CardsUpdater : DefaultViewUpdater() {

    override fun updateView(@NonNull view: View, position: Float) {
        super.updateView(view, position)

        val card: CardView = view as CardView
        val alphaView: View = card.getChildAt(1)
        val imageView: View = card.getChildAt(0)

        if (position < 0) {
            val alpha: Float = ViewCompat.getAlpha(view)
            ViewCompat.setAlpha(view, 1f)
            ViewCompat.setAlpha(alphaView, 0.9f - alpha)
            ViewCompat.setAlpha(imageView, 0.3f + alpha)
        } else {
            ViewCompat.setAlpha(alphaView, 0f)
            ViewCompat.setAlpha(imageView, 1f)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            val lm: CardSliderLayoutManager = getLayoutManager()!!
            val ratio: Float = (lm.getDecoratedLeft(view) / lm.activeCardLeft).toFloat()

            val z: Float
            if (position < 0) {
                z = Z_CENTER_1 * ratio
            } else if (position < 0.5f) {
                z = Z_CENTER_1.toFloat()
            } else if (position < 1f) {
                z = Z_CENTER_2.toFloat()
            } else {
                z = Z_RIGHT.toFloat()
            }

            card.cardElevation = max(0f, z)
        }
    }

}
