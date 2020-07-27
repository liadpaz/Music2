package com.liadpaz.music.ui.adapters

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.liadpaz.music.databinding.ItemArtistBinding
import com.liadpaz.music.utils.C

class ArtistsAdapter(private val onItemClick: (MediaBrowserCompat.MediaItem) -> Unit) : ListAdapter<MediaBrowserCompat.MediaItem, ArtistsAdapter.ArtistViewHolder>(C.diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder =
        ArtistViewHolder(ItemArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false), onItemClick)

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) =
        holder.bind(getItem(position))

    class ArtistViewHolder(private val binding: ItemArtistBinding, onItemClick: (MediaBrowserCompat.MediaItem) -> Unit) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                binding.artist?.let(onItemClick)
            }
        }

        fun bind(artist: MediaBrowserCompat.MediaItem) = with(binding) {
            this.artist = artist
            executePendingBindings()
        }
    }
}