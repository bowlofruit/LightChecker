package com.bowlof.lightchecker.domain.usecase

import com.bowlof.lightchecker.domain.model.CityCatalogRow
import javax.inject.Inject

class ResolveCityUseCase @Inject constructor() {

    /** Індекс міста в каталозі або `null`, якщо не вдалось зіставити. */
    fun resolveIndex(catalog: List<CityCatalogRow>, locality: String?): Int? {
        val loc = locality?.lowercase()?.trim().orEmpty()
        if (loc.isEmpty()) return null
        val idx = catalog.indexOfFirst { city ->
            val name = city.displayName.lowercase()
            val id = city.cityId.lowercase()
            name.contains(loc) || loc.contains(id)
        }
        return idx.takeIf { it >= 0 }
    }
}
