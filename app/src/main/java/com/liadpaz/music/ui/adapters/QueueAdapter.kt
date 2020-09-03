package com.liadpaz.music.ui.adapters

import android.annotation.SuppressLint
import android.support.v4.media.session.MediaSessionCompat
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.liadpaz.music.databinding.ItemQueueBinding
import com.liadpaz.music.utils.C

class QueueAdapter(private val onItemClick: (Int) -> Unit, private val onDragClick: (ItemViewHolder) -> Unit) : ListAdapter<MediaSessionCompat.QueueItem, QueueAdapter.ItemViewHolder>(C.queueDiffCallback) {

    private var toSubmit = false

    private var currentQueue: List<MediaSessionCompat.QueueItem>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder =
        ItemViewHolder(ItemQueueBinding.inflate(LayoutInflater.from(parent.context), parent, false), onItemClick, onDragClick)

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) =
        holder.bind(getItem(position))

    override fun submitList(list: List<MediaSessionCompat.QueueItem>?) {
        currentQueue = list?.let { ArrayList(it) }
        if (toSubmit) {
            super.submitList(currentList)
        } else {
            currentQueue = list
            toSubmit = true
        }
    }

    fun onSwipe(position: Int) {
        toSubmit = false
        notifyItemRemoved(position)
    }

    fun onMove(fromPosition: Int, toPosition: Int) {
        toSubmit = false
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun getCurrentList(): List<MediaSessionCompat.QueueItem> = currentQueue ?: listOf()

    override fun getItem(position: Int): MediaSessionCompat.QueueItem? = currentQueue?.get(position)

    @SuppressLint("ClickableViewAccessibility")
    class ItemViewHolder(private val binding: ItemQueueBinding, onItemClick: (Int) -> Unit, onDragClick: (ItemViewHolder) -> Unit) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener { onItemClick(adapterPosition) }
            binding.ibDrag.setOnTouchListener { _, motionEvent ->
                if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
                    onDragClick(this)
                }
                return@setOnTouchListener true
            }
        }

        fun bind(item: MediaSessionCompat.QueueItem?) {
            binding.item = item
            binding.executePendingBindings()
        }
    }
}