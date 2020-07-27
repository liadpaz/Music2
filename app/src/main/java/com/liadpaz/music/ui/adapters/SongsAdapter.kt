package com.liadpaz.music.ui.adapters

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.liadpaz.music.databinding.ItemSongBinding
import com.liadpaz.music.utils.C

class SongsAdapter(private val onItemClick: (MediaBrowserCompat.MediaItem) -> Unit) : ListAdapter<MediaBrowserCompat.MediaItem, SongsAdapter.SongViewHolder>(C.diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder =
        SongViewHolder(ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false), onItemClick)

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) =
        holder.bind(getItem(position))

    override fun submitList(list: List<MediaBrowserCompat.MediaItem>?) =
        super.submitList(list?.let { ArrayList(list) })

    class SongViewHolder(private val binding: ItemSongBinding, onItemClick: (MediaBrowserCompat.MediaItem) -> Unit) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                binding.song?.let(onItemClick)
            }
        }

        fun bind(song: MediaBrowserCompat.MediaItem) = with(binding) {
            this.song = song
            executePendingBindings()
        }
    }
}