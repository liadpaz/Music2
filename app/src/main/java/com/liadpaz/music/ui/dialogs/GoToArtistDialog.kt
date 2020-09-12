package com.liadpaz.music.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.liadpaz.music.databinding.DialogGoToArtistBinding
import com.liadpaz.music.ui.adapters.SmallArtistsAdapter
import com.liadpaz.music.utils.contentprovider.findArtists

class GoToArtistDialog : DialogFragment() {
    private val args by navArgs<GoToArtistDialogArgs>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        args.song.description.findArtists().let {
            if (it.size == 1) {
                findNavController().navigate(GoToArtistDialogDirections.actionGoToArtistDialogToArtistFragment(it[0]))
                return@let null
            }
            val binding = DialogGoToArtistBinding.inflate(inflater, container, false)
            binding.rvArtists.adapter = SmallArtistsAdapter { artist ->
                findNavController().navigate(GoToArtistDialogDirections.actionGoToArtistDialogToArtistFragment(artist))
            }
            binding.btnCancel.setOnClickListener { dismiss() }
            binding.artists = it
            binding.executePendingBindings()
            return@let binding.root
        }
}