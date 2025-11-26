package com.example.farmforward.utils.loadingUtils

import android.R
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatAutoCompleteTextView

class LimitHeightAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.autoCompleteTextViewStyle
) : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {

    private val MAX_HEIGHT_DP = 250

    override fun showDropDown() {
        val displayMetrics = resources.displayMetrics
        val maxPixels = (MAX_HEIGHT_DP * displayMetrics.density).toInt()

        dropDownHeight = ViewGroup.LayoutParams.WRAP_CONTENT

        super.showDropDown()
    }
}