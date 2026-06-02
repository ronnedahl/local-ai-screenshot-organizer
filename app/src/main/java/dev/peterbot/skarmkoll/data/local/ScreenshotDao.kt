package dev.peterbot.skarmkoll.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Dataåtkomst för skärmdumpar.
 *
 * Läs-queries returnerar [Flow] så att UI-lagret kan observera databasen reaktivt:
 * när OCR-workern fyller på text eller användaren byter kategori uppdateras listan
 * automatiskt utan manuell refresh. Skriv-operationer är `suspend` och körs utanför
 * huvudtråden av Room.
 */
@Dao
interface ScreenshotDao {

    /**
     * Lägg till nyupptäckta skärmdumpar. IGNORE gör synken idempotent: en bild vi redan
     * känner till (samma mediaStoreId) rörs inte, så vi skriver inte över ocrText/kategori
     * som redan satts.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNew(rows: List<ScreenshotEntity>)

    @Upsert
    suspend fun upsert(row: ScreenshotEntity)

    /** Alla aktiva skärmdumpar, nyaste först. */
    @Query("SELECT * FROM screenshots WHERE status = :status ORDER BY dateAdded DESC")
    fun observeByStatus(status: String): Flow<List<ScreenshotEntity>>

    /** Skärmdumpar som ännu inte OCR-bearbetats (för WorkManager-batchen i 1.3). */
    @Query("SELECT * FROM screenshots WHERE processedAt IS NULL")
    suspend fun getUnprocessed(): List<ScreenshotEntity>

    @Query("SELECT * FROM screenshots WHERE mediaStoreId = :id")
    fun observeById(id: Long): Flow<ScreenshotEntity?>

    /** Mängden id:n vi redan har, för att räkna ut vad som är nytt vid synk. */
    @Query("SELECT mediaStoreId FROM screenshots")
    suspend fun getAllIds(): List<Long>

    @Query("UPDATE screenshots SET category = :category WHERE mediaStoreId = :id")
    suspend fun updateCategory(id: Long, category: String)

    @Query("UPDATE screenshots SET status = :status WHERE mediaStoreId = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE screenshots SET ocrText = :ocrText, category = :category, processedAt = :processedAt WHERE mediaStoreId = :id")
    suspend fun updateOcrResult(id: Long, ocrText: String?, category: String, processedAt: Long)

    @Query("DELETE FROM screenshots WHERE mediaStoreId = :id")
    suspend fun deleteById(id: Long)
}
