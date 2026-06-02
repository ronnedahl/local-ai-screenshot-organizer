package dev.peterbot.skarmkoll.domain

/**
 * Domänmodell för en skärmdump — det UI- och affärslogiken arbetar mot.
 * Skild från ScreenshotEntity så att lagrings-detaljer (strängar för enum) inte
 * läcker upp i UI:t, och så att enum-konvertering sker på ett enda ställe.
 */
data class Screenshot(
    val id: Long,
    val uri: String,
    val dateAdded: Long,
    val ocrText: String?,
    val category: Category,
    val status: ScreenshotStatus,
    val processed: Boolean
)
