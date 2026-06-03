package dev.peterbot.skarmkoll.di

import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Tillhandahåller ML Kits on-device textigenkännare (latinskt skript).
 *
 * Vi använder den paketerade/lokala modellen — ingen nedladdning, inga molnanrop.
 * En singleton räcker: klienten är trådsäker och dyr att skapa, så vi återanvänder
 * den över hela OCR-batchen istället för att instansiera per bild.
 */
@Module
@InstallIn(SingletonComponent::class)
object RecognizerModule {

    @Provides
    @Singleton
    fun provideTextRecognizer(): TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
}
