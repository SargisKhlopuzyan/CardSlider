package com.sargis.khlopuzyan.cardslider.vertical


import android.content.Context
import android.content.res.TypedArray
import android.graphics.PointF
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.SparseArray
import android.util.SparseIntArray
import android.view.View
import androidx.annotation.Nullable
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.sargis.khlopuzyan.cardslider.R
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.*
import kotlin.math.*


/**
 * Created by Sargis Khlopuzyan, on 6/4/2021.
 *
 * @author Sargis Khlopuzyan (sargis.khlopuzyan@fastshift.com)
 */
class CardSliderLayoutManager : RecyclerView.LayoutManager, RecyclerView.SmoothScroller.ScrollVectorProvider {

    companion object {
        private const val DEFAULT_ACTIVE_CARD_LEFT_OFFSET = 50
        private const val DEFAULT_CARD_WIDTH = 148
        private const val DEFAULT_CARDS_GAP = 12
        private const val LEFT_CARD_COUNT = 2
    }

    private val viewCache: SparseArray<View> = SparseArray<View>()
    private val cardsXCoords = SparseIntArray()

    var cardWidth = 0
    var activeCardLeft = 0
    var activeCardRight = 0
    var activeCardCenter = 0

    var cardsGap = 0f

    private var scrollRequestedPosition = 0

    private var viewUpdater: ViewUpdater? = null
    private var recyclerView: RecyclerView? = null

    /**
     * A ViewUpdater is invoked whenever a visible/attached card is scrolled.
     */
    interface ViewUpdater {
        /**
         * Called when CardSliderLayoutManager initialized
         */
        fun onLayoutManagerInitialized(lm: CardSliderLayoutManager)

        /**
         * Called on view update (scroll, layout).
         * @param view      Updating view
         * @param position  Position of card relative to the current active card position of the layout manager.
         * 0 is active card. 1 is first right card, and -1 is first left (stacked) card.
         */
        fun updateView(view: View, position: Float)
    }

    class SavedState : Parcelable {
        var anchorPos = 0

        constructor()

        constructor(parcel: Parcel) {
            anchorPos = parcel.readInt()
        }

        constructor(other: SavedState) {
            anchorPos = other.anchorPos
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(parcel: Parcel, i: Int) {
            parcel.writeInt(anchorPos)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }


    /**
     * Creates CardSliderLayoutManager with default values
     *
     * @param context   Current context, will be used to access resources.
     */
    constructor(context: Context) : this(context, null, 0, 0)

    /**
     * Constructor used when layout manager is set in XML by RecyclerView attribute
     * "layoutManager".
     *
     * See [R.styleable.CardSlider_activeCardLeftOffset]
     * See [R.styleable.CardSlider_cardWidth]
     * See [R.styleable.CardSlider_cardsGap]
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {
        val density: Float = context.resources.displayMetrics.density
        val defaultCardWidth = (DEFAULT_CARD_WIDTH * density).toInt()
        val defaultActiveCardLeft = (DEFAULT_ACTIVE_CARD_LEFT_OFFSET * density).toInt()
        val defaultCardsGap = DEFAULT_CARDS_GAP * density
        if (attrs == null) {
            initialize(defaultActiveCardLeft, defaultCardWidth, defaultCardsGap, null)
        } else {
            val attrCardWidth: Int
            val attrActiveCardLeft: Int
            val attrCardsGap: Float
            val viewUpdateClassName: String
            val a: TypedArray =
                context.theme.obtainStyledAttributes(attrs, R.styleable.CardSlider, 0, 0)
            try {
                attrCardWidth =
                    a.getDimensionPixelSize(R.styleable.CardSlider_cardWidth, defaultCardWidth)
                attrActiveCardLeft = a.getDimensionPixelSize(
                    R.styleable.CardSlider_activeCardLeftOffset,
                    defaultActiveCardLeft
                )
                attrCardsGap = a.getDimension(R.styleable.CardSlider_cardsGap, defaultCardsGap)
                viewUpdateClassName = a.getString(R.styleable.CardSlider_viewUpdater) ?: ""
            } finally {
                a.recycle()
            }
            val viewUpdater = loadViewUpdater(context, viewUpdateClassName, attrs)
            initialize(attrActiveCardLeft, attrCardWidth, attrCardsGap, viewUpdater!!)
        }
    }

    /**
     * Creates CardSliderLayoutManager with specified values in pixels.
     *
     * @param activeCardLeft    Active card offset from start of RecyclerView. Default value is 50dp.
     * @param cardWidth         Card width. Default value is 148dp.
     * @param cardsGap          Distance between cards. Default value is 12dp.
     */
    constructor(activeCardLeft: Int, cardWidth: Int, cardsGap: Float) {
        initialize(activeCardLeft, cardWidth, cardsGap, null)
    }

    private fun initialize(left: Int, width: Int, gap: Float, updater: ViewUpdater?) {
        cardWidth = width
        activeCardLeft = left
        activeCardRight = activeCardLeft + cardWidth
        activeCardCenter = activeCardLeft + (activeCardRight - activeCardLeft) / 2
        cardsGap = gap
        viewUpdater = updater
        if (viewUpdater == null) {
            viewUpdater = DefaultViewUpdater()
        }
        viewUpdater?.onLayoutManagerInitialized(this)
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.WRAP_CONTENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )
    }


