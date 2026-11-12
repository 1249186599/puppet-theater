package com.example.woodenpuppetdemo.content

import kotlin.math.*
internal data class GrabV(val x:Float,val y:Float,val z:Float) {
    operator fun plus(b:GrabV)=GrabV(x+b.x,y+b.y,z+b.z)
    operator fun minus(b:GrabV)=GrabV(x-b.x,y-b.y,z-b.z)
    operator fun times(s:Float)=GrabV(x*s,y*s,z*s)
    fun length()=sqrt(x*x+y*y+z*z)
    fun valid()=x.isFinite()&&y.isFinite()&&z.isFinite()
}
internal data class GrabQ(val x:Float=0f,val y:Float=0f,val z:Float=0f,val w:Float=1f) {
    fun inverse()=GrabQ(-x,-y,-z,w)
    operator fun times(b:GrabQ)=GrabQ(w*b.x+x*b.w+y*b.z-z*b.y,w*b.y-x*b.z+y*b.w+z*b.x,w*b.z+x*b.y-y*b.x+z*b.w,w*b.w-x*b.x-y*b.y-z*b.z)
    fun turn(v:GrabV):GrabV {val r=this*GrabQ(v.x,v.y,v.z,0f)*inverse();return GrabV(r.x,r.y,r.z)}
    fun valid()=listOf(x,y,z,w).all{it.isFinite()} && abs(x*x+y*y+z*z+w*w-1f)<.03f
}
internal data class GrabPose(val p:GrabV,val q:GrabQ=GrabQ()) {
    fun valid()=p.valid()&&q.valid()
}
internal data class GrabInput(val id:Int,val pose:GrabPose,val down:Boolean,val direct:Boolean)
internal class ProgramGrabMotion {
    var pose=GrabPose(GrabV(0f,0f,0f))
    var min=GrabV(-.2f,-.3f,0f);var max=GrabV(.2f,.3f,0f)
    var owner:Int?=null;private set
    var placed=false;private set
    private var relative=GrabV(0f,0f,0f)
    private var relativeQ=GrabQ()
    private var elapsed=0f
    private var lost=0f
    private var lastDriver:GrabPose?=null
    private val previous=BooleanArray(4)
    fun placeExternally(next:GrabPose) { if(owner==null && next.valid()){pose=next;placed=true} }
    fun contains(input:GrabInput):Boolean {
        val iq=pose.q.inverse();val o=iq.turn(input.pose.p-pose.p)
        if(input.direct) return o.x in min.x-.025f..max.x+.025f &&
            o.y in (min.y+(max.y-min.y)*.23f)..max.y+.025f && abs(o.z-(min.z+max.z)*.5f)<.065f
        val d=iq.turn(input.pose.q.turn(GrabV(0f,0f,-1f)))
        if(abs(d.z)<.0001f)return false
        val t=((min.z+max.z)*.5f-o.z)/d.z
        if(t<0f || t>5f)return false
        val h=o+d*t
        return h.x in min.x..max.x && h.y in min.y..max.y
    }
    fun update(inputs:List<GrabInput>,seconds:Float) {
        val dt=seconds.coerceIn(0f,.05f)
        val valid=inputs.filter{it.pose.valid()}
        val held=owner
        if(held!=null) {
            val i=valid.firstOrNull{it.id==held}
            if(i==null){lost+=dt;if(lost>.25f)owner=null}
            else if(!i.down){owner=null;lost=0f}
            else {
                lost=0f
                val before=lastDriver
                // Tracking jumps release at the last safe pose instead of throwing the panel.
                if(before!=null && (i.pose.p-before.p).length()>.35f){owner=null}
                else {
                    elapsed+=dt
                    val t=if(i.direct)0f else (elapsed/.45f).coerceIn(0f,1f).let{it*it*(3-2*it)}
                    val offset=relative*(1-t)+GrabV(0f,0f,-.35f)*t
                    pose=GrabPose(i.pose.p+i.pose.q.turn(offset),i.pose.q*relativeQ)
                    lastDriver=i.pose
                }
            }
        } else {
            val i=valid.firstOrNull{it.down && !previous[it.id] && contains(it)}
            if(i!=null){
                owner=i.id;placed=true;lost=0f;elapsed=0f;lastDriver=i.pose
                relative=i.pose.q.inverse().turn(pose.p-i.pose.p)
                relativeQ=i.pose.q.inverse()*pose.q
            }
        }
        // Missing samples stay disarmed until a real open/released sample is seen.
        valid.forEach{previous[it.id]=it.down}
    }
}
