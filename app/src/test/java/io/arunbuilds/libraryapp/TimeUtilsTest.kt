package io.arunbuilds.libraryapp

import io.arunbuilds.libraryapp.utils.TimeUtils
import org.junit.Test

import org.junit.Assert.*


class TimeUtilsTest {
    @Test
    fun `test if 0 seconds returns 00_00_00`() {
        assertEquals(
            "00:00:00",
            TimeUtils.getTimeStampFromElapsedSeconds(0)
        )
    }

    @Test
    fun `test if 10 seconds returns 00_00_10`() {
        assertEquals(
            "00:00:10",
            TimeUtils.getTimeStampFromElapsedSeconds(10)
        )
    }

    @Test
    fun `test if 181 seconds returns 00_03_01`() {
        assertEquals(
            "00:03:01",
            TimeUtils.getTimeStampFromElapsedSeconds(181)
        )
    }
}