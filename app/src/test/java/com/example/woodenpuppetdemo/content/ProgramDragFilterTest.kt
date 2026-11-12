package com.example.woodenpuppetdemo.content
import org.junit.Assert.*
import org.junit.Test
class ProgramDragFilterTest {
    @Test fun jitterIsAttenuatedAndReleaseFreezes() {
        val f=ProgramDragFilter();f.begin(GrabV(0f,0f,0f))
        repeat(20) {i->f.push(GrabV(if(i%2==0).02f else -.02f,0f,0f));assertTrue(f.tick(.016f).x<.015f)}
        val last=f.position;f.release();f.push(GrabV(.1f,0f,0f));repeat(20){f.tick(.02f)}
        assertEquals(last,f.position)
    }
    @Test fun followsIntentButRejectsSpikeAndLimitsSpeed() {
        val f=ProgramDragFilter();f.begin(GrabV(1f,2f,3f))
        f.push(GrabV(1f,0f,0f));assertEquals(GrabV(1f,2f,3f),f.tick(.02f))
        repeat(10){f.push(GrabV(.05f,0f,0f))}
        val first=f.tick(.02f);assertTrue(first.x<=1.02001f)
        repeat(100){f.tick(.02f)};assertEquals(1.5f,f.position.x,.001f)
        f.release();f.begin(GrabV(-1f,0f,0f));assertEquals(GrabV(-1f,0f,0f),f.tick(.02f))
    }
}
