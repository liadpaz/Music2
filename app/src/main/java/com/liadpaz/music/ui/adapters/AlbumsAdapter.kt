package com.liadpaz.music.ui.adapters

import android.support.v4.media.MediaBrowserCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.liadpaz.music.databinding.ItemAlbumBinding
import com.liadpaz.music.utils.C

class AlbumsAdapter(private val onItemClick: (MediaBrowserCompat.MediaItem) -> Unit) : ListAdapter<MediaBrowserCompat.MediaItem, AlbumsAdapter.AlbumViewHolder>(C.diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder =
        AlbumViewHolder(ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false), onItemClick)

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) =
        holder.bind(getItem(position))

    override fun submitList(list: List<MediaBrowserCompat.MediaItem>?) =
        super.submitList(list?.let { ArrayList(list) })

    class AlbumViewHolder(private val binding: ItemAlbumBinding, onItemClick: (MediaBrowserCompat.MediaItem) -> Unit) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                binding.album?.let(onItemClick)
            }
        }

        fun bind(album: MediaBrowserCompat.MediaItem) = with(binding) {
            this.album = album
            executePendingBindings()
        }
    }
}