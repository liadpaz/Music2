package com.liadpaz.music.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.fragment.navArgs
import com.liadpaz.music.databinding.DialogEditPlaylistBinding
import com.liadpaz.music.ui.viewmodels.PlaylistsViewModel
import com.liadpaz.music.utils.InjectorUtils

class EditPlaylistDialog : DialogFragment() {

    private val args by navArgs<EditPlaylistDialogArgs>()

    private val viewModel by viewModels<PlaylistsViewModel>({
        ViewModelStoreOwner { requireActivity().viewModelStore }
    }) {
        InjectorUtils.providePlaylistsViewModelFactory(requireContext())
    }
    private lateinit var binding: DialogEditPlaylistBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        DialogEditPlaylistBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.etPlaylistName.setText(args.playlist.description.title.toString())
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnApply.setOnClickListener {
            // TODO: implement
        }
        binding.btnDelete.setOnClickListener {
            viewModel.deletePlaylist(args.playlist.description.title.toString())
            dismiss()
        }
    }
}