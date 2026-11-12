package com.example.woodenpuppetdemo.content

import kotlin.math.exp

/** Smooths webcam drag targets without moving the panel after release. */
internal class ProgramDragFilter {
    var position=GrabV(0f,0f,0f); private set
    private var target=position
    var active=false; private set
    fun begin(at:GrabV) {position=at;target=at;active=true}
    fun push(delta:GrabV) {
        if(active && delta.valid() && delta.length()<=.12f) target=target+delta
    }
    fun tick(seconds:Float):GrabV {
        if(!active)return position
        val dt=seconds.coerceIn(0f,.05f)
        var step=(target-position)*(1f-exp(-dt/.09f))
        val length=step.length()
        val limit=1.0f*dt
        if(length>limit && length>0f)step=step*(limit/length)
        position=position+step
        return position
    }
    fun release(){active=false;target=position}
}
