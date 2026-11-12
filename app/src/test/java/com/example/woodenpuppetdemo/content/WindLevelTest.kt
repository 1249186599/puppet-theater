package com.example.woodenpuppetdemo.content
import org.junit.Assert.*
import org.junit.Test
class WindLevelTest {
 private fun settle(l:WindLevel,n:Int=2000){repeat(n){l.advance(.033f)}}
 private fun power(l:WindLevel){assertTrue(l.select("scoop"));assertFalse(l.windReady);assertTrue(l.select("scoop"));assertTrue(l.windReady);assertTrue(l.select("power"));assertEquals(1,l.power)}
 private fun fetch(l:WindLevel){power(l);assertTrue(l.select("ferry"));settle(l);assertTrue(l.island);assertTrue(l.select("note0"));settle(l);assertTrue(l.hasChimes);assertTrue(l.select("ferry"));settle(l);assertFalse(l.island)}
 private fun cross(l:WindLevel){fetch(l);l.select("power");assertTrue(l.select("wind1"));settle(l);assertEquals(1,l.crossed);assertTrue(l.select("wind2"));settle(l);assertEquals(2,l.crossed)}
 @Test fun prerequisitesAndWaterClicksCannotSkipTasks(){val l=WindLevel();for(a in listOf("power","ferry","chimes","wind1","wind2","hang","note0","exit"))assertFalse(a,l.select(a));assertFalse(l.select("floor",WalkPoint(.2f,-1.4f)));assertFalse(l.select("floor",WalkPoint(Float.NaN,-.6f)));power(l);l.select("power");assertFalse(l.select("ferry"));assertFalse(l.select("wind1"));assertEquals(3,l.stage)}
 @Test fun fullFiveTasksAndExit(){val l=WindLevel();cross(l);assertEquals(5,l.stage);assertTrue(l.select("hang"));settle(l);assertTrue(l.hung);for(i in WindLevel.ORDER){assertTrue(l.select("note$i"));repeat(12){l.advance(.033f)}};assertTrue(l.tuned);assertTrue(l.select("exit"));settle(l);assertTrue(l.completed);assertEquals(WindLevel.EXIT,l.position);assertEquals(1,l.drainCues().count{it==AdventureCue.CHIME_SUCCESS})}
 @Test fun ferryCarriesActorContinuouslyAndLocksInput(){val l=WindLevel();power(l);l.select("ferry");var old=l.position;var seen=false;repeat(2000){val f=l.advance(.033f);assertTrue(kotlin.math.hypot(f.position.x-old.x,f.position.z-old.z)<.018f);old=f.position;if(l.riding){seen=true;assertFalse(f.moving);assertEquals(.025f,l.elevation,.0001f);assertFalse(l.select("power"));assertFalse(l.select("ferry"))}};assertTrue(seen);assertTrue(l.island);assertFalse(l.select("floor",WindLevel.START));assertFalse(l.select("power"))}
 @Test fun crossingIsProtected(){val l=WindLevel();fetch(l);l.select("power");l.select("wind1");var seen=false;repeat(2000){l.advance(.033f);if(l.crossing>0){seen=true;assertTrue(l.safe(l.crossing));assertFalse(l.select("power"))}};assertTrue(seen);assertEquals(1,l.crossed);assertFalse(l.select("hang"));assertFalse(l.select("floor",WindLevel.LANDING))}
 @Test fun wrongMelodyOnlyResetsNotesAndDebounces(){val l=WindLevel();cross(l);l.select("hang");settle(l);l.select("note0");assertFalse(l.select("note0"));repeat(15){l.advance(.033f)};l.select("note1");assertEquals(0,l.notes);assertTrue(l.hung);assertEquals(2,l.crossed);assertFalse(l.tuned)}
 @Test fun resetClearsRideProgressAndCues(){val l=WindLevel();power(l);l.select("ferry");repeat(1000){if(!l.riding)l.advance(.033f)};assertTrue(l.riding);l.reset();assertEquals(WindLevel.START,l.position);assertFalse(l.riding);assertEquals(WindLevel.NEAR.z,l.ferryZ,0f);assertTrue(l.drainCues().isEmpty());cross(l);l.reset();assertEquals(0,l.crossed);assertEquals(0,l.power);assertFalse(l.hasChimes);assertEquals(1,l.stage)}
 @Test fun ferryStaysBetweenBanksOnBothTripsAndParksFlush(){
  val l=WindLevel();power(l)
  fun clear(){
   assertTrue(l.ferryZ+WindLevel.FERRY_HALF_LENGTH<=WindLevel.NEAR_BANK_EDGE)
   assertTrue(l.ferryZ-WindLevel.FERRY_HALF_LENGTH>=WindLevel.ISLAND_BANK_EDGE)
  }
  clear();assertEquals(.002f,WindLevel.NEAR_BANK_EDGE-(l.ferryZ+WindLevel.FERRY_HALF_LENGTH),.00001f)
  l.select("ferry");repeat(2000){l.advance(.033f);clear()};assertTrue(l.island)
  assertEquals(.002f,l.ferryZ-WindLevel.FERRY_HALF_LENGTH-WindLevel.ISLAND_BANK_EDGE,.00001f)
  l.select("chimes");settle(l);l.select("ferry");repeat(2000){l.advance(.033f);clear()};assertFalse(l.island)
 }
 @Test fun guideFollowsActionAndHidesWhileTravelling(){
  val l=WindLevel();assertEquals("scoop",l.suggestedAction);power(l);assertEquals("ferry",l.suggestedAction)
  l.select("ferry");assertNull(l.suggestedAction);settle(l);assertEquals("chimes",l.suggestedAction)
  l.select("chimes");settle(l);assertEquals("ferry",l.suggestedAction)
  l.select("ferry");settle(l);assertEquals("power",l.suggestedAction)
  l.select("power");assertEquals("wind1",l.suggestedAction);l.select("wind1");settle(l);assertEquals("wind2",l.suggestedAction)
  l.select("wind2");settle(l);assertEquals("hang",l.suggestedAction);l.select("hang");settle(l)
  for(i in WindLevel.ORDER){assertEquals("note$i",l.suggestedAction);l.select("note$i");repeat(12){l.advance(.033f)}}
  assertEquals("exit",l.suggestedAction);l.select("exit");settle(l);assertNull(l.suggestedAction)
 }
 @Test fun rearDeckWalksAfterCrossingAndHangingWithoutLosingMelody(){
  val l=WindLevel();cross(l)
  val beforeHang=WalkPoint(.35f,-1.9f)
  assertTrue(l.select("floor",beforeHang));settle(l);assertEquals(beforeHang,l.position)
  assertTrue(l.select("hang"));settle(l);assertTrue(l.hung)
  assertTrue(l.select("note0"));settle(l,12);assertEquals(1,l.notes)
  for(target in listOf(WalkPoint(-.25f,-1.92f),WalkPoint(.85f,-1.96f))){
   assertTrue(l.select("floor",target));var moved=false
   repeat(800){val f=l.advance(.033f);moved=moved||f.moving;assertEquals(.16f,l.elevation,.00001f)}
   assertTrue(moved);assertFalse(l.busy);assertEquals(target,l.position)
   assertTrue(l.hung);assertEquals(1,l.notes);assertEquals(2,l.crossed)
  }
  for(note in listOf(2,1)){assertTrue(l.select("note$note"));settle(l,12)}
  assertTrue(l.tuned);assertTrue(l.select("floor",WalkPoint(.6f,-1.9f)));settle(l)
  assertTrue(l.select("exit"));settle(l);assertTrue(l.completed)
 }
 @Test fun rearDeckWalkingRejectsWaterAndKeepsBodyInsideClearArea(){
  val l=WindLevel();cross(l);assertTrue(l.select("hang"));settle(l)
  for(p in listOf(WindLevel.START,WindLevel.ISLAND,WindLevel.WAIT2,WalkPoint(1.31f,-1.9f),
   WalkPoint(.2f,-2.39f),WalkPoint(Float.POSITIVE_INFINITY,-1.9f),WalkPoint(.2f,Float.NaN)))
   assertFalse("Unsafe target $p",l.select("floor",p))
  assertTrue(l.select("floor",WalkPoint(-.5f,-2.38f)));settle(l)
  assertEquals(-.38f,l.position.x,.00001f);assertEquals(-1.99f,l.position.z,.00001f)
  assertTrue(l.select("floor",WalkPoint(1.3f,-1.76f)));settle(l)
  assertEquals(1.18f,l.position.x,.00001f);assertEquals(-1.88f,l.position.z,.00001f)
  l.reset();assertFalse(l.select("floor",WindLevel.HANG));assertFalse(l.hung)
 }

}
