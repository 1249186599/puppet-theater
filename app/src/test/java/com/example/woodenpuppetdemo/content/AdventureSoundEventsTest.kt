package com.example.woodenpuppetdemo.content

import org.junit.Assert.*
import org.junit.Test

class AdventureSoundEventsTest {
    @Test fun entireRunEmitsOnceAndResetRearms() {
        val level = AdventureLevel()
        val events = AdventureSoundEvents()
        repeat(2) {
            val cues = mutableListOf<AdventureCue>()
            fun advance(n: Int) { repeat(n) { level.advance(0.033f); cues.addAll(events.collect(level)) } }
            assertTrue(events.collect(level).isEmpty())
            level.turnBridge(); advance(60)
            assertTrue(cues.isEmpty())
            level.select(AdventureLevel.PAD); advance(1000)
            level.turnBridge(); cues.addAll(events.collect(level))
            repeat(10) { level.turnBridge(); cues.addAll(events.collect(level)) }
            advance(60)
            level.select(AdventureLevel.KEY); advance(1000)
            level.select(AdventureLevel.EXIT); advance(1000)
            assertEquals(listOf(AdventureCue.PLATE, AdventureCue.BRIDGE_TURN,
                AdventureCue.BRIDGE_READY, AdventureCue.KEY, AdventureCue.VICTORY), cues)
            level.reset(); events.reset()
        }
    }
    @Test fun lockedSpamThrottledAndResetClearsCooldown() {
        val events = AdventureSoundEvents()
        assertTrue(events.lockedClick(0)); assertFalse(events.lockedClick(799))
        assertTrue(events.lockedClick(800)); assertFalse(events.lockedClick(801))
        events.reset(); assertTrue(events.lockedClick(802))
    }
    @Test fun resetDuringBridgeDoesNotEmitStaleCompletion() {
        val level = AdventureLevel(); val events = AdventureSoundEvents()
        level.select(AdventureLevel.PAD)
        repeat(1000) { level.advance(0.033f); events.collect(level) }
        level.turnBridge(); assertEquals(listOf(AdventureCue.BRIDGE_TURN), events.collect(level))
        level.reset(); events.reset()
        repeat(100) { level.advance(0.033f); assertTrue(events.collect(level).isEmpty()) }
    }
}
