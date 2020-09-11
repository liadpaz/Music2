package com.liadpaz.music.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.liadpaz.music.R
import com.liadpaz.music.databinding.FragmentPlaylistBinding
import com.liadpaz.music.ui.adapters.SongsAdapter
import com.liadpaz.music.ui.viewmodels.PlayingViewModel
import com.liadpaz.music.ui.viewmodels.PlaylistViewModel
import com.liadpaz.music.utils.InjectorUtils

class PlaylistFragment : Fragment() {
    private val navArgs by navArgs<PlaylistFragmentArgs>()

    private val viewModel by viewModels<PlaylistViewModel> {
        InjectorUtils.providePlaylistViewModelFactory(requireContext(), navArgs.playlist.mediaId!!)
    }
    private val playingViewModel by activityViewModels<PlayingViewModel> {
        InjectorUtils.providePlayingViewModelFactory(requireActivity().application)
    }
    private lateinit var binding: FragmentPlaylistBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(navArgs.playlist.description.title != getString(R.string.playlist_recently_added))
        return FragmentPlaylistBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.rvSongs.adapter = SongsAdapter(viewModel::play, { anchor, mediaItem ->
            // TODO: popup menu on item click (playlist menu)
        }, viewModel::playShuffle)
        binding.rvSongs.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.rvSongs.updatePadding(bottom = requireActivity().resources.let { it.getDimensionPixelSize(it.getIdentifier("navigation_bar_height", "dimen", "android")) + it.getDimension(R.dimen.bottomSheetHeight).toInt() })
    }
}