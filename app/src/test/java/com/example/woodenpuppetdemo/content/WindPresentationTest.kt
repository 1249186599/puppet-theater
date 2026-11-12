package com.example.woodenpuppetdemo.content
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
class WindPresentationTest {
 private fun settle(l:WindLevel,n:Int=2000){repeat(n){l.advance(.033f)}}
 @Test fun helpIsDelayedAndDismissedByActionAndReset(){
  val l=WindLevel();assertFalse(l.helpVisible);settle(l,300);assertFalse(l.helpVisible)
  settle(l,100);assertTrue(l.helpVisible);l.select("scoop");assertFalse(l.helpVisible)
  l.select("scoop");l.select("power");l.select("ferry");settle(l,100);assertFalse(l.helpVisible)
  l.reset();assertFalse(l.helpVisible);assertEquals("让风车转起来",l.taskLine)
 }
 @Test fun nozzleCorrectPosePointsAtRotorAndSharesItsHeight(){
  val x=WindmillLayout.hubX-WindmillLayout.SCOOP_X;val z=WindmillLayout.hubZ-WindmillLayout.SCOOP_Z
  val d=hypot(x,z)
  fun dot(q:Int)=(sin(WindmillLayout.scoopYaw(q))*x+cos(WindmillLayout.scoopYaw(q))*z)/d
  assertEquals(1f,dot(2),.00001f);assertEquals(-1f,dot(0),.00001f);assertEquals(0f,dot(1),.00001f)
  assertEquals(WindmillLayout.HUB_Y,WindmillLayout.SCOOP_Y+WindmillLayout.OUTLET_LOCAL_Y,.00001f)
  assertTrue(WindmillLayout.HUB_Y-.258394f>.05f)
 }
 @Test fun quietChimeHintNeverCompletesPlayerSequence(){
  val l=WindLevel();l.select("scoop");l.select("scoop");l.select("power");l.select("ferry");settle(l)
  l.select("chimes");settle(l);l.select("ferry");settle(l);l.select("power")
  l.select("wind1");settle(l);l.select("wind2");settle(l);l.select("hang");settle(l)
  assertEquals(0,l.notes);assertEquals(0,l.consumeHintNote());assertNull(l.consumeHintNote())
  l.select("note0");settle(l,100);assertEquals(1,l.notes);assertEquals(2,l.consumeHintNote());assertFalse(l.tuned)
  l.reset();assertNull(l.consumeHintNote());assertEquals(0,l.notes)
 }
}
