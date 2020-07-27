package com.liadpaz.music.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import com.liadpaz.music.databinding.FragmentSongsBinding
import com.liadpaz.music.ui.adapters.SongsAdapter
import com.liadpaz.music.ui.viewmodels.SongsViewModel
import com.liadpaz.music.utils.InjectorUtils

class SongsFragment : Fragment() {

    private val viewModel by viewModels<SongsViewModel> {
        InjectorUtils.provideSongsViewModelFactory(requireContext())
    }
    private lateinit var binding: FragmentSongsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentSongsBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.rvSongs.adapter = SongsAdapter { mediaItem ->
            viewModel.play(mediaItem)
        }
        binding.rvSongs.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
    }
}