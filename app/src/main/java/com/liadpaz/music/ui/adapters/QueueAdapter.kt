package com.liadpaz.music.ui.adapters

import android.annotation.SuppressLint
import android.support.v4.media.MediaMetadataCompat
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.liadpaz.music.R
import com.liadpaz.music.databinding.ItemQueueSongBinding
import com.liadpaz.music.utils.C
import com.liadpaz.music.utils.extensions.layoutInflater

class QueueAdapter(private val onItemClick: (Int) -> Unit, private val onDragClick: (RecyclerView.ViewHolder) -> Unit) : ListAdapter<MediaMetadataCompat, QueueAdapter.SongViewHolder>(C.metadataDiffCallback) {

	private var toSubmit = false

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder = SongViewHolder.create(parent, onItemClick, onDragClick)

	override fun onBindViewHolder(holder: SongViewHolder, position: Int) = holder.bind(getItem(position))

	override fun onBindViewHolder(holder: SongViewHolder, position: Int, payloads: MutableList<Any>) {
		onBindViewHolder(holder, position)
		holder.binding.ivSongCover.foreground = if (PAYLOAD_PLAYING in payloads) ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_notification_play) else null
	}

	fun onMove(fromPosition: Int, toPosition: Int) {
		toSubmit = false
		notifyItemMoved(fromPosition, toPosition)
	}

	override fun submitList(list: List<MediaMetadataCompat>?) {
		if (toSubmit) {
			super.submitList(list?.let { ArrayList(list) })
		} else {
			toSubmit = true
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	class SongViewHolder private constructor(val binding: ItemQueueSongBinding, onItemClick: (Int) -> Unit, onDragClick: (RecyclerView.ViewHolder) -> Unit) : RecyclerView.ViewHolder(binding.root) {
		init {
			binding.root.setOnClickListener { onItemClick(adapterPosition) }
			binding.ibDrag.setOnTouchListener { _, motionEvent ->
				if (motionEvent.action == MotionEvent.ACTION_DOWN) {
					onDragClick(this)
				}
				true
			}
		}

		fun bind(song: MediaMetadataCompat?) {
			binding.song = song
			binding.executePendingBindings()
		}

		companion object {
			fun create(viewGroup: ViewGroup, onClick: (Int) -> Unit, onDragClick: (RecyclerView.ViewHolder) -> Unit): SongViewHolder =
				SongViewHolder(ItemQueueSongBinding.inflate(viewGroup.layoutInflater, viewGroup, false), onClick, onDragClick)
		}
	}

	companion object {
		const val PAYLOAD_PLAYING = "payload_playing"
	}
}