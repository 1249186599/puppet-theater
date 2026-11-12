package com.example.woodenpuppetdemo.content

import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.resource.*
import com.pico.spatial.core.math.*
import kotlin.math.*

internal data class RibbonFrame(val visible:Boolean,val x:Float=0f,val y:Float=0f,val z:Float=0f,val spin:Float=0f,val scale:Float=1f)

internal object FinaleRibbonMotion {
    const val COUNT=64
    private fun seed(i:Int,k:Int)=((i*k+17)%101)/100f
    fun frame(i:Int,time:Float):RibbonFrame {
        val age=time-i*2.5f/(COUNT-1)
        val life=4.8f+seed(i,37)*.6f
        if(age<0f || age>=life)return RibbonFrame(false)
        val q=age/life
        val x=-.90f+seed(i,43)*1.80f+sin(age*2.2f+i)*.07f*sin(q*PI.toFloat())
        val z=-2.04f+seed(i,61)*.40f+sin(age*1.6f+i)*.025f
        val ground=FinaleLevel.stageHeightAt(WalkPoint(x,z))+.06f
        val y=ground+(1.40f+seed(i,29)*.20f-ground)*(1f-q)
        return RibbonFrame(true,x,y,z,age*(2.2f+seed(i,31)*2f)+i,((1f-q)/.12f).coerceIn(0f,1f))
    }
}

/** Reused curled ribbon mesh; no colliders, per-frame allocations of Entities, or endless emitter. */
internal class FinaleConfetti(parent:Entity,private val floor:Float) {
    private val root=Entity().also {parent.addChild(it);it.enabled=false}
    private val ribbons:List<Entity>
    init {
        val points=mutableListOf<Vector3>();val indices=mutableListOf<Int>()
        for(i in 0..8) {
            val t=i/8f;val x=sin(t*PI.toFloat()*2f)*.012f;val z=cos(t*PI.toFloat()*2f)*.008f
            points+=Vector3(x-.006f,(t-.5f)*.11f,z);points+=Vector3(x+.006f,(t-.5f)*.11f,z)
            if(i<8){val a=i*2;indices+=listOf(a,a+1,a+2,a+1,a+3,a+2,a+2,a+1,a,a+2,a+3,a+1)}
        }
        val mesh=MeshResource.createWithMeshModel(MeshModel(positions=points,triangleIndices=indices),
            BoundingBox(center=Vector3(0f,0f,0f),halfExtent=Vector3(.018f,.055f,.008f)),"finale_curled_ribbon")
        val colors=listOf(Color4(.90f,.24f,.20f,1f),Color4(.96f,.70f,.18f,1f),Color4(.12f,.65f,.60f,1f),Color4(.97f,.89f,.66f,1f))
        val materials=colors.map{c->PhysicallyBasedMaterial.create().apply{setBaseColor(c);setRoughness(.55f);setMetallic(.12f)}}
        ribbons=List(FinaleRibbonMotion.COUNT){i->ModelEntity(mesh=mesh,material=materials[i%materials.size]).also{root.addChild(it)}}
    }
    fun update(level:FinaleLevel) {
        root.enabled=level.completed && level.celebrationTime<FinaleShow.CELEBRATION_DURATION
        if(!root.enabled)return
        ribbons.forEachIndexed {i,e->
            val f=FinaleRibbonMotion.frame(i,level.celebrationTime);e.enabled=f.visible
            if(f.visible)e.components[TransformComponent::class.java]?.apply {
                setPosition(Vector3(f.x,floor+f.y,f.z))
                setQuaternion(Quat(Vector3(.282216f,.940721f,.188144f),f.spin))
                setScaleVector(Vector3(f.scale))
            }
        }
    }
}
