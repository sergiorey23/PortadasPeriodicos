package sergirex.portadasperiodicos.presentation.portadas

import androidx.annotation.StringRes
import sergirex.portadasperiodicos.R
import sergirex.portadasperiodicos.domain.model.HomeTab
import sergirex.portadasperiodicos.domain.model.PeriodicoCategory

@get:StringRes
val HomeTab.titleRes: Int
    get() = when (this) {
        HomeTab.Favorites -> R.string.fav_tab
        is HomeTab.Category -> when (category) {
            PeriodicoCategory.GENERAL -> R.string.first_tab
            PeriodicoCategory.DEPORTES -> R.string.second_tab
            PeriodicoCategory.ECONOMIA -> R.string.third_tab
            PeriodicoCategory.LOCALES -> R.string.fourth_tab
            PeriodicoCategory.INTERNACIONAL -> R.string.fifth_tab
        }
    }
