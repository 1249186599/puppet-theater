package com.example.woodenpuppetdemo.content

import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.resource.*
import com.pico.spatial.core.math.*
import kotlin.math.sin

internal class TheatreProps(val root: Entity,private val floor: Float,
    gearModel: Entity,boxModel: Entity,liftModel: Entity,frameModel: Entity,leafModel: Entity,surfaces: Entity,rampModel: Entity,perimeterModel: Entity,gateBar: Entity,gateMounts: Entity,railModels: Map<String,Entity>,private val lamps:TheatreLamps) {
    val actions=mutableMapOf<Entity,String>()
    private fun material(r: Float,g: Float,b: Float)=UnlitMaterial.create().apply { setBaseColor(Color4(r,g,b,1f)) }
    private val wood=material(.43f,.27f,.15f)
    private val dark=material(.25f,.15f,.09f)
    private val lampMat=lamps.lens()
    private val liftLamp=lamps.lens()
    private val exitMat=material(.30f,.22f,.12f)
    private val gear=Entity()
    private val lift=Entity()
    private val door=Entity()
    private val gate=Entity()
    private var gateOpen=0f
    private var lastGateTime=0f
    private fun position(e: Entity,x: Float,y: Float,z: Float) {
        e.components[TransformComponent::class.java]?.setPosition(Vector3(x,y,z))
    }
    private fun box(parent: Entity,x: Float,y: Float,z: Float,w: Float,h: Float,d: Float,m: UnlitMaterial): ModelEntity =
        ModelEntity(mesh=MeshResource.createBox(Vector3(w,h,d),minOf(w,h,d,.04f)*.1f),material=m).apply {
            position(this,x,y,z); parent.addChild(this)
        }
    private fun clickable(e: Entity,size: Vector3,action: String) {
        e.components.set(CollisionComponent(collisionShape=listOf(ShapeResource.createBox(size)),physicsMaterial=PhysicsMaterialResource()))
        e.components.set(InteractableComponent()); actions[e]=action
    }
    init {
        root.addChild(surfaces);position(surfaces,0f,floor,0f)
        val ground=Entity().apply {root.addChild(this);position(this,0f,floor-.014f,-.895736f)}
        clickable(ground,Vector3(1.697056f,.028f,1.089f),"floor")
        val upper=Entity().apply {root.addChild(this);position(this,0f,floor+TheatreLevel.TOP_HEIGHT-.020f,-1.744264f)}
        clickable(upper,Vector3(1.697056f,.040f,.608528f),"floor")
        // Textured oak perimeter and supports share the approved lift-railing finish.
        root.addChild(perimeterModel);position(perimeterModel,0f,floor,0f)
        // Approved fixed railings align with the actual upper landing front edge.
        val guides=railModels.getValue("fixed_guides")
        root.addChild(guides);position(guides,0f,floor,TheatreLevel.LIFT.z)
        for(name in listOf("landing_left","landing_right")) {
            val railing=railModels.getValue(name)
            root.addChild(railing);position(railing,0f,floor,TheatreLevel.LIFT.z-.085f)
        }
        // Fixed textured plank approach matches the existing .12m walking slope.
        root.addChild(rampModel);position(rampModel,0f,floor,0f)
        root.addChild(lift);lift.addChild(liftModel)
        // Local origins are on the cabin deck; moving rails inherit all lift travel/reset.
        for(name in listOf("moving_left","moving_right")) lift.addChild(railModels.getValue(name))
        clickable(lift,Vector3(.46f,.12f,.46f),"lift")
        // 33mm miniature fittings inside the deck edge; inherit lift movement.
        for(x in listOf(-.19f,.19f)) lamps.square(lift,Vector3(x,.001f,.212f),liftLamp,.5f)
        root.addChild(gear);gear.addChild(gearModel)
        clickable(gear,Vector3(.20f,.20f,.07f),"gear")
        val cabinet=Entity();root.addChild(cabinet);cabinet.addChild(boxModel)
        position(cabinet,.50f,floor+.125f,-.93f)
        clickable(cabinet,Vector3(.29f,.27f,.21f),"box")
        lamps.square(root,Vector3(.67f,floor,-.81f),lampMat)
        val frame=Entity();root.addChild(frame);frame.addChild(frameModel)
        position(frame,.42f,floor+TheatreLevel.TOP_HEIGHT+.35f,-1.96f)
        root.addChild(door)
        val width=.36588475f
        position(door,.42f-width/2f,floor+TheatreLevel.TOP_HEIGHT+.287f,-1.934f)
        val leaf=Entity();door.addChild(leaf);leaf.addChild(leafModel)
        position(leaf,width/2f,0f,-.026f);clickable(leaf,Vector3(width,.55f,.05f),"exit")
        val exit=box(root,TheatreLevel.EXIT.x,floor+TheatreLevel.TOP_HEIGHT+.003f,TheatreLevel.EXIT.z,.14f,.006f,.14f,exitMat)
        clickable(exit,Vector3(.18f,.02f,.18f),"exit")
        // Gate fittings follow the cabin; the bar pivots upward around its left pin.
        lift.addChild(gateMounts)
        lift.addChild(gate);gate.addChild(gateBar)
        position(gate,-.198f,.105f,.207f)
    }
    fun update(level: TheatreLevel,time: Float) {
        position(lift,0f,floor+level.liftHeight,TheatreLevel.LIFT.z)
        val gateDt=(time-lastGateTime).coerceIn(0f,.1f);lastGateTime=time
        gateOpen=if(!level.powered) 0f else (gateOpen+gateDt/0.55f).coerceAtMost(1f)
        val gateEase=gateOpen*gateOpen*(3f-2f*gateOpen)
        gate.components[TransformComponent::class.java]?.setQuaternion(
            Quat(Vector3(0f,0f,1f),gateEase*1.5707964f))
        val pulse=.80f+.20f*sin(time*2.4f)
        lamps.color(lampMat,if(level.powered) Color4(.25f,.70f,.36f,1f)
            else if(level.hasGear) Color4(.88f*pulse,.65f*pulse,.25f,1f) else Color4(.25f,.18f,.09f,1f))
        lamps.color(liftLamp,if(level.upstairs) Color4(.25f,.70f,.36f,1f)
            else if(level.powered) Color4(.85f*pulse,.65f*pulse,.25f,1f) else Color4(.25f,.18f,.09f,1f))
        gear.enabled=!level.completed
        val p=when {level.installed -> WalkPoint(.50f,-.817f);level.hasGear -> level.position;else -> TheatreLevel.GEAR}
        val y=when {level.installed -> .134f;level.hasGear -> .64f;else -> .13f+sin(time*2f)*.008f}
        position(gear,p.x,floor+y,p.z)
        gear.components[TransformComponent::class.java]?.setQuaternion(
            Quat(Vector3(0f,0f,1f),if(level.powered) time*.55f else 0f))
        exitMat.setBaseColor(if(level.completed) Color4(.25f,.70f,.36f,1f)
            else if(level.upstairs) Color4(.85f*pulse,.65f*pulse,.25f,1f) else Color4(.30f,.22f,.12f,1f))
        door.components[TransformComponent::class.java]?.setQuaternion(
            Quat(Vector3(0f,1f,0f),if(level.completed) -1.5f else 0f))
    }
}
