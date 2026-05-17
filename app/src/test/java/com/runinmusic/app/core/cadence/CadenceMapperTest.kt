package com.runinmusic.app.core.cadence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CadenceMapperTest {
    @Test
    fun tenSecondStepCountConvertsToSpm() {
        assertEquals(84.0, CadenceMapper.stepsToSpm(14), 0.001)
    }

    @Test
    fun highSpmMapsToHalfBpm() {
        assertEquals(80.0, CadenceMapper.targetBpmForSpm(160.0), 0.001)
    }

    @Test
    fun lowSpmMapsDirectlyToBpm() {
        assertEquals(88.0, CadenceMapper.targetBpmForSpm(88.0), 0.001)
    }

    @Test
    fun doubledSongBpmCanStillMatchTarget() {
        assertTrue(CadenceMapper.isWithinTolerance(originalBpm = 168.0, targetBpm = 84.0))
    }
}
