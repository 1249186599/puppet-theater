package com.example.woodenpuppetdemo.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickToWalkMotionTest {
    @Test fun rayHitsOnlyInsideTheStageInFrontOfTheController() {
        val hit = ClickToWalkMotion.boardIntersection(0f, 1.2f, -1.2f,
            0.1f, -1f, -0.2f, 0.531f)
        assertNotNull(hit)
        assertNotNull(ClickToWalkMotion.boardIntersection(0.75f, 1.2f, -1.2f,
            0f, -1f, 0f, 0.531f))
        assertEquals(0.0669f, hit!!.x, 0.002f)
        assertNull(ClickToWalkMotion.boardIntersection(0f, 1.2f, -1.2f,
            0f, 1f, 0f, 0.531f))
        assertNull(ClickToWalkMotion.boardIntersection(0f, 1.2f, -1.2f,
            2f, -1f, 0f, 0.531f))
    }

    @Test fun destinationClampsAndPuppetArrivesWithoutOvershoot() {
        val walk = ClickToWalkMotion()
        val destination = walk.setDestination(WalkPoint(2f, -3f))
        assertEquals(0.668528f, destination.x, 0.001f)
        assertEquals(-1.868528f, destination.z, 0.001f)
        var frame = walk.advance(0.033f)
        assertTrue(frame.moving)
        repeat(300) { frame = walk.advance(0.033f) }
        assertFalse(frame.moving)
        assertEquals(destination.x, frame.position.x, 0.001f)
        assertEquals(destination.z, frame.position.z, 0.001f)
    }

}
