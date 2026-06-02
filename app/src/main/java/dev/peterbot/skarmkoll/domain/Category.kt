package dev.peterbot.skarmkoll.domain

/**
 * Kategorier för en skärmdump. Konstanterna lever i koden (engelsk-ish, stabila),
 * medan de svenska etiketterna som visas för användaren mappas i UI-lagret via
 * strings.xml — så att domänen förblir fri från Android-beroenden.
 *
 * Lagras som [name]-sträng i Room (se ScreenshotEntity.category).
 */
enum class Category {
    KVITTO, PRODUKT, KOD, TRANING, RESA, JOBB, OVRIGT;

    companion object {
        /** Tål okända/korrupta värden från databasen utan att krascha. */
        fun fromStorage(value: String?): Category =
            entries.firstOrNull { it.name == value } ?: OVRIGT
    }
}
