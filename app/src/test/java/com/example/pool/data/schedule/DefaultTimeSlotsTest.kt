package com.example.pool.data.schedule

import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultTimeSlotsTest {
    @Test
    fun defaultTimeSlots_matchBuaaFourteenPeriodSchedule() {
        val slots = ScheduleRepositoryImpl.defaultTimeSlots()

        assertEquals((1..14).toList(), slots.map { it.sectionNumber })
        assertEquals(8 * 60, slots.first().startTimeMinutes)
        assertEquals(12 * 60 + 15, slots[4].endTimeMinutes)
        assertEquals(14 * 60, slots[5].startTimeMinutes)
        assertEquals(19 * 60, slots[10].startTimeMinutes)
        assertEquals(22 * 60 + 15, slots.last().endTimeMinutes)
    }
}
