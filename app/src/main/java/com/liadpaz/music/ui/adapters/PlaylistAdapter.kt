package com.liadpaz.music.ui.adapters

import android.annotation.SuppressLint
import android.support.v4.media.MediaBrowserCompat
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.liadpaz.music.databinding.ItemPlaylistSongBinding
import com.liadpaz.music.service.PLAYLIST_RECENTLY_ADDED
import com.liadpaz.music.service.utils.PLAYLISTS_ROOT
import com.liadpaz.music.ui.utils.*
import com.liadpaz.music.utils.C
import com.liadpaz.music.utils.extensions.layoutInflater

class PlaylistAdapter(private val id: String, private val onItemClick: (MediaBrowserCompat.MediaItem, Int) -> Unit, private val onOpenMenu: (View, MediaBrowserCompat.MediaItem, Int) -> Unit, private val onShuffleClick: () -> Unit, private val onMoveClick: (RecyclerView.ViewHolder) -> Unit) : AbstractHeaderListAdapter<MediaBrowserCompat.MediaItem, RecyclerView.ViewHolder>(C.diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
        TYPE_SHUFFLE -> ShuffleViewHolder.create(parent, onShuffleClick)
        TYPE_SONG -> SongViewHolder.create(parent, onItemClick, { anchor, mediaItem -> onOpenMenu(anchor, mediaItem, -1) })
        TYPE_MOVABLE_SONG -> MovableSongViewHolder.create(parent, onItemClick, onOpenMenu, onMoveClick)
        TYPE_NO_SONGS -> NoSongsViewHolder.create(parent)
        else -> throw IllegalArgumentException("Unknown viewType: $viewType")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            TYPE_SONG -> (holder as SongViewHolder).bind(getItem(position))
            TYPE_MOVABLE_SONG -> (holder as MovableSongViewHolder).bind(getItem(position))
        }
    }

    fun onItemRemoved(position: Int) = submitList(currentList.toMutableList().apply { removeAt(position) })

    override fun getItemViewType(position: Int): Int =
        if (itemCount == 1) TYPE_NO_SONGS else {
            if (position == 0) TYPE_SHUFFLE else {
                if (id == "${PLAYLISTS_ROOT}${PLAYLIST_RECENTLY_ADDED}") TYPE_SONG
                else TYPE_MOVABLE_SONG
            }
        }

    /**
     * This class is a view holder for a movable song item type
     */
    @SuppressLint("ClickableViewAccessibility")
    class MovableSongViewHolder private constructor(private val binding: ItemPlaylistSongBinding, onClick: (MediaBrowserCompat.MediaItem, Int) -> Unit, onShowMenu: (View, MediaBrowserCompat.MediaItem, Int) -> Unit, onMoveClick: (RecyclerView.ViewHolder) -> Unit) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener { onClick(binding.song!!, adapterPosition - 1) }
            binding.root.setOnLongClickListener {
                onShowMenu(it, binding.song!!, adapterPosition)
                true
            }
            binding.ibMore.setOnClickListener { onShowMenu(it, binding.song!!, adapterPosition) }
            binding.ibDrag.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    onMoveClick(this)
                }
                true
            }
        }

        fun bind(song: MediaBrowserCompat.MediaItem) {
            binding.song = song
            binding.executePendingBindings()
        }

        companion object {
            fun create(viewGroup: ViewGroup, onClick: (MediaBrowserCompat.MediaItem, Int) -> Unit, onShowMenu: (View, MediaBrowserCompat.MediaItem, Int) -> Unit, onMoveClick: (RecyclerView.ViewHolder) -> Unit) =
                MovableSongViewHolder(ItemPlaylistSongBinding.inflate(viewGroup.layoutInflater, viewGroup, false), onClick, onShowMenu, onMoveClick)
        }
    }
}