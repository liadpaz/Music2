package com.liadpaz.music.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.liadpaz.music.R
import com.liadpaz.music.databinding.FragmentArtistsBinding
import com.liadpaz.music.ui.adapters.ArtistsAdapter
import com.liadpaz.music.ui.viewmodels.MainViewModel
import com.liadpaz.music.utils.InjectorUtils

class ArtistsFragment : Fragment() {
    private val viewModel by activityViewModels<MainViewModel> {
        InjectorUtils.provideMainViewModelFactory(requireContext())
    }
    private lateinit var binding: FragmentArtistsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        FragmentArtistsBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.rvArtists.adapter = ArtistsAdapter { mediaItem ->
            findNavController().navigate(MainFragmentDirections.actionMainFragmentToArtistFragment(mediaItem.description.subtitle.toString()))
        }
        binding.rvArtists.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvArtists.updatePadding(bottom = requireActivity().resources.let { it.getDimensionPixelSize(it.getIdentifier("navigation_bar_height", "dimen", "android")) + it.getDimension(R.dimen.bottomSheetHeight).toInt() })
    }
}