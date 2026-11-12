package com.example.woodenpuppetdemo.content

import org.junit.Assert.*
import org.junit.Test

class AdventureLevelTest {
    private fun settle(level: AdventureLevel, frames: Int = 1000) {
        repeat(frames) {
            val p = level.advance(0.033f).position
            if (AdventureLevel.inRiver(p)) assertTrue("Must cross on bridge: $p", kotlin.math.abs(p.x) < 0.15f)
        }
    }
    @Test fun lockedBridgeBlocksCrossingAndLever() {
        val level = AdventureLevel()
        assertFalse(level.select(AdventureLevel.KEY))
        level.turnBridge(); settle(level, 60)
        assertFalse(level.bridgeReady)
        assertEquals(AdventureLevel.START, level.position)
    }
    @Test fun fullAdventureAndReset() {
        val level = AdventureLevel()
        level.select(AdventureLevel.PAD); settle(level)
        assertTrue(level.unlocked)
        level.turnBridge(); settle(level, 60)
        assertTrue(level.bridgeReady)
        level.select(AdventureLevel.KEY); settle(level)
        assertTrue(level.hasKey)
        level.select(AdventureLevel.EXIT); settle(level)
        assertTrue(level.completed)
        level.reset()
        assertFalse(level.unlocked); assertFalse(level.hasKey); assertFalse(level.completed)
        assertFalse(level.bridgeReady); assertEquals(AdventureLevel.START, level.position)
    }
    @Test fun reselectDuringCrossingStaysOnBridge() {
        val level = AdventureLevel()
        level.select(AdventureLevel.PAD); settle(level)
        level.turnBridge(); settle(level, 60)
        level.select(AdventureLevel.KEY)
        repeat(600) {
            level.advance(0.033f)
            if (AdventureLevel.inRiver(level.position)) {
                level.select(AdventureLevel.PAD); settle(level)
                assertTrue(level.position.z > -1.06f)
                return
            }
        }
        fail("Expected to enter the bridge")
    }
    @Test fun openingWaitPointClearsDoorSweep() {
        val level = AdventureLevel()
        level.select(AdventureLevel.PAD); settle(level)
        level.turnBridge(); settle(level,60)
        level.select(AdventureLevel.KEY); settle(level)
        level.select(AdventureLevel.EXIT); settle(level)
        assertTrue(level.completed)
        val p=level.position
        assertTrue(kotlin.math.hypot(p.x-AdventureLevel.EXIT.x,p.z-AdventureLevel.EXIT.z)<0.035f)
        // Swept leaf bounds plus 8cm puppet body margin, at any opening angle.
        val pivotX=0.42f-0.36588475f/2f
        val pivotZ=-1.834f
        val distance=kotlin.math.hypot(p.x-pivotX,p.z-pivotZ)
        assertTrue("Puppet must stand outside full leaf sweep",distance>0.37f+0.08f)
    }
}
