package com.liadpaz.music.ui.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

class BottomNavBehavior<V : View>(context: Context, attributeSet: AttributeSet) : CoordinatorLayout.Behavior<V>(context, attributeSet) {

    private var callback: BottomSheetCallback? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    override fun layoutDependsOn(parent: CoordinatorLayout, child: V, dependency: View): Boolean =
        try {
            val bottom = BottomSheetBehavior.from(dependency)
            callback = BottomSheetCallback(child).also {
                bottom.addBottomSheetCallback(it)
            }
            true
        } catch (e: Exception) {
            false
        }

    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: V, dependency: View) {
        callback?.let { bottomSheetBehavior?.removeBottomSheetCallback(it) }
        bottomSheetBehavior = null
        callback = null
    }

    inner class BottomSheetCallback(private val child: V) : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            if (slideOffset >= 0) {
                child.translationY = slideOffset * child.measuredHeight
            }
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) = Unit
    }
}