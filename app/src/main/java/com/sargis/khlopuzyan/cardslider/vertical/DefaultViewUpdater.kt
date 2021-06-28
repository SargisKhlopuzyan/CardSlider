package com.sargis.khlopuzyan.cardslider.vertical

import android.view.View
import androidx.core.view.ViewCompat


/**
 * Created by Sargis Khlopuzyan, on 6/4/2021.
 *
 * @author Sargis Khlopuzyan (sargis.khlopuzyan@fastshift.com)
 */
open class DefaultViewUpdater : CardSliderLayoutManager.ViewUpdater{

    companion object {
        const val SCALE_LEFT = 0.65f
        const val SCALE_CENTER = 0.95f
        const val SCALE_RIGHT = 0.8f
        const val SCALE_CENTER_TO_LEFT = 0.3f
        const val SCALE_CENTER_TO_RIGHT = 0.14999998f
        const val Z_CENTER_1 = 12
        const val Z_CENTER_2 = 16
        const val Z_RIGHT = 8
    }

    private var cardWidth = 0
    private var activeCardLeft = 0
    private var activeCardRight = 0
    private var activeCardCenter = 0
    private var cardsGap = 0f
    private var transitionEnd = 0
    private var transitionDistance = 0
    private var transitionRight2Center = 0f
    private var lm: CardSliderLayoutManager? = null
    private var previewView: View? = null


    override fun onLayoutManagerInitialized(lm: CardSliderLayoutManager) {
        this.lm = lm
        cardWidth = lm.cardWidth
        activeCardLeft = lm.activeCardLeft
        activeCardRight = lm.activeCardRight
        activeCardCenter = lm.activeCardCenter
        cardsGap = lm.cardsGap
        transitionEnd = activeCardCenter
        transitionDistance = activeCardRight - transitionEnd
        val centerBorder = (cardWidth.toFloat() - cardWidth.toFloat() * 0.95f) / 2.0f
        val rightBorder = (cardWidth.toFloat() - cardWidth.toFloat() * 0.8f) / 2.0f
        val right2centerDistance =
            activeCardRight.toFloat() + centerBorder - (activeCardRight.toFloat() - rightBorder)
        transitionRight2Center = right2centerDistance - cardsGap
    }

    override fun updateView(view: View, position: Float) {
        val scale: Float
        val alpha: Float
        val z: Float
        val x: Float
        val prevViewScale: Float
        if (position < 0.0f) {
            prevViewScale = lm!!.getDecoratedLeft(view).toFloat() / activeCardLeft.toFloat()
            scale = 0.65f + 0.3f * prevViewScale
            alpha = 0.1f + prevViewScale
            z = 12.0f * prevViewScale
            x = 0.0f
        } else if (position < 0.5f) {
            scale = 0.95f
            alpha = 1.0f
            z = 12.0f
            x = 0.0f
        } else {
            val prevTransition: Float
            if (position < 1.0f) {
                val viewLeft = lm!!.getDecoratedLeft(view)
                prevTransition =
                    (viewLeft - activeCardCenter).toFloat() / (activeCardRight - activeCardCenter).toFloat()
                scale = 0.95f - 0.14999998f * prevTransition
                alpha = 1.0f
                z = 16.0f
                x = if (Math.abs(transitionRight2Center) < Math.abs(
                        transitionRight2Center * (viewLeft - transitionEnd).toFloat() / transitionDistance.toFloat()
                    )
                ) {
                    -transitionRight2Center
                } else {
                    -transitionRight2Center * (viewLeft - transitionEnd).toFloat() / transitionDistance.toFloat()
                }
            } else {
                scale = 0.8f
                alpha = 1.0f
                z = 8.0f
                if (previewView != null && lm != null) {
                    val isFirstRight = lm!!.getDecoratedRight(previewView!!) <= activeCardRight
                    val prevRight: Int
                    if (isFirstRight) {
                        prevViewScale = 0.95f
                        prevRight = activeCardRight
                        prevTransition = 0.0f
                    } else {
                        prevViewScale = ViewCompat.getScaleX(previewView)
                        prevRight = lm!!.getDecoratedRight(previewView!!)
                        prevTransition = ViewCompat.getTranslationX(previewView)
                    }
                    val prevBorder =
                        (cardWidth.toFloat() - cardWidth.toFloat() * prevViewScale) / 2.0f
                    val currentBorder = (cardWidth.toFloat() - cardWidth.toFloat() * 0.8f) / 2.0f
                    val distance = lm!!.getDecoratedLeft(view).toFloat() + currentBorder - (prevRight.toFloat() - prevBorder + prevTransition)
                    val transition = distance - cardsGap
                    x = -transition
                } else {
                    x = 0.0f
                }
            }
        }
        ViewCompat.setScaleX(view, scale)
        ViewCompat.setScaleY(view, scale)
        ViewCompat.setZ(view, z)
        ViewCompat.setTranslationX(view, x)
        ViewCompat.setAlpha(view, alpha)
        previewView = view
    }

    fun getLayoutManager(): CardSliderLayoutManager? {
        return lm
    }
}