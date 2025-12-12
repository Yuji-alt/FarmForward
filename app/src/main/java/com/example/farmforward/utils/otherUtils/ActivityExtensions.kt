package com.example.farmforward.utils.otherUtils

import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun AppCompatActivity.hideSystemUI() {
    // 1. Tell the Window to let us handle everything
    WindowCompat.setDecorFitsSystemWindows(window, false)

    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

    // 2. Hide both Status Bar (top) and Navigation Bar (bottom)
    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

    // 3. Behavior: Show bars when user swipes from edge
    windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}