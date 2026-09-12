package com.example.guione

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * Gets a dish photo into the app and hands it to [ScanStore].
 *
 * Capture goes through the system camera app (ACTION_IMAGE_CAPTURE behind
 * [ActivityResultContracts.TakePicture]) rather than an embedded CameraX
 * preview. Two reasons: the calling app needs no CAMERA permission that way, so
 * there is no runtime-permission flow to get wrong in a demo; and the system
 * camera already gives the user focus and exposure control, which matters more
 * for a flat overhead shot than a custom viewfinder would.
 *
 * The cost is that the framing guide stays advisory — the UI tells the user to
 * hold the phone flat overhead, it cannot enforce it. For a poster prototype
 * that is the right trade; a study deployment would want CameraX so the overhead
 * constraint can be checked from the accelerometer before the shutter fires.
 */
class DishCapture internal constructor(
    private val context: Context,
    private val launchCamera: (Uri) -> Unit,
    private val launchGallery: () -> Unit,
) {
    fun fromCamera() = launchCamera(newPhotoUri(context))
    fun fromGallery() = launchGallery()
}

@Composable
fun rememberDishCapture(onCaptured: (Bitmap) -> Unit): DishCapture {
    val context = LocalContext.current

    // TakePicture reports only success/failure, so the destination Uri has to be
    // remembered across the activity hop.
    val pending = remember { arrayOfNulls<Uri>(1) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pending[0]
        pending[0] = null
        if (ok && uri != null) decodeUpright(context, uri)?.let(onCaptured)
    }

    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { decodeUpright(context, it)?.let(onCaptured) } }

    return remember {
        DishCapture(
            context = context,
            launchCamera = { uri -> pending[0] = uri; camera.launch(uri) },
            launchGallery = {
                gallery.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
        )
    }
}

/** Longest edge kept after downsampling. The model sees 300 px; this is headroom. */
private const val MAX_EDGE = 1280

private fun newPhotoUri(context: Context): Uri {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    // One reused filename: these frames are disposable and a demo should not
    // quietly fill the cache with full-resolution photos.
    val file = File(dir, "dish_capture.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/**
 * Decodes [uri] downsampled to [MAX_EDGE] and rotates it upright from its EXIF
 * orientation.
 *
 * Both steps matter. A modern phone photo decoded at full size is tens of
 * megabytes for an image that is about to become 300x300. And most cameras write
 * the sensor frame plus an orientation tag rather than rotating pixels, so
 * skipping the EXIF step feeds the model sideways food — which it will happily
 * score, because the training augmentation included rotations and flips, so a
 * sideways plate produces a confident and subtly wrong answer rather than a
 * visibly broken one.
 */
private fun decodeUpright(context: Context, uri: Uri): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_EDGE)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        val degrees = context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        if (degrees == 0f) decoded else rotate(decoded, degrees)
    } catch (e: Exception) {
        null
    }
}

private fun rotate(src: Bitmap, degrees: Float): Bitmap {
    val m = Matrix().apply { postRotate(degrees) }
    val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    if (out !== src) src.recycle()
    return out
}

private fun sampleSizeFor(width: Int, height: Int, maxEdge: Int): Int {
    var sample = 1
    var w = width
    var h = height
    while (w / 2 >= maxEdge || h / 2 >= maxEdge) {
        w /= 2
        h /= 2
        sample *= 2
    }
    return sample
}
