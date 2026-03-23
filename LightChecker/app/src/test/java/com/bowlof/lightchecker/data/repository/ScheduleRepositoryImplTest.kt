package com.bowlof.lightchecker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.bowlof.lightchecker.data.local.db.LightCheckerDatabase
import com.bowlof.lightchecker.data.remote.FirestoreScheduleDataSource
import com.bowlof.lightchecker.data.remote.dto.FirestoreScheduleDto
import com.bowlof.lightchecker.domain.time.KyivTime
import io.mockk.coEvery
import io.mockk.coVerify
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
class ScheduleRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var db: LightCheckerDatabase
    private lateinit var remote: FirestoreScheduleDataSource
    private lateinit var repo: ScheduleRepositoryImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, LightCheckerDatabase::class.java).build()
        remote = mockk()
        repo = ScheduleRepositoryImpl(context, db, remote)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `sequential refresh for today then tomorrow keeps both days in db`() = runTest {
        val today = KyivTime.todayYyyymmdd()
        val tomorrow = KyivTime.tomorrowYyyymmdd()
        coEvery { remote.fetchSchedule(any()) } returnsMany listOf(
            FirestoreScheduleDto(1, 1L, today, listOf(0, 60), null),
            FirestoreScheduleDto(1, 1L, tomorrow, listOf(120, 180), null),
        )
        repo.refreshSchedule("reg1", "q1")
        repo.refreshSchedule("reg1", "q1")
        assertEquals(1, db.outageSlotDao().getSlots("reg1", "q1", today).size)
        assertEquals(1, db.outageSlotDao().getSlots("reg1", "q1", tomorrow).size)
    }

    @Test
    fun `second refresh same day replaces slots`() = runTest {
        val today = KyivTime.todayYyyymmdd()
        coEvery { remote.fetchSchedule(any()) } returnsMany listOf(
            FirestoreScheduleDto(1, 1L, today, listOf(0, 60), null),
            FirestoreScheduleDto(1, 2L, today, listOf(300, 360), null),
        )
        repo.refreshSchedule("reg1", "q1")
        repo.refreshSchedule("reg1", "q1")
        val slots = db.outageSlotDao().getSlots("reg1", "q1", today)
        assertEquals(1, slots.size)
        assertEquals(300, slots.first().startMinute)
        assertEquals(360, slots.first().endMinute)
    }

    @Test
    fun `syncIfNewerVersion skips fetch when meta version matches`() = runTest {
        val today = KyivTime.todayYyyymmdd()
        coEvery { remote.fetchSchedule(any()) } returns
            FirestoreScheduleDto(1, 5L, today, listOf(0, 30), null)
        repo.refreshSchedule("r", "q")
        coEvery { remote.fetchSchedule(any()) } returns
            FirestoreScheduleDto(1, 99L, today, listOf(30, 60), null)
        repo.syncIfNewerVersion("r", "q", 5L, today)
        assertEquals(0, db.outageSlotDao().getSlots("r", "q", today).first().startMinute)
        coVerify(exactly = 1) { remote.fetchSchedule(any()) }
    }
}
