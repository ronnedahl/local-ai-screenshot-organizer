package dev.peterbot.skarmkoll.ui

import androidx.annotation.StringRes
import dev.peterbot.skarmkoll.R
import dev.peterbot.skarmkoll.domain.Category

/**
 * Mappar en [Category] till dess svenska etikett i strings.xml. Bor i UI-lagret så att
 * domänens enum förblir fri från Android-resurser (och lätt att enhetstesta).
 */
@get:StringRes
val Category.labelRes: Int
    get() = when (this) {
        Category.KVITTO -> R.string.cat_kvitto
        Category.PRODUKT -> R.string.cat_produkt
        Category.KOD -> R.string.cat_kod
        Category.TRANING -> R.string.cat_traning
        Category.RESA -> R.string.cat_resa
        Category.JOBB -> R.string.cat_jobb
        Category.OVRIGT -> R.string.cat_ovrigt
    }
