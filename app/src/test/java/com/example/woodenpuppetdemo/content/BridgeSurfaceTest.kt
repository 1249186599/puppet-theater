package com.example.woodenpuppetdemo.content
import org.junit.Assert.*
import org.junit.Test
class BridgeSurfaceTest {
    @Test fun surfaceMatchesDeckAndBothRamps() {
        val center = AdventureLevel.CENTER_Z
        assertEquals(0.065f, BridgeSurface.height(0f, center, true), 0.00001f)
        for (sign in listOf(-1f,1f)) {
            assertEquals(0.065f, BridgeSurface.height(0f, center+sign*0.215f,true),0.00001f)
            assertEquals(0.0325f, BridgeSurface.height(0f, center+sign*0.29f,true),0.00001f)
            assertEquals(0f, BridgeSurface.height(0f, center+sign*0.365f,true),0.00001f)
        }
        assertEquals(0f,BridgeSurface.height(0f,center,false),0f)
        assertEquals(0f,BridgeSurface.height(0.2f,center,true),0f)
    }
    @Test fun bridgeBottomClearsGroundAndApproachesStartOutsideRamps() {
        assertTrue(BridgeSurface.LIFT - 0.055569f > 0.009f)
        assertEquals(0f,BridgeSurface.height(0f,AdventureLevel.NEAR_GATE.z,true),0f)
        assertEquals(0f,BridgeSurface.height(0f,AdventureLevel.FAR_GATE.z,true),0f)
    }
}