    override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        if (itemCount == 0) {
            removeAndRecycleAllViews(recycler)
            return
        }
        if (childCount == 0 && state.isPreLayout) {
            return
        }
        var anchorPos = getActiveCardPosition()
        if (state.isPreLayout) {
            val removed: LinkedList<Int> = LinkedList()
            var i = 0
            val cnt = childCount
            while (i < cnt) {
                val child = getChildAt(i)
                val isRemoved =
                    (child!!.layoutParams as RecyclerView.LayoutParams).isItemRemoved
                if (isRemoved) {
                    removed.add(getPosition(child))
                }
                i++
            }
            if (removed.contains(anchorPos)) {
                val first: Int = removed.getFirst()
                val last: Int = removed.getLast()
                val left = first - 1
                val right =
                    if (last == itemCount + removed.size - 1) RecyclerView.NO_POSITION else last
                anchorPos = Math.max(left, right)
            }
            scrollRequestedPosition = anchorPos
        }
        detachAndScrapAttachedViews(recycler)
        fill(anchorPos, recycler, state)
        if (cardsXCoords.size() != 0) {
            layoutByCoords()
        }
        if (state.isPreLayout) {
            recyclerView!!.postOnAnimationDelayed({ updateViewScale() }, 415)
        } else {
            updateViewScale()
        }
    }

    override fun supportsPredictiveItemAnimations(): Boolean {
        return true
    }

    override fun onAdapterChanged(
        oldAdapter: RecyclerView.Adapter<*>?,
        newAdapter: RecyclerView.Adapter<*>?
    ) {
        removeAllViews()
    }

    override fun canScrollHorizontally(): Boolean {
        return childCount != 0
    }

    override fun scrollToPosition(position: Int) {
        if (position < 0 || position >= itemCount) {
            return
        }
        scrollRequestedPosition = position
        requestLayout()
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: Recycler, state: RecyclerView.State): Int {
        scrollRequestedPosition = RecyclerView.NO_POSITION
        val delta: Int
        if (dx < 0) {
            delta = scrollRight(Math.max(dx, -cardWidth))
        } else {
            delta = scrollLeft(dx)
        }
        fill(getActiveCardPosition(), recycler, state)
        updateViewScale()
        cardsXCoords.clear()
        var i = 0
        val cnt = childCount
        while (i < cnt) {
            val view = getChildAt(i)
            cardsXCoords.put(getPosition(view!!), getDecoratedLeft(view))
            i++
        }
        return delta
    }

    override fun computeScrollVectorForPosition(targetPosition: Int): PointF {
        return PointF((targetPosition - getActiveCardPosition()).toFloat(), 0f)
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State?,
        position: Int
    ) {
        if (position < 0 || position >= itemCount) {
            return
        }
        val scroller = getSmoothScroller(recyclerView)
        scroller.targetPosition = position
        startSmoothScroll(scroller)
    }

