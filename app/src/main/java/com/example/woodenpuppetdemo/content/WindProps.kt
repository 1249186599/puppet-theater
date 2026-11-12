package com.example.woodenpuppetdemo.content

import android.util.Log
import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.animation.AnimationPlayConfig
import com.pico.spatial.core.ecs.animation.AnimationPlaybackController
import com.pico.spatial.core.ecs.animation.AnimationTransitionMode
import com.pico.spatial.core.ecs.resource.*
import com.pico.spatial.core.math.*
import kotlin.math.*

internal class WindProps(private val root:Entity,private val floor:Float,private val models:Map<String,Entity>) {
    val actions=mutableMapOf<Entity,String>()
    private fun mat(r:Float,g:Float,b:Float)=UnlitMaterial.create().apply {setBaseColor(Color4(r,g,b,1f))}
    private val wood=mat(.39f,.23f,.12f);private val dark=mat(.22f,.14f,.09f);private val brass=mat(.62f,.46f,.24f)
    private val cream=mat(.90f,.79f,.52f);private val teal=mat(.12f,.29f,.29f)
    private val windmill=Entity();private val rotor=Entity();private val scoop=Entity();private val ferry=Entity();private val power=Entity()
    private val shields=List(2){Entity()};private val streams=List(2){Entity()}
    private val chimes=Entity();private val notes=List(3){Entity()}
    // Seats sampled from wind_frame.glb after its authored frameFit transform.
    // These are frame-local points; each note root is also its fixed string/sway pivot.
    private val hookSeats=listOf(Vector3(-.0904f,.2000f,0f),
        Vector3(-.0027f,.2005f,0f),Vector3(.0875f,.2002f,0f))
    private val frameCenterY=.16f+.285f
    private var chimesWereHung=false
    private val door=Entity()
    private val shieldAngles=floatArrayOf(1.35f,1.35f)
    private val riverMotion: RiverMotion
    private var previousTime=0f
    private val ribbonMesh = models.getValue("scoopRibbon").findSkinnedMeshEntity().firstOrNull()
    private val ribbonClips = ribbonMesh?.getAnimationResources().orEmpty().associateBy { it.getName() }
    private var ribbonPlayback: AnimationPlaybackController? = null
    private var ribbonWind = false
    private val air=Entity()
    private val airMat=mat(.40f,.40f,.34f)
    private val scoopLens = PhysicallyBasedMaterial.create().apply {
        setMetallic(0f)
        setRoughness(.39f)
    }
    private val leverLight: Indicator
    private val branchLights: List<Indicator>
    private val ferryLight: Indicator
    private val dockLights: List<Indicator>
    private val gateButtons = mutableListOf<WorkshopButton>()
    private val noteButtons = mutableListOf<WorkshopButton>()
    private val routes=List(2){mutableListOf<UnlitMaterial>()}
    private var scoopAngle=WindmillLayout.scoopYaw(0)
    private var rotorAngle=0f
    private var powerAngle=0f
    var airflowOn=false;private set
    private fun pos(e:Entity,x:Float,y:Float,z:Float){e.components[TransformComponent::class.java]?.setPosition(Vector3(x,y,z))}
    private fun box(p:Entity,x:Float,y:Float,z:Float,w:Float,h:Float,d:Float,m:UnlitMaterial):Entity =
        ModelEntity(mesh=MeshResource.createBox(Vector3(w,h,d),minOf(w,h,d,.04f)*.10f),material=m).apply{pos(this,x,y,z);p.addChild(this)}
    private fun click(e:Entity,size:Vector3,a:String){e.components.set(CollisionComponent(collisionShape=listOf(ShapeResource.createBox(size)),physicsMaterial=PhysicsMaterialResource()));e.components.set(InteractableComponent());actions[e]=a}
    private fun model(name:String,parent:Entity,x:Float,y:Float,z:Float):Entity {val e=Entity();parent.addChild(e);pos(e,x,y,z);e.addChild(models.getValue(name));return e}
    init {
        model("floor",root,0f,floor,0f)
        val ground=Entity();root.addChild(ground);pos(ground,0f,floor-.025f,-1.36f);click(ground,Vector3(2.8f,.04f,2.04f),"floor")
        // Hit the elevated walking surface, not the water plane underneath it.
        // Root-local bounds match the authored rear deck; buttons/frame remain above this slab.
        val rearGround=Entity();root.addChild(rearGround)
        pos(rearGround,(WindLevel.DECK_MIN_X+WindLevel.DECK_MAX_X)/2f,
            floor+WindLevel.DECK_HEIGHT-.005f,(WindLevel.DECK_MIN_Z+WindLevel.DECK_MAX_Z)/2f)
        click(rearGround,Vector3(WindLevel.DECK_MAX_X-WindLevel.DECK_MIN_X,.01f,
            WindLevel.DECK_MAX_Z-WindLevel.DECK_MIN_Z),"floor")
        Log.i("WoodenPuppet","windRearFloorReady topY=${floor+WindLevel.DECK_HEIGHT} x=-.5..1.3 z=-2.38..-1.76")
        // The original box is the water volume; the ripple mesh is its visible surface.
        box(root,0f,floor-.023f,-1.36f,2.8f,.012f,2.04f,teal)
        riverMotion = RiverMotion(model("river",root,0f,floor,0f))
        for(x in listOf(-1.39f,1.39f))box(root,x,floor+.015f,-1.36f,.025f,.05f,2.04f,dark)
        for(x in listOf(-.43f,1.23f)) for(z in listOf(-1.84f,-2.30f))
            box(root,x,floor+.05f,z,.07f,.20f,.07f,dark)
        // The tower includes its stone footing. The independent rotor shares the fixed bearing axis.
        root.addChild(windmill);pos(windmill,WindmillLayout.X,floor,WindmillLayout.Z)
        windmill.components[TransformComponent::class.java]?.setQuaternion(
            Quat(Vector3(0f,1f,0f),WindmillLayout.facing))
        model("housing",windmill,0f,0f,0f)
        windmill.addChild(rotor)
        pos(rotor,0f,WindmillLayout.HUB_Y,WindmillLayout.HUB_Z)
        rotor.addChild(models.getValue("rotor"))
        // The fixed spindle and the moving barrel share the same Y axis.
        val base = model("scoopBase",root,WindmillLayout.SCOOP_X,floor,WindmillLayout.SCOOP_Z)
        findNamed(base,"Jade")?.let { lens ->
            lens.components[ModelComponent::class.java]?.let { component ->
                lens.components.set(ModelComponent(component.mesh,scoopLens))
            }
        }
        root.addChild(scoop)
        pos(scoop,WindmillLayout.SCOOP_X,floor+WindmillLayout.SCOOP_Y,WindmillLayout.SCOOP_Z)
        scoop.addChild(models.getValue("scoop"))
        click(scoop,Vector3(.26f,.26f,.37f),"scoop")
        val ribbon = models.getValue("scoopRibbon")
        pos(ribbon,0f,-.023f,.184f)
        scoop.addChild(ribbon)
        Log.i("WoodenPuppet","windRibbonLoaded mesh=${ribbonMesh!=null} clips=${ribbonClips.keys}")
        scoop.addChild(air)
        repeat(3){row->repeat(7){i->
            box(air,(row-1)*.016f,WindmillLayout.OUTLET_LOCAL_Y+sin(i*.8f+row)*.005f,
                .215f+i*.031f,.001f,.001f,.018f,airMat)
        }}
        val console = model("powerPanel",root,.08f,floor,-.46f)
        leverLight = indicator(console,"LensPower")
        branchLights = listOf(indicator(console,"LensFerry"),indicator(console,"LensWind"))
        val control = model("lever",console,0f,.1173f,0f)
        click(control,Vector3(.28f,.23f,.16f),"power")
        // Base bottom rests on the console's 18 mm top; keep the existing hit center.
        val controlBaseY = .018f - .1173f
        pos(models.getValue("lever"),0f,controlBaseY,0f)
        control.addChild(power)
        pos(power,0f,controlBaseY+.061f,0f)
        power.addChild(models.getValue("handle"))
        // Small inset brass transmission traces.
        for(branch in 0..1) repeat(10){i->
            val m=mat(.25f,.20f,.12f);routes[branch].add(m)
            val x=if(branch==0)-.10f-i*.06f else .24f+i*.04f
            box(root,x,floor+.003f,-.55f,.035f,.002f,.004f,m)
        }
        root.addChild(ferry)
        ferry.addChild(models.getValue("ferry"))
        click(ferry,Vector3(.40f,.08f,.44f),"ferry")
        ferryLight = indicator(model("ferryLights",ferry,0f,0f,0f),"Lens0","Lens1","Lens2","Lens3")
        dockLights = listOf(
            indicator(model("dockNear",root,-1.103f,floor,-.906f),"Lens"),
            indicator(model("dockIsland",root,-1.103f,floor+.025f,-1.624f),"Lens")
        )
        for(x in listOf(-1.06f,-.64f)) box(root,x,floor-.008f,-1.28f,.012f,.012f,.9f,brass)
        // Authored in floor-relative coordinates; deck follows WindLevel.elevation.
        // Open ends and the left-side clearance preserve both wind shields' movement.
        model("walkway",root,0f,floor,0f)
        Log.i("WoodenPuppet","windWalkwayLoaded oakPlanks=18 matchingLiftRails=true rise=0.16 clearWidth=0.386")
        for(i in 0..1) {
            val z=if(i==0)-1.17f else -1.64f
            val h=((- .94f-z)/.9f)*.16f
            box(root,.34f,floor+(h-.025f)/2f,z,.12f,h+.025f,.12f,dark)
            box(root,1.05f,floor+(h-.025f)/2f,z,.10f,h+.025f,.10f,dark)
            root.addChild(shields[i]);pos(shields[i],.34f,floor+h,z)
            val panel=model("shield$i",shields[i],0f,.14f,0f)
            panel.components[TransformComponent::class.java]?.setQuaternion(Quat(Vector3(0f,1f,0f),Math.PI.toFloat()/2f))
            click(panel,Vector3(.36f,.32f,.05f),"wind${i+1}")
            // The stationary hit area remains usable in either shield position.
            val key = model("gateKey$i",root,1.05f,floor+h,z)
            click(key,Vector3(.16f,.112f,.16f),"wind${i+1}")
            gateButtons += WorkshopButton(findNamed(key,"Cap"),indicator(key,"Lens"))
            root.addChild(streams[i]);pos(streams[i],.6f,floor+h+.11f,z)
            repeat(3){n->box(streams[i],0f,n*.035f,0f,.60f,.002f,.003f,teal)}
        }
        val frame=model("frame",root,.05f,floor+frameCenterY,-2.12f);click(frame,Vector3(.50f,.59f,.20f),"hang")
        root.addChild(chimes)
        for(i in 0..2) {
            chimes.addChild(notes[i]);pos(notes[i],(i-1)*.13f,0f,0f)
            val string=box(notes[i],0f,-.04f,0f,.002f,.08f,.002f,cream)
            val tube=model("tube$i",notes[i],0f,-.18f,0f)
            val scale=1f-i*.15f;tube.components[TransformComponent::class.java]?.setScaleVector(Vector3(1f,scale,1f))
            pos(tube,0f,-.08f-.12f*scale,0f)
            click(tube,Vector3(.085f,.28f,.085f),"note$i")
            val key = model("noteKey$i",root,.05f+(i-1)*.13f,floor+.1605f,-1.995f)
            click(key,Vector3(.11f,.095f,.095f),"note$i")
            noteButtons += WorkshopButton(findNamed(key,"Cap"),indicator(key,"Lens"))
        }
        // Keep the accepted pickup volume; the bottom-centered base rests on the island.
        val pickup=Entity();root.addChild(pickup);pos(pickup,-.85f,floor+.06f,-1.98f)
        click(pickup,Vector3(.46f,.40f,.22f),"chimes")
        val chimeBase=model("chimeBase",pickup,0f,.025f-.06f,0f)
        val baseBounds=chimeBase.getVisualBounds(root)
        Log.i("WoodenPuppet","windChimeBaseLoaded min=${baseBounds.min} max=${baseBounds.max} supportY=${floor+.025f} nativeScale=true pickupPreserved=true")
        model("doorframe",root,.83f,floor+.16f+.35f,-2.23f)
        root.addChild(door);pos(door,.83f-.36588475f/2f,floor+.16f+.287f,-2.204f)
        val leaf=model("doorleaf",door,.36588475f/2f,0f,-.026f);click(leaf,Vector3(.366f,.55f,.05f),"exit")

    }
    private fun findNamed(entity:Entity,name:String):Entity? {
        if(entity.getName()==name) return entity
        return entity.getChildren().firstNotNullOfOrNull { findNamed(it,name) }
    }

