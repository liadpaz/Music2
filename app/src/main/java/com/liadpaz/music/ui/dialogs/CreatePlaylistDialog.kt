package com.liadpaz.music.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.liadpaz.music.R
import com.liadpaz.music.databinding.DialogCreatePlaylistBinding
import com.liadpaz.music.ui.viewmodels.PlaylistsViewModel
import com.liadpaz.music.utils.InjectorUtils

class CreatePlaylistDialog : DialogFragment() {
    private val args by navArgs<CreatePlaylistDialogArgs>()

    private val viewModel by activityViewModels<PlaylistsViewModel> {
        InjectorUtils.providePlaylistsViewModelFactory(requireContext())
    }
    private lateinit var binding: DialogCreatePlaylistBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        DialogCreatePlaylistBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnApply.setOnClickListener {
            viewModel.createNewPlaylist(binding.etPlaylistName.text.toString(), args.songs)
            dismiss()
        }
        binding.etPlaylistName.addTextChangedListener {
            binding.btnApply.isEnabled = !it.isNullOrEmpty() && it.toString() != getString(R.string.playlist_recently_added) && it.toString() !in viewModel.playlists
        }
    }
}