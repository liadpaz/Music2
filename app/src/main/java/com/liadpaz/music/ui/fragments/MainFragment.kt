package com.liadpaz.music.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.liadpaz.music.R
import com.liadpaz.music.databinding.FragmentMainBinding

class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return FragmentMainBinding.inflate(inflater, container, false).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val titles =
            arrayOf(R.string.nav_songs, R.string.nav_playlists, R.string.nav_albums, R.string.nav_artists)

        binding.mainViewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 4

            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> SongsFragment()
                1 -> PlaylistsFragment()
                2 -> AlbumsFragment()
                3 -> ArtistsFragment()
                else -> throw IndexOutOfBoundsException()
            }
        }

        TabLayoutMediator(binding.tabLayout, binding.mainViewPager) { tab, position ->
            tab.text = getString(titles[position])
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
    }
}