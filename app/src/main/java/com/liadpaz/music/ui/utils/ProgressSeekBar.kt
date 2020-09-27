package com.liadpaz.music.ui.utils

import android.content.Context
import android.util.AttributeSet
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatSeekBar

class ProgressSeekBar(context: Context, attr: AttributeSet? = null) : AppCompatSeekBar(context, attr) {
    private var fromUser = false

    private var onSeekBar: OnSeekBarChangeListener? = null
    private var originalOnSeekListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            onSeekBar?.onProgressChanged(seekBar, progress, fromUser)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            onSeekBar?.onStartTrackingTouch(seekBar)
            fromUser = true
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            onSeekBar?.onStopTrackingTouch(seekBar)
            fromUser = false
        }
    }

    override fun setProgress(progress: Int) {
        if (!fromUser) {
            super.setProgress(progress)
        }
    }

    override fun setOnSeekBarChangeListener(listener: OnSeekBarChangeListener?) {
        onSeekBar = listener
        super.setOnSeekBarChangeListener(originalOnSeekListener)
    }


}