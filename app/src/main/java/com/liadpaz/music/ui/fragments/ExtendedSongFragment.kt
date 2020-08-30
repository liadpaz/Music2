package com.liadpaz.music.ui.fragments

import android.media.session.MediaSession
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.liadpaz.music.R
import com.liadpaz.music.databinding.FragmentExtendedSongBinding
import com.liadpaz.music.utils.GlideApp

class ExtendedSongFragment : Fragment() {

    private var queueItem: MediaSessionCompat.QueueItem? = null

    private lateinit var binding: FragmentExtendedSongBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            queueItem = it.getParcelable("song")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        FragmentExtendedSongBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        GlideApp.with(this).load(queueItem?.description?.iconUri).into(binding.ivArt)
    }

    companion object {
        @JvmStatic
        fun newInstance(song: MediaSessionCompat.QueueItem) =
            ExtendedSongFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("song", song)
                }
            }
    }
}