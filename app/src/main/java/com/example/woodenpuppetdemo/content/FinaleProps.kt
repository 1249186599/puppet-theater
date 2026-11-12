package com.example.woodenpuppetdemo.content

import android.util.Log
import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.resource.*
import com.pico.spatial.core.math.*
import kotlin.math.*

/** Fitted approved props with separate collision targets and gameplay signals. */
internal class FinaleProps(
    private val root: Entity,
    private val floor: Float,
    stageModel: Entity,
    private val art: Entity
) {
    val actions = mutableMapOf<Entity, String>()
    private val props = (1..22).associateWith { part(art, "Prop" + it.toString().padStart(2, '0')) }
    private val meshes = mutableMapOf<String, MeshResource>()
    private val brass = material(.46f, .29f, .12f)
    private val signalMaterials = mutableMapOf<String, PhysicallyBasedMaterial>()
    private val signalStates = mutableMapOf<String, Int>()
    private val plateCaps = (9..11).map { part(prop(it), "PressSurface") }
    private val panels = (12..14).map { part(prop(it), "SceneryRotate") }
    private val powerHandle = part(prop(8), "LeverPivot")
    private val powerBeacon = PhysicallyBasedMaterial.create().apply {
        setMetallic(0f)
        setRoughness(.24f)
        setBaseColor(Color4(.11f, .045f, .008f, 1f))
        setEmissiveColor(Color4(0f, 0f, 0f, 1f))
    }
    private var beaconPowered: Boolean? = null
    private val lampPan = part(prop(6), "LampPan")
    private val latches = listOf(prop(17), part(art, "LatchRight"))
    private val lockBars = latches.map { part(it, "SlidingBolt") }
    private val winch = part(prop(18), "WinchDrum")
    private val star = prop(15)
    private val closed = prop(22)
    private val leaves = listOf(part(closed, "LeftLeaf"), part(closed, "RightLeaf"))
    private val open = listOf(prop(1), prop(2))
    private val rings = List(2) { part(prop(20), "PullRing$it") }
    private val strings = Entity()
    private val tethers = List(2) { Entity() }
    private val brightnessBars = mutableListOf<Entity>()
    private val spot = Entity()
    // SDK spotlight uses degrees, lumens, and local -Z as its emission axis.
    // A small bright core and wide penumbra illuminate the actual PBR wood.
    private val spotLight = SpotLightComponent(Color4(1f, .86f, .64f, 1f), 0f, 2f, 8f, 36f, false)
    private var spotState: Pair<Int, Int>? = null
    private val confetti = FinaleConfetti(root, floor)

    private fun part(parent: Entity, name: String) = checkNotNull(parent.findEntity(name)) {
        "Missing approved chapter-five prop node $name"
    }
    private fun prop(id: Int) = props.getValue(id)
    private fun material(r: Float, g: Float, b: Float) = PhysicallyBasedMaterial.create().apply {
        setBaseColor(Color4(r, g, b, 1f))
        setRoughness(.55f)
        setMetallic(0f)
    }
    private fun transform(e: Entity) = checkNotNull(e.components[TransformComponent::class.java])
    private fun pos(e: Entity, x: Float, y: Float, z: Float) = transform(e).setPosition(Vector3(x, y, z))
    private fun rot(e: Entity, axis: Vector3, angle: Float) = transform(e).setQuaternion(Quat(axis, angle))
    private fun place(e: Entity, x: Float, y: Float, z: Float, scale: Float = 1f) {
        pos(e, x, floor + y, z)
        transform(e).setScaleVector(Vector3(scale))
    }
    private fun group(parent: Entity, x: Float, y: Float, z: Float) = Entity().apply {
        parent.addChild(this)
        pos(this, x, y, z)
    }
    private fun interact(e: Entity, size: Vector3, action: String) {
        e.components.set(CollisionComponent(collisionShape = listOf(ShapeResource.createBox(size)),
            physicsMaterial = PhysicsMaterialResource()))
        e.components.set(InteractableComponent())
        actions[e] = action
    }
    private fun hit(action: String, x: Float, y: Float, z: Float, size: Vector3) {
        interact(group(root, x, floor + y, z), size, action)
    }
    private fun hitModel(e: Entity, action: String, padding: Float = .02f) {
        val b = e.getVisualBounds(e, recursive = true, enabledOnly = false)
        check(!b.isEmpty()) { "No visual bounds for $action" }
        val center = b.center
        interact(group(e, center.x, center.y, center.z), Vector3(
            b.max.x - b.min.x + padding, b.max.y - b.min.y + padding, b.max.z - b.min.z + padding
        ), action)
    }
    private fun signalMaterial(action: String) = signalMaterials.getOrPut(action) { material(.24f, .27f, .29f) }
    private fun signal(action: String, x: Float, y: Float, z: Float, r: Float = .013f): Entity {
        val mesh = meshes.getOrPut("signal$r") { MeshResource.createCylinder(r, .007f) }
        return ModelEntity(mesh = mesh, material = signalMaterial(action)).apply {
            root.addChild(this)
            place(this, x, y, z)
        }
    }
    private fun outline(parent: Entity, kind: Int, radius: Float, width: Float, surface: PhysicallyBasedMaterial) {
        val count = when (kind) { 0 -> 10; 1 -> 3; else -> 24 }
        val points = List(count) { i ->
            val angle = -PI.toFloat() / 2 + i * 2 * PI.toFloat() / count
            val r = if (kind == 0 && i % 2 == 1) radius * .44f else radius
            Vector3(cos(angle) * r, 0f, sin(angle) * r)
        }
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % count]
            val dx = b.x - a.x
            val dz = b.z - a.z
            val length = hypot(dx, dz)
            val mesh = meshes.getOrPut("outline:$length:$width") {
                MeshResource.createBox(Vector3(width, length, width))
            }
            val line = ModelEntity(mesh = mesh, material = surface)
            parent.addChild(line)
            pos(line, (a.x + b.x) / 2, 0f, (a.z + b.z) / 2)
            rot(line, Vector3(dz / length, 0f, -dx / length), PI.toFloat() / 2)
        }
    }

    init {
        root.addChild(stageModel)
        val native = stageModel.getVisualBounds(root, recursive = true, enabledOnly = false)
        check(!native.isEmpty() && abs(native.min.y) < .002f && abs(native.max.y - 1.125f) < .002f &&
            abs(native.min.x + 1.5f) < .002f && abs(native.max.x - 1.5f) < .002f &&
            abs(native.min.z + .9f) < .002f && abs(native.max.z - .9025f) < .002f) { "Unexpected stage bounds $native" }
        pos(stageModel, 0f, floor - FinaleLevel.MODEL_FLOOR, FinaleLevel.MODEL_CENTER_Z)
        root.addChild(art)
        hit("floor", 0f, -.05f, FinaleLevel.MODEL_CENTER_Z, Vector3(3f, .10f, 1.80f))

        // The imported amber material was permanently emissive. Give only this dome
        // its own stateful material, already dark before its first visible frame.
        val beacon = part(prop(7), "Body_amber")
        val beaconModel = checkNotNull(beacon.components[ModelComponent::class.java])
        beacon.components.set(ModelComponent(beaconModel.mesh, powerBeacon))
        updatePowerBeacon(false)
        place(prop(7), -1.12f, 0f, -.68f)
        place(prop(8), -.89f, 0f, -.65f)
        hitModel(prop(7), "power")
        hitModel(prop(8), "power")
        signal("power", -.89f, .038f, -.59f)
        for (i in 0..2) {
            val hint = group(root, -1.19f + i * .07f, floor + .252f, -.541f)
            rot(hint, Vector3(1f, 0f, 0f), PI.toFloat() / 2)
            outline(hint, i, .013f, .003f, brass)
            val point = FinaleLevel.PLATES[i]
            place(prop(9 + i), point.x, 0f, point.z)
            hitModel(prop(9 + i), "plate$i", .028f)
            signal("plate$i", point.x, .008f, point.z - .128f)
        }
        for (i in 0..2) {
            val z = -1.20f - i * .37f
            val scenery = prop(12 + i)
            place(scenery, -1.17f, 0f, z)
            hitModel(panels[i], "scenery$i")
            hit("scenery$i", -1.17f, .035f, z, Vector3(.26f, .075f, .28f))
            signal("scenery$i", -1.07f, .035f, z + .075f)
        }
        place(prop(16), -.73f, 0f, -1.14f, .82f)
        val dock = part(art, "Dock")
        place(dock, -.55f, .145f, -1.49f, .48f)
        hitModel(dock, "dock", .065f)
        signal("dock", -.55f, .148f, -1.405f)
        place(star, -.73f, .151f, -1.14f, .82f)
        hitModel(star, "prop", .038f)

        place(prop(6), 1.04f, 0f, -1.29f)
        hitModel(part(prop(6), "Button0"), "aimLeft", .026f)
        hitModel(part(prop(6), "Button1"), "aimRight", .026f)
        hitModel(part(lampPan, "LampPanTilt"), "brightness", .036f)
        signal("aimLeft", .958f, .123f, -1.163f, .009f)
        signal("aimRight", 1.122f, .123f, -1.163f, .009f)
        signal("brightness", 1.022f, .175f, -1.302f, .010f)
        brightnessBars += signal("brightnessLevel", 1.116f, .191f, -1.327f, .008f)
        brightnessBars += signal("brightnessLevel", 1.142f, .191f, -1.327f, .008f)
        root.addChild(spot)
        spot.components.set(spotLight)
        rot(spot, Vector3(1f, 0f, 0f), -PI.toFloat() / 2f)
        spot.enabled = false
        hit("perform", 0f, .146f, FinaleLevel.PERFORM.z, Vector3(.40f, .06f, .40f))
        hit("perform", 0f, .147f, -1.78f, Vector3(1.80f, .035f, .66f))

        place(prop(4), -.66f, .145f, -1.79f, .64f)
        place(part(art, "SpotRight"), .66f, .145f, -1.79f, .64f)
        place(prop(5), 0f, .141f, -1.39f, .75f)
        place(part(art, "FootLeft"), -.56f, .141f, -1.48f, .75f)
        place(part(art, "FootRight"), .56f, .141f, -1.48f, .75f)
        // Seat the valance behind the carved header and inside its outer silhouette.
        place(prop(3), 0f, .706f, -2.018f, .84f)
        place(closed, 0f, .146f, -2.033f, .92f)
        open.forEachIndexed { i, e -> place(e, if (i == 0) -.555f else .555f, .146f, -1.995f, .91f) }
        latches.forEachIndexed { i, e ->
            val x = if (i == 0) -.825f else .825f
            place(e, x, .475f, -1.952f, .75f)
            hitModel(e, "lock$i", .04f)
            signal("lock$i", x, .635f, -1.927f, .011f)
        }
        place(prop(18), 1.23f, 0f, -1.92f, .90f)
        hitModel(prop(18), "winch", .025f)
        signal("winch", 1.23f, .059f, -1.813f)

        place(prop(19), .60f, 0f, -.77f)
        hitModel(prop(19), "practice", .036f)
        signal("practice", .60f, .008f, -.884f)
        place(prop(20), .99f, 0f, -.69f, 1.10f)
        rings.forEachIndexed { i, ring -> hitModel(ring, if (i == 0) "pullLeft" else "pullRight", .024f) }
        place(prop(21), 1.29f, .0025f, -.70f, .82f)
        root.addChild(strings)
        val bar = ModelEntity(mesh = MeshResource.createBox(Vector3(.38f, .018f, .035f)), material = brass)
        strings.addChild(bar)
        pos(bar, 0f, .61f, 0f)
        val tetherMesh = MeshResource.createBox(Vector3(.0025f, 1f, .0025f))
        tethers.forEach { tether ->
            tether.addChild(ModelEntity(mesh = tetherMesh, material = brass))
            strings.addChild(tether)
        }
        for (i in 0..5) signal("progress$i", -.25f + i * .10f, .006f, -.469f)
        update(FinaleLevel(), 0f)
        val bounds = art.getVisualBounds(root, recursive = true, enabledOnly = false)
        check(!bounds.isEmpty() && bounds.min.x >= -1.50f && bounds.max.x <= 1.50f &&
            bounds.min.z >= -2.25f && bounds.max.z <= -.447f && bounds.min.y >= floor - .005f) {
            "Approved props exceed their stage support bounds $bounds"
        }
        Log.i("WoodenPuppet", "finaleApprovedPropsReady designs=${props.size} actions=${actions.values.toSet().sorted()} min=${bounds.min} max=${bounds.max}")
    }

    private fun show(action: String, state: Int) {
        if (signalStates[action] == state) return
        signalStates[action] = state
        val color = when (state) {
            1 -> Color4(.80f, .54f, .17f, 1f)
            2 -> Color4(.22f, .57f, .35f, 1f)
            else -> Color4(.24f, .27f, .29f, 1f)
        }
        signalMaterials[action]?.apply {
            setBaseColor(color)
            setEmissiveColor(Color4(color.red * .12f, color.green * .12f, color.blue * .12f, 1f))
        }
    }

    private fun updatePowerBeacon(powered: Boolean) {
        if (beaconPowered == powered) return
        beaconPowered = powered
        powerBeacon.setBaseColor(if (powered) Color4(1f, .46f, .025f, 1f)
            else Color4(.11f, .045f, .008f, 1f))
        powerBeacon.setEmissiveColor(if (powered) Color4(.4f, .12f, .006f, 1f)
            else Color4(0f, 0f, 0f, 1f))
        Log.i("WoodenPuppet", "finalePowerBeacon powered=$powered emissive=$powered")
    }

    fun update(l: FinaleLevel, time: Float) {
        confetti.update(l)
        val suggested = l.suggestedActions
        for (action in signalMaterials.keys) show(action, if (action in suggested) 1 else 0)
        for (i in 0..2) {
            // Cap top is .020m at rest; the fixed base top is .016m.
            // Keep 2mm of clearance when pressed instead of making the two faces coplanar.
            pos(plateCaps[i], 0f, .012f - if (i < l.plates) .002f else 0f, 0f)
            if (i < l.plates) show("plate$i", 2)
            rot(panels[i], Vector3(0f, 1f, 0f), l.scenery[i] * PI.toFloat() / 2)
            if (l.scenery[i] == 0) show("scenery$i", 2)
        }
        updatePowerBeacon(l.powered)
        rot(powerHandle, Vector3(0f, 0f, 1f), if (l.powered) -.65f else 0f)
        if (l.powered) show("power", 2)
        if (l.delivered) {
            place(star, -.55f, .233f, -1.49f, .82f)
            show("dock", 2)
        } else if (l.carrying) place(star, l.position.x + .10f, l.elevation + .12f, l.position.z + .04f, .82f)
        else place(star, -.73f, .151f, -1.14f, .82f)
        rot(lampPan, Vector3(0f, 1f, 0f), -1.90f + (l.lampAim - 2) * .18f)
        // Deck top is floor + .145; emitter is .65m above it. The wider
        // cone leaves a visible soft halo around the star, fading to zero.
        pos(spot, (l.lampAim - 2) * .25f, floor + .145f + .65f, FinaleLevel.PERFORM.z)
        val lightLevel = if (l.powered) l.brightness else 0
        val nextSpotState = l.lampAim to lightLevel
        if (spotState != nextSpotState) {
            spotLight.intensity = when (lightLevel) { 1 -> 650f; 2 -> 1600f; else -> 0f }
            spot.components.set(spotLight)
            spot.enabled = lightLevel > 0
            spotState = nextSpotState
            Log.i("WoodenPuppet", "finaleSpot aim=${l.lampAim} level=$lightLevel lumens=${spotLight.intensity}")
        }
        show("brightnessLevel", 1)
        brightnessBars.forEachIndexed { i, e -> e.enabled = i < l.brightness }
        val opened = l.curtain >= 1f
        val squeeze = 1f - l.curtain * .70f
        closed.enabled = !opened
        for (i in 0..1) {
            val side = if (i == 0) -1f else 1f
            open[i].enabled = opened
            pos(leaves[i], side * .80f * (1f - squeeze), .8f, 0f)
            transform(leaves[i]).setScaleVector(Vector3(squeeze, 1f, 1f))
            if (l.locks[i]) show("lock$i", 2)
            pos(lockBars[i], if (l.locks[i]) side * .048f else 0f, .104f, .024f)
            val pull = if (i == 0) l.leftPull else l.rightPull
            rings[i].enabled = l.stage <= 5
            pos(rings[i], side * .04f, .26f + pull * .045f, .033f)
            val length = .46f - pull * .12f
            pos(tethers[i], side * .12f, .61f - length / 2, 0f)
            transform(tethers[i]).setScaleVector(Vector3(1f, length, 1f))
        }
        rot(winch, Vector3(1f, 0f, 0f), if (l.opening) time * 3f else l.curtain * 5.4f)
        if (opened) show("winch", 2)
        strings.enabled = l.stringsActive || l.performing || l.completed
        pos(strings, l.position.x, floor + l.elevation, l.position.z)
        if (l.practice == 3) show("practice", 2)
        for (i in 0..5) show("progress$i", if (l.completed || i < l.stage - 1) 2 else if (i == l.stage - 1) 1 else 0)
    }
}
