package dev.peterbot.skarmkoll.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.peterbot.skarmkoll.data.local.ScreenshotDao
import dev.peterbot.skarmkoll.data.local.SkarmKollDatabase
import javax.inject.Singleton

/**
 * Tillhandahåller databasen och dess DAO som singletons i hela appen.
 * Hilt sköter livscykeln; vi behöver aldrig instansiera dessa manuellt.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SkarmKollDatabase =
        Room.databaseBuilder(
            context,
            SkarmKollDatabase::class.java,
            "skarmkoll.db"
        ).build()

    @Provides
    fun provideScreenshotDao(database: SkarmKollDatabase): ScreenshotDao =
        database.screenshotDao()
}
