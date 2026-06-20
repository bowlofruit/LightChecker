package com.bowlof.lightchecker.domain.catalog

import com.bowlof.lightchecker.domain.model.CityCatalog

/** Джерело статичного довідника міст/черг (абстракція над data-layer). */
interface CityCatalogProvider {

    suspend fun load(): Result<CityCatalog>
}
