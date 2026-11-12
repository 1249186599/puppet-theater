package com.example.woodenpuppetdemo.content

import android.util.Log
import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.resource.*
import com.pico.spatial.core.math.*
import kotlin.math.*

internal class CargoProps(val root: Entity,private val floor: Float,
    cartModel: Entity,coreModel: Entity,cradleModel: Entity,frameModel: Entity,leafModel: Entity,surface: Entity,tracksModel: Entity,trackDiscModel: Entity,supportBoxModel: Entity,rotorBaseModel: Entity,private val art:CargoArt) {
    val actions=mutableMapOf<Entity,String>()
    private fun mat(r: Float,g: Float,b: Float)=UnlitMaterial.create().apply {setBaseColor(Color4(r,g,b,1f))}
    private val wood=mat(.46f,.29f,.16f)
    private val dark=mat(.22f,.14f,.09f)
    private val brass=mat(.62f,.46f,.24f)
    private val cream=mat(.90f,.79f,.52f)
    private val blue=mat(.20f,.43f,.48f)
    private val green=mat(.25f,.72f,.38f)
    private val red=mat(.66f,.28f,.12f)
    private val cart=Entity()
    private val core=Entity()
    private val loadHit=Entity()
    private val loadMat=mat(.14f,.30f,.34f)
    private val loadHalo=Entity()
    private val tray=Entity()
    private val turntable=Entity()
    private val branchPointer=Entity()
    private val dial=Entity()
    private val coreMark=Entity()
    private val lock=Entity()
    private val door=Entity()
    private val clampLeft=Entity()
    private val clampRight=Entity()
    private val loadingArm=Entity()
    private fun pos(e: Entity,x: Float,y: Float,z: Float) {e.components[TransformComponent::class.java]?.setPosition(Vector3(x,y,z))}
    private fun box(p: Entity,x: Float,y: Float,z: Float,w: Float,h: Float,d: Float,m: UnlitMaterial):ModelEntity=
        ModelEntity(mesh=MeshResource.createBox(Vector3(w,h,d),minOf(w,h,d,.04f)*.10f),material=m).apply {pos(this,x,y,z);p.addChild(this)}
    private fun cylinder(p: Entity,x: Float,y: Float,z: Float,r: Float,h: Float,m: UnlitMaterial):ModelEntity=
        ModelEntity(mesh=MeshResource.createCylinder(r,h),material=m).apply {pos(this,x,y,z);p.addChild(this)}
    private fun click(e: Entity,size: Vector3,action: String) {
        e.components.set(CollisionComponent(collisionShape=listOf(ShapeResource.createBox(size)),physicsMaterial=PhysicsMaterialResource()))
        e.components.set(InteractableComponent());actions[e]=action
    }
    private fun line(p: Entity,x1: Float,z1: Float,x2: Float,z2: Float,y: Float,width: Float,height: Float,m: UnlitMaterial) {
        val dx=x2-x1;val dz=z2-z1
        val e=box(p,(x1+x2)/2,y,(z1+z2)/2,width,height,hypot(dx,dz),m)
        e.components[TransformComponent::class.java]?.setQuaternion(Quat(Vector3(0f,1f,0f),atan2(dx,dz)))
    }
    private fun symbol(p: Entity,x: Float,y: Float,z: Float,type: Int,r: Float=.025f) {
        val n=if(type==0) 12 else if(type==1) 4 else 3
        val offset=if(type==1) Math.PI.toFloat()/4f else 0f
        repeat(n){i ->
            val a=i*2f*Math.PI.toFloat()/n+offset;val b=(i+1)*2f*Math.PI.toFloat()/n+offset
            line(p,x+cos(a)*r,z+sin(a)*r,x+cos(b)*r,z+sin(b)*r,y,.004f,.003f,cream)
        }
    }
    private fun button(x:Float,z:Float,action:String,type:Int,material:UnlitMaterial=brass):Entity {
        val e=Entity();pos(e,x,floor+.025f,z);root.addChild(e)
        val model=if(action=="switch" || action=="dial")art.control(action) else art.button(action)
        model.components[TransformComponent::class.java]?.apply {
            setPosition(Vector3(0f,-.025f,0f))
            setScaleVector(Vector3(if(action=="switch")1.05f else if(action=="dial")1.10f else 1.6f))
        }
        e.addChild(model);click(e,Vector3(.14f,.075f,.13f),action);return e
    }
    fun press(action:String,time:Float) {art.pulse(action,time)}
    fun resetFeedback() {art.resetPresses()}
    init {
        // Approved deck local Y=.048. 2% uniform fit clears the unchanged front keys.
        val floorScale=1.02f
        root.addChild(surface)
        surface.components[TransformComponent::class.java]?.setScaleVector(Vector3(floorScale))
        pos(surface,0f,floor-.048f*floorScale,-1.27f)
        fun findPart(e:Entity,name:String):Entity? {
            if(e.getName()==name)return e
            for(c in e.getChildren())findPart(c,name)?.let{return it}
            return null
        }
        val deck=requireNotNull(findPart(surface,"FloorPlanks"))
        val deckBounds=deck.getVisualBounds(root,recursive=true,enabledOnly=false)
        val floorBounds=surface.getVisualBounds(root,recursive=true,enabledOnly=false)
        check(abs(deckBounds.max.y-floor)<.001f && abs(floorBounds.center.z+1.27f)<.001f)
        Log.i("WoodenPuppet","chapter3GroundReady scale=$floorScale deckY=${deckBounds.max.y} min=${floorBounds.min} max=${floorBounds.max}")
        val ground=Entity().apply {root.addChild(this);pos(this,0f,floor-.015f,-1.27f)}
        click(ground,Vector3(2.8f,.03f,1.86f),"floor")
        // Fitted source GLBs share the original .19m gauge and .013m rail top.
        root.addChild(tracksModel);pos(tracksModel,0f,floor,0f)
        pos(turntable,0f,floor,-1.20f);root.addChild(turntable)
        turntable.addChild(trackDiscModel)
        val iron=mat(.23f,.22f,.20f)
        // Short steel tongues bridge the circular rim to the straight rail ends.
        for(x in listOf(-.188f,.188f)) for(z in listOf(-.095f,.095f))
            box(turntable,x,.010f,z,.049f,.006f,.018f,iron)
        // No overlapping wooden sleepers at the T junction.
        for(x in listOf(.255f,.445f))
            box(root,x,floor+.010f,-1.2725f,.018f,.006f,.155f,iron)
        click(turntable,Vector3(.40f,.04f,.40f),"track")
        art.lamps(floor)
        val frontKeys=listOf(button(-1.02f,-.46f,"lockSquare",1),
            button(-.83f,-.46f,"lockCircle",0),button(-.64f,-.46f,"lockTriangle",2))
        val frontInnerEdge=-1.27f+.858f*floorScale
        val keyFront=frontKeys.maxOf{it.getVisualBounds(root,recursive=true,enabledOnly=false).max.z}
        check(frontInnerEdge-keyFront>=.009f)
        Log.i("WoodenPuppet","chapter3FrontKeyClearance gap=${frontInnerEdge-keyFront} originalPositions=true")
        root.addChild(lock);pos(lock,CargoLevel.ORIGIN.x-.23f,floor+.04f,-1.20f)
        art.lock(lock)
        root.addChild(cart);cart.addChild(cartModel);click(cart,Vector3(.28f,.25f,.34f),"cart")
        root.addChild(core);core.addChild(coreModel)
        root.addChild(tray);pos(tray,-.55f,floor+.215f,-1.72f)
        // Matching PBR assembly: independent fixed shelf, moving cradle and guides.
        val movingCradle=requireNotNull(findPart(rotorBaseModel,"MovingTray"))
        val guideRails=requireNotNull(findPart(rotorBaseModel,"GuideRails"))
        movingCradle.removeFromParent();tray.addChild(movingCradle)
        pos(movingCradle,0f,-.018f,0f) // Recover the original tray animation pivot.
        guideRails.removeFromParent();root.addChild(loadingArm);loadingArm.addChild(guideRails)
        pos(guideRails,-.55f,floor+.197f,-1.72f)
        val loadingShelf=rotorBaseModel
        root.addChild(loadingShelf);pos(loadingShelf,-.55f,floor+.197f,-1.72f)
        // Approved bottom-centred box: feet on deck; wood top supports unchanged shelf.
        val supportScale=.197f/.210f
        root.addChild(supportBoxModel)
        requireNotNull(supportBoxModel.components[TransformComponent::class.java]).apply {
            setScaleVector(Vector3(supportScale))
            setPosition(Vector3(-.55f,floor,-1.72f))
        }
        val supportBounds=supportBoxModel.getVisualBounds(root,recursive=true,enabledOnly=false)
        val supportTop=requireNotNull(findPart(supportBoxModel,"Top"))
            .getVisualBounds(root,recursive=true,enabledOnly=false)
        val shelfBounds=loadingShelf.getVisualBounds(root,recursive=true,enabledOnly=false)
        check(abs(supportBounds.min.y-deckBounds.max.y)<.001f)
        check(abs(supportTop.max.y-shelfBounds.min.y)<.001f)
        check(abs(supportBounds.center.x+.55f)<.001f && abs(supportBounds.center.z+1.72f)<.001f)
        check(supportTop.min.x>=shelfBounds.min.x && supportTop.max.x<=shelfBounds.max.x)
        check(supportTop.min.z>=shelfBounds.min.z && supportTop.max.z<=shelfBounds.max.z)
        Log.i("WoodenPuppet","chapter3SupportBoxReady scale=$supportScale min=${supportBounds.min} max=${supportBounds.max} floorGap=${supportBounds.min.y-deckBounds.max.y} shelfGap=${shelfBounds.min.y-supportTop.max.y} lowerBoxUnchanged=true")
        val cradleBounds=movingCradle.getVisualBounds(root,recursive=true,enabledOnly=false)
        val railBounds=guideRails.getVisualBounds(root,recursive=true,enabledOnly=false)
        check(abs(cradleBounds.min.y-(floor+.213f))<.001f)
        check(abs(railBounds.max.z-(-1.22016f))<.001f)
        Log.i("WoodenPuppet","chapter3RotorBaseReady sharedBoxPbr=true cradleMin=${cradleBounds.min} cradleMax=${cradleBounds.max} railMin=${railBounds.min} railMax=${railBounds.max} shelfMin=${shelfBounds.min} shelfMax=${shelfBounds.max} separateMovingCradle=true")
        // Keep the loading control in the unobstructed front row.
        button(-.30f,-.48f,"load",0,loadMat)
        root.addChild(loadHit)
        pos(loadHit,-.55f,floor+.305f,-1.72f)
        click(loadHit,Vector3(.23f,.20f,.21f),"load")
        root.addChild(loadHalo)
        pos(loadHalo,-.55f,floor+.217f,-1.72f)
        // Availability is shown by the crafted loading key, without a floating frame.
        art.floorMarks(floor)
        cart.addChild(clampLeft);cart.addChild(clampRight)
        art.clamps(clampLeft,clampRight)
        // Route selector shows the same circular destination symbol as the dock.
        button(.28f,-.48f,"switch",0)
        pos(branchPointer,.35f,floor+.030f,-1.20f);root.addChild(branchPointer)
        art.pointer(branchPointer)
        button(.05f,-1.91f,"rewind",1,blue)
        // Dock's bearing seats and transfer track are stationary.
        val cradle=Entity();root.addChild(cradle);cradle.addChild(cradleModel)
        pos(cradle,CargoLevel.CRADLE.x,floor+.08f,CargoLevel.CRADLE.z)
        click(cradle,Vector3(.30f,.18f,.25f),"dock")
        for(x in listOf(.76f,1f)) box(root,x,floor+.075f,-1.48f,.012f,.012f,.50f,brass)
        button(.88f,-.48f,"dial",0)
        root.addChild(coreMark);pos(coreMark,1.005f,floor+.145f,-1.72f)
        art.dock(coreMark)
        val frame=Entity();root.addChild(frame);frame.addChild(frameModel)
        pos(frame,.92f,floor+.35f,-2.04f)
        root.addChild(door);pos(door,.92f+.36588475f/2f,floor+.287f,-2.014f)
        val leaf=Entity();door.addChild(leaf);leaf.addChild(leafModel)
        pos(leaf,-.36588475f/2f,0f,-.026f);click(leaf,Vector3(.36588475f,.55f,.05f),"exit")
    }
    fun update(level: CargoLevel,time: Float) {
        pos(cart,level.cart.x,floor+.128f,level.cart.z)
        cart.components[TransformComponent::class.java]?.setQuaternion(Quat(Vector3(0f,1f,0f),level.cartYaw+Math.PI.toFloat()))
        lock.enabled=!level.unlocked
        turntable.components[TransformComponent::class.java]?.setQuaternion(Quat(Vector3(0f,1f,0f),level.trackQuarter*Math.PI.toFloat()/2f))
        branchPointer.components[TransformComponent::class.java]?.setQuaternion(Quat(Vector3(0f,1f,0f),if(level.correctBranch)0f else Math.PI.toFloat()/2f))
        val pulse=.78f+.22f*sin(time*2.4f)
        loadHit.enabled=!level.loaded && level.transferKind.isEmpty()
        loadHalo.enabled=level.canLoad
        loadMat.setBaseColor(when {
            level.loaded -> Color4(.25f,.70f,.36f,1f)
            level.canLoad -> Color4(.25f*pulse,.72f*pulse,.90f*pulse,1f)
            else -> Color4(.14f,.30f,.34f,1f)
        })
        art.update(level,time)
        val t=level.transfer*level.transfer*(3f-2f*level.transfer)
        var x=if(level.loaded)level.cart.x else -.55f
        var z=if(level.loaded)level.cart.z else -1.72f
        var y=if(level.loaded).245f else .305f
        if(level.seated) {x=.88f;z=-1.72f;y=.145f}
        if(level.transferKind=="load") {x=-.55f;z=-1.72f+.52f*t;y=.305f+(.245f-.305f)*t+sin(t*Math.PI.toFloat())*.12f}
        if(level.transferKind=="dock") {x=.88f;z=-1.20f-.52f*t;y=.245f+(.145f-.245f)*t+sin(t*Math.PI.toFloat())*.12f}
        pos(core,x,floor+y,z)
        val angle=if(level.powered)time*.9f else if(level.seated)level.coreQuarter*Math.PI.toFloat()/2f else 0f
        core.components[TransformComponent::class.java]?.setQuaternion(Quat(Vector3(1f,0f,0f),angle))
        tray.enabled=level.transferKind.isNotEmpty() || !level.loaded
        pos(tray,x,floor+y-.09f,z)
        loadingArm.enabled=!level.loaded || level.transferKind=="load"
        clampLeft.enabled=level.loaded && !level.seated && level.transferKind!="dock"
        clampRight.enabled=clampLeft.enabled
        pos(clampLeft,-.115f,.10f,0f);pos(clampRight,.115f,.10f,0f)
        coreMark.enabled=level.seated
        door.components[TransformComponent::class.java]?.setQuaternion(Quat(Vector3(0f,1f,0f),if(level.completed)1.5f else 0f))
    }
}
