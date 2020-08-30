package com.liadpaz.music.ui.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.liadpaz.music.R

class ProgressMotionLayout @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null) : MotionLayout(context, attributeSet) {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        BottomSheetBehavior.from(findViewById<CoordinatorLayout>(R.id.coordinatorLayout).findViewById(R.id.bottomSheet)).addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) = Unit

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset > 0) {
                    progress = slideOffset
                }
            }
        })
    }
}
