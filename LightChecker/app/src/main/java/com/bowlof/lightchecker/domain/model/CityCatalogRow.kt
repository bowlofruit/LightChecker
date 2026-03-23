package com.bowlof.lightchecker.domain.model

/** Мінімальні поля довідника міста для резолву з геолокації (без залежності data-layer). */
data class CityCatalogRow(
    val cityId: String,
    val displayName: String,
)
