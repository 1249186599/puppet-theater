package com.example.woodenpuppetdemo.content
import org.junit.Assert.*
import org.junit.Test
class WalkSoundTimingTest {
    @Test fun followsPhaseOncePerHalfCycleAndWrap() {
        val t = WalkSoundTiming()
        assertNull(t.update(true, 0.1f))
        assertNull(t.update(true, 3f))
        assertEquals(AdventureCue.STEP_LEFT, t.update(true, 3.2f))
        assertNull(t.update(true, 3.3f))
        assertNull(t.update(true, 6.2f))
        assertEquals(AdventureCue.STEP_RIGHT, t.update(true, 0.1f))
    }
    @Test fun stoppingAndResetDoNotCreatePhantomStep() {
        val t = WalkSoundTiming()
        t.update(true, 4f)
        assertNull(t.update(false, 0f))
        assertNull(t.update(true, 0.1f))
        t.update(true, 4f); t.reset()
        assertNull(t.update(true, 0.1f))
    }
}
