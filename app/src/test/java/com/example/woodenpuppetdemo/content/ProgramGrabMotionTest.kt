package com.example.woodenpuppetdemo.content
import org.junit.Assert.*
import org.junit.Test

class ProgramGrabMotionTest {
    private fun panel()=ProgramGrabMotion().apply{pose=GrabPose(GrabV(0f,0f,-2f))}
    private fun controller(down:Boolean,p:GrabV=GrabV(0f,0f,0f),id:Int=0)=GrabInput(id,GrabPose(p),down,false)
    @Test fun rayMustHitAndRequireFreshPress() {
        val m=panel();m.update(listOf(controller(true,GrabV(1f,0f,0f))),.03f);assertNull(m.owner)
        m.update(listOf(controller(true)),.03f);assertNull(m.owner)
        m.update(listOf(controller(false)),.03f);m.update(listOf(controller(true)),.03f);assertEquals(0,m.owner)
    }
    @Test fun remoteGrabPullsToHandAndReleaseFreezes() {
        val m=panel();m.update(listOf(controller(true)),.03f)
        repeat(20){m.update(listOf(controller(true)),.03f)}
        assertEquals(-.35f,m.pose.p.z,.001f)
        m.update(listOf(controller(true,GrabV(.1f,0f,0f))),.03f);assertEquals(.1f,m.pose.p.x,.001f)
        m.update(listOf(controller(false,GrabV(.1f,0f,0f))),.03f);val last=m.pose
        m.update(listOf(controller(false,GrabV(.5f,0f,0f))),.03f)
        assertEquals(last,m.pose);assertTrue(m.placed);assertNull(m.owner)
    }
    @Test fun directPinchIsNearPaperAndAvoidsButtons() {
        val m=panel()
        assertFalse(m.contains(GrabInput(2,GrabPose(GrabV(0f,0f,-1f)),true,true)))
        assertFalse(m.contains(GrabInput(2,GrabPose(GrabV(0f,-.28f,-2f)),true,true)))
        val hand=GrabInput(2,GrabPose(GrabV(.05f,.1f,-1.98f)),true,true)
        m.update(listOf(hand),.03f);assertEquals(2,m.owner)
        m.update(listOf(hand.copy(pose=GrabPose(hand.pose.p+GrabV(.1f,0f,0f)))),.03f)
        assertEquals(.1f,m.pose.p.x,.001f);assertEquals(-2f,m.pose.p.z,.001f)
    }
    @Test fun ownerLockAndTrackingLossRequireRelease() {
        val m=panel();m.update(listOf(controller(true)),.03f)
        m.update(listOf(controller(true),controller(true,id=1)),.03f);assertEquals(0,m.owner)
        repeat(10){m.update(emptyList(),.03f)};assertNull(m.owner)
        m.update(listOf(controller(true)),.03f);assertNull(m.owner)
    }
    @Test fun rotatedPanelUsesLocalHitPlane() {
        val m=panel();val q=GrabQ(0f,.70710677f,0f,.70710677f);m.pose=GrabPose(GrabV(-2f,0f,0f),q)
        assertTrue(m.contains(GrabInput(0,GrabPose(GrabV(0f,0f,0f),q),true,false)))
        assertFalse(m.contains(controller(true)))
    }
    @Test fun trackingJumpDoesNotTeleportPanel() {
        val m=panel();m.update(listOf(controller(true)),.03f);val p=m.pose
        m.update(listOf(controller(true,GrabV(5f,0f,0f))),.03f)
        assertNull(m.owner);assertEquals(p,m.pose)
    }
}
