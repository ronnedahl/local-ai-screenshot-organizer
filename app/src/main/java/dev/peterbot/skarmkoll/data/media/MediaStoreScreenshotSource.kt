package dev.peterbot.skarmkoll.data.media

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** En skärmdump som den ser ut i MediaStore, innan den synkats till Room. */
data class MediaScreenshot(
    val mediaStoreId: Long,
    val uri: String,
    val dateAdded: Long
)

/**
 * Läser skärmdumpar ur MediaStore. Detta är appens enda fönster mot användarens
 * bilder — vi rör bara Screenshots-mappen och läser aldrig något annat.
 *
 * Versionsskillnad: RELATIVE_PATH finns först från Android 10 (API 29). På äldre
 * enheter (API 26–28) filtrerar vi istället på BUCKET_DISPLAY_NAME = "Screenshots".
 */
class MediaStoreScreenshotSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun queryScreenshots(): List<MediaScreenshot> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )

        val (selection, selectionArgs) = screenshotSelection()
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val result = mutableListOf<MediaScreenshot>()
        context.contentResolver.query(
            collection, projection, selection, selectionArgs, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val dateAdded = cursor.getLong(dateCol) // epoch-sekunder
                val uri = ContentUris.withAppendedId(collection, id)
                result += MediaScreenshot(
                    mediaStoreId = id,
                    uri = uri.toString(),
                    dateAdded = dateAdded
                )
            }
        }
        return result
    }

    /** Bygger ett WHERE-villkor som bara matchar skärmdumpar, anpassat efter API-nivå. */
    private fun screenshotSelection(): Pair<String, Array<String>> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?" to arrayOf("%Screenshots%")
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " = ?" to arrayOf("Screenshots")
        }
}
