package com.liadpaz.music.utils

import android.app.Application
import android.content.Context
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.ServiceConnection
import com.liadpaz.music.ui.viewmodels.*

object InjectorUtils {

    fun provideMainViewModelFactory(context: Context) =
        MainViewModel.Factory(ServiceConnection.getInstance(context), Repository.getInstance(context))

    fun provideSongsViewModelFactory(context: Context) =
        SongsViewModel.Factory(ServiceConnection.getInstance(context), Repository.getInstance(context))

    fun providePlaylistsViewModelFactory(context: Context) =
        PlaylistsViewModel.Factory(ServiceConnection.getInstance(context), Repository.getInstance(context))

    fun providePlaylistViewModelFactory(context: Context, playlist: String) =
        PlaylistViewModel.Factory(ServiceConnection.getInstance(context), Repository.getInstance(context), playlist)

    fun provideAlbumsViewModelFactory(context: Context) =
        AlbumsViewModel.Factory(ServiceConnection.getInstance(context), Repository.getInstance(context))

    fun provideAlbumViewModelFactory(context: Context, album: String) =
        AlbumViewModel.Factory(ServiceConnection.getInstance(context), Repository.getInstance(context), album)

    fun provideArtistsViewModelFactory(context: Context) =
        ArtistsViewModel.Factory(ServiceConnection.getInstance(context), Repository.getInstance(context))

    fun provideArtistViewModelFactory(context: Context, artist: String) =
        ArtistViewModel.Factory(ServiceConnection.getInstance(context), Repository.getInstance(context), artist)

    fun providePlayingViewModelFactory(application: Application) =
        PlayingViewModel.Factory(application, ServiceConnection.getInstance(application), Repository.getInstance(application))

    fun provideExtendedSongViewModelFactory(context: Context) =
        ExtendedSongViewModel.Factory(ServiceConnection.getInstance(context), Repository.getInstance(context))
}