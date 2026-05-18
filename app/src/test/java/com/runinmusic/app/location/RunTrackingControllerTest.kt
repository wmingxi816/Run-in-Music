package com.runinmusic.app.location

import com.runinmusic.app.core.run.RunLocationSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunTrackingControllerTest {
    @Test
    fun startRunMovesToRunningState() {
        val controller = RunTrackingController()

        controller.start(startedAtMillis = 1_000L)

        val state = controller.state.value
        assertEquals(RunTrackingStatus.Running, state.status)
        assertEquals(1_000L, state.startedAtMillis)
        assertEquals(0L, state.elapsedMillis)
        assertTrue(state.isRunning)
    }

    @Test
    fun locationUpdatesDistanceWhileRunning() {
        val controller = RunTrackingController()
        controller.start(startedAtMillis = 0L)

        controller.addLocation(RunLocationSample(latitude = 0.0, longitude = 0.0, accuracyMeters = 4f, timeMillis = 0L))
        controller.addLocation(RunLocationSample(latitude = 0.0, longitude = 0.001, accuracyMeters = 4f, timeMillis = 60_000L))

        val state = controller.state.value
        assertEquals(RunTrackingStatus.Running, state.status)
        assertEquals(111.2, state.distanceMeters, 1.0)
        assertEquals(60_000L, state.elapsedMillis)
    }

    @Test
    fun finishRunProducesFinishedSnapshot() {
        val controller = RunTrackingController()
        controller.start(startedAtMillis = 0L)
        controller.addLocation(RunLocationSample(latitude = 0.0, longitude = 0.0, accuracyMeters = 4f, timeMillis = 0L))

        val finished = controller.finish(endedAtMillis = 90_000L)

        assertEquals(RunTrackingStatus.Finished, finished.status)
        assertEquals(90_000L, finished.endedAtMillis)
        assertEquals(90_000L, finished.elapsedMillis)
        assertFalse(finished.isRunning)
        assertEquals(finished, controller.state.value)
    }
}
