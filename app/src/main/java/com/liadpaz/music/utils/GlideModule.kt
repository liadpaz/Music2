package com.liadpaz.music.utils

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.liadpaz.music.R

@GlideModule
class GlideModule : AppGlideModule() {

    override fun isManifestParsingEnabled(): Boolean = false

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setDefaultRequestOptions(RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).placeholder(R.drawable.ic_album).error(R.drawable.ic_album).fallback(R.drawable.ic_album))
    }
}