    private enum class LightState(val color:Color4) {
        OFF(Color4(.18f,.15f,.11f,1f)),
        READY(Color4(.88f,.55f,.16f,1f)),
        DONE(Color4(.29f,.59f,.35f,1f))
    }

    private class Indicator(private val lenses:List<PhysicallyBasedMaterial>) {
        private var state: LightState? = null
        fun show(next:LightState) {
            if(state==next) return
            state = next
            val color = next.color
            val glow = if(next==LightState.OFF) 0f else .18f
            for(lens in lenses) {
                lens.setBaseColor(color)
                lens.setEmissiveColor(Color4(color.red*glow,color.green*glow,color.blue*glow,1f))
            }
        }
    }

    private class WorkshopButton(private val cap:Entity?,private val light:Indicator) {
        private var pressedAt = Float.NEGATIVE_INFINITY
        private var lastY = 0f

        fun press(time:Float) { pressedAt = time }

        fun update(time:Float,state:LightState,reset:Boolean) {
            if(reset) pressedAt = Float.NEGATIVE_INFINITY
            light.show(state)
            val age = time-pressedAt
            val y = if(age in 0f..0.22f) -.0045f*sin(age/.22f*Math.PI.toFloat()) else 0f
            if(y==lastY) return
            lastY = y
            cap?.components?.get(TransformComponent::class.java)?.setPosition(Vector3(0f,y,0f))
        }
    }

