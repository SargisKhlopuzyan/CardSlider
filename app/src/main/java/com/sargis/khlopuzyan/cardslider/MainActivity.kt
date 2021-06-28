package com.sargis.khlopuzyan.cardslider


import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewTreeObserver
import android.widget.*
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.sargis.khlopuzyan.cardslider.cards.SliderAdapter
import com.sargis.khlopuzyan.cardslider.utils.DecodeBitmapTask
import com.sargis.khlopuzyan.cardslider.vertical.CardSliderLayoutManager
import com.sargis.khlopuzyan.cardslider.vertical.CardSnapHelper
import java.util.*
import kotlin.math.max

class MainActivity : AppCompatActivity() {


    //    private val dotCoords: Array<Array<Int>> = Array(2,0) // int[5][2]
    private val dotCoords = Array(2) { IntArray(5) }

    private val pics: IntArray =
        intArrayOf(R.drawable.p1, R.drawable.p2, R.drawable.p3, R.drawable.p4, R.drawable.p5)
    private val maps: IntArray = intArrayOf(
        R.drawable.map_paris,
        R.drawable.map_seoul,
        R.drawable.map_london,
        R.drawable.map_beijing,
        R.drawable.map_greece
    )
    private val descriptions: IntArray =
        intArrayOf(R.string.text1, R.string.text2, R.string.text3, R.string.text4, R.string.text5)
    private val countries = arrayOf("PARIS", "SEOUL", "LONDON", "BEIJING", "THIRA")
    private val places =
        arrayOf("The Louvre", "Gwanghwamun", "Tower Bridge", "Temple of Heaven", "Aegeana Sea")
    private val temperatures = arrayOf("21°C", "19°C", "17°C", "23°C", "20°C")
    private val times = arrayOf(
        "Aug 1 - Dec 15    7:00-18:00",
        "Sep 5 - Nov 10    8:00-16:00",
        "Mar 8 - May 21    7:00-18:00"
    )

    private var sliderAdapter: SliderAdapter = SliderAdapter(pics, 120, OnCardClickListener())

    private lateinit var layoutManger: CardSliderLayoutManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var mapSwitcher: ImageSwitcher
    private lateinit var temperatureSwitcher: TextSwitcher
    private lateinit var placeSwitcher: TextSwitcher
    private lateinit var clockSwitcher: TextSwitcher
    private lateinit var descriptionsSwitcher: TextSwitcher
    private lateinit var greenDot: View

    private lateinit var country1TextView: TextView
    private lateinit var country2TextView: TextView
    private var countryOffset1: Int = 0
    private var countryOffset2: Int = 0
    private var countryAnimDuration: Long = 0
    private var currentPosition: Int = 0

