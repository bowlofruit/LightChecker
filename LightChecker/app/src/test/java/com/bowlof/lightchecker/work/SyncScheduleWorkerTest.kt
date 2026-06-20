@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.bowlof.lightchecker.work

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.bowlof.lightchecker.data.local.db.LightCheckerDatabase
import com.bowlof.lightchecker.domain.repository.ScheduleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncScheduleWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val repo = mockk<ScheduleRepository>(relaxed = true)
    private lateinit var db: LightCheckerDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, LightCheckerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun buildWorker(
        vararg data: Pair<String, String>,
        runAttemptCount: Int = 0,
    ): SyncScheduleWorker =
        TestListenableWorkerBuilder<SyncScheduleWorker>(context)
            .setInputData(workDataOf(*data))
            .setRunAttemptCount(runAttemptCount)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = SyncScheduleWorker(appContext, workerParameters, repo, db)
            })
            .build()

    @Test
    fun `missing region returns failure`() = runTest {
        val worker = buildWorker(SyncScheduleWorker.KEY_QUEUE to "q")
        assertEquals(ListenableWorker.Result.failure(), worker.doWork())
    }

    @Test
    fun `valid input calls syncIfNewerVersion and succeeds`() = runTest {
        val worker = buildWorker(
            SyncScheduleWorker.KEY_REGION to "kyiv",
            SyncScheduleWorker.KEY_QUEUE to "1",
            SyncScheduleWorker.KEY_VERSION to "5",
            SyncScheduleWorker.KEY_DAY to "20260620",
        )
        assertEquals(ListenableWorker.Result.success(), worker.doWork())
        coVerify { repo.syncIfNewerVersion("kyiv", "1", 5L, 20260620L) }
    }

    @Test
    fun `repository failure retries before attempt limit`() = runTest {
        coEvery { repo.syncIfNewerVersion(any(), any(), any(), any()) } throws RuntimeException("net")
        val worker = buildWorker(
            SyncScheduleWorker.KEY_REGION to "r",
            SyncScheduleWorker.KEY_QUEUE to "q",
            runAttemptCount = 0,
        )
        assertEquals(ListenableWorker.Result.retry(), worker.doWork())
    }

    @Test
    fun `repository failure fails after attempt limit`() = runTest {
        coEvery { repo.syncIfNewerVersion(any(), any(), any(), any()) } throws RuntimeException("net")
        val worker = buildWorker(
            SyncScheduleWorker.KEY_REGION to "r",
            SyncScheduleWorker.KEY_QUEUE to "q",
            runAttemptCount = 4,
        )
        assertEquals(ListenableWorker.Result.failure(), worker.doWork())
    }
}
