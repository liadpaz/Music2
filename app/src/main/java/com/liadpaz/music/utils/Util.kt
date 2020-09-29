package com.liadpaz.music.utils

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun getBitmap(glide: GlideRequests, uri: Uri): Bitmap =
	suspendCancellableCoroutine { cont ->
		glide.asBitmap().load(uri).into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) =
                cont.resume(resource)

            override fun onLoadFailed(errorDrawable: Drawable?) =
                cont.resume(errorDrawable!!.toBitmap())

            override fun onLoadCleared(placeholder: Drawable?) = Unit
        })
	}

fun MediaDescriptionCompat.Builder.from(description: MediaDescriptionCompat): MediaDescriptionCompat.Builder {
	setMediaId(description.mediaId)
	setExtras(description.extras)
	setTitle(description.title)
	setSubtitle(description.subtitle)
	setDescription(description.description)
	setMediaUri(description.mediaUri)
	setIconUri(description.iconUri)
	return this
}

private const val TAG = "Util"
