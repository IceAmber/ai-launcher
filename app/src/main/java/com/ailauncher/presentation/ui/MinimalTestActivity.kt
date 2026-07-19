package com.ailauncher.presentation.ui

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

class MinimalTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            setBackgroundColor(ContextCompat.getColor(this@MinimalTestActivity, android.R.color.holo_blue_dark))
        }
        
        val textView = TextView(this).apply {
            text = "✅ AI Desktop Launcher\n工作正常！"
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@MinimalTestActivity, android.R.color.white))
            gravity = Gravity.CENTER
        }
        
        layout.addView(textView)
        setContentView(layout)
    }
}
