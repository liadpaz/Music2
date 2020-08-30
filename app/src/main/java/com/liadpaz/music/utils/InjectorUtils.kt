package com.liadpaz.music.utils

import android.app.Application
import android.content.ComponentName
import android.content.Context
import com.liadpaz.music.repository.Repository
import com.liadpaz.music.service.MusicService
import com.liadpaz.music.service.ServiceConnection
import com.liadpaz.music.ui.viewmodels.*

object InjectorUtils {

    fun provideMainViewModelFactory(context: Context) =
        MainViewModel.Factory(ServiceConnection.getInstance(context, ComponentName(context, MusicService::class.java)), Repository.getInstance(context))

    fun provideSongsViewModelFactory(context: Context) =
        SongsViewModel.Factory(ServiceConnection.getInstance(context, ComponentName(context, MusicService::class.java)), Repository.getInstance(context))

    fun providePlaylistsViewModelFactory(context: Context) =
        PlaylistsViewModel.Factory(Repository.getInstance(context))

    fun provideAlbumsViewModelFactory(context: Context) =
        AlbumsViewModel.Factory(ServiceConnection.getInstance(context, ComponentName(context, MusicService::class.java)), Repository.getInstance(context))

    fun provideAlbumViewModelFactory(context: Context, album: String) =
        AlbumViewModel.Factory(ServiceConnection.getInstance(context, ComponentName(context, MusicService::class.java)), Repository.getInstance(context), album)

    fun provideArtistsViewModelFactory(context: Context) =
        ArtistsViewModel.Factory(ServiceConnection.getInstance(context, ComponentName(context, MusicService::class.java)), Repository.getInstance(context))

    fun provideArtistViewModelFactory(context: Context, artist: String) =
        ArtistViewModel.Factory(ServiceConnection.getInstance(context, ComponentName(context, MusicService::class.java)), Repository.getInstance(context), artist)

    fun providePlayingViewModelFactory(application: Application) =
        PlayingViewModel.Factory(application, ServiceConnection.getInstance(application, ComponentName(application, MusicService::class.java)), Repository.getInstance(application))

    fun provideExtendedSongViewModelFactory(context: Context) =
        ExtendedSongViewModel.Factory(ServiceConnection.getInstance(context, ComponentName(context, MusicService::class.java)))
}