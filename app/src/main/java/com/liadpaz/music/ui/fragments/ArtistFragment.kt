package com.liadpaz.music.ui.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.liadpaz.music.R
import com.liadpaz.music.databinding.FragmentArtistBinding
import com.liadpaz.music.ui.adapters.SongsAdapter
import com.liadpaz.music.ui.viewmodels.ArtistViewModel
import com.liadpaz.music.ui.viewmodels.PlayingViewModel
import com.liadpaz.music.utils.InjectorUtils
import com.liadpaz.music.utils.extensions.isNullOrZero

class ArtistFragment : Fragment() {
    private val navArgs by navArgs<ArtistFragmentArgs>()

    private val viewModel by viewModels<ArtistViewModel> {
        InjectorUtils.provideArtistViewModelFactory(requireContext(), navArgs.artist)
    }
    private val playingViewModel by viewModels<PlayingViewModel> {
        InjectorUtils.providePlayingViewModelFactory(requireActivity().application)
    }
    private lateinit var binding: FragmentArtistBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        FragmentArtistBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.rvSongs.adapter = SongsAdapter(viewModel::play, { anchor, mediaItem ->
            PopupMenu(requireContext(), anchor, Gravity.NO_GRAVITY, android.R.attr.contextPopupMenuStyle, android.R.attr.contextPopupMenuStyle).apply {
                inflate(R.menu.menu_song)
                menu.findItem(R.id.menu_go_to_artist).isVisible = false
                if (playingViewModel.queue.value?.size.isNullOrZero()) {
                    menu.findItem(R.id.menu_play_next).isVisible = false
                    menu.findItem(R.id.menu_add_to_queue).isVisible = false
                }
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.menu_play_next -> playingViewModel.addNextQueueItem(mediaItem.description)
                        R.id.menu_add_to_queue -> playingViewModel.addQueueItem(mediaItem.description)
                        R.id.menu_go_to_album -> findNavController().navigate(ArtistFragmentDirections.actionArtistFragmentToAlbumFragment(mediaItem.description.description.toString()))
                        R.id.menu_add_to_playlist -> findNavController().navigate(ArtistFragmentDirections.actionArtistFragmentToAddPlaylistDialog(intArrayOf(mediaItem.mediaId!!.toInt())))
                    }
                    true
                }
            }.show()
        }, viewModel::playShuffle)
        binding.rvSongs.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.rvSongs.updatePadding(bottom = requireActivity().resources.let { it.getDimensionPixelSize(it.getIdentifier("navigation_bar_height", "dimen", "android")) + it.getDimension(R.dimen.bottomSheetHeight).toInt() })
    }
}