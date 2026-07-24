package io.kelly.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.app.ActivityOptionsCompat

@Composable
fun rememberLauncherForPickVisualMedia(
    maxItems: Int = 1,
    onResult: (List<Uri>) -> Unit
): ActivityResultLauncher<PickVisualMediaRequest> {
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        PickVisualMediaContract(maxItems)
    ) {
        currentOnResult(it)
    }
    return launcher
}


internal class PickVisualMediaContract(
    maxItems: Int = 1,
) : ActivityResultContract<PickVisualMediaRequest, List<Uri>>() {

    private val internalPicker = if (maxItems > 1) {
        ActivityResultContracts.PickMultipleVisualMedia(maxItems)
    } else {
        ActivityResultContracts.PickVisualMedia()
    }

    override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent {
        return internalPicker.createIntent(context, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        return when (val result = internalPicker.parseResult(resultCode, intent).apply {
        }) {
            is List<*> -> result.filterIsInstance<Uri>()
            is Uri -> listOf(result)
            else -> emptyList()
        }
    }
}

fun ActivityResultLauncher<PickVisualMediaRequest>.launchImageOnly(
    options: ActivityOptionsCompat? = null
) {
    launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        options
    )
}

fun ActivityResultLauncher<PickVisualMediaRequest>.launchVideoOnly(
    options: ActivityOptionsCompat? = null,
) {
    launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
        options
    )
}

fun ActivityResultLauncher<PickVisualMediaRequest>.launchImageAndVideo(
    options: ActivityOptionsCompat? = null,
) {
    launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
        options
    )
}

fun ActivityResultLauncher<PickVisualMediaRequest>.launchMimeType(
    mime: String,
    options: ActivityOptionsCompat? = null,
) {
    launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.SingleMimeType(mime)),
        options
    )
}
