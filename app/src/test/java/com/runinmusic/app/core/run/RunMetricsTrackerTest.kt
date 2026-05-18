package com.runinmusic.app.core.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunMetricsTrackerTest {
    @Test
    fun firstGpsSampleDoesNotAddDistance() {
        val tracker = RunMetricsTracker(startedAtMillis = 0L)

        val snapshot = tracker.addSample(
            RunLocationSample(
                latitude = 31.2304,
                longitude = 121.4737,
                accuracyMeters = 5f,
                timeMillis = 1_000L,
            ),
        )

        assertEquals(0.0, snapshot.distanceMeters, 0.001)
        assertEquals(1_000L, snapshot.elapsedMillis)
        assertNull(snapshot.averagePaceSecondsPerKm)
    }

    @Test
    fun accumulatesDistanceBetweenGpsSamples() {
        val tracker = RunMetricsTracker(startedAtMillis = 0L)
        tracker.addSample(RunLocationSample(latitude = 0.0, longitude = 0.0, accuracyMeters = 4f, timeMillis = 0L))

        val snapshot = tracker.addSample(
            RunLocationSample(
                latitude = 0.0,
                longitude = 0.001,
                accuracyMeters = 4f,
                timeMillis = 60_000L,
            ),
        )

        assertEquals(111.2, snapshot.distanceMeters, 1.0)
    }

    @Test
    fun computesAveragePaceSecondsPerKm() {
        val tracker = RunMetricsTracker(startedAtMillis = 0L)
        tracker.addSample(RunLocationSample(latitude = 0.0, longitude = 0.0, accuracyMeters = 4f, timeMillis = 0L))

        val snapshot = tracker.addSample(
            RunLocationSample(
                latitude = 0.0,
                longitude = 0.008993216,
                accuracyMeters = 4f,
                timeMillis = 360_000L,
            ),
        )

        assertEquals(1_000.0, snapshot.distanceMeters, 2.0)
        assertEquals(360.0, snapshot.averagePaceSecondsPerKm!!, 2.0)
    }
}
