package com.example.woodenpuppetdemo.content

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class FinaleLevelTest {
    private fun settle(l:FinaleLevel){repeat(3000){l.advance(.033f)}}
    private fun act(l:FinaleLevel,a:String){assertTrue(a,l.select(a));settle(l)}
    private fun prepare(l:FinaleLevel) {
        for(i in 0..2)act(l,"plate$i");act(l,"power")
        for(i in 0..2)repeat(3-i){act(l,"scenery$i")}
        act(l,"prop");act(l,"dock")
        repeat(2){act(l,"aimRight")};repeat(2){act(l,"brightness")}
        act(l,"lock0");act(l,"lock1");act(l,"winch");act(l,"practice")
    }
    private fun pose(l:FinaleLevel,left:Float,right:Float){l.setTrackedPull(left,right);repeat(25){l.advance(.033f)}}
    @Test fun rehearsalThenStageClickRunsAutomaticShowAndCelebrationWithoutMoreInput() {
        val l=FinaleLevel();prepare(l);assertEquals(5,l.stage)
        pose(l,1f,0f);pose(l,0f,1f);pose(l,1f,1f)
        assertEquals(6,l.stage);assertFalse(l.completed)
        assertTrue(l.select("floor",FinaleLevel.PERFORM))
        while(!l.performing)l.advance(.016f)
        assertEquals(FinaleLevel.PERFORM,l.position);assertEquals(.145f,l.elevation,.001f)
        assertFalse(l.stringsActive);assertTrue(l.busy);assertTrue(l.suggestedActions.isEmpty())
        var raisedLeft=false;var raisedRight=false;var danced=false;var bowed=false
        while(!l.completed) {
            assertFalse(l.select("pullLeft"));assertFalse(l.select("perform"))
            val left=l.leftPull;val right=l.rightPull;l.setTrackedPull(1f,1f)
            assertEquals(left,l.leftPull,0f);assertEquals(right,l.rightPull,0f)
            l.advance(.016f)
            raisedLeft=raisedLeft || l.leftPull>.8f;raisedRight=raisedRight || l.rightPull>.8f
            danced=danced || abs(l.walkStride)>.3f;bowed=bowed || l.bow>.8f
            assertEquals(FinaleLevel.PERFORM,l.position)
            assertTrue(l.showTime<=FinaleShow.DURATION)
        }
        assertTrue(raisedLeft && raisedRight && danced && bowed)
        assertEquals(FinaleShow.DURATION,l.showTime,0f);assertEquals(0f,l.celebrationTime,0f)
        assertEquals(0f,l.leftPull,0f);assertEquals(0f,l.rightPull,0f);assertEquals(0f,l.bow,0f)
        settle(l);assertEquals(FinaleShow.CELEBRATION_DURATION,l.celebrationTime,0f)
        assertFalse(l.select("perform"))
    }
    @Test fun resetDuringAutomaticShowCancelsPerformanceAndCelebration() {
        val l=FinaleLevel();prepare(l);pose(l,1f,0f);pose(l,0f,1f);pose(l,1f,1f)
        assertTrue(l.select("perform"));while(!l.performing)l.advance(.016f)
        repeat(120){l.advance(.016f)};assertTrue(l.leftPull>0f)
        l.reset();settle(l)
        assertFalse(l.performing);assertFalse(l.completed);assertEquals(0f,l.showTime,0f)
        assertEquals(0f,l.celebrationTime,0f);assertEquals(0f,l.leftPull,0f)
    }
    @Test fun celebrationRibbonsStartOverheadFallOnDeckAndFinish() {
        for(i in 0 until FinaleRibbonMotion.COUNT) {
            var previous=Float.POSITIVE_INFINITY;var seen=false
            for(frame in 0..850) {
                val f=FinaleRibbonMotion.frame(i,frame*.01f)
                if(f.visible) {
                    if(!seen)assertTrue(f.y>1.35f)
                    seen=true;assertTrue(f.y<=previous+.001f);previous=f.y
                    assertTrue(f.x in -1f..1f);assertTrue(f.z in -2.1f..-1.6f)
                    assertTrue(f.y>=.145f+.05f);assertTrue(f.scale in 0f..1f)
                }
            }
            assertTrue(seen);assertFalse(FinaleRibbonMotion.frame(i,FinaleShow.CELEBRATION_DURATION).visible)
        }
    }
    @Test fun prerequisitesAndWrongPlateCannotSkipProgress() {
        val l=FinaleLevel()
        for(a in listOf("power","scenery0","prop","dock","brightness","lock0","winch","practice","perform","pullLeft"))assertFalse(a,l.select(a))
        assertFalse(l.select("plate1"));assertEquals(0,l.plates)
        act(l,"plate0");assertFalse(l.select("plate2"));assertEquals(1,l.plates)
        assertFalse(l.select("floor",WalkPoint(0f,-1.8f)))
    }
    @Test fun movementIsContinuousAndPlateWaitsForArrival() {
        val l=FinaleLevel();l.select("plate0");assertEquals(0,l.plates);assertFalse(l.select("plate1"))
        var previous=l.position
        repeat(1200){val f=l.advance(.033f);assertTrue(hypot(f.position.x-previous.x,f.position.z-previous.z)<.007f);previous=f.position}
        assertEquals(1,l.plates);assertEquals(FinaleLevel.PLATES[0],l.position)
        assertEquals(.025f,l.elevation,.001f)
    }
    @Test fun plateTravelFollowsDirectSegmentsWithoutAisleDetours() {
        val l=FinaleLevel()
        for(i in 0..2) {
            val start=l.position;val end=FinaleLevel.PLATES[i]
            assertTrue(l.select("plate$i"));assertEquals(i,l.plates)
            var elapsed=0f
            while(l.busy && elapsed<8f) {
                val p=l.advance(.033f).position;elapsed+=.033f
                val cross=(p.x-start.x)*(end.z-start.z)-(p.z-start.z)*(end.x-start.x)
                assertEquals("plate $i left the direct segment",0f,cross,.0001f)
                if(l.busy)assertEquals("must arrive before plate triggers",i,l.plates)
            }
            assertFalse(l.busy);assertEquals(end,l.position);assertEquals(i+1,l.plates)
            if(i>0)assertTrue("turn then cross adjacent plates without detours",elapsed<2.4f)
        }
    }
    @Test fun lampNeedsBothConditionsAndWinchBothLocks() {
        val l=FinaleLevel();for(i in 0..2)act(l,"plate$i");act(l,"power")
        for(i in 0..2)repeat(3-i){act(l,"scenery$i")};act(l,"prop");act(l,"dock")
        repeat(2){act(l,"brightness")};assertEquals(3,l.stage)
        repeat(2){act(l,"aimRight")};assertEquals(4,l.stage)
        act(l,"lock0");assertFalse(l.select("winch"));assertEquals(0f,l.curtain,0f)
        act(l,"lock1");assertTrue(l.select("winch"));assertTrue(l.opening);assertFalse(l.select("practice"))
        settle(l);assertEquals(5,l.stage)
    }
    @Test fun allFlatTargetsTurnInPlaceThenFollowOneStraightSegment() {
        val l=FinaleLevel()
        fun travel(action:String,end:WalkPoint,raw:WalkPoint?=null) {
            val start=l.position;assertTrue(action,l.select(action,raw))
            var previous=start;var stride=0f;var height=l.elevation
            var turns=0;var walked=false;var frames=0
            while(l.busy && frames++<4000) {
                val f=l.advance(.016f);val dx=f.position.x-previous.x;val dz=f.position.z-previous.z
                assertEquals("$action detoured",0f,(f.position.x-start.x)*(end.z-start.z)-(f.position.z-start.z)*(end.x-start.x),.0001f)
                if(hypot(dx,dz)>.000001f) {
                    walked=true
                    val error=atan2(sin(atan2(dx,dz)-f.yawRadians),cos(atan2(dx,dz)-f.yawRadians))
                    assertEquals("$action moved sideways before facing target",0f,error,.003f)
                } else if(!walked)turns++
                assertTrue("$action gait snapped",abs(l.walkStride-stride)<.25f)
                assertTrue("$action abruptly jumped off a plate",abs(l.elevation-height)<.004f)
                previous=f.position;stride=l.walkStride;height=l.elevation
            }
            assertFalse(action,l.busy);assertEquals(end,l.position);assertEquals(0f,l.walkStride,0f)
            if(action=="plate0")assertTrue("must turn in place before departing",turns>0)
        }
        travel("plate0",FinaleLevel.PLATES[0]);travel("plate1",FinaleLevel.PLATES[1]);travel("plate2",FinaleLevel.PLATES[2])
        travel("power",WalkPoint(-.86f,-.92f))
        for(i in 0..2)repeat(3-i){act(l,"scenery$i")}
        travel("prop",FinaleLevel.PROP_APPROACH);travel("dock",FinaleLevel.DOCK_APPROACH)
        val floor=WalkPoint(.5f,-.72f);travel("floor",floor,floor)
        repeat(2){act(l,"aimRight")};repeat(2){act(l,"brightness")}
        act(l,"lock0");act(l,"lock1");act(l,"winch")
        travel("practice",FinaleLevel.PRACTICE)
    }
    @Test fun gestureRequiresCorrectPoseAndContinuousHold() {
        val l=FinaleLevel();prepare(l)
        pose(l,1f,1f);assertEquals(0,l.practice)
        l.setTrackedPull(1f,0f);repeat(10){l.advance(.033f)}
        l.setTrackedPull(0f,0f);l.advance(.033f)
        l.setTrackedPull(1f,0f);repeat(10){l.advance(.033f)};assertEquals(0,l.practice)
        repeat(15){l.advance(.033f)};assertEquals(1,l.practice)
        settle(l);assertEquals(1,l.practice)
    }
    @Test fun ringControlsCanCompleteWithoutTrackedHands() {
        val l=FinaleLevel();prepare(l)
        act(l,"pullLeft");assertEquals(1,l.practice)
        act(l,"pullLeft");act(l,"pullRight");assertEquals(2,l.practice)
        act(l,"pullLeft");assertEquals(3,l.practice)
        assertFalse(l.stringsActive);assertEquals(0f,l.leftPull,0f)
    }
    @Test fun resetClearsMovementCurtainHeldStringsAndProgress() {
        val l=FinaleLevel();prepare(l);pose(l,1f,0f);l.reset()
        assertEquals(1,l.stage);assertEquals(FinaleLevel.START,l.position);assertFalse(l.busy)
        assertEquals(0f,l.curtain,0f);assertFalse(l.powered);assertFalse(l.delivered)
        assertEquals(0,l.practice);assertEquals(0,l.performance);assertEquals(0f,l.leftPull,0f)
        assertArrayEquals(intArrayOf(1,2,3),l.scenery);assertFalse(l.locks.any{it})
        l.select("plate0");l.advance(.05f);l.reset();settle(l);assertEquals(0,l.plates)
    }
    @Test fun nonFiniteInputDoesNotPoisonMotion() {
        val l=FinaleLevel();assertFalse(l.select("floor",WalkPoint(Float.NaN,0f)))
        l.advance(Float.NaN);assertTrue(l.time.isFinite());assertEquals(FinaleLevel.START,l.position)
    }
    @Test fun authoredStairTreadsAndCurvedDeckSupportThePuppet() {
        // Sample actual exporter surfaces in app space, including outside the rounded deck.
        assertEquals(0f,FinaleLevel.stageHeightAt(WalkPoint(0f,-1.10f)),.0001f)
        assertEquals(.145f/3f,FinaleLevel.stageHeightAt(WalkPoint(0f,-1.16f)),.0001f)
        assertEquals(2f*.145f/3f,FinaleLevel.stageHeightAt(WalkPoint(0f,-1.23f)),.0001f)
        assertEquals(.145f,FinaleLevel.stageHeightAt(WalkPoint(0f,-1.30f)),.0001f)
        assertEquals(.145f,FinaleLevel.stageHeightAt(FinaleLevel.PERFORM),.0001f)
        assertEquals(0f,FinaleLevel.stageHeightAt(WalkPoint(.95f,-1.38f)),.0001f)
        assertEquals(.145f,FinaleLevel.stageHeightAt(WalkPoint(.95f,-1.70f)),.0001f)
        assertEquals(-1.640f,FinaleLevel.PERFORM.z,.0001f)
    }
}
