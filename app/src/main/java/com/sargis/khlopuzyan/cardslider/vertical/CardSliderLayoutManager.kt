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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.sargis.khlopuzyan.cardslider.R
import com.sargis.khlopuzyan.cardslider.custom.CreditCardView
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.*
import kotlin.math.*


/**
 * Created by Sargis Khlopuzyan, on 6/4/2021.
 *
 * @author Sargis Khlopuzyan (sargis.khlopuzyan@fastshift.com)
 */
class CardSliderLayoutManager : LinearLayoutManager,
    RecyclerView.SmoothScroller.ScrollVectorProvider {

    companion object {
        private const val DEFAULT_ACTIVE_CARD_TOP_OFFSET = 148 //150
        private const val DEFAULT_CARD_HEIGHT = 148
        private const val DEFAULT_CARDS_GAP = 12
        private const val TOP_CARD_COUNT = 2
    }

    private val viewCache: SparseArray<CreditCardView> = SparseArray<CreditCardView>()
    private val cardsYCoords = SparseIntArray()

    var cardHeight = 0
    var activeCardTop = 0
    var activeCardBottom = 0
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
         * 0 is active card. 1 is first bottom card, and -1 is first top (stacked) card.
         */
//        fun updateView(view: View, position: Float)
        fun updateView(view: CreditCardView, position: Float)
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
     * See [R.styleable.CardSlider_activeCardTopOffset]
     * See [R.styleable.CardSlider_cardHeight]
     * See [R.styleable.CardSlider_cardsGap]
     */

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, VERTICAL, false) {
        val density: Float = context.resources.displayMetrics.density
        val defaultCardHeight = (DEFAULT_CARD_HEIGHT * density).toInt()
        val defaultActiveCardTop = (DEFAULT_ACTIVE_CARD_TOP_OFFSET * density).toInt()
        val defaultCardsGap = DEFAULT_CARDS_GAP * density
        if (attrs == null) {
            initialize(defaultActiveCardTop, defaultCardHeight, defaultCardsGap, null)
        } else {
            val attrCardHeight: Int
            val attrActiveCardTop: Int
            val attrCardsGap: Float
            val viewUpdateClassName: String
            val a: TypedArray =
                context.theme.obtainStyledAttributes(attrs, R.styleable.CardSlider, 0, 0)
            try {
                attrCardHeight =
                    a.getDimensionPixelSize(R.styleable.CardSlider_cardHeight, defaultCardHeight)
                attrActiveCardTop = a.getDimensionPixelSize(
                    R.styleable.CardSlider_activeCardTopOffset,
                    defaultActiveCardTop
                )
                attrCardsGap = a.getDimension(R.styleable.CardSlider_cardsGap, defaultCardsGap)
                viewUpdateClassName = a.getString(R.styleable.CardSlider_viewUpdater) ?: ""
            } finally {
                a.recycle()
            }
            val viewUpdater = loadViewUpdater(context, viewUpdateClassName, attrs)
            initialize(attrActiveCardTop, attrCardHeight, attrCardsGap, viewUpdater!!)
        }
    }

    /**
     * Creates CardSliderLayoutManager with specified values in pixels.
     *
     * @param activeCardTop    Active card offset from start of RecyclerView. Default value is 50dp.
     * @param cardHeight         Card height. Default value is 148dp.
     * @param cardsGap          Distance between cards. Default value is 12dp.
     */
    constructor(
        context: Context, // TODO - Added By Me
        activeCardTop: Int,
        cardHeight: Int,
        cardsGap: Float
    ) : super(
        context,
        VERTICAL,
        false
    ) {
        initialize(activeCardTop, cardHeight, cardsGap, null)
    }

    private fun initialize(top: Int, height: Int, gap: Float, updater: ViewUpdater?) {
        cardHeight = height
        activeCardTop = top
        activeCardBottom = activeCardTop + cardHeight
        activeCardCenter = activeCardTop + (activeCardBottom - activeCardTop) / 2
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
                val first: Int = removed.first
                val last: Int = removed.last
                val top = first - 1
                val bottom =
                    if (last == itemCount + removed.size - 1) RecyclerView.NO_POSITION else last
                anchorPos = max(top, bottom)
            }

            scrollRequestedPosition = anchorPos
        }

        detachAndScrapAttachedViews(recycler)
        fill(anchorPos, recycler, state)

        if (cardsYCoords.size() != 0) {
            layoutByCoords()
        }

        if (state.isPreLayout) {
            recyclerView?.postOnAnimationDelayed({ updateViewScale() }, 415)
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

    override fun canScrollVertically(): Boolean {
        return childCount != 0
    }

    override fun scrollToPosition(position: Int) {
        if (position < 0 || position >= itemCount) {
            return
        }
        scrollRequestedPosition = position
        requestLayout()
    }

    override fun scrollVerticallyBy(dy: Int, recycler: Recycler, state: RecyclerView.State): Int {
        scrollRequestedPosition = RecyclerView.NO_POSITION

        val delta: Int = if (dy < 0) {
            scrollBottom(max(dy, -cardHeight))
        } else {
            scrollTop(dy)
        }

        fill(getActiveCardPosition(), recycler, state)
        updateViewScale()
        cardsYCoords.clear()
        var i = 0
        val cnt = childCount
        while (i < cnt) {
            val view = getChildAt(i)
            cardsYCoords.put(getPosition(view!!), getDecoratedTop(view))
            i++
        }
        return delta
    }

    //TODO
    override fun computeScrollVectorForPosition(targetPosition: Int): PointF {
//        return PointF((targetPosition - getActiveCardPosition()).toFloat(), 0f)
        return PointF(0f, (targetPosition - getActiveCardPosition()).toFloat())
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
        val state = SavedState()
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
//            var biggestView: View? = null
            var biggestView: CreditCardView? = null
            var lastScaleY = 0f
            var i = 0
            val cnt = childCount
            while (i < cnt) {
                val child = getChildAt(i) as? CreditCardView
                val viewTop = getDecoratedTop(child!!)
                if (viewTop >= activeCardBottom) {
                    i++
                    continue
                }
                val scaleY = ViewCompat.getScaleY(child)
                if (lastScaleY < scaleY && viewTop < activeCardCenter) {
                    lastScaleY = scaleY
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
    fun getTopView(): CreditCardView? {
        if (childCount == 0) {
            return null
        }
        var result: CreditCardView? = null
        var lastValue = cardHeight.toFloat()
        var i = 0
        val cnt = childCount
        while (i < cnt) {
            val child = getChildAt(i) as? CreditCardView
            if (getDecoratedTop(child!!) >= activeCardBottom) {
                i++
                continue
            }
            val viewTop = getDecoratedTop(child)
            val diff = activeCardBottom - viewTop
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

            override fun calculateDyToMakeVisible(view: View, snapPreference: Int): Int {
//            override fun calculateDxToMakeVisible(view: View, snapPreference: Int): Int {
                val viewTop2 = getDecoratedTop(view)
                if (viewTop2 > activeCardTop) {
                    return activeCardTop - viewTop2
                } else {
                    var delta = 0
                    var topViewPos = 0
                    val topView = getTopView()
                    if (topView != null) {
                        topViewPos = getPosition(topView)
                        if (topViewPos != targetPosition) {
                            val topViewTop = getDecoratedTop(topView)
                            if (topViewTop in activeCardTop until activeCardBottom) {
                                delta = activeCardBottom - topViewTop
                            }
                        }
                    }
                    return delta + cardHeight * max(0, topViewPos - targetPosition - 1)
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
                attrs?.positionDescription.toString() + ": Error creating LayoutManager " + className,
                e
            )
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException(
                (attrs?.positionDescription.toString() + ": Unable to find ViewUpdater" + className),
                e
            )
        } catch (e: InvocationTargetException) {
            throw IllegalStateException(
                (attrs?.positionDescription.toString() + ": Could not instantiate the ViewUpdater: " + className),
                e
            )
        } catch (e: InstantiationException) {
            throw IllegalStateException(
                (attrs?.positionDescription.toString() + ": Could not instantiate the ViewUpdater: " + className),
                e
            )
        } catch (e: IllegalAccessException) {
            throw IllegalStateException(
                (attrs?.positionDescription.toString() + ": Cannot access non-public constructor " + className),
                e
            )
        } catch (e: ClassCastException) {
            throw IllegalStateException(
                (attrs?.positionDescription.toString() + ": Class is not a ViewUpdater " + className),
                e
            )
        }
        return updater
    }

    private fun scrollBottom(dy: Int): Int {
        val childCount = childCount
        if (childCount == 0) {
            return 0
        }
        val bottomestView = getChildAt(childCount - 1) as? CreditCardView
        val deltaBorder = activeCardTop + getPosition(bottomestView!!) * cardHeight
        val delta = getAllowedBottomDelta(bottomestView, dy, deltaBorder)
        val bottomViews: LinkedList<CreditCardView> = LinkedList()
        val topViews: LinkedList<CreditCardView> = LinkedList()
        for (i in childCount - 1 downTo 0) {
            val view = getChildAt(i) as? CreditCardView
            val viewTop = getDecoratedTop(view!!)
            if (viewTop >= activeCardBottom) {
                bottomViews.add(view)
            } else {
                topViews.add(view)
            }
        }
        for (view: CreditCardView in bottomViews) {
            val border = activeCardTop + getPosition(view) * cardHeight
            val allowedDelta = getAllowedBottomDelta(view, dy, border)
            view.offsetTopAndBottom(-allowedDelta)
        }
        val step = activeCardTop / TOP_CARD_COUNT
        val jDelta = floor((1f * delta * step / cardHeight).toDouble()).toInt()
        var prevView: CreditCardView? = null
        var j = 0
        var i = 0
        val cnt: Int = topViews.size
        while (i < cnt) {
            val view: CreditCardView = topViews[i]
            if (prevView == null || getDecoratedTop(prevView) >= activeCardBottom) {
                val border = activeCardTop + getPosition(view) * cardHeight
                val allowedDelta = getAllowedBottomDelta(view, dy, border)
                view.offsetTopAndBottom(-allowedDelta)
            } else {
                val border = activeCardTop - step * j
                view.offsetTopAndBottom(-getAllowedBottomDelta(view, jDelta, border))
                j++
            }
            prevView = view
            i++
        }
        return delta
    }

    private fun scrollTop(dy: Int): Int {
        val childCount = childCount
        if (childCount == 0) {
            return 0
        }
        val lastView = getChildAt(childCount - 1)
        val isLastItem = getPosition(lastView!!) == itemCount - 1
        val delta: Int
        if (isLastItem) {
            delta = min(dy, getDecoratedBottom(lastView) - activeCardBottom)
        } else {
            delta = dy
        }
        val step = activeCardTop / TOP_CARD_COUNT
        val jDelta = ceil((1f * delta * step / cardHeight).toDouble()).toInt()
        for (i in childCount - 1 downTo 0) {
            val view = getChildAt(i) as? CreditCardView
            val viewTop = getDecoratedTop(view!!)
            if (viewTop > activeCardTop) {
                view.offsetTopAndBottom(getAllowedTopDelta(view, delta, activeCardTop))
            } else {
                var border = activeCardTop - step
                for (j in i downTo 0) {
                    val jView = getChildAt(j) as? CreditCardView
                    jView!!.offsetTopAndBottom(getAllowedTopDelta(jView, jDelta, border))
                    border -= step
                }
                break
            }
        }
        return delta
    }

    private fun getAllowedTopDelta(view: CreditCardView, dy: Int, border: Int): Int {
        val viewTop = getDecoratedTop(view)
        return if (viewTop - dy > border) {
            -dy
        } else {
            border - viewTop
        }
    }

    private fun getAllowedBottomDelta(view: CreditCardView, dy: Int, border: Int): Int {
        val viewTop = getDecoratedTop(view)
        return if (viewTop + abs(dy) < border) {
            dy
        } else {
            viewTop - border
        }
    }

    private fun layoutByCoords() {
        val count = min(childCount, cardsYCoords.size())
        for (i in 0 until count) {
            val view = getChildAt(i)
            val viewTop = cardsYCoords[getPosition(view!!)]
            layoutDecorated(
                view,
                0,
                viewTop,
                getDecoratedRight(view), // TODO - OK
                viewTop + cardHeight
            )
        }
        cardsYCoords.clear()
    }

    private fun fill(anchorPos: Int, recycler: Recycler, state: RecyclerView.State) {
        viewCache.clear()
        run {
            var i: Int = 0
            val cnt: Int = childCount
            while (i < cnt) {
                val view: CreditCardView? = getChildAt(i) as? CreditCardView
                val pos: Int = getPosition(view!!)
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
            fillTop(anchorPos, recycler)
            fillBottom(anchorPos, recycler)
        }
        var i = 0
        val cnt = viewCache.size()
        while (i < cnt) {
            recycler.recycleView(viewCache.valueAt(i))
            i++
        }
    }

    private fun fillTop(anchorPos: Int, recycler: Recycler) {
        if (anchorPos == RecyclerView.NO_POSITION) {
            return
        }
        val layoutStep = activeCardTop / TOP_CARD_COUNT
        var pos = max(0, anchorPos - TOP_CARD_COUNT - 1)
        var viewTop = max(-1, TOP_CARD_COUNT - (anchorPos - pos)) * layoutStep
        while (pos < anchorPos) {
            var view = viewCache[pos]
            if (view != null) {
                attachView(view)
                viewCache.remove(pos)
            } else {
                view = recycler.getViewForPosition(pos) as? CreditCardView
                addView(view)
                measureChildWithMargins(view, 0, 0)
                val viewWidth2 = getDecoratedMeasuredWidth(view) // TODO - OK
                //TODO
                layoutDecorated(view, 0, viewTop, viewWidth2, viewTop + cardHeight)
            }
            viewTop += layoutStep
            pos++
        }
    }

    private fun fillBottom(anchorPos: Int, recycler: Recycler) {
        if (anchorPos == RecyclerView.NO_POSITION) {
            return
        }
        val height = height
        val itemCount = itemCount
        var pos = anchorPos
        var viewTop = activeCardTop
        var fillBottom = true
        while (fillBottom && pos < itemCount) {
            var view = viewCache[pos]
            if (view != null) {
                attachView(view)
                viewCache.remove(pos)
            } else {
                view = recycler.getViewForPosition(pos) as? CreditCardView
                addView(view)
                measureChildWithMargins(view, 0, 0)
                val viewWidth2 = getDecoratedMeasuredWidth(view) // TODO - OK
                layoutDecorated(view, 0, viewTop, viewWidth2, viewTop + cardHeight)
            }
            viewTop = getDecoratedBottom(view)
            fillBottom = viewTop < height + cardHeight
            pos++
        }
    }

    private fun updateViewScale() {
        var i = 0
        val cnt = childCount
        while (i < cnt) {
            val view = getChildAt(i) as? CreditCardView
            val viewTop = getDecoratedTop(view!!)
            val position = ((viewTop - activeCardTop).toFloat() / cardHeight)
            viewUpdater!!.updateView(view, position)
            i++
        }
    }
}