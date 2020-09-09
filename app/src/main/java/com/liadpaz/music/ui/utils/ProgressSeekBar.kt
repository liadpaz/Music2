package com.liadpaz.music.ui.utils

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar

class ProgressSeekBar(context: Context, attrs: AttributeSet) : AppCompatSeekBar(context, attrs) {

    var isUser = false

    override fun setProgress(progress: Int) {
        if (!isUser) {
            super.setProgress(progress)
        }
    }
}