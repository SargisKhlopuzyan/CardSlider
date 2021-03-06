package com.sargis.khlopuzyan.cardslider.cards

import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView
import com.sargis.khlopuzyan.cardslider.custom.CreditCardView

class SliderAdapter constructor(
    private val content: IntArray,
    private val count: Int,
    private val listener: View.OnClickListener?
) : RecyclerView.Adapter<SliderCard>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderCard {

        val view = CreditCardView(parent.context)
//        val view: View = LayoutInflater
//            .from(parent.context)
//            .inflate(R.layout.layout_slider_card, parent, false)


//        if (listener != null) {
//            view.setOnClickListener { listener.onClick(it) }
//        }

        return SliderCard(view)
    }

    override fun onBindViewHolder(holder: SliderCard, position: Int) {
//        holder.setContent(content[position % content.size])
    }

    override fun onViewRecycled(holder: SliderCard) {
//        holder.clearContent()
    }

    override fun getItemCount(): Int {
        return count
    }

}
