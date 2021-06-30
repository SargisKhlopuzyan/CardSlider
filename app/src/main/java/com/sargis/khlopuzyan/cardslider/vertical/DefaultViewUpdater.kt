package com.sargis.khlopuzyan.cardslider.vertical

import android.view.View
import androidx.core.view.ViewCompat
import kotlin.math.abs


/**
 * Created by Sargis Khlopuzyan, on 6/4/2021.
 *
 * @author Sargis Khlopuzyan (sargis.khlopuzyan@fastshift.com)
 */
open class DefaultViewUpdater : CardSliderLayoutManager.ViewUpdater{

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
    private var transitionEnd = 0
    private var transitionDistance = 0
    private var transitionBottom2Center = 0f
    private var lm: CardSliderLayoutManager? = null
    private var previewView: View? = null


    override fun onLayoutManagerInitialized(lm: CardSliderLayoutManager) {
        this.lm = lm
        cardHeight = lm.cardHeight
        activeCardTop = lm.activeCardTop
        activeCardBottom = lm.activeCardBottom
        activeCardCenter = lm.activeCardCenter
        cardsGap = lm.cardsGap
        transitionEnd = activeCardCenter
        transitionDistance = activeCardBottom - transitionEnd
        val centerBorder = (cardHeight.toFloat() - cardHeight.toFloat() * 0.95f) / 2.0f
        val bottomBorder = (cardHeight.toFloat() - cardHeight.toFloat() * 0.8f) / 2.0f
        val bottom2centerDistance =
            activeCardBottom.toFloat() + centerBorder - (activeCardBottom.toFloat() - bottomBorder)
        transitionBottom2Center = bottom2centerDistance - cardsGap
    }

    override fun updateView(view: View, position: Float) {
        val scale: Float
        val alpha: Float
        val z: Float
        val y: Float
        val prevViewScale: Float
        if (position < 0.0f) {
            prevViewScale = lm!!.getDecoratedTop(view).toFloat() / activeCardTop.toFloat()
            scale = 0.65f + 0.3f * prevViewScale
            alpha = 0.1f + prevViewScale
            z = 12.0f * prevViewScale
            y = 0.0f
        } else if (position < 0.5f) {
            scale = 0.95f
            alpha = 1.0f
            z = 12.0f
            y = 0.0f
        } else {
            val prevTransition: Float
            if (position < 1.0f) {
                val viewTop = lm!!.getDecoratedTop(view)
                prevTransition =
                    (viewTop - activeCardCenter).toFloat() / (activeCardBottom - activeCardCenter).toFloat()
                scale = 0.95f - 0.14999998f * prevTransition
                alpha = 1.0f
                z = 16.0f
                y = if (abs(transitionBottom2Center) < abs(
                        transitionBottom2Center * (viewTop - transitionEnd).toFloat() / transitionDistance.toFloat()
                    )
                ) {
                    -transitionBottom2Center
                } else {
                    -transitionBottom2Center * (viewTop - transitionEnd).toFloat() / transitionDistance.toFloat()
                }
            } else {
                scale = 0.8f
                alpha = 1.0f
                z = 8.0f
                if (previewView != null && lm != null) {
                    val isFirstBottom = lm!!.getDecoratedBottom(previewView!!) <= activeCardBottom
                    val prevBottom: Int
                    if (isFirstBottom) {
                        prevViewScale = 0.95f
                        prevBottom = activeCardBottom
                        prevTransition = 0.0f
                    } else {
                        prevViewScale = ViewCompat.getScaleX(previewView)
                        prevBottom = lm!!.getDecoratedBottom(previewView!!)
                        prevTransition = ViewCompat.getTranslationX(previewView)
                    }
                    val prevBorder =
                        (cardHeight.toFloat() - cardHeight.toFloat() * prevViewScale) / 2.0f
                    val currentBorder = (cardHeight.toFloat() - cardHeight.toFloat() * 0.8f) / 2.0f
                    val distance = lm!!.getDecoratedTop(view).toFloat() + currentBorder - (prevBottom.toFloat() - prevBorder + prevTransition)
                    val transition = distance - cardsGap
                    y = -transition
                } else {
                    y = 0.0f
                }
            }
        }
        ViewCompat.setScaleX(view, scale)
        ViewCompat.setScaleY(view, scale)
        ViewCompat.setZ(view, z)
        ViewCompat.setTranslationY(view, y)
        ViewCompat.setAlpha(view, alpha)
        previewView = view
    }

    fun getLayoutManager(): CardSliderLayoutManager? {
        return lm
    }
}