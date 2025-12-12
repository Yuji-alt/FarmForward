package com.example.farmforward.utils.onBoarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.farmforward.R

// 1. Data Model for a Slide
data class OnboardingItem(
    val title: String,
    val description: String,
    val imageRes: Int
)

// 2. The Adapter
class OnboardingAdapter(private val items: List<OnboardingItem>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textTitle = view.findViewById<TextView>(R.id.textTitle)
        private val textDescription = view.findViewById<TextView>(R.id.textDescription)
        private val imageOnboarding = view.findViewById<ImageView>(R.id.imageOnboarding)

        fun bind(item: OnboardingItem) {
            textTitle.text = item.title
            textDescription.text = item.description
            imageOnboarding.setImageResource(item.imageRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        // We will create the layout 'item_onboarding.xml' in the next step
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}