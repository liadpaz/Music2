package com.liadpaz.music.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.liadpaz.music.databinding.ItemPlaylistSmallBinding
import com.liadpaz.music.utils.extensions.layoutInflater

class SmallPlaylistsAdapter(private val onItemClick: (String) -> Unit) : RecyclerView.Adapter<SmallPlaylistsAdapter.SmallPlaylistViewHolder>() {
    private var list: List<String> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmallPlaylistViewHolder =
        SmallPlaylistViewHolder.create(parent, onItemClick)

    override fun onBindViewHolder(holder: SmallPlaylistViewHolder, position: Int) =
        holder.bind(getItem(position))

    fun getItem(position: Int): String = list[position]

    override fun getItemCount(): Int = list.size

    fun setList(list: List<String>?) {
        this.list = list ?: listOf()
    }

    class SmallPlaylistViewHolder(private val binding: ItemPlaylistSmallBinding, onClick: (String) -> Unit) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener { onClick(binding.playlist!!) }
        }

        fun bind(playlist: String) {
            binding.playlist = playlist
            binding.executePendingBindings()
        }

        companion object {
            fun create(viewGroup: ViewGroup, onClick: (String) -> Unit): SmallPlaylistViewHolder =
                SmallPlaylistViewHolder(ItemPlaylistSmallBinding.inflate(viewGroup.layoutInflater, viewGroup, false), onClick)
        }
    }
}