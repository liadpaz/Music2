package com.liadpaz.music.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelStoreOwner
import com.liadpaz.music.databinding.DialogCreatePlaylistBinding
import com.liadpaz.music.ui.viewmodels.PlaylistsViewModel
import com.liadpaz.music.utils.InjectorUtils

class CreatePlaylistDialog : DialogFragment() {

    private val viewModel by viewModels<PlaylistsViewModel>({
        ViewModelStoreOwner { requireActivity().viewModelStore }
    }, {
        InjectorUtils.providePlaylistsViewModelFactory(requireContext())
    })
    private lateinit var binding: DialogCreatePlaylistBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        DialogCreatePlaylistBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnApply.setOnClickListener {
            binding.etPlaylistName.text.toString().takeIf { it.isNotEmpty() }?.let {
                if (viewModel.createNewPlaylist(it)) {
                    dismiss()
                } else {
                    // TODO: notify the user that the playlist name is already taken
                }
            }
        }
    }
}