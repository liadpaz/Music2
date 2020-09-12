package com.liadpaz.music.ui.adapters

import androidx.annotation.CallSuper
import androidx.recyclerview.widget.*

abstract class AbstractHeaderListAdapter<T, VH : RecyclerView.ViewHolder>(private val diffCallback: DiffUtil.ItemCallback<T>, private val headerOffset: Int = 1) : RecyclerView.Adapter<VH>() {
    private val helper by lazy {
        AsyncListDiffer<T>(
            OffsetListUpdateCallback(this, headerOffset),
            AsyncDifferConfig.Builder(diffCallback).build()
        )
    }

    val currentList: List<T>
        get() = helper.currentList

    @CallSuper
    fun submitList(list: List<T>?) = helper.submitList(list)

    fun getItem(position: Int): T = helper.currentList[position - headerOffset]

    override fun getItemCount(): Int = helper.currentList.size + 1

    private class OffsetListUpdateCallback(private val adapter: RecyclerView.Adapter<*>, private val offset: Int) : ListUpdateCallback {
        private fun offsetPosition(position: Int): Int = position + offset

        override fun onInserted(position: Int, count: Int) = adapter.notifyItemRangeInserted(offsetPosition(position), count)

        override fun onRemoved(position: Int, count: Int) = adapter.notifyItemRangeRemoved(offsetPosition(position), count)

        override fun onMoved(fromPosition: Int, toPosition: Int) = adapter.notifyItemMoved(offsetPosition(fromPosition), offsetPosition(toPosition))

        override fun onChanged(position: Int, count: Int, payload: Any?) = adapter.notifyItemRangeChanged(offsetPosition(position), count, payload)
    }
}