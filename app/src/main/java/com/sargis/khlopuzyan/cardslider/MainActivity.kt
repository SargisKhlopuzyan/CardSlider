package com.sargis.khlopuzyan.cardslider


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.sargis.khlopuzyan.cardslider.cards.SliderAdapter
import com.sargis.khlopuzyan.cardslider.vertical.CardSliderLayoutManager
import com.sargis.khlopuzyan.cardslider.vertical.CardSnapHelper

class MainActivity : AppCompatActivity() {

    private val pics: IntArray =
        intArrayOf(R.drawable.p1, R.drawable.p2, R.drawable.p3, R.drawable.p4, R.drawable.p5)

    private var sliderAdapter: SliderAdapter =
        SliderAdapter(pics, 20, null /*OnCardClickListener()*/)

    private lateinit var layoutManger: CardSliderLayoutManager
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initRecyclerView()
    }

    private fun initRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.adapter = sliderAdapter
        recyclerView.setHasFixedSize(true)

//        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                    onActiveCardChange()
//                }
//            }
//        })

        layoutManger = recyclerView.layoutManager as CardSliderLayoutManager

        // TODO
        CardSnapHelper().attachToRecyclerView(recyclerView)
    }
}