    private fun indicator(parent:Entity,vararg names:String):Indicator {
        val lenses = names.mapNotNull { name ->
            val entity = findNamed(parent,name) ?: return@mapNotNull null
            val model = entity.components[ModelComponent::class.java] ?: return@mapNotNull null
            val material = PhysicallyBasedMaterial.create().apply {
                setMetallic(0f)
                setRoughness(.35f)
            }
            entity.components.set(ModelComponent(model.mesh,material))
            material
        }
        if(lenses.size!=names.size) Log.w("WoodenPuppet","windControlMissingLens expected=${names.size} found=${lenses.size}")
        return Indicator(lenses)
    }

    fun press(action:String,time:Float) {
        when(action) {
            "wind1" -> gateButtons[0].press(time)
            "wind2" -> gateButtons[1].press(time)
            "note0" -> noteButtons[0].press(time)
            "note1" -> noteButtons[1].press(time)
            "note2" -> noteButtons[2].press(time)
        }
    }

    private fun updateRibbon(wind:Boolean,reset:Boolean) {
        // Chapter assets are prepared before their root is enabled.
        if(!root.enabled) return
        val mesh = ribbonMesh ?: return
        val current = ribbonPlayback
        val firstPlayback = current?.valid != true
        if(!reset && ribbonWind==wind && current?.valid==true && !current.isComplete()) return
        val name = if(wind) "Wind" else "Idle"
        val resource = ribbonClips[name] ?: return
        val config = AnimationPlayConfig(
            transitionDuration = if(!reset && current?.valid==true && ribbonWind!=wind) .28f else 0f,
            transitionMode = AnimationTransitionMode.STOP_AND_CROSSFADE
        )
        ribbonPlayback = resource.repeat(15).use { mesh.playAnimation(it,config) }
        current?.close()
        if(firstPlayback || ribbonWind!=wind || reset) {
            Log.i("WoodenPuppet","windRibbonPlayback clip=$name valid=${ribbonPlayback?.valid}")
        }
        ribbonWind = wind
    }

