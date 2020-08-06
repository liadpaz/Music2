package com.liadpaz.music.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.liadpaz.music.databinding.FragmentExtendedBinding
import com.liadpaz.music.ui.viewmodels.MainViewModel
import com.liadpaz.music.ui.viewmodels.PlayingViewModel
import com.liadpaz.music.utils.InjectorUtils

class ExtendedFragment : Fragment() {

    private lateinit var mainViewModel: MainViewModel
    private lateinit var viewModel: PlayingViewModel
    private lateinit var binding: FragmentExtendedBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        FragmentExtendedBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel =
            ViewModelProvider(requireActivity().viewModelStore, InjectorUtils.providePlayingViewModelFactory(requireActivity().application))[PlayingViewModel::class.java]

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        mainViewModel =
            ViewModelProvider(requireActivity().viewModelStore, InjectorUtils.provideMainViewModelFactory(requireActivity()))[MainViewModel::class.java]
    }
}

private const val TAG = "ExtendedFragment"