package com.example.woodenpuppetdemo.content

import org.junit.Assert.*
import org.junit.Test

class CargoLevelTest {
    private fun settle(l: CargoLevel,n: Int=1800) {repeat(n){ l.advance(.033f) }}
    private fun unlock(l: CargoLevel) {assertTrue(l.select("lockCircle"));settle(l);assertTrue(l.unlocked)}
    private fun load(l: CargoLevel) {unlock(l);assertTrue(l.select("cart"));settle(l);assertEquals(CargoLevel.LOAD,l.cart);assertTrue(l.select("load"));settle(l);assertTrue(l.loaded)}
    private fun track(l: CargoLevel) {load(l);assertTrue(l.select("track"));assertTrue(l.trackReady)}
    private fun dock(l: CargoLevel) {
        track(l);l.select("switch");assertTrue(l.correctBranch)
        l.select("cart");settle(l);assertTrue(l.atJunction);assertFalse(l.docked)
        l.select("cart");settle(l);assertTrue(l.docked)
    }
    @Test fun loadAvailabilityRequiresParkedEmptyCartAndClearsDuringTransfer() {
        val l=CargoLevel();assertFalse(l.canLoad)
        unlock(l);assertFalse(l.canLoad)
        l.select("cart");assertFalse(l.canLoad);settle(l);assertTrue(l.canLoad)
        assertTrue(l.select("load"));assertFalse(l.canLoad)
        assertFalse(l.select("load"));settle(l);assertTrue(l.loaded);assertFalse(l.canLoad)
        assertEquals(1,l.drainCues().count {it==AdventureCue.GEAR_SEATED})
        l.reset();assertFalse(l.canLoad)
    }
    @Test fun prerequisitesAndWrongMatchCannotSkipChallenges() {
        val l=CargoLevel()
        for(a in listOf("cart","load","track","switch","dock","dial","exit","rewind","lockSquare","lockTriangle")) assertFalse(a,l.select(a))
        settle(l);assertFalse(l.unlocked);assertEquals(CargoLevel.ORIGIN,l.cart)
        assertFalse(l.select("floor",WalkPoint(Float.NaN,-.6f)))
    }
    @Test fun fullFiveChallengesAndDoorClearance() {
        val l=CargoLevel();assertEquals(1,l.stage);unlock(l);assertEquals(2,l.stage)
        l.select("cart");settle(l);l.select("load");settle(l);assertEquals(3,l.stage)
        l.select("track");assertEquals(4,l.stage)
        l.select("switch");l.select("cart");settle(l);assertTrue(l.atJunction)
        l.select("cart");settle(l);assertEquals(5,l.stage);assertTrue(l.docked)
        assertFalse(l.select("dial"));l.select("dock");settle(l);assertTrue(l.seated)
        repeat(2) {l.select("dial");assertFalse(l.powered)}
        l.select("dial");assertTrue(l.powered);l.select("exit");settle(l);assertTrue(l.completed)
        assertEquals(CargoLevel.EXIT,l.position)
        assertTrue(CargoLevel.distance(l.position,WalkPoint(.92f+.36588475f/2f,-2.014f))>.45f)
    }
    @Test fun brokenRailStopsCartBeforeTurntable() {
        val l=CargoLevel();load(l);l.select("cart");settle(l)
        assertEquals(CargoLevel.SAFE_LINE,l.cart);assertFalse(l.select("cart"));settle(l)
        assertFalse(l.atJunction);l.select("track");l.select("cart");settle(l);assertTrue(l.atJunction)
    }
    @Test fun wrongBranchCanRewindWithoutLosingProgress() {
        val l=CargoLevel();track(l);l.select("cart");settle(l);assertTrue(l.atJunction)
        l.select("cart");settle(l);assertTrue(l.atBranch);assertFalse(l.docked)
        assertFalse(l.select("switch"));assertTrue(l.select("rewind"))
        val actor=l.position;settle(l);assertEquals(actor,l.position);assertEquals(CargoLevel.JUNCTION,l.cart)
        assertTrue(l.loaded && l.trackReady && l.unlocked);assertTrue(l.atJunction)
        l.select("switch");l.select("cart");settle(l);assertTrue(l.docked)
    }
    @Test fun movingCartLocksInputsAndActorDoesNotTeleportAtCorner() {
        val l=CargoLevel();track(l);l.select("cart");settle(l);l.select("cart")
        var old=l.position;var seen=false
        repeat(1800) {
            l.advance(.033f)
            assertTrue("No actor jump",CargoLevel.distance(old,l.position)<.025f);old=l.position
            if(l.pushing) {seen=true;assertFalse(l.select("switch"));assertFalse(l.select("cart"));assertFalse(l.select("floor",CargoLevel.START))}
        }
        assertTrue(seen);assertTrue(l.atBranch)
    }
    @Test fun resetDuringLoadDriveAndDockRestoresEverything() {
        val l=CargoLevel();unlock(l);l.select("cart");settle(l);l.select("load");l.advance(.05f)
        assertTrue(l.transferKind.isNotEmpty());l.reset();assertReset(l)
        track(l);l.select("cart")
        repeat(600) {
            l.advance(.033f)
            if(l.pushing) {l.reset();assertReset(l);return@repeat}
        }
        assertReset(l);dock(l);l.select("dock");l.advance(.05f);l.reset();assertReset(l)
    }
    private fun assertReset(l: CargoLevel) {
        settle(l,60);assertEquals(CargoLevel.START,l.position);assertEquals(CargoLevel.ORIGIN,l.cart)
        assertFalse(l.unlocked || l.loaded || l.trackReady || l.docked || l.seated || l.powered || l.completed || l.busy)
        assertEquals(1,l.stage);assertTrue(l.drainCues().isEmpty())
    }
    @Test fun successfulTaskCuesDoNotRepeatWhenIdle() {
        val l=CargoLevel();dock(l);l.drainCues();l.select("dock");settle(l)
        assertEquals(listOf(AdventureCue.GEAR_SEATED),l.drainCues());settle(l);assertTrue(l.drainCues().isEmpty())
        repeat(3){l.select("dial")};assertEquals(1,l.drainCues().count{it==AdventureCue.CORE_RUN})
        assertFalse(l.select("dial"));assertTrue(l.drainCues().isEmpty())
    }
}
