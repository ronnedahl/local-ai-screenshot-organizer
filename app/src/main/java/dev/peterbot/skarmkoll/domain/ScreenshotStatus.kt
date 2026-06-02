package dev.peterbot.skarmkoll.domain

/**
 * Livscykelstatus för en skärmdump. Styr swipe-flödet (spara/arkivera/radera) senare.
 * I fas 1 raderar vi ALDRIG filen på disk — vi flaggar bara i databasen.
 *
 * Lagras som [name]-sträng i Room (se ScreenshotEntity.status).
 */
enum class ScreenshotStatus {
    ACTIVE, ARCHIVED, TRASH;

    companion object {
        fun fromStorage(value: String?): ScreenshotStatus =
            entries.firstOrNull { it.name == value } ?: ACTIVE
    }
}
