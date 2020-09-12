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
import com.liadpaz.music.databinding.DialogEditPlaylistBinding
import com.liadpaz.music.service.utils.PLAYLISTS_ROOT
import com.liadpaz.music.ui.viewmodels.PlaylistsViewModel
import com.liadpaz.music.utils.InjectorUtils

class EditPlaylistDialog : DialogFragment() {
    private val args by navArgs<EditPlaylistDialogArgs>()

    private val viewModel by activityViewModels<PlaylistsViewModel> {
        InjectorUtils.providePlaylistsViewModelFactory(requireContext())
    }
    private lateinit var binding: DialogEditPlaylistBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        DialogEditPlaylistBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.etPlaylistName.setText(args.playlist.description.title.toString())
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnApply.setOnClickListener {
            viewModel.changeName(args.playlist.mediaId!!.substring(PLAYLISTS_ROOT.length), binding.etPlaylistName.text.toString())
            dismiss()
        }
        binding.etPlaylistName.addTextChangedListener {
            binding.btnApply.isEnabled = !it.isNullOrEmpty() && it.toString() != getString(R.string.playlist_recently_added) && it.toString() !in viewModel.playlists
        }
        binding.btnDelete.setOnClickListener {
            viewModel.deletePlaylist(args.playlist.description.title.toString())
            dismiss()
        }
    }
}