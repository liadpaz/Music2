package com.liadpaz.music.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.liadpaz.music.R
import com.liadpaz.music.databinding.FragmentPlaylistsBinding
import com.liadpaz.music.ui.adapters.PlaylistsAdapter
import com.liadpaz.music.ui.viewmodels.PlaylistsViewModel
import com.liadpaz.music.utils.InjectorUtils

class PlaylistsFragment : Fragment() {

    private val viewModel by viewModels<PlaylistsViewModel>({
        ViewModelStoreOwner { requireActivity().viewModelStore }
    }) {
        InjectorUtils.providePlaylistsViewModelFactory(requireContext())
    }
    private lateinit var binding: FragmentPlaylistsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        FragmentPlaylistsBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.rvPlaylists.adapter = PlaylistsAdapter(onPlaylistClick = { playlist ->
            findNavController().navigate(MainFragmentDirections.actionMainFragmentToPlaylistFragment(playlist))
        }, onPlaylistLongClick = { playlist ->
            findNavController().navigate(MainFragmentDirections.actionMainFragmentToEditPlaylistDialog(playlist))
        }) {
            findNavController().navigate(MainFragmentDirections.actionMainFragmentToCreatePlaylistDialog())
        }
        binding.rvPlaylists.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.rvPlaylists.updatePadding(bottom = requireActivity().resources.let { it.getDimensionPixelSize(it.getIdentifier("navigation_bar_height", "dimen", "android")) + it.getDimension(R.dimen.bottomSheetHeight).toInt() })
    }
}