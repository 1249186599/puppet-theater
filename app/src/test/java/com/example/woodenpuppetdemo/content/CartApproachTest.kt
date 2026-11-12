package com.example.woodenpuppetdemo.content
import org.junit.Assert.*
import org.junit.Test

class CartApproachTest {
    @Test fun alreadyBehindStartsPushingWithoutDetour() {
        val l=CargoLevel();l.select("lockCircle");repeat(600){l.advance(.033f)}
        val start=l.position;l.select("cart");l.advance(.033f)
        assertTrue(l.pushing);assertEquals(start,l.position)
    }
    @Test fun unobstructedSideUsesDirectPath() {
        val cart=WalkPoint(0f,-1.2f);val goal=WalkPoint(-.28f,-1.2f)
        assertEquals(listOf(goal),CartApproach.route(WalkPoint(-.4f,-.95f),cart,goal))
    }
    @Test fun frontAndRearApproachesClearCartAndStayLocal() {
        val cart=WalkPoint(0f,-1.2f);val goal=WalkPoint(-.28f,-1.2f)
        for(start in listOf(WalkPoint(.4f,-1.2f),WalkPoint(.2f,-.76f),WalkPoint(0f,-1.50f))) {
            var old=start;var length=0f
            val path=CartApproach.route(start,cart,goal)
            for(p in path) {
                assertFalse("Avoid cart body",CartApproach.crossesCart(old,p,cart))
                length+=CargoLevel.distance(old,p);old=p
            }
            assertEquals(goal,path.last());assertTrue("No front aisle detour",length<1.2f)
        }
    }
}
