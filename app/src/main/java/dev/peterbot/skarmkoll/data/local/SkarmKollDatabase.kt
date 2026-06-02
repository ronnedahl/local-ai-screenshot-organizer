package dev.peterbot.skarmkoll.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room-databasen. En enda entitet räcker för fas 1.
 *
 * exportSchema = false: vi har inga migrationer ännu och vill inte tvinga fram en
 * schemamapp i fas 1. När datamodellen stabiliseras (eller inför fas 2) slår vi på
 * schema-export och skriver riktiga migrationer istället för destruktiv fallback.
 */
@Database(
    entities = [ScreenshotEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SkarmKollDatabase : RoomDatabase() {
    abstract fun screenshotDao(): ScreenshotDao
}
