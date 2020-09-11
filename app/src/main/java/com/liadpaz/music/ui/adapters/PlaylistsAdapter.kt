package com.liadpaz.music.ui.adapters

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.liadpaz.music.databinding.ItemCreatePlaylistBinding
import com.liadpaz.music.databinding.ItemPlaylistBinding
import com.liadpaz.music.service.PLAYLIST_RECENTLY_ADDED
import com.liadpaz.music.service.utils.PLAYLISTS_ROOT
import com.liadpaz.music.utils.C
import com.liadpaz.music.utils.extensions.layoutInflater

class PlaylistsAdapter(private val onPlaylistClick: (MediaBrowserCompat.MediaItem) -> Unit, private val onPlaylistLongClick: (MediaBrowserCompat.MediaItem) -> Unit, private val onCreateClick: () -> Unit) : AbstractHeaderListAdapter<MediaBrowserCompat.MediaItem, RecyclerView.ViewHolder>(C.diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
        TYPE_CREATE -> CreateViewHolder.create(parent, onCreateClick)
        TYPE_PLAYLIST -> PlaylistViewHolder.create(parent, onPlaylistClick, onPlaylistLongClick)
        else -> throw IllegalArgumentException("Unknown viewType: $viewType")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position != 0) {
            (holder as PlaylistViewHolder).bind(getItem(position))
        }
    }

    override fun getItemViewType(position: Int): Int = if (position == 0) TYPE_CREATE else TYPE_PLAYLIST

    companion object {
        private const val TYPE_CREATE = 1
        private const val TYPE_PLAYLIST = 2
    }

    class PlaylistViewHolder private constructor(private val binding: ItemPlaylistBinding, onPlaylistClick: (MediaBrowserCompat.MediaItem) -> Unit, onPlaylistLongClick: (MediaBrowserCompat.MediaItem) -> Unit) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener { onPlaylistClick(binding.playlist!!) }
            binding.root.setOnLongClickListener {
                onPlaylistLongClick(binding.playlist!!)
                true
            }
        }

        fun bind(playlist: MediaBrowserCompat.MediaItem) {
            binding.playlist = playlist
            binding.executePendingBindings()
            if (binding.playlist?.mediaId == "$PLAYLISTS_ROOT$PLAYLIST_RECENTLY_ADDED") {
                binding.root.setOnLongClickListener { true }
            }
        }

        companion object {
            fun create(viewGroup: ViewGroup, onClick: (MediaBrowserCompat.MediaItem) -> Unit, onLongClick: (MediaBrowserCompat.MediaItem) -> Unit): PlaylistViewHolder =
                PlaylistViewHolder(ItemPlaylistBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false), onClick, onLongClick)
        }
    }

    class CreateViewHolder private constructor(binding: ViewBinding, onClick: () -> Unit) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener { onClick() }
        }

        companion object {
            fun create(viewGroup: ViewGroup, onClick: () -> Unit): CreateViewHolder =
                CreateViewHolder(ItemCreatePlaylistBinding.inflate(viewGroup.layoutInflater, viewGroup, false), onClick)
        }
    }
}