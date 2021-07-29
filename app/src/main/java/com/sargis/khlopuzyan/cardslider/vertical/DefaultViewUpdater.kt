package com.sargis.khlopuzyan.cardslider.vertical

import com.sargis.khlopuzyan.cardslider.custom.CreditCardView
import kotlin.math.abs


/**
 * Created by Sargis Khlopuzyan, on 6/4/2021.
 *
 * @author Sargis Khlopuzyan (sargis.khlopuzyan@fastshift.com)
 */
open class DefaultViewUpdater : CardSliderLayoutManager.ViewUpdater {

    companion object {
        const val SCALE_TOP = 0.65f
        const val SCALE_CENTER = 0.95f
        const val SCALE_BOTTOM = 0.8f
        const val SCALE_CENTER_TO_TOP = 0.3f
        const val SCALE_CENTER_TO_BOTTOM = 0.14999998f
        const val Z_CENTER_1 = 12
        const val Z_CENTER_2 = 16
        const val Z_BOTTOM = 8
    }

    private var cardHeight = 0
    private var activeCardTop = 0
    private var activeCardBottom = 0
    private var activeCardCenter = 0
    private var cardsGap = 0f
    private var transitionBottom = 0
    private var transitionDistance = 0
    private var transitionBottom2Center = 0f
    private var lm: CardSliderLayoutManager? = null

    //    private var previewView: View? = null
    private var previewView: CreditCardView? = null

    override fun onLayoutManagerInitialized(lm: CardSliderLayoutManager) {
        this.lm = lm
        cardHeight = lm.cardHeight
        activeCardTop = lm.activeCardTop
        activeCardBottom = lm.activeCardBottom
        activeCardCenter = lm.activeCardCenter
        cardsGap = lm.cardsGap
        transitionBottom = activeCardCenter
        transitionDistance = activeCardBottom - transitionBottom
        val centerBorder = (cardHeight.toFloat() - cardHeight.toFloat() * 0.95f) / 2.0f
        val bottomBorder = (cardHeight.toFloat() - cardHeight.toFloat() * 0.8f) / 2.0f
        val bottom2centerDistance =
            activeCardBottom.toFloat() + centerBorder - (activeCardBottom.toFloat() - bottomBorder)
        transitionBottom2Center = bottom2centerDistance - cardsGap
    }

    //    override fun updateView(view: View, position: Float) {
    override fun updateView(view: CreditCardView, position: Float) {

        var scale: Float
        val alpha: Float
        val z: Float
        var y: Float
        var prevViewScale: Float = 0f

/*        if (position < 0.0f) {
            prevViewScale = lm!!.getDecoratedTop(view).toFloat() / activeCardTop.toFloat()
            scale = 0.65f + 0.3f * prevViewScale
            alpha = 0.1f + prevViewScale
//            Log.e("LOG_TAG", "prevViewScale : $prevViewScale")

            z = 12.0f * prevViewScale
            y = 0.0f
//            skew = 270 -
        } else */
//        if (position <= 1f) {
//            scale = 0.8f //0.95f
//            alpha = 1.0f
//            z = 12.0f
//            y = 0.0f
//        } else {
        val prevTransition: Float
//            if (position < 2.0f) {
        val viewTop = lm!!.getDecoratedTop(view)
        prevTransition =
            (viewTop - activeCardCenter).toFloat() / (activeCardBottom - activeCardCenter).toFloat()
//                scale = 0.95f - 0.14999998f * prevTransition
        scale = 0.8f + 0.14999998f * prevTransition

        if (scale > 0.9) {
            scale = 0.9f
        } else if (scale < 0.8f) {
            scale = 0.8f
        }

        alpha = 1.0f
        z = 16.0f

        y =
            if (abs(transitionBottom2Center) < abs(transitionBottom2Center * (viewTop - transitionBottom).toFloat() / transitionDistance.toFloat())) {
                -transitionBottom2Center
            } else {
                -transitionBottom2Center * (viewTop - transitionBottom).toFloat() / transitionDistance.toFloat()
            }

//                y =  -transitionBottom2Center

        if (position < 0) {
//            Log.e("LOG_TAG", "-y : $y")
        } else {
//            Log.e("LOG_TAG", "+y : $y")
        }

//            }
//            else
//            {
//                scale = 0.8f
//                alpha = 1.0f
//                z = 8.0f
//                if (previewView != null && lm != null) {
//                    val isFirstBottom = lm!!.getDecoratedBottom(previewView!!) <= activeCardBottom
//                    val prevBottom: Int
//                    if (isFirstBottom) {
//                        prevViewScale = 1f /*0.95f*/
//                        prevBottom = activeCardBottom
//                        prevTransition = 0.0f
//                    } else {
//                        prevViewScale = ViewCompat.getScaleX(previewView)
//                        prevBottom = lm!!.getDecoratedBottom(previewView!!)
//                        prevTransition = ViewCompat.getTranslationX(previewView)
//                    }
//                    val prevBorder =
//                        (cardHeight.toFloat() - cardHeight.toFloat() * prevViewScale) / 2.0f
//                    val currentBorder = (cardHeight.toFloat() - cardHeight.toFloat() * 0.8f) / 2.0f
//                    val distance = lm!!.getDecoratedTop(view)
//                        .toFloat() + currentBorder - (prevBottom.toFloat() - prevBorder + prevTransition)
//                    val transition = distance - cardsGap
//                    y = -transition
//                } else {
//                    y = 0.0f
//                }
//            }
//        }

//        Log.e("LOG_TAG", "position : $position")
        view.scaleX = scale
        view.scaleY = scale
        view.z = z
//        view.translationY = y
        view.translationY = y - 400
        view.alpha = alpha

        val delta = when {
            position < 0f -> -90 * position * 4
            position < 2f -> 90 * position / 6
            position >= 2f -> 90f
            else -> 0f
        }

        var skew = 360f - delta
        if (position < -0.5) {
            skew = 270f
        } else if (skew < 330 && position > 0) {
            skew = 330f
        }

        view.scaleY = scale * skew / 360

        view.setSkew(skew, position)

        previewView = view
    }

    fun getLayoutManager(): CardSliderLayoutManager? {
        return lm
    }

}