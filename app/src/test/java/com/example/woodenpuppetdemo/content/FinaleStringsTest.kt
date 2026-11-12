package com.example.woodenpuppetdemo.content
import org.junit.Assert.*
import org.junit.Test
class FinaleStringsTest {
    private fun sample(y:Float,p:Float=1f)=StringHandSample(true,p,y)
    @Test fun relativeRaiseAndIndependentHands() {
        val s=FinaleStrings();s.update(listOf(sample(1f),sample(1.3f)),.033f)
        var result=0f to 0f
        repeat(20){result=s.update(listOf(sample(1.12f),sample(1.3f)),.033f)!!}
        assertTrue(result.first>.98f);assertEquals(0f,result.second,0f)
    }
    @Test fun trackingLossReleasesAndReacquisitionReanchors() {
        val s=FinaleStrings();s.update(listOf(sample(1f)),.05f)
        repeat(20){s.update(listOf(sample(1.2f)),.05f)}
        assertEquals(0f to 0f,s.update(emptyList(),.05f));assertNull(s.update(emptyList(),.05f))
        assertEquals(0f,s.update(listOf(sample(1.8f)),.05f)!!.first,0f)
    }
    @Test fun unpinchingReleasesAndInvalidDataIsIgnored() {
        val s=FinaleStrings();assertNull(s.update(listOf(sample(Float.NaN)),.05f))
        s.update(listOf(sample(1f)),.05f);repeat(20){s.update(listOf(sample(1.2f)),.05f)}
        assertEquals(0f,s.update(listOf(sample(1.2f,0f)),.05f)!!.first,0f)
    }
}
