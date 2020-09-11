package com.liadpaz.music.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.liadpaz.music.databinding.ItemPlaylistSmallBinding
import com.liadpaz.music.utils.extensions.layoutInflater

class SmallArtistsAdapter(private val onItemClick: (String) -> Unit) : RecyclerView.Adapter<SmallArtistsAdapter.SmallArtistViewHolder>() {
    private var list: List<String> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmallArtistViewHolder =
        SmallArtistViewHolder.create(parent, onItemClick)

    override fun onBindViewHolder(holder: SmallArtistViewHolder, position: Int) =
        holder.bind(getItem(position))

    fun getItem(position: Int): String = list[position]

    override fun getItemCount(): Int = list.size

    fun setList(list: List<String>?) {
        this.list = list ?: listOf()
    }

    class SmallArtistViewHolder(private val binding: ItemPlaylistSmallBinding, onClick: (String) -> Unit) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener { onClick(binding.playlist!!) }
        }

        fun bind(playlist: String) {
            binding.playlist = playlist
            binding.executePendingBindings()
        }

        companion object {
            fun create(viewGroup: ViewGroup, onClick: (String) -> Unit): SmallArtistViewHolder =
                SmallArtistViewHolder(ItemPlaylistSmallBinding.inflate(viewGroup.layoutInflater, viewGroup, false), onClick)
        }
    }
}