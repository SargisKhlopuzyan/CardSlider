package com.sargis.khlopuzyan.cardslider.vertical

import android.util.Log
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
                val distance = calculateScrollDistance(velocityX, velocityY)[1] // 0
//                Log.e("LOG_TAG", "distance : $distance")
                var deltaJump: Int
                deltaJump = if (distance > 0) {
                    floor((distance / lm.cardHeight).toDouble()).toInt()
                } else {
                    ceil((distance / lm.cardHeight).toDouble()).toInt()
                }
                val deltaSign = Integer.signum(deltaJump)
                deltaJump = deltaSign * min(3, abs(deltaJump))
                Log.e("LOG_TAG", "deltaJump : $deltaJump")

                if (vectorForEnd.y < 0.0f) {
                    deltaJump = -deltaJump
                }
                if (deltaJump == 0) {
//                    Log.e("LOG_TAG", "-1 : -1")
                    RecyclerView.NO_POSITION
                } else {
                    val currentPosition = lm.getActiveCardPosition()
                    if (currentPosition == RecyclerView.NO_POSITION) {
                        RecyclerView.NO_POSITION
                    } else {
                        var targetPos = currentPosition + deltaJump
                        if (targetPos < 0 || targetPos >= itemCount) {
                            targetPos = RecyclerView.NO_POSITION
                        }
//                        Log.e("LOG_TAG", "targetPos : $targetPos")
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
    ): IntArray {
        val lm = layoutManager as CardSliderLayoutManager
        val viewTop = lm.getDecoratedTop(targetView)
        val activeCardTop = lm.activeCardTop
        val activeCardCenter = lm.activeCardTop + lm.cardHeight / 2
        val activeCardBottom = lm.activeCardTop + lm.cardHeight
        val out = intArrayOf(0, 0)
        if (viewTop < activeCardCenter) {
            val targetPos = lm.getPosition(targetView)
            val activeCardPos = lm.getActiveCardPosition()
            if (targetPos != activeCardPos) {
                out[1] = -(activeCardPos - targetPos) * lm.cardHeight
            } else {
                out[1] = viewTop - activeCardTop
            }
        } else {
            out[1] = viewTop - activeCardBottom + 1
        }

        if (out[1] != 0) {
            recyclerView?.smoothScrollBy(0, out[1], AccelerateInterpolator())
        }

        return intArrayOf(0, 0)
    }

    override fun createSnapScroller(layoutManager: RecyclerView.LayoutManager): LinearSmoothScroller {
        return (layoutManager as CardSliderLayoutManager).getSmoothScroller(
            recyclerView!!
        )
    }

}