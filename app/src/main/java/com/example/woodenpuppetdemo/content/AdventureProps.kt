package com.example.woodenpuppetdemo.content

import android.util.Log
import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.resource.*
import com.pico.spatial.core.math.*
import kotlin.math.sin
import kotlin.math.abs

/** Code-owned toy scenery, sharing the level's metric positions. */
internal class AdventureProps(private val root: Entity, private val floor: Float, keyModel: Entity, bridgeModel: Entity, leverModel: Entity, plateModel: Entity, doorFrameModel: Entity, doorPanelModel: Entity, groundModel: Entity, leverHandleModel: Entity, lamps:TheatreLamps) {
    private fun material(r: Float, g: Float, b: Float) = UnlitMaterial.create().apply {
        setBaseColor(Color4(r, g, b, 1f))
    }
    private val wood = material(0.52f, 0.30f, 0.15f)
    private val paleWood = material(0.76f, 0.57f, 0.34f)
    private val darkWood = material(0.28f, 0.16f, 0.10f)
    private val gold = material(0.78f, 0.56f, 0.22f)
    private val doorMat = material(0.34f, 0.19f, 0.10f)
    private val keyMat = material(1f, 0.72f, 0.13f)
    private val lamps=lamps
    private val bridgeLampMat = lamps.lens()
    private val stageLampMat = lamps.lens()
    private lateinit var pad: Entity
    private val atmosphere = StageAtmosphere(root, floor, lamps)
    private var padPress = 0f
    private var lastTime: Float? = null
    private var pickupTime: Float? = null
    private var hadKey = false
    val actions = mutableMapOf<Entity, String>()
    private val leverHandle = Entity()
    private val bridge = Entity()
    private val key = Entity()
    private val door = Entity()
    private var won = false

    private fun box(parent: Entity, x: Float, y: Float, z: Float,
        w: Float, h: Float, d: Float, mat: UnlitMaterial): ModelEntity =
        ModelEntity(mesh = MeshResource.createBox(Vector3(w,h,d), minOf(w,h,d,0.04f)*0.12f), material = mat).apply {
            components[TransformComponent::class.java]?.setPosition(Vector3(x,y,z))
            parent.addChild(this)
        }
    private fun clickable(entity: Entity, size: Vector3, action: String) {
        entity.components.set(CollisionComponent(collisionShape = listOf(ShapeResource.createBox(size)),
            physicsMaterial = PhysicsMaterialResource()))
        entity.components.set(InteractableComponent())
        actions[entity] = action
    }
    init {
        // Approved native-size dry base. Its measured bank tops are at local Y=.048.
        // Keep the existing gameplay floor while fitting the independent river into its recess.
        root.addChild(groundModel)
        val nativeGround = groundModel.getVisualBounds(root, recursive = true, enabledOnly = false)
        check(!nativeGround.isEmpty() && abs(nativeGround.min.y) < .002f &&
            abs(nativeGround.max.y - .07383f) < .002f &&
            abs(nativeGround.min.x + .851558f) < .002f &&
            abs(nativeGround.max.x - .851558f) < .002f &&
            abs(nativeGround.min.z + .851558f) < .002f &&
            abs(nativeGround.max.z - .851558f) < .002f) {
            "Unexpected chapter1 ground bounds $nativeGround"
        }
        groundModel.components[TransformComponent::class.java]?.setPosition(
            Vector3(0f,floor-.048f,AdventureLevel.CENTER_Z))
        val groundBounds = groundModel.getVisualBounds(root, recursive = true, enabledOnly = false)
        check(abs(groundBounds.min.y - (floor-.048f)) < .002f &&
            abs(groundBounds.center.z - AdventureLevel.CENTER_Z) < .002f)
        Log.i("WoodenPuppet", "chapter1DryGroundReady nativeScale=1 bankY=$floor channelY=${floor-.034f} rimY=${floor+.022f} min=${groundBounds.min} max=${groundBounds.max}")
        // Textured plate retains its physical press animation without colored hints.
        pad = Entity().apply {
            components[TransformComponent::class.java]?.setPosition(
                Vector3(AdventureLevel.PAD.x,floor+0.018f,AdventureLevel.PAD.z))
            addChild(plateModel)
            root.addChild(this)
        }
        clickable(pad,Vector3(0.27f,0.032f,0.266f),"pad")
        // Wooden support only; colored lever status slab removed.
        box(root,0.60f,floor+0.004f,-0.78f,0.27f,0.008f,0.15f,darkWood)
        val leverCenterY = .010f + .1986387f / 2f
        val leverBaseY = .008f - leverCenterY
        val lever = Entity().apply {
            components[TransformComponent::class.java]?.setPosition(
                Vector3(0.60f, floor + leverCenterY, -0.78f))
            leverModel.components[TransformComponent::class.java]?.setPosition(Vector3(0f,leverBaseY,0f))
            addChild(leverModel)
            root.addChild(this)
        }
        clickable(lever,Vector3(0.26f,0.22f,0.14f),"lever")
        // The approved model's axle is 61 mm above its base bottom.
        leverHandle.components[TransformComponent::class.java]?.setPosition(Vector3(0f,leverBaseY+.061f,0f))
        leverHandle.components[TransformComponent::class.java]?.setQuaternion(
            Quat(Vector3(0f,0f,1f),22f*Math.PI.toFloat()/180f))
        leverHandle.addChild(leverHandleModel)
        lever.addChild(leverHandle)
        root.addChild(bridge)
        bridge.components[TransformComponent::class.java]?.setPosition(Vector3(0f,floor + BridgeSurface.LIFT,-1.2f))
        // Imported bridge is deck-aligned, centered, and fitted offline.
        bridge.addChild(bridgeModel)
        // Status lamps at both bridge landings, outside the walking corridor.
        for(z in listOf(-0.96f,-1.44f)) for(x in listOf(-0.22f,0.22f)) {
            lamps.square(root,Vector3(x,floor,z),bridgeLampMat)
        }
        // Warm stage bulbs remain subdued until the exit opens.
        for(x in listOf(-0.70f,-0.42f,-0.14f,0.14f,0.42f,0.70f))
            for(z in listOf(AdventureLevel.CENTER_Z+.812528f,AdventureLevel.CENTER_Z-.812528f)) {
                lamps.edge(root,Vector3(x,floor+.022f,z),stageLampMat)
            }
        root.addChild(key)
        val keyDisplay = Entity()
        keyDisplay.components[TransformComponent::class.java]?.apply {
            setScaleVector(Vector3(2f / 3f))
            setQuaternion(Quat(Vector3(0f, 0f, 1f), Math.PI.toFloat()))
        }
        keyDisplay.addChild(keyModel)
        key.addChild(keyDisplay)
        clickable(key,Vector3(0.12f,0.20f,0.16f),"key")
        val doorFrame = Entity().apply {
            components[TransformComponent::class.java]?.setPosition(Vector3(0.42f,floor+0.35f,-1.86f))
            addChild(doorFrameModel)
            root.addChild(this)
        }
        root.addChild(door)
        // The pivot stays on the left edge; the leaf center is offset from it.
        val leafWidth = 0.36588475f
        door.components[TransformComponent::class.java]?.setPosition(
            Vector3(0.42f-leafWidth/2f,floor+0.012f+0.275f,-1.834f))
        val leaf = Entity().apply {
            components[TransformComponent::class.java]?.setPosition(Vector3(leafWidth/2f,0f,-0.026f))
            addChild(doorPanelModel)
            door.addChild(this)
        }
        val exitHit = box(root,AdventureLevel.EXIT.x,floor+0.002f,AdventureLevel.EXIT.z,
            0.14f,0.004f,0.14f,doorMat)
        clickable(exitHit,Vector3(0.18f,0.02f,0.18f),"exit")
        clickable(leaf,Vector3(leafWidth,0.55f,0.05f),"exit")

    }
    fun flashLocked(time: Float) = atmosphere.flashLocked(time)
    fun resetFeedback() = atmosphere.reset()

