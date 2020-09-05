package com.liadpaz.music.ui.adapters

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.liadpaz.music.databinding.ItemPlaylistBinding
import com.liadpaz.music.utils.C

class PlaylistsAdapter(private val onPlaylistClick: (MediaBrowserCompat.MediaItem) -> Unit, private val onPlaylistLongClick: (MediaBrowserCompat.MediaItem) -> Unit) : ListAdapter<MediaBrowserCompat.MediaItem, PlaylistsAdapter.PlaylistViewHolder>(C.diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder =
        PlaylistViewHolder(ItemPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false), onPlaylistClick, onPlaylistLongClick)

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) = holder.bind(getItem(position))

    class PlaylistViewHolder(private val binding: ItemPlaylistBinding, onPlaylistClick: (MediaBrowserCompat.MediaItem) -> Unit, onPlaylistLongClick: (MediaBrowserCompat.MediaItem) -> Unit) : RecyclerView.ViewHolder(binding.root) {

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
        }
    }
}