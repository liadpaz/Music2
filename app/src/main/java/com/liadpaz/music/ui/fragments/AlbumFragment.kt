package com.liadpaz.music.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.liadpaz.music.R
import com.liadpaz.music.databinding.FragmentAlbumBinding
import com.liadpaz.music.ui.adapters.SongsAdapter
import com.liadpaz.music.ui.viewmodels.AlbumViewModel
import com.liadpaz.music.utils.InjectorUtils

class AlbumFragment : Fragment() {

    private val navArgs by navArgs<AlbumFragmentArgs>()

    private val viewModel by viewModels<AlbumViewModel> {
        InjectorUtils.provideAlbumViewModelFactory(requireContext(), navArgs.album.mediaId!!)
    }
    private lateinit var binding: FragmentAlbumBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        FragmentAlbumBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.rvSongs.adapter = SongsAdapter { mediaItem ->
            viewModel.play(mediaItem)
        }
        binding.rvSongs.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        binding.rvSongs.updatePadding(bottom = requireActivity().resources.let { it.getDimensionPixelSize(it.getIdentifier("navigation_bar_height", "dimen", "android")) + it.getDimension(R.dimen.bottomSheetHeight).toInt() })
    }
}