package dev.peterbot.skarmkoll.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.peterbot.skarmkoll.domain.Category
import dev.peterbot.skarmkoll.domain.ScreenshotStatus

/**
 * En rad per skärmdump. Vi använder MediaStore-id:t som primärnyckel eftersom det är
 * den stabila kopplingen tillbaka till själva bilden i MediaStore — då blir synken
 * idempotent (samma bild ger alltid samma rad).
 *
 * [category] och [status] lagras som strängar (enum-namn) precis enligt datamodellen
 * i CLAUDE.md. Konvertering till/från enum sker vid domängränsen, inte här, så att
 * ett oväntat värde i databasen aldrig kan krascha Room-deserialiseringen.
 */
@Entity(tableName = "screenshots")
data class ScreenshotEntity(
    @PrimaryKey val mediaStoreId: Long,
    val uri: String,
    val dateAdded: Long,          // epoch-sekunder från MediaStore
    val ocrText: String?,         // null tills OCR körts
    val category: String,         // Category.name, default OVRIGT
    val status: String,           // ScreenshotStatus.name
    val processedAt: Long?         // när OCR/kategorisering kördes; null = ej bearbetad
) {
    companion object {
        /** Rad för en nyupptäckt skärmdump som ännu inte bearbetats. */
        fun newlyDiscovered(mediaStoreId: Long, uri: String, dateAdded: Long) =
            ScreenshotEntity(
                mediaStoreId = mediaStoreId,
                uri = uri,
                dateAdded = dateAdded,
                ocrText = null,
                category = Category.OVRIGT.name,
                status = ScreenshotStatus.ACTIVE.name,
                processedAt = null
            )
    }
}
