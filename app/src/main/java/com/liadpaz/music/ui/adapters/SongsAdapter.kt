package com.liadpaz.music.ui.adapters

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.liadpaz.music.databinding.ItemShuffleBinding
import com.liadpaz.music.databinding.ItemSongBinding
import com.liadpaz.music.utils.C

class SongsAdapter(private val onItemClick: (MediaBrowserCompat.MediaItem, Int) -> Unit, private val onMoreClick: (View, MediaBrowserCompat.MediaItem) -> Unit, private val onShuffleClick: () -> Unit) : AbstractHeaderListAdapter<MediaBrowserCompat.MediaItem, RecyclerView.ViewHolder>(C.diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
        TYPE_SHUFFLE -> ShuffleViewHolder.create(parent, onShuffleClick)
        TYPE_SONG -> SongViewHolder.create(parent, onItemClick, onMoreClick)
        TYPE_NO_SONGS -> ShuffleViewHolder.create(parent, onShuffleClick) // TODO: replace with 'no songs found' view holder when implemented
        else -> throw IllegalArgumentException("Unknown viewType: $viewType")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == TYPE_SONG) {
            (holder as SongViewHolder).bind(getItem(position))
        }
    }

    override fun submitList(list: List<MediaBrowserCompat.MediaItem>?) =
        super.submitList(list?.let { ArrayList(list) })

    override fun getItemViewType(position: Int): Int = if (position == 0) {
        if (itemCount == 1) {
            TYPE_NO_SONGS
        } else {
            TYPE_SHUFFLE
        }
    } else TYPE_SONG

    /**
     * This class is a view holder for a song item type
     */
    class SongViewHolder private constructor(private val binding: ItemSongBinding, onItemClick: (MediaBrowserCompat.MediaItem, Int) -> Unit, onMoreClick: (View, MediaBrowserCompat.MediaItem) -> Unit) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                binding.song?.let { onItemClick(it, adapterPosition - 1) }
            }
            binding.ibMore.setOnClickListener { onMoreClick(it, binding.song!!) }
        }

        fun bind(song: MediaBrowserCompat.MediaItem) = with(binding) {
            this.song = song
            executePendingBindings()
        }

        companion object {
            fun create(viewGroup: ViewGroup, onClick: (MediaBrowserCompat.MediaItem, Int) -> Unit, onMoreClick: (View, MediaBrowserCompat.MediaItem) -> Unit) =
                SongViewHolder(ItemSongBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false), onClick, onMoreClick)
        }
    }

    /**
     * This class is a view holder for a shuffle item type
     */
    class ShuffleViewHolder private constructor(binding: ItemShuffleBinding, private val onClick: () -> Unit) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener { onClick() }
        }

        companion object {
            fun create(viewGroup: ViewGroup, onClick: () -> Unit) =
                ShuffleViewHolder(ItemShuffleBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false), onClick)
        }
    }

    // TODO: class for no songs found and implement it
}

private const val TYPE_SONG = 1
private const val TYPE_SHUFFLE = 2
private const val TYPE_NO_SONGS = 3