    fun plateHeight(x: Float, z: Float) = PlateSurface.height(x,z,padPress)

    fun update(level: AdventureLevel, time: Float) {
        atmosphere.update(level, time)
        // Follow the bridge from the approved +22-degree pose to -40 degrees.
        val progress=level.bridgeProgress.coerceIn(0f,1f)
        val eased=progress*progress*(3f-2f*progress)
        leverHandle.components[TransformComponent::class.java]?.setQuaternion(
            Quat(Vector3(0f,0f,1f),(22f-62f*eased)*Math.PI.toFloat()/180f))
        val dt = lastTime?.let { (time-it).coerceIn(0f,0.10f) } ?: 0f
        lastTime = time
        if(!level.unlocked) padPress = 0f
        else padPress = (padPress + dt/0.25f).coerceAtMost(1f)
        pad.components[TransformComponent::class.java]?.setPosition(
            Vector3(AdventureLevel.PAD.x,floor+0.018f-padPress*0.006f,AdventureLevel.PAD.z))
        lamps.color(bridgeLampMat,if(level.bridgeReady) Color4(.29f,.60f,.36f,1f)
            else Color4(.18f,.20f,.16f,1f))
        lamps.color(stageLampMat,if(level.completed) Color4(.82f,.56f,.23f,1f)
            else Color4(.23f,.18f,.10f,1f))
        if(level.hasKey && !hadKey) pickupTime=time
        if(!level.hasKey) pickupTime=null
        hadKey=level.hasKey
        val age=pickupTime?.let { time-it } ?: 2f
        val flash=if(age in 0f..0.9f) sin(age/0.9f*Math.PI.toFloat()).coerceAtLeast(0f) else 0f
        keyMat.setBaseColor(Color4(1f,0.72f+0.28f*flash,0.13f+0.87f*flash,1f))
        key.components[TransformComponent::class.java]?.setScaleVector(Vector3(1f+0.16f*flash))
        bridge.components[TransformComponent::class.java]?.setQuaternion(
            Quat(Vector3(0f,1f,0f),(1f-level.bridgeProgress)*Math.PI.toFloat()/2f))
        key.enabled = !level.completed
        key.components[TransformComponent::class.java]?.apply {
            val p = if(level.hasKey) level.position else AdventureLevel.KEY
            setPosition(Vector3(p.x, floor + (if(level.hasKey) 0.65f else 0.23f) + sin(time*2f)*0.012f,p.z))
            setQuaternion(Quat(Vector3(0f,1f,0f),time*0.6f))
        }
        if(won != level.completed) {
            won = level.completed
            doorMat.setBaseColor(if(won) Color4(0.2f,0.75f,0.36f,1f) else Color4(0.34f,0.19f,0.10f,1f))
        }
        door.components[TransformComponent::class.java]?.setQuaternion(
            Quat(Vector3(0f,1f,0f),if(won) -1.5f else 0f))
    }
}
