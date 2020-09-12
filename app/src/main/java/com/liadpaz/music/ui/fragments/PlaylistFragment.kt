package com.liadpaz.music.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.liadpaz.music.R
import com.liadpaz.music.databinding.FragmentPlaylistBinding
import com.liadpaz.music.service.PLAYLIST_RECENTLY_ADDED
import com.liadpaz.music.service.utils.PLAYLISTS_ROOT
import com.liadpaz.music.ui.adapters.PlaylistAdapter
import com.liadpaz.music.ui.viewmodels.PlayingViewModel
import com.liadpaz.music.ui.viewmodels.PlaylistViewModel
import com.liadpaz.music.utils.InjectorUtils
import com.liadpaz.music.utils.extensions.isNullOrZero

class PlaylistFragment : Fragment() {
    private val args by navArgs<PlaylistFragmentArgs>()

    private val viewModel by viewModels<PlaylistViewModel> {
        InjectorUtils.providePlaylistViewModelFactory(requireContext(), args.playlist.mediaId!!)
    }
    private val playingViewModel by activityViewModels<PlayingViewModel> {
        InjectorUtils.providePlayingViewModelFactory(requireActivity().application)
    }
    private lateinit var binding: FragmentPlaylistBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(args.playlist.description.title != getString(R.string.playlist_recently_added))
        return FragmentPlaylistBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        ItemTouchHelper(object : ItemTouchHelper.Callback() {
            private var fromPosition = -1

            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
                makeMovementFlags(if (viewHolder.adapterPosition != 0) ItemTouchHelper.UP or ItemTouchHelper.DOWN else 0, 0)

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                if (target.adapterPosition != 0) {
                    recyclerView.adapter?.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
                    return true
                }
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder!!.itemView.setBackgroundColor(Color.BLACK)
                    fromPosition = viewHolder.adapterPosition
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                val typedArray = requireActivity().theme.obtainStyledAttributes(R.style.AppTheme, intArrayOf(R.attr.selectableItemBackground))
                val backgroundId = typedArray.getResourceIdOrThrow(0)
                typedArray.recycle()
                viewHolder.itemView.setBackgroundResource(backgroundId)
                if (fromPosition != -1 && fromPosition != viewHolder.adapterPosition) {
                    viewModel.moveSong(fromPosition - 1, viewHolder.adapterPosition - 1)
                    fromPosition = -1
                }
            }

            override fun isLongPressDragEnabled(): Boolean = false
        }).also {
            binding.rvSongs.adapter = PlaylistAdapter(args.playlist.mediaId.toString(), viewModel::play, { anchor, mediaItem, position ->
                PopupMenu(requireContext(), anchor, Gravity.NO_GRAVITY, android.R.attr.contextPopupMenuStyle, android.R.attr.contextPopupMenuStyle).apply {
                    inflate(R.menu.menu_playlist_song)
                    if (playingViewModel.queue.value?.size.isNullOrZero()) {
                        menu.findItem(R.id.menu_play_next).isVisible = false
                        menu.findItem(R.id.menu_add_to_queue).isVisible = false
                    }
                    if (args.playlist.mediaId == "${PLAYLISTS_ROOT}${PLAYLIST_RECENTLY_ADDED}") {
                        menu.findItem(R.id.menu_remove_from_playlist).isVisible = false
                    }
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.menu_play_next -> playingViewModel.addNextQueueItem(mediaItem.description)
                            R.id.menu_add_to_queue -> playingViewModel.addQueueItem(mediaItem.description)
                            R.id.menu_go_to_album -> findNavController().navigate(PlaylistFragmentDirections.actionPlaylistFragmentToAlbumFragment(mediaItem.description.description.toString()))
                            R.id.menu_go_to_artist -> findNavController().navigate(PlaylistFragmentDirections.actionPlaylistFragmentToGoToArtistDialog(mediaItem))
                            R.id.menu_remove_from_playlist -> {
                                viewModel.deleteSong(position - 1)
                                (binding.rvSongs.adapter as PlaylistAdapter).onItemRemoved(position - 1)
                            }
                        }
                        true
                    }
                }.show()
            }, viewModel::playShuffle, it::startDrag)
        }.attachToRecyclerView(binding.rvSongs)

        binding.rvSongs.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.rvSongs.updatePadding(bottom = requireActivity().resources.let { it.getDimensionPixelSize(it.getIdentifier("navigation_bar_height", "dimen", "android")) + it.getDimension(R.dimen.bottomSheetHeight).toInt() })
    }
}