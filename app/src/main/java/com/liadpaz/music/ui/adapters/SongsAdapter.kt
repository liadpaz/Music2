package com.liadpaz.music.ui.adapters

import android.support.v4.media.MediaBrowserCompat
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.liadpaz.music.ui.utils.*
import com.liadpaz.music.utils.C

class SongsAdapter(private val onItemClick: (MediaBrowserCompat.MediaItem, Int) -> Unit, private val onOpenMenu: (View, MediaBrowserCompat.MediaItem) -> Unit, private val onShuffleClick: () -> Unit) : AbstractHeaderListAdapter<MediaBrowserCompat.MediaItem, RecyclerView.ViewHolder>(C.diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
        TYPE_SHUFFLE -> ShuffleViewHolder.create(parent, onShuffleClick)
        TYPE_SONG -> SongViewHolder.create(parent, onItemClick, onOpenMenu)
        TYPE_NO_SONGS -> NoSongsViewHolder.create(parent)
        else -> throw IllegalArgumentException("Unknown viewType: $viewType")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == TYPE_SONG) {
            (holder as SongViewHolder).bind(getItem(position))
        }
    }

    override fun getItemViewType(position: Int): Int = if (position == 0) {
        if (itemCount == 1) {
            TYPE_NO_SONGS
        } else {
            TYPE_SHUFFLE
        }
    } else TYPE_SONG
}