package dev.peterbot.skarmkoll.ui.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Hur mycket åtkomst vi faktiskt har till bilderna just nu. */
enum class MediaAccess {
    /** Full åtkomst till alla bilder. */
    FULL,

    /** Android 14+: användaren delade bara utvalda bilder. Vi jobbar med det vi fått. */
    PARTIAL,

    /** Ingen åtkomst — visa förklaring och be om behörighet. */
    NONE
}

/**
 * Samlar all plattformsversion-logik kring bildbehörigheter på ett ställe.
 *
 * - Android 13+ (API 33+): granulära READ_MEDIA_IMAGES.
 * - Android 14+ (API 34+): användaren kan välja "delvis åtkomst" via
 *   READ_MEDIA_VISUAL_USER_SELECTED — då har vi PARTIAL, inte NONE.
 * - Android < 13: READ_EXTERNAL_STORAGE.
 */
object MediaPermissions {

    /** Behörigheterna vi ska begära i systemdialogen, beroende på API-nivå. */
    val required: Array<String> =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            )
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

    fun currentAccess(context: Context): MediaAccess {
        val fullImages = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            isGranted(context, Manifest.permission.READ_MEDIA_IMAGES)

        val legacyStorage = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
            isGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE)

        val partialSelected = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            isGranted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

        return when {
            fullImages || legacyStorage -> MediaAccess.FULL
            partialSelected -> MediaAccess.PARTIAL
            else -> MediaAccess.NONE
        }
    }

    /** Tolkar resultatet från systemdialogen till en [MediaAccess]. */
    fun accessFromResult(granted: Map<String, Boolean>): MediaAccess {
        val images = granted[Manifest.permission.READ_MEDIA_IMAGES] == true
        val legacy = granted[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        val partial = granted[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true
        return when {
            images || legacy -> MediaAccess.FULL
            partial -> MediaAccess.PARTIAL
            else -> MediaAccess.NONE
        }
    }

    private fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
