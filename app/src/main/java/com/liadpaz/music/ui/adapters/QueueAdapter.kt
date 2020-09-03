package com.liadpaz.music.ui.adapters

import android.annotation.SuppressLint
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.liadpaz.music.databinding.ItemQueueBinding
import com.liadpaz.music.utils.C
import java.util.*
import kotlin.collections.ArrayList
import kotlin.time.ExperimentalTime

class QueueAdapter(private val onItemClick: (Int) -> Unit, private val onDragClick: (ItemViewHolder) -> Unit) : ListAdapter<MediaSessionCompat.QueueItem, QueueAdapter.ItemViewHolder>(C.queueDiffCallback) {

    private var toSubmit = false

    private var currentQueue: MutableList<MediaSessionCompat.QueueItem>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder =
        ItemViewHolder(ItemQueueBinding.inflate(LayoutInflater.from(parent.context), parent, false), onItemClick, onDragClick)

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) =
        holder.bind(getItem(position))

    @ExperimentalTime
    override fun submitList(list: List<MediaSessionCompat.QueueItem>?) {
        Log.d(TAG, "submitList: $toSubmit")
        currentQueue = list?.let { ArrayList(it) }
        if (toSubmit) {
            super.submitList(currentQueue)
        } else {
            toSubmit = true
        }
    }

    fun onSwipe(position: Int) {
        toSubmit = false
        currentQueue?.removeAt(position)
        notifyItemRemoved(position)
    }

    fun onMove(fromPosition: Int, toPosition: Int) {
        toSubmit = false
        currentQueue?.let { Collections.swap(it, fromPosition, toPosition) }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun getItem(position: Int): MediaSessionCompat.QueueItem? = currentQueue?.get(position)


    @SuppressLint("ClickableViewAccessibility")
    class ItemViewHolder(private val binding: ItemQueueBinding, onItemClick: (Int) -> Unit, onDragClick: (ItemViewHolder) -> Unit) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener { onItemClick(adapterPosition) }
            binding.ibDrag.setOnTouchListener { _, motionEvent ->
                if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
                    onDragClick(this)
                }
                true
            }
        }

        fun bind(item: MediaSessionCompat.QueueItem?) {
            binding.item = item
            binding.executePendingBindings()
        }
    }
}

private const val TAG = "QueueAdapter"