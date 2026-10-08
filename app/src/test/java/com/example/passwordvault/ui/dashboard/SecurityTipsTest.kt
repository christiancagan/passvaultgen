package com.example.passwordvault.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityTipsTest {

    @Test
    fun forEpochDay_firstDay_returnsFirstTip() {
        assertEquals(SecurityTips.tips[0], SecurityTips.forEpochDay(0))
    }

    @Test
    fun forEpochDay_wrapsAroundList() {
        val size = SecurityTips.tips.size.toLong()
        assertEquals(SecurityTips.tips[1], SecurityTips.forEpochDay(size + 1))
    }

    @Test
    fun forEpochDay_consecutiveDaysGiveConsecutiveTips() {
        for (day in 0L until SecurityTips.tips.size - 1) {
            assertEquals(SecurityTips.tips[(day + 1).toInt()], SecurityTips.forEpochDay(day + 1))
        }
    }

    @Test
    fun forEpochDay_negativeDay_stillInRange() {
        assertTrue(SecurityTips.forEpochDay(-1) in SecurityTips.tips)
    }
}
