package com.liadpaz.music.ui.utils

import android.support.v4.media.MediaBrowserCompat
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.liadpaz.music.databinding.ItemNoSongsBinding
import com.liadpaz.music.databinding.ItemShuffleBinding
import com.liadpaz.music.databinding.ItemSongBinding
import com.liadpaz.music.utils.extensions.layoutInflater

/**
 * This class is a view holder for a no songs item type
 */
class NoSongsViewHolder private constructor(binding: ItemNoSongsBinding) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun create(viewGroup: ViewGroup): NoSongsViewHolder =
            NoSongsViewHolder(ItemNoSongsBinding.inflate(viewGroup.layoutInflater, viewGroup, false))
    }
}

/**
 * This class is a view holder for a shuffle item type
 */
class ShuffleViewHolder private constructor(binding: ItemShuffleBinding, onClick: () -> Unit) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.root.setOnClickListener { onClick() }
    }

    companion object {
        fun create(viewGroup: ViewGroup, onClick: () -> Unit): ShuffleViewHolder =
            ShuffleViewHolder(ItemShuffleBinding.inflate(viewGroup.layoutInflater, viewGroup, false), onClick)
    }
}

/**
 * This class is a view holder for a song item type
 */
class SongViewHolder private constructor(private val binding: ItemSongBinding, onClick: (MediaBrowserCompat.MediaItem, Int) -> Unit, onShowMenu: (View, MediaBrowserCompat.MediaItem) -> Unit) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.root.setOnClickListener { onClick(binding.song!!, adapterPosition - 1) }
        binding.root.setOnLongClickListener {
            onShowMenu(it, binding.song!!)
            true
        }
        binding.ibMore.setOnClickListener { onShowMenu(it, binding.song!!) }
    }

    fun bind(song: MediaBrowserCompat.MediaItem) {
        binding.song = song
        binding.executePendingBindings()
    }

    companion object {
        fun create(viewGroup: ViewGroup, onClick: (MediaBrowserCompat.MediaItem, Int) -> Unit, onShowMenu: (View, MediaBrowserCompat.MediaItem) -> Unit): SongViewHolder =
            SongViewHolder(ItemSongBinding.inflate(viewGroup.layoutInflater, viewGroup, false), onClick, onShowMenu)
    }
}

const val TYPE_SHUFFLE = 1
const val TYPE_SONG = 2
const val TYPE_MOVABLE_SONG = 3
const val TYPE_NO_SONGS = 4