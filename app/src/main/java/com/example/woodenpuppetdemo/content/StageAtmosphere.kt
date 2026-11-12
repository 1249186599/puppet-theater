package com.example.woodenpuppetdemo.content

import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.resource.*
import com.pico.spatial.core.math.*
import kotlin.math.sin

/** Decorative only: never adds colliders or changes walking/support coordinates. */
internal class StageAtmosphere(private val root: Entity, private val floor: Float, private val lamps:TheatreLamps) {
    private fun material(r: Float,g: Float,b: Float) = UnlitMaterial.create().apply {
        setBaseColor(Color4(r,g,b,1f))
    }
    private fun box(parent: Entity,x: Float,y: Float,z: Float,w: Float,h: Float,d: Float,m: UnlitMaterial) =
        ModelEntity(mesh=MeshResource.createBox(Vector3(w,h,d)),material=m).apply {
            components[TransformComponent::class.java]?.setPosition(Vector3(x,y,z))
            parent.addChild(this)
        }
    private inner class Signal(x: Float,z: Float,halfX: Float,halfZ: Float) {
        val light = lamps.lens()
        val edge = material(.35f,.28f,.10f)
        val halo = Entity()
        init {
            root.addChild(halo)
            halo.components[TransformComponent::class.java]?.setPosition(Vector3(x,floor+.006f,z))
            for (side in listOf(-1f,1f)) {
                box(halo,side*halfX,0f,0f,.0025f,.0015f,halfZ*2f,edge)
                box(halo,0f,0f,side*halfZ,halfX*2f,.0015f,.0025f,edge)
            }
            lamps.round(root,Vector3(x,floor,z+halfZ+.023f),light)
        }
        fun update(available: Boolean,done: Boolean,time: Float,flash: Boolean=false) {
            val pulse=.78f+.22f*sin(time*2.4f)
            val color=when {
                done -> Color4(.24f,.65f,.36f,1f)
                flash -> Color4(.95f,.34f,.11f,1f)
                available -> Color4(.85f*pulse,.64f*pulse,.27f*pulse,1f)
                else -> Color4(.20f,.16f,.10f,1f)
            }
            lamps.color(light,color)
            edge.setBaseColor(color)
            halo.enabled=available || flash
        }
    }
    // Keep the plate's crafted round indicator, independently of the removed halo.
    private val padLight = lamps.lens()
    init {
        lamps.round(root,Vector3(AdventureLevel.PAD.x,floor,AdventureLevel.PAD.z+.169f),padLight)
    }
    private val lever = Signal(.60f,-.78f,.148f,.092f)
    private var lockedUntil = -1f
    fun flashLocked(time: Float) { lockedUntil=time+.65f }
    fun reset() { lockedUntil=-1f }
    fun update(level: AdventureLevel,time: Float) {
        lamps.color(padLight,if(level.unlocked) Color4(.24f,.65f,.36f,1f)
            else Color4(.85f,.64f,.27f,1f))
        lever.update(level.unlocked && !level.bridgeReady,level.bridgeReady,time,time<lockedUntil)
    }
}
