package com.liadpaz.music.ui.adapters

import android.support.v4.media.MediaMetadataCompat
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.liadpaz.music.ui.utils.AlbumArtViewHolder
import com.liadpaz.music.utils.C

class AlbumArtViewPagerAdapter : ListAdapter<MediaMetadataCompat, AlbumArtViewHolder>(C.metadataDiffCallback) {
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumArtViewHolder = AlbumArtViewHolder.create(parent)

	override fun onBindViewHolder(holder: AlbumArtViewHolder, position: Int) = holder.bind(getItem(position))

	override fun submitList(list: List<MediaMetadataCompat>?) = super.submitList(list?.let { ArrayList(list) })
}