package com.oxywatt.diagnostic.preview

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import com.oxywatt.diagnostic.R

class PreviewActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var text: TextView
    private val refresh =
        object : Runnable {
            override fun run() {
                text.text = PreviewRun.snapshot ?: getString(R.string.preview_empty)
                handler.postDelayed(this, 500)
            }
        }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        if (PreviewRun.snapshot == null)
            PreviewRun.start(applicationContext, false, restoreOnly = true)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(TextView(this).apply { setText(R.string.preview_title) })
        text = TextView(this)
        layout.addView(text)
        fun button(label: Int, action: () -> Unit) {
            layout.addView(
                Button(this).apply {
                    setText(label)
                    setOnClickListener { action() }
                }
            )
        }
        button(R.string.preview_demo) { PreviewRun.start(applicationContext, false) }
        button(R.string.preview_fit) { PreviewRun.start(applicationContext, true) }
        button(R.string.preview_stop) { PreviewRun.stop() }
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    override fun onStart() {
        super.onStart()
        handler.post(refresh)
    }

    override fun onStop() {
        handler.removeCallbacks(refresh)
        super.onStop()
    }
}
