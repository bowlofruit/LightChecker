package com.bowlof.lightchecker.domain.usecase

import com.bowlof.lightchecker.domain.model.CatalogCity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveCityUseCaseTest {

    private val useCase = ResolveCityUseCase()

    private val catalog = listOf(
        CatalogCity("kyiv", "Київ", emptyList()),
        CatalogCity("lviv", "Львів", emptyList()),
    )

    @Test
    fun `matches display name substring`() {
        assertEquals(0, useCase.resolveIndex(catalog, "київ"))
    }

    @Test
    fun `matches city id in locality`() {
        assertEquals(1, useCase.resolveIndex(catalog, "near lviv center"))
    }

    @Test
    fun `null for blank locality`() {
        assertNull(useCase.resolveIndex(catalog, "   "))
    }
}
