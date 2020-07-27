package com.liadpaz.music.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.liadpaz.music.databinding.FragmentArtistBinding
import com.liadpaz.music.ui.adapters.SongsAdapter
import com.liadpaz.music.ui.viewmodels.ArtistViewModel
import com.liadpaz.music.utils.InjectorUtils

class ArtistFragment : Fragment() {

    private val navArgs by navArgs<ArtistFragmentArgs>()

    private val viewModel by viewModels<ArtistViewModel> {
        InjectorUtils.provideArtistViewModelFactory(requireContext(), navArgs.artist.mediaId!!)
    }
    private lateinit var binding: FragmentArtistBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        FragmentArtistBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.rvSongs.adapter = SongsAdapter { mediaItem ->
            viewModel.play(mediaItem)
        }
        binding.rvSongs.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
    }
}