package dev.peterbot.skarmkoll

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * App-klassen. @HiltAndroidApp genererar Hilt-komponenten som binder ihop hela
 * objektgrafen (Room, repositories, ViewModels, workers).
 *
 * Vi implementerar [Configuration.Provider] för att WorkManager ska kunna skapa
 * våra @HiltWorker-jobb via [HiltWorkerFactory]. Det innebär att OCR-workern i fas 1.3
 * kan få in sina beroenden (DAO, ML Kit-klient) genom konstruktorn istället för att
 * slå upp dem manuellt. Vi stänger av WorkManagers automatiska initialisering i
 * manifestet så att den här konfigurationen verkligen används.
 */
@HiltAndroidApp
class SkarmKollApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
