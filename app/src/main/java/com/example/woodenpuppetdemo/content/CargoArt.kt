package com.example.woodenpuppetdemo.content

import android.util.Log
import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.resource.*
import com.pico.spatial.core.math.*
import kotlin.math.*

/** Approved GLB parts, retaining their authored local pivots and PBR surfaces. */
internal class CargoArt(private val models:Map<String,Entity>, private val scene:Entity) {
    private data class Press(val node:Entity,val rest:Vector3,var start:Float=-100f)
    private val presses=mutableMapOf<String,Press>()
    private val indicators=mutableListOf<PhysicallyBasedMaterial>()
    private var loadFace:PhysicallyBasedMaterial?=null
    private var selectorHandle:Entity?=null
    private var dialHandle:Entity?=null
    private var dockPointer:Entity?=null
    init {models.values.forEach {it.enabled=false;scene.addChild(it)}}
    private fun find(e:Entity,name:String):Entity? {
        if(e.getName()==name)return e
        for(c in e.getChildren()) find(c,name)?.let {return it}
        return null
    }
    private fun take(asset:String,names:List<String>,center:Vector3=Vector3(0f)):Entity {
        val group=Entity();val offset=Entity();group.addChild(offset)
        offset.components[TransformComponent::class.java]?.setPosition(Vector3(-center.x,-center.y,-center.z))
        for(name in names) {
            val e=requireNotNull(find(models.getValue(asset),name)) {"Missing cargo art part $asset/$name"}
            e.removeFromParent();offset.addChild(e)
        }
        return group
    }
    private fun place(e:Entity,parent:Entity,p:Vector3,scale:Float=1f,yaw:Float=0f):Entity {
        e.components[TransformComponent::class.java]?.apply {setPosition(p);setScaleVector(Vector3(scale));setQuaternion(Quat(Vector3(0f,1f,0f),yaw))}
        parent.addChild(e);return e
    }
    fun button(action:String):Entity {
        val shape=when(action){"lockSquare"->"square";"lockTriangle"->"triangle";else->"circle"}
        val prefix=when(action){"load"->"load";"rewind"->"return";else->shape}
        val asset=if(action=="load" || action=="rewind") "03_load_return" else "01_symbol_keys"
        val x=when(prefix){"square"->-.079f;"triangle"->.079f;"load"->-.043f;"return"->.043f;else->0f}
        val e=take(asset,listOf(prefix+"_wood",prefix+"_bezel",prefix+"_key"),Vector3(x,0f,0f))
        val key=requireNotNull(find(e,prefix+"_key"));val t=requireNotNull(key.components[TransformComponent::class.java])
        presses[action]=Press(key,t.position)
        if(action=="load") loadFace=find(e,"load_face")?.components?.get(ModelComponent::class.java)?.materials?.firstOrNull() as? PhysicallyBasedMaterial
        return e
    }
    fun pulse(action:String,time:Float) {presses[action]?.start=time}
    fun control(action:String):Entity {
        if(action=="switch") {
            val e=take("04_route_selector",listOf("round_wood","brass_perimeter","circle_rim","circle_badge","square_rim","square_badge","SelectorHandle"))
            // Match the stage destinations: square on the left, circle on the right.
            // Each symbol and its brass rim move together; authored centers are +/-27mm.
            for((prefix,dx) in listOf("circle" to .054f,"square" to -.054f)) {
                for(suffix in listOf("_rim","_badge")) {
                    find(e,prefix+suffix)?.components?.get(TransformComponent::class.java)
                        ?.setPosition(Vector3(dx,0f,0f))
                }
            }
            selectorHandle=find(e,"SelectorHandle");return e
        }
        val e=take("05_alignment_dial",listOf("wood_base","wood_dial","bezel","index_0","index_1","index_2","index_3","fixed_notch","DialHandle"))
        dialHandle=find(e,"DialHandle")
        // The approved asset was posed at -0.6 radians for its preview. Its
        // handle center must also lie on the pivot axis before removing that pose.
        val radius=hypot(.010f,.012f)
        find(e,"teal_handle")?.components?.get(TransformComponent::class.java)?.setPosition(
            Vector3(radius*sin(.6f)-.010f,0f,-radius*cos(.6f)+.012f))
        return e
    }
    fun lock(parent:Entity) {
        val e=take("02_track_lock",listOf("LockBar","foot_0_-0.031","foot_0_0.031","foot_1_-0.031","foot_1_0.031"))
        // Across the original track: the bar's authored long X axis becomes Z.
        place(e,parent,Vector3(0f,-.04f,0f),1.15f,Math.PI.toFloat()/2f)
    }
    fun pointer(parent:Entity) {
        val e=take("04_route_selector",listOf("pointer_base","TrackPointer"),Vector3(.105f,0f,0f))
        place(e,parent,Vector3(0f,-.025f,0f),2.2f,-Math.PI.toFloat()/4f)
    }
    fun lamps(floor:Float) {
        for((i,x) in listOf(-1.04f,-.55f,0f,.35f,.88f).withIndex()) {
            val prefix="step_${i+1}"
            val e=take("06_progress_lamps",listOf(prefix+"_base",prefix+"_bezel",prefix+"_lens"),Vector3((i-2)*.055f,0f,0f))
            val lens=requireNotNull(find(e,prefix+"_lens"));val model=requireNotNull(lens.components[ModelComponent::class.java])
            val m=PhysicallyBasedMaterial.create().apply {setRoughness(.36f);setMetallic(0f);setBaseColor(Color4(.14f,.115f,.08f,1f))}
            lens.components.set(ModelComponent(model.mesh,m));indicators.add(m)
            place(e,scene,Vector3(x,floor,-.83f))
        }
    }
    fun tray(parent:Entity) {place(take("07_loading_clamps",listOf("SlidingTray"),Vector3(-.058f,.0235f,0f)),parent,Vector3(0f),3f)}
    fun clamps(left:Entity,right:Entity) {
        for((side,x,parent) in listOf(Triple("left",.025f,left),Triple("right",.078f,right))) {
            val g=if(side=="left") "LeftClamp" else "RightClamp"
            place(take("07_loading_clamps",listOf(g,side+"_foot"),Vector3(x,.015f,0f)),parent,Vector3(0f),1.1f)
        }
    }
    fun loadingRail(parent:Entity,floor:Float) {
        // Use both approved rails, aligned with the existing transfer path.
        for((name,x) in listOf("slide_rail_-0.025" to -.67f,"slide_rail_0.025" to -.43f)) {
            val z=if(x<-.55f)-.025f else .025f
            val e=take("07_loading_clamps",listOf(name),Vector3(0f,.016f,z))
            place(e,parent,Vector3(x,floor+.19f,-1.48f),2.32f,Math.PI.toFloat()/2f)
        }
    }
    fun floorMarks(floor:Float) {
        for((prefix,x,z) in listOf(Triple("circle",.88f,-1.42f),Triple("square",.35f,-2.04f))) {
            val extras=if(prefix=="circle")listOf("circle_symbol") else listOf("square_z-0.016","square_z0.016","square_x-0.016","square_x0.016")
            val e=take("09_floor_marks",listOf(prefix+"_wood",prefix+"_rim")+extras,Vector3(if(prefix=="circle")-.056f else .056f,0f,0f))
            place(e,scene,Vector3(x,floor+.001f,z),1.2f)
        }
        val line=take("09_floor_marks",listOf("stop_line_brass","stop_line_ivory"),Vector3(0f,0f,.058f))
        place(line,scene,Vector3(CargoLevel.LOAD.x,floor+.001f,-1.20f),3f,Math.PI.toFloat()/2f)
    }
    fun dock(parent:Entity) {
        val e=take("08_dock_alignment",listOf("hub_collar","hub_inset","MovingIndex","fixed_ivory_tick","fixed_ivory_foot"))
        dockPointer=find(e,"MovingIndex")
        e.components[TransformComponent::class.java]?.apply {setScaleVector(Vector3(.85f));setQuaternion(Quat(Vector3(0f,0f,1f),-Math.PI.toFloat()/2f))}
        parent.addChild(e)
        Log.i("WoodenPuppet","cargoCraftedArtReady categories=9 splitKeys=${presses.size} lenses=${indicators.size} selector=${selectorHandle!=null} dial=${dialHandle!=null} dock=${dockPointer!=null}")
    }
    fun update(level:CargoLevel,time:Float) {
        for(p in presses.values) {
            val age=time-p.start;val down=if(age in 0f.. .26f) sin(age/.26f*Math.PI.toFloat())*.0015f else 0f
            p.node.components[TransformComponent::class.java]?.setPosition(Vector3(p.rest.x,p.rest.y-down,p.rest.z))
        }
        selectorHandle?.components?.get(TransformComponent::class.java)?.setQuaternion(Quat(Vector3(0f,1f,0f),if(level.correctBranch)0f else Math.PI.toFloat()/2f))
        dialHandle?.components?.get(TransformComponent::class.java)?.setQuaternion(Quat(Vector3(0f,1f,0f),.6f-level.coreQuarter*Math.PI.toFloat()/2f))
        val angle=if(level.powered)0f else level.coreQuarter*Math.PI.toFloat()/2f
        dockPointer?.components?.get(TransformComponent::class.java)?.setQuaternion(Quat(Vector3(0f,1f,0f),Math.PI.toFloat()+angle))
        loadFace?.setEmissiveColor(when {
            level.loaded -> Color4(.025f,.05f,.015f,1f)
            level.canLoad -> Color4(.02f,.05f,.055f,1f)
            else -> Color4(0f,0f,0f,1f)
        })
        val done=listOf(level.unlocked,level.loaded,level.trackReady,level.docked,level.powered)
        val pulse=.78f+.22f*sin(time*2.4f)
        for((i,m) in indicators.withIndex()) {
            val color=if(done[i])Color4(.24f,.45f,.22f,1f) else if(level.stage==i+1)Color4(.65f*pulse,.37f*pulse,.10f*pulse,1f) else Color4(.14f,.115f,.08f,1f)
            m.setBaseColor(color);m.setEmissiveColor(Color4(color.red*.08f,color.green*.08f,color.blue*.08f,1f))
        }
    }
    fun resetPresses() {presses.values.forEach {it.start=-100f}}
}
