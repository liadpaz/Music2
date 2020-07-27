package com.liadpaz.music.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.liadpaz.music.databinding.FragmentNowPlayingBinding
import com.liadpaz.music.ui.viewmodels.MainViewModel
import com.liadpaz.music.ui.viewmodels.PlayingViewModel
import com.liadpaz.music.utils.InjectorUtils

class NowPlayingFragment : Fragment() {

    private val mainViewModel by viewModels<MainViewModel> {
        InjectorUtils.provideMainViewModelFactory(requireContext())
    }
    private val viewModel by viewModels<PlayingViewModel> {
        InjectorUtils.providePlayingViewModelFactory(requireActivity().application)
    }
    private lateinit var binding: FragmentNowPlayingBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        FragmentNowPlayingBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        mainViewModel
    }
}