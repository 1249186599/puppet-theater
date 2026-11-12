package com.example.woodenpuppetdemo.content

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class TheatreLevelTest {
    private fun settle(l: TheatreLevel,n: Int=600) { repeat(n) { l.advance(.033f) } }
    private fun power(l: TheatreLevel) {
        assertTrue(l.select("gear"));settle(l)
        assertTrue(l.hasGear);assertTrue(l.select("box"));settle(l)
        assertTrue(l.installed);assertTrue(l.powered)
    }
    @Test fun cannotSkipPuzzleOrTeleportUpstairs() {
        val l=TheatreLevel()
        assertFalse(l.select("box"));assertFalse(l.select("lift"));assertFalse(l.select("exit"))
        assertFalse(l.select("floor",WalkPoint(.5f,-1.6f)))
        assertFalse(l.select("floor",WalkPoint(Float.NaN,-.6f)))
        settle(l);assertEquals(TheatreLevel.START,l.position);assertFalse(l.completed)
    }
    @Test fun fullJourneyRidesSmoothlyAndExitsThroughOpenEnd() {
        val l=TheatreLevel();power(l);assertTrue(l.select("lift"))
        var riding=false;var previous=TheatreLevel.BASE_HEIGHT
        repeat(1000) {
            val frame=l.advance(.033f)
            if(l.position.z < -.94f) assertTrue("Must enter between rails",abs(l.position.x)<.01f)
            if(l.rising) {
                riding=true;assertFalse(frame.moving);assertEquals(TheatreLevel.LIFT,l.position)
                assertTrue(l.liftHeight>=previous);assertTrue(l.liftHeight-previous<.005f)
                previous=l.liftHeight
                assertFalse(l.select("gear"));assertFalse(l.select("floor",WalkPoint(.5f,-.6f)))
            }
        }
        assertTrue(riding);assertTrue(l.upstairs);assertEquals(TheatreLevel.TOP_HEIGHT,l.liftHeight,.0001f)
        assertTrue(l.select("exit"))
        var reachedLanding=false
        repeat(600) {
            val f=l.advance(.033f)
            if(f.position.z <= -1.56f) reachedLanding=true
            if(!reachedLanding) assertTrue("Exit through open rear end",abs(f.position.x)<.01f)
        }
        assertTrue(reachedLanding)
        assertTrue(l.completed)
        val distance=kotlin.math.hypot(l.position.x-(.42f-.36588475f/2f),l.position.z-(-1.934f))
        assertTrue("Wait outside door swing",distance>.45f)
        assertFalse(l.select("floor",TheatreLevel.START))
    }
    @Test fun cancelBoardingReturnsThroughFrontOpening() {
        val l=TheatreLevel();power(l);l.select("lift")
        repeat(600) {
            l.advance(.033f)
            if(l.position.z < -1.02f && !l.rising) {
                assertTrue(l.select("floor",WalkPoint(-.5f,-.7f)))
                repeat(600) {
                    l.advance(.033f)
                    if(l.position.z < -.94f) assertTrue(abs(l.position.x)<.01f)
                }
                assertFalse(l.rising);assertFalse(l.upstairs);return
            }
        }
        fail("Should reach lift approach")
    }
    @Test fun resetWhileRisingRestoresGroundAndAllFlags() {
        val l=TheatreLevel();power(l);l.select("lift")
        repeat(600) {
            l.advance(.033f)
            if(l.rising && l.liftHeight>.10f) {
                l.reset();assertEquals(TheatreLevel.START,l.position)
                assertFalse(l.hasGear);assertFalse(l.installed);assertFalse(l.powered)
                assertFalse(l.rising);assertFalse(l.upstairs);assertFalse(l.completed)
                assertEquals(TheatreLevel.BASE_HEIGHT,l.liftHeight,.0001f)
                settle(l);assertEquals(TheatreLevel.START,l.position);return
            }
        };fail("Expected lift to rise")
    }
    @Test fun elevatedFloorCannotLeadOffPlatform() {
        val l=TheatreLevel();power(l);l.select("lift");settle(l,1000)
        assertFalse(l.select("floor",TheatreLevel.START))
        assertTrue(l.select("floor",WalkPoint(-10f,-10f)));settle(l)
        assertTrue(l.position.x>=-.65f);assertTrue(l.position.z>=-1.65f)
        assertEquals(TheatreLevel.TOP_HEIGHT,l.heightAt(l.position),.0001f)
    }
    @Test fun cuesFireOnceAndResetCanReplay() {
        val l=TheatreLevel();val events=TheatreSoundEvents();val heard=mutableListOf<AdventureCue>()
        fun run(n: Int=600) { repeat(n) { l.advance(.033f);heard+=events.collect(l) } }
        l.select("gear");run();l.select("box");run();l.select("lift");run(1000);l.select("exit");run()
        assertEquals(listOf(AdventureCue.GEAR_PICKUP,AdventureCue.GEAR_SEATED,AdventureCue.DRIVE_START,
            AdventureCue.LIFT_RUN,AdventureCue.LIFT_ARRIVE,AdventureCue.VICTORY),heard)
        l.reset();events.reset();assertTrue(events.collect(l).isEmpty());l.select("gear");run()
        assertEquals(2,heard.count { it==AdventureCue.GEAR_PICKUP })
    }
}
