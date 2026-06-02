package dev.peterbot.skarmkoll.data

import dev.peterbot.skarmkoll.data.local.ScreenshotDao
import dev.peterbot.skarmkoll.data.local.ScreenshotEntity
import dev.peterbot.skarmkoll.data.media.MediaStoreScreenshotSource
import dev.peterbot.skarmkoll.domain.Category
import dev.peterbot.skarmkoll.domain.Screenshot
import dev.peterbot.skarmkoll.domain.ScreenshotStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Resultat av en synk, så att UI:t kan visa vad som hände. */
data class SyncResult(val added: Int, val total: Int)

/**
 * Enda sanningskällan för skärmdumpar. Knyter ihop MediaStore (vad som finns på
 * enheten) med Room (vad vi vet om varje bild). UI-lagret pratar bara med det här.
 */
@Singleton
class ScreenshotRepository @Inject constructor(
    private val dao: ScreenshotDao,
    private val mediaSource: MediaStoreScreenshotSource
) {

    /** Aktiva skärmdumpar som en reaktiv ström av domänmodeller. */
    fun observeActive(): Flow<List<Screenshot>> =
        dao.observeByStatus(ScreenshotStatus.ACTIVE.name).map { rows -> rows.map(::toDomain) }

    /**
     * Synkar MediaStore → Room. Nya skärmdumpar läggs till med processedAt = null så att
     * OCR-batchen (1.3) plockar upp dem. IGNORE-strategin i DAO:n gör att redan kända
     * bilder inte skrivs över. Vi rör inte bilder som försvunnit ur MediaStore i fas 1.
     */
    suspend fun sync(): SyncResult {
        val onDevice = mediaSource.queryScreenshots()
        val known = dao.getAllIds().toHashSet()
        val newRows = onDevice
            .filter { it.mediaStoreId !in known }
            .map { ScreenshotEntity.newlyDiscovered(it.mediaStoreId, it.uri, it.dateAdded) }
        if (newRows.isNotEmpty()) dao.insertNew(newRows)
        return SyncResult(added = newRows.size, total = onDevice.size)
    }

    suspend fun setCategory(id: Long, category: Category) =
        dao.updateCategory(id, category.name)

    suspend fun setStatus(id: Long, status: ScreenshotStatus) =
        dao.updateStatus(id, status.name)

    private fun toDomain(e: ScreenshotEntity) = Screenshot(
        id = e.mediaStoreId,
        uri = e.uri,
        dateAdded = e.dateAdded,
        ocrText = e.ocrText,
        category = Category.fromStorage(e.category),
        status = ScreenshotStatus.fromStorage(e.status),
        processed = e.processedAt != null
    )
}