    override fun onItemsRemoved(recyclerView: RecyclerView, positionStart: Int, count: Int) {
        val anchorPos = getActiveCardPosition()
        if (positionStart + count <= anchorPos) {
            scrollRequestedPosition = anchorPos - 1
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val state: SavedState = SavedState()
        state.anchorPos = getActiveCardPosition()
        return state
    }

    override fun onRestoreInstanceState(parcelable: Parcelable) {
        if (parcelable is SavedState) {
            scrollRequestedPosition = parcelable.anchorPos
            requestLayout()
        }
    }

    override fun onAttachedToWindow(view: RecyclerView) {
        super.onAttachedToWindow(view)
        recyclerView = view
    }

    override fun onDetachedFromWindow(view: RecyclerView?, recycler: Recycler?) {
        super.onDetachedFromWindow(view, recycler)
        recyclerView = null
    }

    /**
     * @return active card position or RecyclerView.NO_POSITION
     */
    fun getActiveCardPosition(): Int {
        if (scrollRequestedPosition != RecyclerView.NO_POSITION) {
            return scrollRequestedPosition
        } else {
            var result = RecyclerView.NO_POSITION
            var biggestView: View? = null
            var lastScaleX = 0f
            var i = 0
            val cnt = childCount
            while (i < cnt) {
                val child = getChildAt(i)
                val viewLeft = getDecoratedLeft(child!!)
                if (viewLeft >= activeCardRight) {
                    i++
                    continue
                }
                val scaleX = ViewCompat.getScaleX(child)
                if (lastScaleX < scaleX && viewLeft < activeCardCenter) {
                    lastScaleX = scaleX
                    biggestView = child
                }
                i++
            }
            if (biggestView != null) {
                result = getPosition(biggestView)
            }
            return result
        }
    }

    @Nullable
    fun getTopView(): View? {
        if (childCount == 0) {
            return null
        }
        var result: View? = null
        var lastValue = cardWidth.toFloat()
        var i = 0
        val cnt = childCount
        while (i < cnt) {
            val child = getChildAt(i)
            if (getDecoratedLeft(child!!) >= activeCardRight) {
                i++
                continue
            }
            val viewLeft = getDecoratedLeft(child)
            val diff = activeCardRight - viewLeft
            if (diff < lastValue) {
                lastValue = diff.toFloat()
                result = child
            }
            i++
        }
        return result
    }

    fun getSmoothScroller(recyclerView: RecyclerView): LinearSmoothScroller {
        return object : LinearSmoothScroller(recyclerView.context) {
            override fun calculateDxToMakeVisible(view: View, snapPreference: Int): Int {
                val viewStart = getDecoratedLeft(view)
                if (viewStart > activeCardLeft) {
                    return activeCardLeft - viewStart
                } else {
                    var delta = 0
                    var topViewPos = 0
                    val topView = getTopView()
                    if (topView != null) {
                        topViewPos = getPosition(topView)
                        if (topViewPos != targetPosition) {
                            val topViewLeft = getDecoratedLeft(topView)
                            if (topViewLeft >= activeCardLeft && topViewLeft < activeCardRight) {
                                delta = activeCardRight - topViewLeft
                            }
                        }
                    }
                    return delta + cardWidth * Math.max(0, topViewPos - targetPosition - 1)
                }
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return 0.5f
            }
        }
    }

    private fun loadViewUpdater(
        context: Context,
        className: String?,
        attrs: AttributeSet?
    ): ViewUpdater? {
        if (className == null || className.trim { it <= ' ' }.isEmpty()) {
            return null
        }
        val fullClassName: String
        if (className[0] == '.') {
            fullClassName = context.packageName.toString() + className
        } else if (className.contains(".")) {
            fullClassName = className
        } else {
            fullClassName =
                CardSliderLayoutManager::class.java.getPackage().name + '.' + className
        }
        val updater: ViewUpdater?
        try {
            val classLoader: ClassLoader = context.classLoader
            val viewUpdaterClass = classLoader.loadClass(fullClassName).asSubclass(
                ViewUpdater::class.java
            )
            val constructor: Constructor<out ViewUpdater?> = viewUpdaterClass.getConstructor()
            constructor.isAccessible = true
            updater = constructor.newInstance()
        } catch (e: NoSuchMethodException) {
            throw IllegalStateException(
                attrs?.positionDescription.toString() + ": Error creating LayoutManager " + className, e
            )
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException(
                (attrs?.positionDescription.toString() + ": Unable to find ViewUpdater" + className), e
            )
        } catch (e: InvocationTargetException) {
            throw IllegalStateException(
                (attrs?.positionDescription.toString() + ": Could not instantiate the ViewUpdater: " + className), e
            )
        } catch (e: InstantiationException) {
            throw IllegalStateException(
                (attrs?.positionDescription.toString() + ": Could not instantiate the ViewUpdater: " + className), e
            )
        } catch (e: IllegalAccessException) {
            throw IllegalStateException(
                (attrs?.positionDescription.toString() + ": Cannot access non-public constructor " + className), e
            )
        } catch (e: ClassCastException) {
            throw IllegalStateException(
                (attrs?.positionDescription.toString() + ": Class is not a ViewUpdater " + className), e
            )
        }
        return updater
    }

    private fun scrollRight(dx: Int): Int {
        val childCount = childCount
        if (childCount == 0) {
            return 0
        }
        val rightestView = getChildAt(childCount - 1)
        val deltaBorder = activeCardLeft + getPosition((rightestView)!!) * cardWidth
        val delta = getAllowedRightDelta((rightestView), dx, deltaBorder)
        val rightViews: LinkedList<View> = LinkedList()
        val leftViews: LinkedList<View> = LinkedList()
        for (i in childCount - 1 downTo 0) {
            val view = getChildAt(i)
            val viewLeft = getDecoratedLeft((view)!!)
            if (viewLeft >= activeCardRight) {
                rightViews.add(view)
            } else {
                leftViews.add(view)
            }
        }
        for (view: View in rightViews) {
            val border = activeCardLeft + getPosition(view) * cardWidth
            val allowedDelta = getAllowedRightDelta(view, dx, border)
            view.offsetLeftAndRight(-allowedDelta)
        }
        val step = activeCardLeft / LEFT_CARD_COUNT
        val jDelta = floor((1f * delta * step / cardWidth).toDouble()).toInt()
        var prevView: View? = null
        var j = 0
        var i = 0
        val cnt: Int = leftViews.size
        while (i < cnt) {
            val view: View = leftViews[i]
            if (prevView == null || getDecoratedLeft(prevView) >= activeCardRight) {
                val border = activeCardLeft + getPosition(view) * cardWidth
                val allowedDelta = getAllowedRightDelta(view, dx, border)
                view.offsetLeftAndRight(-allowedDelta)
            } else {
                val border = activeCardLeft - step * j
                view.offsetLeftAndRight(-getAllowedRightDelta(view, jDelta, border))
                j++
            }
            prevView = view
            i++
        }
        return delta
    }

    private fun scrollLeft(dx: Int): Int {
        val childCount = childCount
        if (childCount == 0) {
            return 0
        }
        val lastView = getChildAt(childCount - 1)
        val isLastItem = getPosition((lastView)!!) == itemCount - 1
        val delta: Int
        if (isLastItem) {
            delta = Math.min(dx, getDecoratedRight((lastView)) - activeCardRight)
        } else {
            delta = dx
        }
        val step = activeCardLeft / LEFT_CARD_COUNT
        val jDelta = ceil((1f * delta * step / cardWidth).toDouble()).toInt()
        for (i in childCount - 1 downTo 0) {
            val view = getChildAt(i)
            val viewLeft = getDecoratedLeft((view)!!)
            if (viewLeft > activeCardLeft) {
                view.offsetLeftAndRight(getAllowedLeftDelta((view), delta, activeCardLeft))
            } else {
                var border = activeCardLeft - step
                for (j in i downTo 0) {
                    val jView = getChildAt(j)
                    jView!!.offsetLeftAndRight(getAllowedLeftDelta((jView), jDelta, border))
                    border -= step
                }
                break
            }
        }
        return delta
    }

    private fun getAllowedLeftDelta(view: View, dx: Int, border: Int): Int {
        val viewLeft = getDecoratedLeft(view)
        return if (viewLeft - dx > border) {
            -dx
        } else {
            border - viewLeft
        }
    }

    private fun getAllowedRightDelta(view: View, dx: Int, border: Int): Int {
        val viewLeft = getDecoratedLeft(view)
        return if (viewLeft + abs(dx) < border) {
            dx
        } else {
            viewLeft - border
        }
    }

    private fun layoutByCoords() {
        val count = min(childCount, cardsXCoords.size())
        for (i in 0 until count) {
            val view = getChildAt(i)
            val viewLeft = cardsXCoords[getPosition((view)!!)]
            layoutDecorated(
                (view),
                viewLeft,
                0,
                viewLeft + cardWidth,
                getDecoratedBottom((view))
            )
        }
        cardsXCoords.clear()
    }

    private fun fill(anchorPos: Int, recycler: Recycler, state: RecyclerView.State) {
        viewCache.clear()
        run {
            var i: Int = 0
            val cnt: Int = childCount
            while (i < cnt) {
                val view: View? = getChildAt(i)
                val pos: Int = getPosition((view)!!)
                viewCache.put(pos, view)
                i++
            }
        }
        run {
            var i: Int = 0
            val cnt: Int = viewCache.size()
            while (i < cnt) {
                detachView(viewCache.valueAt(i))
                i++
            }
        }
        if (!state.isPreLayout) {
            fillLeft(anchorPos, recycler)
            fillRight(anchorPos, recycler)
        }
        var i = 0
        val cnt = viewCache.size()
        while (i < cnt) {
            recycler.recycleView(viewCache.valueAt(i))
            i++
        }
    }

    private fun fillLeft(anchorPos: Int, recycler: Recycler) {
        if (anchorPos == RecyclerView.NO_POSITION) {
            return
        }
        val layoutStep = activeCardLeft / LEFT_CARD_COUNT
        var pos = max(0, anchorPos - LEFT_CARD_COUNT - 1)
        var viewLeft = max(-1, LEFT_CARD_COUNT - (anchorPos - pos)) * layoutStep
        while (pos < anchorPos) {
            var view = viewCache[pos]
            if (view != null) {
                attachView(view)
                viewCache.remove(pos)
            } else {
                view = recycler.getViewForPosition(pos)
                addView(view)
                measureChildWithMargins(view, 0, 0)
                val viewHeight = getDecoratedMeasuredHeight(view)
                layoutDecorated(view, viewLeft, 0, viewLeft + cardWidth, viewHeight)
            }
            viewLeft += layoutStep
            pos++
        }
    }

    private fun fillRight(anchorPos: Int, recycler: Recycler) {
        if (anchorPos == RecyclerView.NO_POSITION) {
            return
        }
        val width = width
        val itemCount = itemCount
        var pos = anchorPos
        var viewLeft = activeCardLeft
        var fillRight = true
        while (fillRight && pos < itemCount) {
            var view = viewCache[pos]
            if (view != null) {
                attachView(view)
                viewCache.remove(pos)
            } else {
                view = recycler.getViewForPosition(pos)
                addView(view)
                measureChildWithMargins(view, 0, 0)
                val viewHeight = getDecoratedMeasuredHeight(view)
                layoutDecorated(view, viewLeft, 0, viewLeft + cardWidth, viewHeight)
            }
            viewLeft = getDecoratedRight(view)
            fillRight = viewLeft < width + cardWidth
            pos++
        }
    }

    private fun updateViewScale() {
        var i = 0
        val cnt = childCount
        while (i < cnt) {
            val view = getChildAt(i)
            val viewLeft = getDecoratedLeft((view)!!)
            val position = ((viewLeft - activeCardLeft).toFloat() / cardWidth)
            viewUpdater!!.updateView((view), position)
            i++
        }
    }
}