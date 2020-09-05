package com.liadpaz.music.service.utils

//class FileMusicSource(context: Context) : AbstractMusicSource() {
//
//    private var catalog = emptyList<MediaMetadataCompat>()
//    private val provider = SongProvider(context)
//
//    private suspend fun updateCatalog(): List<MediaMetadataCompat>? = withContext(Dispatchers.IO) {
//        provider.getContentProviderValue()?.map { song ->
//            MediaMetadataCompat.Builder().from(song).build()
//        }
//    }
//}