package com.liadpaz.music.utils

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.liadpaz.music.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object Util {
    suspend fun getBitmap(glide: GlideRequests, uri: Uri, @DrawableRes errorDrawable: Int = 0): Bitmap =
        suspendCancellableCoroutine { cont ->
            glide.asBitmap().apply {
                if (errorDrawable != 0) {
                    error(R.drawable.ic_album_color)
                }
            }.load(uri).into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) =
                    cont.resume(resource)

                override fun onLoadFailed(errorDrawable: Drawable?) =
                    cont.resume((errorDrawable as VectorDrawable).toBitmap())

                override fun onLoadCleared(placeholder: Drawable?) = Unit
            })
        }
}