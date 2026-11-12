package io.github.magisk317.uikit.surface

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** An application icon is identified only by its installed package and requested size. */
data class AppIconRequest(
    val packageName: String?,
    val sizePx: Int,
)

internal fun AppIconRequest.cacheKey(): String =
    "app_icon:${packageName.orEmpty()}:$sizePx"

object AppIconLoader {

    private val icons by lazy {
        object : LruCache<String, ImageBitmap>(2 * 1024 * 1024) {
            override fun sizeOf(key: String, value: ImageBitmap): Int =
                value.width * value.height * 4
        }
    }

    /** Package identity is required; display labels must not trigger an all-apps scan. */
    internal fun resolvePackageName(packageName: String?): String? =
        packageName?.takeIf(String::isNotBlank)

    internal fun load(context: Context, request: AppIconRequest): ImageBitmap? {
        val packageName = resolvePackageName(request.packageName) ?: return null
        val normalizedRequest = request.copy(sizePx = request.sizePx.coerceIn(1, MAX_ICON_SIZE_PX))
        val cacheKey = normalizedRequest.cacheKey()
        icons.get(cacheKey)?.let { return it }
        val pm = context.packageManager
        val image = try {
            pm.getApplicationInfo(packageName, 0).loadIcon(pm).toBitmap(
                width = normalizedRequest.sizePx,
                height = normalizedRequest.sizePx,
                config = Bitmap.Config.ARGB_8888,
            ).asImageBitmap()
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        } catch (_: RuntimeException) {
            return null
        }
        icons.put(cacheKey, image)
        return image
    }
}

@Composable
fun AppIconImage(
    packageName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    contentDescription: String? = null,
    fallbackIcon: ImageVector = Icons.Default.Android,
) {
    val context = LocalContext.current.applicationContext
    val density = LocalDensity.current
    val targetIconPx = remember(size, density) {
        with(density) { size.roundToPx() }.coerceIn(1, MAX_ICON_SIZE_PX)
    }
    val request = remember(packageName, targetIconPx) {
        AppIconRequest(packageName = packageName, sizePx = targetIconPx)
    }
    Box(modifier = modifier.size(size)) {
        key(request) {
            val image by produceState<ImageBitmap?>(initialValue = null, context, request) {
                value = withContext(Dispatchers.IO) { AppIconLoader.load(context, request) }
            }
            val loadedImage = image
            if (loadedImage == null) {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(size),
                    tint = MaterialTheme.colorScheme.outline,
                )
            } else {
                Image(
                    bitmap = loadedImage,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(size).clip(MaterialTheme.shapes.small),
                )
            }
        }
    }
}

private const val MAX_ICON_SIZE_PX = 256
