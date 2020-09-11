package com.liadpaz.music.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.liadpaz.music.databinding.DialogAddPlaylistBinding
import com.liadpaz.music.ui.adapters.SmallPlaylistsAdapter
import com.liadpaz.music.ui.viewmodels.MainViewModel
import com.liadpaz.music.ui.viewmodels.PlaylistsViewModel
import com.liadpaz.music.utils.InjectorUtils

class AddPlaylistDialog : DialogFragment() {
    private val args by navArgs<AddPlaylistDialogArgs>()

    private val viewModel by activityViewModels<MainViewModel> {
        InjectorUtils.provideMainViewModelFactory(requireContext())
    }
    private val playlistsViewModel by activityViewModels<PlaylistsViewModel> {
        InjectorUtils.providePlaylistsViewModelFactory(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DialogAddPlaylistBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.rvPlaylists.adapter = SmallPlaylistsAdapter { playlist ->
            playlistsViewModel.addSongsToPlaylist(playlist, args.songs)
//            Toast.makeText(requireContext(), , Toast.LENGTH_SHORT).show()
            // TODO: show 'added' toast
            dismiss()
        }
        binding.rvPlaylists.addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
        (binding.rvPlaylists.layoutManager as LinearLayoutManager)
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnNewPlaylist.setOnClickListener {
            findNavController().navigate(AddPlaylistDialogDirections.actionAddPlaylistDialogToCreatePlaylistDialog(args.songs))
        }
        return binding.root
    }
}