    fun release() {
        ribbonPlayback?.close()
        ribbonPlayback = null
    }

    fun update(l:WindLevel,time:Float) {
        if(root.enabled) riverMotion.update(time)
        val restarted = l.time < previousTime
        val dt=(l.time-previousTime).coerceIn(0f,.1f)
        if(restarted) {shieldAngles.fill(1.35f);scoopAngle=WindmillLayout.scoopYaw(0);rotorAngle=0f;powerAngle=0f}
        previousTime=l.time
        val goal=WindmillLayout.scoopYaw(l.scoopQuarter)
        scoopAngle+=(goal-scoopAngle).coerceIn(-dt*4f,dt*4f)
        val idleSway=if(!l.windReady && abs(goal-scoopAngle)<.01f)sin(time*1.6f)*.012f else 0f
        scoop.components[TransformComponent::class.java]?.setQuaternion(Quat(Vector3(0f,1f,0f),scoopAngle+idleSway))
        airflowOn=l.windReady && abs(goal-scoopAngle)<.025f && !l.completed
        if(airflowOn)rotorAngle+=dt*.65f
        rotor.components[TransformComponent::class.java]?.setQuaternion(Quat(Vector3(0f,0f,1f),rotorAngle))
        air.enabled=airflowOn
        air.components[TransformComponent::class.java]?.setPosition(Vector3(0f,sin(time*2f)*.003f,0f))
        updateRibbon(airflowOn, restarted)
        val breathe=.92f+.08f*sin(time*2f)
        val lensColor = when {
            airflowOn -> Color4(.25f,.46f,.34f,1f)
            !l.windReady -> Color4(.53f*breathe,.34f*breathe,.13f,1f)
            else -> Color4(.20f,.16f,.10f,1f)
        }
        scoopLens.setBaseColor(lensColor)
        scoopLens.setEmissiveColor(Color4(lensColor.red*.10f,lensColor.green*.10f,lensColor.blue*.10f,1f))
        val powerTarget = when(l.power) { 1 -> 40f; 2 -> -40f; else -> 0f } * Math.PI.toFloat()/180f
        powerAngle += (powerTarget-powerAngle).coerceIn(-dt*2.6f,dt*2.6f)
        power.components[TransformComponent::class.java]?.setQuaternion(Quat(Vector3(0f,0f,1f),powerAngle))
        pos(ferry,-.85f,floor+.025f,l.ferryZ)
        for(i in 0..1) {
            val closed=l.safe(i+1)
            val target=if(closed)0f else 1.35f
            shieldAngles[i]+=(target-shieldAngles[i]).coerceIn(-dt*4.5f,dt*4.5f)
            shields[i].components[TransformComponent::class.java]?.setQuaternion(Quat(Vector3(0f,0f,1f),shieldAngles[i]))
            streams[i].enabled=l.power==2 && !closed && !l.completed
        }
        val action=l.suggestedAction
        leverLight.show(if(action=="power") LightState.READY else LightState.OFF)
        branchLights.forEachIndexed { i,light ->
            light.show(if(l.power==i+1) LightState.READY else LightState.OFF)
        }
        val returned = l.hasChimes && !l.island && !l.riding
        ferryLight.show(when {
            returned -> LightState.DONE
            l.power==1 && (action=="ferry" || l.riding) -> LightState.READY
            else -> LightState.OFF
        })
        val currentDock = if(l.island) 1 else 0
        dockLights.forEachIndexed { i,light ->
            light.show(when {
                l.power!=1 -> LightState.OFF
                !l.riding && i==currentDock -> LightState.DONE
                l.riding && i!=currentDock -> LightState.READY
                else -> LightState.OFF
            })
        }
        gateButtons.forEachIndexed { i,key ->
            val gate = i+1
            key.update(l.time,when {
                l.crossed>=gate -> LightState.DONE
                action=="wind$gate" || l.waiting==gate || l.crossing==gate -> LightState.READY
                else -> LightState.OFF
            },restarted)
        }
        routes.forEachIndexed {branch,list->list.forEachIndexed {i,m->
            val active=l.power==branch+1 && !l.completed
            val pulse=if(active).62f+.20f*sin(time*3f-i*.7f)else .30f
            m.setBaseColor(Color4(pulse,pulse*.73f,pulse*.37f,1f))
        }}
        val p=if(l.hung)WalkPoint(.05f,-2.12f)else if(l.hasChimes)l.position else WalkPoint(-.85f,-1.98f)
        pos(chimes,p.x,floor+(if(l.hung)frameCenterY else if(l.hasChimes)l.elevation+.64f else .40f),p.z)
        notes.forEachIndexed { i,e ->
            val seat=hookSeats[i]
            if(l.hung)pos(e,seat.x,seat.y,seat.z)
            else pos(e,(i-1)*.13f,0f,0f)
        }
        if(l.hung && !chimesWereHung) {
            hookSeats.forEachIndexed { i,seat ->
                Log.i("WoodenPuppet","windChimeHook index=$i anchor=${Vector3(p.x+seat.x,floor+frameCenterY+seat.y,p.z+seat.z)} stringLength=.08 swayPivotAtHook=true")
            }
        }
        chimesWereHung=l.hung
        chimes.components[TransformComponent::class.java]?.setScaleVector(Vector3(if(l.hasChimes&&!l.hung).60f else 1f))
        notes.forEachIndexed{i,e->e.components[TransformComponent::class.java]?.setQuaternion(Quat(Vector3(0f,0f,1f),if(l.hung && !l.tuned && WindLevel.ORDER[l.notes]==i)sin(time*2.4f)*.085f else if(l.hung || action=="chimes")sin(time*1.6f+i)*.018f else 0f))}
        noteButtons.forEachIndexed { i,key ->
            key.update(l.time,when {
                l.tuned || WindLevel.ORDER.take(l.notes).contains(i) -> LightState.DONE
                action=="note$i" -> LightState.READY
                else -> LightState.OFF
            },restarted)
        }
        door.components[TransformComponent::class.java]?.setQuaternion(Quat(Vector3(0f,1f,0f),if(l.completed)-1.5f else 0f))
    }
}