    private var decodeMapBitmapTask: DecodeBitmapTask? = null
    private lateinit var mapLoadListener: DecodeBitmapTask.Listener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initRecyclerView()
//        initCountryText()
//        initSwitchers()
//        initGreenDot()
    }

    private fun initRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.adapter = sliderAdapter
        recyclerView.setHasFixedSize(true)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    onActiveCardChange()
                }
            }
        })

        layoutManger = recyclerView.layoutManager as CardSliderLayoutManager

        CardSnapHelper().attachToRecyclerView(recyclerView)
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing && decodeMapBitmapTask != null) {
            decodeMapBitmapTask?.cancel(true)
        }
    }

    private fun initSwitchers() {
        temperatureSwitcher = findViewById(R.id.ts_temperature)
        temperatureSwitcher.setFactory(TextViewFactory(R.style.TemperatureTextView, true))
        temperatureSwitcher.setCurrentText(temperatures[0])

        placeSwitcher = findViewById(R.id.ts_place)
        placeSwitcher.setFactory(TextViewFactory(R.style.PlaceTextView, false))
        placeSwitcher.setCurrentText(places[0])

        clockSwitcher = findViewById(R.id.ts_clock)
        clockSwitcher.setFactory(TextViewFactory(R.style.ClockTextView, false))
        clockSwitcher.setCurrentText(times[0])

        descriptionsSwitcher = findViewById(R.id.ts_description)
        descriptionsSwitcher.setInAnimation(this, android.R.anim.fade_in)
        descriptionsSwitcher.setOutAnimation(this, android.R.anim.fade_out)
        descriptionsSwitcher.setFactory(TextViewFactory(R.style.DescriptionTextView, false))
        descriptionsSwitcher.setCurrentText(getString(descriptions[0]))

        mapSwitcher = findViewById(R.id.ts_map)
        mapSwitcher.setInAnimation(this, R.anim.fade_in)
        mapSwitcher.setOutAnimation(this, R.anim.fade_out)
        mapSwitcher.setFactory(ImageViewFactory())
        mapSwitcher.setImageResource(maps[0])

        mapLoadListener = object : DecodeBitmapTask.Listener {

            override fun onPostExecuted(bitmap: Bitmap) {
                (mapSwitcher.nextView as? ImageView)?.setImageBitmap(bitmap)
                mapSwitcher.showNext()
            }
        }
    }

    private fun initCountryText() {
        countryAnimDuration = resources.getInteger(R.integer.labels_animation_duration).toLong()
        countryOffset1 = resources.getDimensionPixelSize(R.dimen.left_offset)
        countryOffset2 = resources.getDimensionPixelSize(R.dimen.card_width)
        country1TextView = findViewById(R.id.tv_country_1)
        country2TextView = findViewById(R.id.tv_country_2)

        country1TextView.x = countryOffset1.toFloat()
        country2TextView.x = countryOffset2.toFloat()
        country1TextView.text = countries[0]
        country2TextView.alpha = 0f

        country1TextView.typeface = Typeface.createFromAsset(
            assets,
            "open-sans-extrabold.ttf"
        )
        country2TextView.typeface = Typeface.createFromAsset(
            assets,
            "open-sans-extrabold.ttf"
        )
    }

    private fun initGreenDot() {
        mapSwitcher.viewTreeObserver?.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mapSwitcher.viewTreeObserver?.removeOnGlobalLayoutListener(this)

                val viewLeft = mapSwitcher.left
                val viewTop = mapSwitcher.top + mapSwitcher.height / 3

                val border = 100
                val xRange = max(1, mapSwitcher.width - border * 2)
                val yRange = max(1, (mapSwitcher.height / 3) * 2 - border * 2)

                val rnd = Random()

                val cnt = dotCoords.size
                for (i in 0 until cnt) {
                    dotCoords[i][0] = viewLeft + border + rnd.nextInt(xRange)
                    dotCoords[i][1] = viewTop + border + rnd.nextInt(yRange)
                }

                greenDot = findViewById(R.id.green_dot)
                greenDot.x = dotCoords[0][0].toFloat()
                greenDot.y = dotCoords[0][1].toFloat()
            }
        })
    }

    private fun setCountryText(text: String, left2right: Boolean) {
        val invisibleText: TextView
        val visibleText: TextView
        if (country1TextView.alpha > country2TextView.alpha) {
            visibleText = country1TextView
            invisibleText = country2TextView
        } else {
            visibleText = country2TextView
            invisibleText = country1TextView
        }

        val vOffset: Int
        if (left2right) {
            invisibleText.x = 0f
            vOffset = countryOffset2
        } else {
            invisibleText.x = countryOffset2.toFloat()
            vOffset = 0
        }

        invisibleText.text = text

        val iAlpha: ObjectAnimator = ObjectAnimator.ofFloat(invisibleText, "alpha", 1f)
        val vAlpha: ObjectAnimator = ObjectAnimator.ofFloat(visibleText, "alpha", 0f)
        val iX: ObjectAnimator =
            ObjectAnimator.ofFloat(invisibleText, "x", countryOffset1.toFloat())
        val vX: ObjectAnimator = ObjectAnimator.ofFloat(visibleText, "x", vOffset.toFloat())

        val animSet = AnimatorSet()
        animSet.playTogether(iAlpha, vAlpha, iX, vX)
        animSet.duration = countryAnimDuration
        animSet.start()
    }

    private fun onActiveCardChange() {
        val pos = layoutManger.getActiveCardPosition()
        if (pos == RecyclerView.NO_POSITION || pos == currentPosition) {
            return
        }

        onActiveCardChange(pos)
    }

    private fun onActiveCardChange(pos: Int) {
        val animH: IntArray = intArrayOf(R.anim.slide_in_right, R.anim.slide_out_left)
        val animV: IntArray = intArrayOf(R.anim.slide_in_top, R.anim.slide_out_bottom)

        val left2right = pos < currentPosition
        if (left2right) {
            animH[0] = R.anim.slide_in_left
            animH[1] = R.anim.slide_out_right

            animV[0] = R.anim.slide_in_bottom
            animV[1] = R.anim.slide_out_top
        }

//        setCountryText(countries[pos % countries.size], left2right)
//
//        temperatureSwitcher.setInAnimation(this, animH[0])
//        temperatureSwitcher.setOutAnimation(this, animH[1])
//        temperatureSwitcher.setText(temperatures[pos % temperatures.size])
//
//        placeSwitcher.setInAnimation(this, animV[0])
//        placeSwitcher.setOutAnimation(this, animV[1])
//        placeSwitcher.setText(places[pos % places.size])
//
//        clockSwitcher.setInAnimation(this, animV[0])
//        clockSwitcher.setOutAnimation(this, animV[1])
//        clockSwitcher.setText(times[pos % times.size])
//
//        descriptionsSwitcher.setText(getString(descriptions[pos % descriptions.size]))
//
//        showMap(maps[pos % maps.size])
//
//        ViewCompat.animate(greenDot)
//            .translationX(dotCoords[pos % dotCoords.size][0].toFloat())
//            .translationY(dotCoords[pos % dotCoords.size][1].toFloat())
//            .start()

        currentPosition = pos
    }

    private fun showMap(@DrawableRes resId: Int) {
        if (decodeMapBitmapTask != null) {
            decodeMapBitmapTask?.cancel(true)
        }

        val w: Int = mapSwitcher.width
        val h: Int = mapSwitcher.height

        decodeMapBitmapTask = DecodeBitmapTask(resources, resId, w, h, mapLoadListener)
        decodeMapBitmapTask?.execute()
    }

    inner class TextViewFactory constructor(
        @StyleRes private val styleId: Int,
        private val center: Boolean
    ) : ViewSwitcher.ViewFactory {

        @SuppressWarnings("deprecation")
        override fun makeView(): View {
            val textView: TextView = TextView(this@MainActivity)

            if (center) {
                textView.gravity = Gravity.CENTER
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                textView.setTextAppearance(this@MainActivity, styleId)
            } else {
                textView.setTextAppearance(styleId)
            }

            return textView
        }
    }

    inner class ImageViewFactory : ViewSwitcher.ViewFactory {
        override fun makeView(): View {
            val imageView = ImageView(this@MainActivity)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            val lp = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            imageView.layoutParams = lp

            return imageView
        }
    }

    inner class OnCardClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            val lm: CardSliderLayoutManager? =
                recyclerView.layoutManager as? CardSliderLayoutManager

            if (lm?.isSmoothScrolling != false) {
                return
            }

            val activeCardPosition: Int = lm.getActiveCardPosition()
            if (activeCardPosition == RecyclerView.NO_POSITION) {
                return
            }

            val clickedPosition: Int = recyclerView.getChildAdapterPosition(view)
            if (clickedPosition == activeCardPosition) {
                val intent = Intent(
                    this@MainActivity,
                    DetailsActivity::class.java
                )
                intent.putExtra(
                    DetailsActivity.BUNDLE_IMAGE_ID, pics[activeCardPosition % pics.size]
                )

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    startActivity(intent)
                } else {
                    val cardView: CardView = view as CardView
                    val sharedView: View = cardView.getChildAt(cardView.childCount - 1)
                    val options: ActivityOptions = ActivityOptions.makeSceneTransitionAnimation(
                        this@MainActivity,
                        sharedView,
                        "shared"
                    )
                    startActivity(intent, options.toBundle())
                }
            } else if (clickedPosition > activeCardPosition) {
                recyclerView.smoothScrollToPosition(clickedPosition)
                onActiveCardChange(clickedPosition)
            }
        }
    }
}