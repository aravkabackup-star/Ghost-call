package com.example

import com.example.scheduler.GhostCallScheduler
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testGhostCallSchedulerCalculatesFutureTime() {
        val now = System.currentTimeMillis()
        val nextOccurrence = GhostCallScheduler.calculateNextOccurrence(
            targetHour = 19,
            targetMinute = 30,
            selectedDays = setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)
        )
        assertTrue("Next occurrence should be in the future", nextOccurrence > now)
    }
}

