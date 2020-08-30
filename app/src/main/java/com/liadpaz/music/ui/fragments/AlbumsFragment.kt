package com.liadpaz.music.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.liadpaz.music.R
import com.liadpaz.music.databinding.FragmentAlbumsBinding
import com.liadpaz.music.ui.adapters.AlbumsAdapter
import com.liadpaz.music.ui.utils.ItemOffsetDecoration
import com.liadpaz.music.ui.viewmodels.AlbumsViewModel
import com.liadpaz.music.utils.InjectorUtils

class AlbumsFragment : Fragment() {

    private val viewModel by viewModels<AlbumsViewModel> {
        InjectorUtils.provideAlbumsViewModelFactory(requireContext())
    }
    private lateinit var binding: FragmentAlbumsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        FragmentAlbumsBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.rvAlbums.adapter = AlbumsAdapter { mediaItem ->
            findNavController().navigate(MainFragmentDirections.actionMainFragmentToAlbumFragment(mediaItem))
        }
        binding.rvAlbums.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvAlbums.addItemDecoration(ItemOffsetDecoration(16))

        binding.rvAlbums.updatePadding(bottom = requireActivity().resources.let { it.getDimensionPixelSize(it.getIdentifier("navigation_bar_height", "dimen", "android")) + it.getDimension(R.dimen.bottomSheetHeight).toInt() })
    }
}