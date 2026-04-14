package com.bowlof.lightchecker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bowlof.lightchecker.data.local.db.LightCheckerDatabase
import com.bowlof.lightchecker.data.messaging.FirebaseTopicManager
import com.bowlof.lightchecker.domain.model.LocationSource
import io.mockk.coJustRun
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocationsRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var db: LightCheckerDatabase
    private lateinit var topics: FirebaseTopicManager
    private lateinit var repo: LocationsRepositoryImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, LightCheckerDatabase::class.java).build()
        topics = mockk()
        coJustRun { topics.syncSubscriptionsAfterDataChange() }
        repo = LocationsRepositoryImpl(db, context, topics)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `only one widget primary after two additions with primary flag`() = runTest {
        repo.addPlace(
            regionId = "r1",
            queueId = "q1",
            cityId = "c1",
            cityDisplayName = "A",
            queueDisplayName = "1.1",
            latitude = null,
            longitude = null,
            locationSource = LocationSource.USER_MANUAL,
            setAsWidgetPrimary = true,
        )
        repo.addPlace(
            regionId = "r2",
            queueId = "q2",
            cityId = "c2",
            cityDisplayName = "B",
            queueDisplayName = "2.1",
            latitude = null,
            longitude = null,
            locationSource = LocationSource.USER_MANUAL,
            setAsWidgetPrimary = true,
        )
        val primaries = db.savedLocationDao().getAllSnapshot().count { it.isWidgetPrimary }
        assertEquals(1, primaries)
        val primary = db.savedLocationDao().getAllSnapshot().single { it.isWidgetPrimary }
        assertEquals("r2", primary.regionId)
    }

    @Test
    fun `setWidgetPrimary clears previous primary`() = runTest {
        val id1 = repo.addPlace(
            "r1", "q1", "c1", "A", "1.1",
            null, null, LocationSource.USER_MANUAL,
            setAsWidgetPrimary = true,
        )
        val id2 = repo.addPlace(
            "r2", "q2", "c2", "B", "2.1",
            null, null, LocationSource.USER_MANUAL,
            setAsWidgetPrimary = false,
        )
        repo.setWidgetPrimary(id2)
        val rows = db.savedLocationDao().getAllSnapshot()
        assertTrue(rows.single { it.id == id1 }.isWidgetPrimary.not())
        assertTrue(rows.single { it.id == id2 }.isWidgetPrimary)
    }
}
