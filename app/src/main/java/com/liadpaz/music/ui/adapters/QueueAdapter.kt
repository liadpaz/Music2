package com.liadpaz.music.ui.adapters

import android.annotation.SuppressLint
import android.support.v4.media.session.MediaSessionCompat
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.liadpaz.music.databinding.ItemQueueSongBinding
import com.liadpaz.music.utils.extensions.layoutInflater

class QueueAdapter(private val onItemClick: (Int) -> Unit, private val onDragClick: (SongViewHolder) -> Unit) : RecyclerView.Adapter<QueueAdapter.SongViewHolder>() {

    private var toSubmit = false

    private var currentQueue: MutableList<MediaSessionCompat.QueueItem>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder =
        SongViewHolder.create(parent, onItemClick, onDragClick)

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) =
        holder.bind(getItem(position))

    fun onSwipe(position: Int) {
        toSubmit = false
        currentQueue!!.removeAt(position)
        notifyItemRemoved(position)
    }

    fun onMove(fromPosition: Int, toPosition: Int) {
        toSubmit = false
        val value = currentQueue!!.removeAt(fromPosition)
        currentQueue!!.add(toPosition, value)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun submitList(list: List<MediaSessionCompat.QueueItem>?) {
        currentQueue = list?.let { ArrayList(it) }
        if (toSubmit) {
            notifyDataSetChanged()
        } else {
            toSubmit = true
        }
    }

    private fun getItem(position: Int): MediaSessionCompat.QueueItem? = currentQueue?.get(position)

    override fun getItemCount(): Int = currentQueue?.size ?: 0

    @SuppressLint("ClickableViewAccessibility")
    class SongViewHolder private constructor(private val binding: ItemQueueSongBinding, onItemClick: (Int) -> Unit, onDragClick: (SongViewHolder) -> Unit) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener { onItemClick(adapterPosition) }
            binding.ibDrag.setOnTouchListener { _, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    onDragClick(this)
                }
                true
            }
        }

        fun bind(song: MediaSessionCompat.QueueItem?) {
            binding.song = song
            binding.executePendingBindings()
        }

        companion object {
            fun create(viewGroup: ViewGroup, onClick: (Int) -> Unit, onDragClick: (SongViewHolder) -> Unit): SongViewHolder =
                SongViewHolder(ItemQueueSongBinding.inflate(viewGroup.layoutInflater, viewGroup, false), onClick, onDragClick)
        }
    }
}