package com.example.woodenpuppetdemo.content

internal data class StringHandSample(val ready:Boolean,val pinch:Float,val height:Float)

/** Relative hand height avoids requiring the player to reach the miniature stage. */
internal class FinaleStrings {
    private val pinched=BooleanArray(2)
    private val baseline=FloatArray(2)
    private val value=FloatArray(2)
    private var usedTracking=false
    fun reset(){pinched.fill(false);baseline.fill(0f);value.fill(0f);usedTracking=false}
    fun update(samples:List<StringHandSample>,seconds:Float):Pair<Float,Float>? {
        val valid=samples.any{it.ready && it.pinch.isFinite() && it.height.isFinite()}
        if(!valid){val release=usedTracking;reset();return if(release)0f to 0f else null}
        usedTracking=true
        for(i in 0..1) {
            val s=samples.getOrNull(i)
            if(s==null || !s.ready || !s.pinch.isFinite() || !s.height.isFinite()){pinched[i]=false;value[i]=0f;continue}
            val down=if(pinched[i])s.pinch>.35f else s.pinch>.75f
            if(down && !pinched[i])baseline[i]=s.height
            pinched[i]=down
            val target=if(down)((s.height-baseline[i])/.12f).coerceIn(0f,1f) else 0f
            if(!down)value[i]=0f else value[i]+=(target-value[i])*(seconds.coerceIn(0f,.05f)*16f).coerceIn(0f,1f)
        }
        return value[0] to value[1]
    }
}
