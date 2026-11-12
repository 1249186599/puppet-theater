package com.example.woodenpuppetdemo.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FootstepRecognizerTest {
    private val leftRest = StepVector(0.07f, 0.04f)
    private val rightRest = StepVector(-0.06f, 0.03f)

    private fun settle(r: FootstepRecognizer) {
        for (time in listOf(0L, 200L, 400L, 500L, 600L, 700L))
            assertNull(r.update(leftRest, rightRest, time))
    }

    private fun swingAndReturn(r: FootstepRecognizer, left: Boolean, dx: Float, dz: Float,
        at: Long): StepVector? {
        fun feed(amount: Float, time: Long): StepVector? {
            val base = if (left) leftRest else rightRest
            val moved = StepVector(base.x + dx * amount, base.z + dz * amount)
            return if (left) r.update(moved, rightRest, time)
                else r.update(leftRest, moved, time)
        }
        assertNull(feed(0.4f, at))
        assertNull(feed(1f, at + 50))
        val swing = feed(1f, at + 190)
        assertNull(feed(0.4f, at + 240))
        assertNull(feed(0f, at + 300))
        assertNull(feed(0f, at + 490))
        return swing
    }

    @Test fun deliberateSingleLegSwingsMoveAndRearmAtRest() {
        val r = FootstepRecognizer()
        settle(r)
        val forward = swingAndReturn(r, true, 0f, -0.05f, 800)!!
        assertEquals(-0.05f, forward.z, 0.001f)
        val repeated = swingAndReturn(r, true, 0f, -0.05f, 1500)!!
        assertEquals(-0.05f, repeated.z, 0.001f)
        val left = swingAndReturn(r, false, -0.05f, 0f, 2200)!!
        assertEquals(-0.05f, left.x, 0.001f)
    }

    @Test fun simultaneousBendAndUnevenReleaseNeverBecomeSteps() {
        val r = FootstepRecognizer()
        settle(r)
        val leftBent = StepVector(leftRest.x, leftRest.z - 0.05f)
        val rightBent = StepVector(rightRest.x, rightRest.z - 0.05f)
        assertNull(r.update(leftBent, rightBent, 800))
        assertNull(r.update(leftBent, rightBent, 1000))
        assertNull(r.update(leftRest, rightBent, 1100))
        assertNull(r.update(leftRest, rightBent, 1250))
        assertNull(r.update(leftRest, rightRest, 1350))
        assertNull(r.update(leftRest, rightRest, 1550))
        assertEquals(-0.05f, swingAndReturn(r, true, 0f, -0.05f, 1700)!!.z, 0.001f)
    }

    @Test fun trackingLossAndJumpDoNotStartSteps() {
        val r = FootstepRecognizer()
        settle(r)
        assertNull(r.update(StepVector(leftRest.x, leftRest.z - 0.09f), rightRest, 800))
        assertNull(r.update(null, rightRest, 900))
        assertNull(r.update(leftRest, rightRest, 1000))
        assertNull(r.update(leftRest, rightRest, 1200))
        assertNull(r.update(leftRest, rightRest, 1400))
    }
}
