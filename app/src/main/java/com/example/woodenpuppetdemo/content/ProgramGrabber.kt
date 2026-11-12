package com.example.woodenpuppetdemo.content

import android.os.SystemClock
import android.util.Log
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.core.math.Quat
import com.pico.spatial.tracking.controller.*
import com.pico.spatial.tracking.hand.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

internal class ProgramGrabber {
    val motion=ProgramGrabMotion()
    val emulatorInput = android.os.Build.HARDWARE == "ranchu"
    private var previewHeld=false
    private val previewFilter=ProgramDragFilter()
    fun previewDrag(panel:Entity, phase:Int, delta:GrabV) {
        if(!emulatorInput || motion.owner!=null)return
        val t=panel.components[TransformComponent::class.java]?:return
        if(phase==0) {
            previewHeld=true;previewFilter.begin(v(t.position))
            Log.i("WoodenPuppet","programPreviewGrab begin")
        }
        if(phase==1 && previewHeld) previewFilter.push(delta)
        if(phase==2) {
            previewFilter.release();previewHeld=false;blockedUntil=SystemClock.uptimeMillis()+250
            Log.i("WoodenPuppet","programPreviewGrab end position=${t.position}")
        }
    }
    private val controllers=ControllerTrackingProvider()
    private val hands=HandTrackingProvider()
    private var scope:CoroutineScope?=null
    private var controllerData:ControllerTrackingData?=null
    private var handData:HandTrackingData?=null
    private var controllerAt=0L;private var handAt=0L
    @Volatile private var actions:ControllerActionData?=null
    @Volatile private var actionAt=0L
    private val listener=ControllerTrackingProvider.ControllerActionListener { a->actions=a;actionAt=SystemClock.uptimeMillis() }
    private var measured=false
    private var width=0f;private var height=0f
    fun measure(w:Float,h:Float) { if(w>0f && h>0f && (w!=width || h!=height)){width=w;height=h;measured=false} }
    private var diagnosticAt=0L
    private var last=0L
    private var blockedUntil=0L
    private val pinched=BooleanArray(2)
    val blocksClicks get()=previewHeld || motion.owner!=null || SystemClock.uptimeMillis()<blockedUntil
    fun start() {
        if(scope!=null)return
        val s=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate);scope=s
        s.launch{controllers.dataFlow.collect{controllerData=it;controllerAt=SystemClock.uptimeMillis()}}
        s.launch{hands.dataFlow.collect{handData=it;handAt=SystemClock.uptimeMillis()}}
        controllers.addControllerActionListener(listener)
        controllers.start();hands.start()
        Log.i("WoodenPuppet","programTracking controller=${controllers.supportState} hand=${hands.supportState}")
    }
    fun stop(){controllers.removeControllerActionListener(listener);controllers.stop();hands.stop();scope?.cancel();scope=null}
    private fun v(p:Vector3)=GrabV(p.x,p.y,p.z)
    private fun q(p:Quat)=GrabQ(p.x,p.y,p.z,p.w)
    private fun converted(root:Entity,p:Vector3,r:Quat)=GrabPose(v(root.convertPositionFrom(p,null)),q(root.convertRotationFrom(r,null)))
    fun stringHandSamples(root:Entity):List<StringHandSample> {
        if(emulatorInput || SystemClock.uptimeMillis()-handAt>=250)return emptyList()
        return listOf(handData?.leftPinch,handData?.rightPinch).map { p ->
            if(p==null || !p.isReady)StringHandSample(false,0f,0f)
            else StringHandSample(true,p.pinchValue,root.convertPositionFrom(p.pinchPose.position,null).y)
        }
    }
    fun update(root:Entity,panel:Entity) {
        val now=SystemClock.uptimeMillis();val dt=if(last==0L)0f else (now-last)/1000f;last=now
        // ECS polling also handles providers becoming available after an input-mode switch.
        hands.latestData?.let { data ->
            if(data !== handData) { handData=data;handAt=now }
        }
        controllers.latestData?.let { data ->
            if(data !== controllerData) { controllerData=data;controllerAt=now }
        }
        val t=panel.components[TransformComponent::class.java]?:return
        if(!measured) {
            if(width<=0f || height<=0f)return
            val s=t.scaleVector
            motion.min=GrabV(-width*s.x/2f,-height*s.y/2f,0f)
            motion.max=GrabV(width*s.x/2f,height*s.y/2f,0f)
            measured=true
            Log.i("WoodenPuppet","programGrabBounds min=${motion.min} max=${motion.max}")
        }
        if(previewHeld) {
            val position=previewFilter.tick(dt)
            motion.placeExternally(GrabPose(position,q(t.quaternion)))
            t.setPosition(Vector3(position.x,position.y,position.z))
            return
        }
        if(!motion.placed)motion.pose=GrabPose(v(t.position),q(t.quaternion))
        val samples=mutableListOf<GrabInput>()
        val c=controllerData.takeIf{now-controllerAt<250}
        val a=actions.takeIf{now-actionAt<250}
        if(c!=null && a!=null) {
            c.left?.let{samples.add(GrabInput(0,converted(root,it.position,it.rotation),a.left.gripPressed || a.left.gripValue>.65f,false))}
            c.right?.let{samples.add(GrabInput(1,converted(root,it.position,it.rotation),a.right.gripPressed || a.right.gripValue>.65f,false))}
        }
        // Direct hand pinching is enabled only while neither controller has a tracked pose.
        if(c?.left==null && c?.right==null && now-handAt<250) {
            val h=handData
            listOf(h?.leftPinch,h?.rightPinch).forEachIndexed {i,p->
                if(p!=null && p.isReady) {
                    pinched[i]=if(pinched[i])p.pinchValue>.35f else p.pinchValue>.75f
                    samples.add(GrabInput(i+2,converted(root,p.pinchPose.position,p.pinchPose.rotation),pinched[i],true))
                }
            }
        }
        if(now-diagnosticAt>1500) {
            diagnosticAt=now
            Log.i("WoodenPuppet","programInput controller=${controllers.supportState} hand=${hands.supportState}/${hands.state} handAge=${now-handAt} trackedController=${c?.left!=null || c?.right!=null} left=${handData?.leftPinch} right=${handData?.rightPinch} panel=${motion.pose} samples=${samples.map { "${it.id}:down=${it.down},valid=${it.pose.valid()},hit=${motion.contains(it)},pose=${it.pose}" }}")
        }
        val old=motion.owner
        motion.update(samples,dt)
        if(motion.owner!=null) {
            val p=motion.pose
            t.setPosition(Vector3(p.p.x,p.p.y,p.p.z));t.setQuaternion(Quat(p.q.x,p.q.y,p.q.z,p.q.w))
        }
        if(old!=motion.owner) {
            blockedUntil=now+250
            Log.i("WoodenPuppet","programGrab owner=${motion.owner} previous=$old position=${motion.pose.p}")
        }
    }
}
