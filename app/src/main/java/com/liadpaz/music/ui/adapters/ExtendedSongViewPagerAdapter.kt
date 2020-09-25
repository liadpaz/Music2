package com.liadpaz.music.ui.adapters

import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.liadpaz.music.ui.fragments.ExtendedSongFragment
import com.liadpaz.music.ui.viewmodels.ExtendedSongViewModel
import com.liadpaz.music.utils.InjectorUtils

class ExtendedSongViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    private val viewModel by fragmentActivity.viewModels<ExtendedSongViewModel> {
        InjectorUtils.provideExtendedSongViewModelFactory(fragmentActivity)
    }

    override fun getItemCount(): Int = viewModel.queueSize

    override fun createFragment(position: Int): Fragment = ExtendedSongFragment.newInstance(viewModel.queue[position])
}