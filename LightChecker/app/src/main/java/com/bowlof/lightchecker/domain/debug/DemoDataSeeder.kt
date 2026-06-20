package com.bowlof.lightchecker.domain.debug

/**
 * DEBUG-only utility: populates the local store with a coherent demo dataset
 * (places, today/tomorrow outage slots, sync meta, history) so the full app can
 * be demonstrated when there are no real outages. Replaces any existing local data.
 */
interface DemoDataSeeder {

    suspend fun seed()
}
