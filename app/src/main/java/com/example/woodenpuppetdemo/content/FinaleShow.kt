package com.example.woodenpuppetdemo.content

import kotlin.math.*

internal data class FinaleShowPose(val left:Float=0f,val right:Float=0f,val stride:Float=0f,val yaw:Float=0f,val bow:Float=0f)

/** A finite, deterministic show. Player input never drives this timeline. */
internal object FinaleShow {
    const val DURATION=12f
    const val CELEBRATION_DURATION=8.5f
    private fun pulse(t:Float,start:Float,end:Float):Float =
        if(t<=start || t>=end)0f else sin((t-start)/(end-start)*PI.toFloat()).pow(2)
    fun pose(t:Float):FinaleShowPose = when {
        t<2f->FinaleShowPose(left=pulse(t,0f,2f))
        t<4f->FinaleShowPose(right=pulse(t,2f,4f))
        t<6f->{val p=pulse(t,4f,6f);FinaleShowPose(left=p,right=p)}
        t<10f->{
            val envelope=pulse(t,6f,10f);val beat=(t-6f)*2f*PI.toFloat()
            FinaleShowPose(envelope*(.55f+.25f*sin(beat)),envelope*(.55f-.25f*sin(beat)),
                envelope*.45f*sin(beat),envelope*.35f*sin(beat/2f))
        }
        t<DURATION->FinaleShowPose(bow=pulse(t,10f,DURATION))
        else->FinaleShowPose()
    }
}
