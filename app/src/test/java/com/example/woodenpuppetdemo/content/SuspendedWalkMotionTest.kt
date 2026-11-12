package com.example.woodenpuppetdemo.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuspendedWalkMotionTest {
    @Test fun bodyStartsMovingDuringSwingAndSettlesAfterPlant() {
        val motion = SuspendedWalkMotion()
        val first = motion.update(StepVector(0f, -0.05f), -1f, 0.016f)
        assertTrue(first.z < 0f)
        assertTrue(first.z > -0.07f)
        assertTrue(first.y > 0f)
        assertTrue(first.rollRadians < 0f)
        var settled = first
        repeat(100) { settled = motion.update(null, 0f, 0.016f) }
        assertEquals(-0.07f, settled.z, 0.003f)
        assertEquals(0f, settled.y, 0.001f)
        assertEquals(0f, settled.rollRadians, 0.005f)
    }

    @Test fun oppositeSwingCanReverseAndIdleDoesNotDrift() {
        val motion = SuspendedWalkMotion()
        repeat(30) { motion.update(null, 0f, 0.016f) }
        val forward = motion.update(StepVector(0f, -0.05f), -1f, 0.016f)
        assertTrue(forward.z < 0f)
        var back = motion.update(StepVector(0f, 0.05f), 1f, 0.016f)
        repeat(100) { back = motion.update(null, 0f, 0.016f) }
        assertEquals(0f, back.z, 0.003f)
    }
}
