package com.liadpaz.music.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.liadpaz.music.databinding.FragmentPlaylistsBinding
import com.liadpaz.music.ui.viewmodels.PlaylistsViewModel
import com.liadpaz.music.utils.InjectorUtils

class PlaylistsFragment : Fragment() {

    private val viewModel by viewModels<PlaylistsViewModel> {
        InjectorUtils.providePlaylistsViewModelFactory(requireContext())
    }
    private lateinit var binding: FragmentPlaylistsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        FragmentPlaylistsBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    }
}