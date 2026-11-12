package com.example.woodenpuppetdemo.content
import org.junit.Assert.*
import org.junit.Test
class PlateSurfaceTest {
    @Test fun centralSurfaceFollowsPressAndReturnsOnReset() {
        val p=AdventureLevel.PAD
        val resting=PlateSurface.height(p.x,p.z,0f)
        assertTrue(resting > 0.008f && resting < 0.029f)
        assertEquals(resting-0.006f,PlateSurface.height(p.x,p.z,1f),0.00001f)
        assertEquals(resting,PlateSurface.height(p.x,p.z,0f),0f)
    }
    @Test fun outsidePlateStaysOnGround() {
        assertEquals(0f,PlateSurface.height(0f,-0.65f,0f),0f)
        assertEquals(0f,PlateSurface.height(0f,-1.2f,1f),0f)
    }
}
