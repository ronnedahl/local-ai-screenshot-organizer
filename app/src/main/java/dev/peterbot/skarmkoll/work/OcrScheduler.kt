package dev.peterbot.skarmkoll.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schemalägger OCR-batchen. Inkapslar WorkManager-detaljerna så att repository/ViewModel
 * bara behöver säga "kör OCR vid tillfälle".
 */
@Singleton
class OcrScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Köar OCR-jobbet som unikt arbete. KEEP gör att vi inte staplar flera batcher på
     * varandra — om en redan är på gång bearbetar den ändå allt som är obearbetat när
     * den körs. Nästa synk köar en ny batch när den föregående är klar.
     */
    fun schedule() {
        val request = OneTimeWorkRequestBuilder<OcrWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            OcrWorker.UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
