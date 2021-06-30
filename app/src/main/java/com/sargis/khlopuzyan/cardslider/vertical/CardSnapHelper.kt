package com.sargis.khlopuzyan.cardslider.vertical

import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider
import java.security.InvalidParameterException
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

/**
 * Created by Sargis Khlopuzyan, on 6/4/2021.
 *
 * @author Sargis Khlopuzyan (sargis.khlopuzyan@fastshift.com)
 */
class CardSnapHelper : LinearSnapHelper() {

    private var recyclerView: RecyclerView? = null

    @Throws(IllegalStateException::class)
    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        super.attachToRecyclerView(recyclerView)
        if (recyclerView != null && recyclerView.layoutManager !is CardSliderLayoutManager) {
            throw InvalidParameterException("LayoutManager must be instance of CardSliderLayoutManager")
        } else {
            this.recyclerView = recyclerView
        }
    }

    override fun findTargetSnapPosition(
        layoutManager: RecyclerView.LayoutManager,
        velocityX: Int,
        velocityY: Int
    ): Int {
        val lm = layoutManager as CardSliderLayoutManager
        val itemCount = lm.itemCount
        return if (itemCount == 0) {
            -1
        } else {
            val vectorProvider = layoutManager as ScrollVectorProvider
            val vectorForEnd = vectorProvider.computeScrollVectorForPosition(itemCount - 1)
            if (vectorForEnd == null) {
                -1
            } else {
                val distance = calculateScrollDistance(velocityX, velocityY)[0]
                var deltaJump: Int
                deltaJump = if (distance > 0) {
                    floor((distance / lm.cardHeight).toDouble()).toInt()
                } else {
                    ceil((distance / lm.cardHeight).toDouble()).toInt()
                }
                val deltaSign = Integer.signum(deltaJump)
                deltaJump = deltaSign * min(3, abs(deltaJump))
                if (vectorForEnd.x < 0.0f) {
                    deltaJump = -deltaJump
                }
                if (deltaJump == 0) {
                    -1
                } else {
                    val currentPosition = lm.getActiveCardPosition()
                    if (currentPosition == -1) {
                        -1
                    } else {
                        var targetPos = currentPosition + deltaJump
                        if (targetPos < 0 || targetPos >= itemCount) {
                            targetPos = -1
                        }
                        targetPos
                    }
                }
            }
        }
    }

    override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
        return (layoutManager as CardSliderLayoutManager).getTopView()
    }

    override fun calculateDistanceToFinalSnap(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View
    ): IntArray? {
        val lm = layoutManager as CardSliderLayoutManager
        val viewLeft = lm.getDecoratedLeft(targetView)
        val activeCardLeft = lm.activeCardTop
        val activeCardCenter = lm.activeCardTop + lm.cardHeight / 2
        val activeCardRight = lm.activeCardTop + lm.cardHeight
        val out = intArrayOf(0, 0)
        if (viewLeft < activeCardCenter) {
            val targetPos = lm.getPosition(targetView)
            val activeCardPos = lm.getActiveCardPosition()
            if (targetPos != activeCardPos) {
                out[0] = -(activeCardPos - targetPos) * lm.cardHeight
            } else {
                out[0] = viewLeft - activeCardLeft
            }
        } else {
            out[0] = viewLeft - activeCardRight + 1
        }
        if (out[0] != 0) {
            recyclerView!!.smoothScrollBy(out[0], 0, AccelerateInterpolator())
        }
        return intArrayOf(0, 0)
    }

    override fun createSnapScroller(layoutManager: RecyclerView.LayoutManager): LinearSmoothScroller {
        return (layoutManager as CardSliderLayoutManager).getSmoothScroller(
            recyclerView!!
        )
    }

}