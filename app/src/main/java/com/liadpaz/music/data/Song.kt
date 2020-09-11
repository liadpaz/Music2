package com.liadpaz.music.data

import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.liadpaz.music.utils.extensions.artist

data class Song(val mediaId: Long, val mediaUri: Uri, val title: String, val artist: String, val album: String, val artUri: Uri, val duration: Int)

fun MediaMetadataCompat.findArtists() =
    artistsRegex.findAll(artist.toString()).toList().map(MatchResult::value)

fun MediaMetadataCompat.firstFirstArtist() = findArtists()[0]

fun MediaDescriptionCompat.findArtists(): List<String> = artistsRegex.findAll(subtitle.toString()).toList().map(MatchResult::value)

private val artistsRegex = Regex("([^ &,]([^,&])*[^ ,&]+)")