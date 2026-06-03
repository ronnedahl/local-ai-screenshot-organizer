package dev.peterbot.skarmkoll.work

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.peterbot.skarmkoll.data.local.ScreenshotDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Kör OCR på alla obearbetade skärmdumpar och fyller på `ocrText` i Room.
 *
 * Varför WorkManager och inte bara en coroutine i ViewModel?
 * - Batchen kan vara tung (många bilder) och ska INTE dö för att användaren lämnar
 *   skärmen eller appen hamnar i bakgrunden. WorkManager kör jobbet till slut och
 *   schemalägger om det om processen dödas.
 * - Det är rätt verktyg för "gör det här pålitligt, någon gång snart, utanför UI:t".
 * En coroutine i ViewModel hade bundits till skärmens livscykel och tappat arbetet.
 *
 * @HiltWorker låter Hilt injicera DAO och ML Kit-klienten via konstruktorn
 * (jfr HiltWorkerFactory som registreras i SkarmKollApp).
 */
@HiltWorker
class OcrWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dao: ScreenshotDao,
    private val recognizer: TextRecognizer
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val unprocessed = dao.getUnprocessed()
        for (row in unprocessed) {
            try {
                val image = InputImage.fromFilePath(applicationContext, Uri.parse(row.uri))
                // ML Kit returnerar en Task; Tasks.await blockerar den här IO-tråden
                // tills igenkänningen är klar (vi är redan utanför huvudtråden).
                val recognized = Tasks.await(recognizer.process(image))
                val text = recognized.text.takeIf { it.isNotBlank() }
                dao.markOcrProcessed(
                    id = row.mediaStoreId,
                    ocrText = text,
                    processedAt = System.currentTimeMillis() / 1000
                )
            } catch (e: Exception) {
                // Fel per bild (korrupt fil, ingen text, borttagen URI) får inte fälla
                // hela jobbet. Vi loggar och går vidare; raden förblir obearbetad och
                // plockas upp nästa körning.
                Log.w(TAG, "OCR misslyckades för ${row.uri}", e)
            }
        }
        Result.success()
    }

    companion object {
        private const val TAG = "OcrWorker"
        const val UNIQUE_NAME = "ocr_batch"
    }
}
