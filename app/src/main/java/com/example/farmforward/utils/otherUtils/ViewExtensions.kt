package com.example.farmforward.utils.otherUtils

import android.graphics.Rect
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max

fun ScrollView.handleKeyboardVisibility(scrollBuffer: Int = 100) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        val bottomPadding = max(imeInsets.bottom, systemBars.bottom)
        v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bottomPadding)
        val isKeyboardVisible = imeInsets.bottom > 0
        if (isKeyboardVisible) {
            val focusedChild = v.findFocus()

            if (focusedChild != null) {
                v.post {
                    try {
                        val rect = Rect()
                        focusedChild.getDrawingRect(rect)
                        (v as ViewGroup).offsetDescendantRectToMyCoords(focusedChild, rect)
                        val scrollY = rect.top - scrollBuffer
                        this.smoothScrollTo(0, scrollY)
                    } catch (e: IllegalArgumentException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        insets
    }
}