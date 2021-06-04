package com.sargis.khlopuzyan.cardslider

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.transition.Transition
import android.util.DisplayMetrics
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.sargis.khlopuzyan.cardslider.utils.DecodeBitmapTask

class DetailsActivity : AppCompatActivity(), DecodeBitmapTask.Listener {

    companion object {
        const val BUNDLE_IMAGE_ID = "BUNDLE_IMAGE_ID"
    }

    private lateinit var imageView: ImageView
    private var decodeBitmapTask: DecodeBitmapTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        val smallResId = intent.getIntExtra(BUNDLE_IMAGE_ID, -1)
        if (smallResId == -1) {
            finish()
            return
        }

        imageView = findViewById(R.id.image)
        imageView.setImageResource(smallResId)

        imageView.setOnClickListener {
            onBackPressed()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            loadFullSizeBitmap(smallResId)
        } else {
            window.sharedElementEnterTransition
                .addListener(object : Transition.TransitionListener {

                    private var isClosing = false

                    override fun onTransitionPause(transition: Transition) {}
                    override fun onTransitionResume(transition: Transition) {}
                    override fun onTransitionCancel(transition: Transition) {}

                    override fun onTransitionStart(transition: Transition) {
                        if (isClosing) {
                            addCardCorners()
                        }
                    }

                    override fun onTransitionEnd(transition: Transition) {
                        if (!isClosing) {
                            isClosing = true

                            removeCardCorners()
                            loadFullSizeBitmap(smallResId)
                        }
                    }
                })
        }
    }

    override fun onPause() {
        super.onPause()

        if (isFinishing) {
            decodeBitmapTask?.cancel(true)
        }
    }

    private fun addCardCorners() {
//        val cardView: CardView = findViewById(R.id.card)
//        cardView.radius = 25f
    }

    private fun removeCardCorners() {
//        val cardView: CardView = findViewById(R.id.card)
//        ObjectAnimator.ofFloat(cardView, "radius", 0f).setDuration(50).start()
    }

    private fun loadFullSizeBitmap(smallResId: Int) {
        val bigResId = when (smallResId) {
            R.drawable.p1 -> R.drawable.p1_big
            R.drawable.p2 -> R.drawable.p2_big
            R.drawable.p3 -> R.drawable.p3_big
            R.drawable.p4 -> R.drawable.p4_big
            R.drawable.p5 -> R.drawable.p5_big
            else -> R.drawable.p1_big
        }

        val metrics: DisplayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val w = metrics.widthPixels
        val h = metrics.heightPixels

        decodeBitmapTask = DecodeBitmapTask(resources, bigResId, w, h, this)
        decodeBitmapTask?.execute()
    }

    override fun onPostExecuted(bitmap: Bitmap) {
        imageView.setImageBitmap(bitmap)
    }
}

