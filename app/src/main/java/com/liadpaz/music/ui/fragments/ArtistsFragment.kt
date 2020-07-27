package com.liadpaz.music.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.liadpaz.music.databinding.FragmentArtistsBinding
import com.liadpaz.music.ui.adapters.ArtistsAdapter
import com.liadpaz.music.ui.utils.ItemOffsetDecoration
import com.liadpaz.music.ui.viewmodels.ArtistsViewModel
import com.liadpaz.music.utils.InjectorUtils

class ArtistsFragment : Fragment() {

    private val viewModel by viewModels<ArtistsViewModel> {
        InjectorUtils.provideArtistsViewModelFactory(requireContext())
    }
    private lateinit var binding: FragmentArtistsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        FragmentArtistsBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.rvArtists.adapter = ArtistsAdapter { mediaItem ->
            findNavController().navigate(ArtistsFragmentDirections.actionArtistsFragmentToArtistFragment(mediaItem))
        }
        binding.rvArtists.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvArtists.addItemDecoration(ItemOffsetDecoration(16))
    }
}