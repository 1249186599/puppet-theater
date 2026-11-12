package com.example.woodenpuppetdemo.content

import android.util.Log
import com.pico.spatial.core.ecs.*
import com.pico.spatial.core.ecs.resource.*
import com.pico.spatial.core.math.*

/** Approved meshes; decorative only, no collision or input targets. */
internal class TheatreLamps(
    private val models: Map<String,List<Entity>>,
    private val textures: Map<String,TextureResource>
) {
    data class Binding(val kind:String,val material:PhysicallyBasedMaterial,var approved:Boolean=true)
    class Light {
        val bindings=mutableListOf<Binding>()
    }
    private val used=mutableMapOf<String,Int>()
    fun lens()=Light()
    fun color(light:Light,c:Color4) {
        for(binding in light.bindings) {
            val kind=binding.kind;val mat=binding.material
            val approved = if(kind=="edge") c.red>.5f else c.green>.45f && c.green>c.red && c.green>c.blue
            if(binding.approved!=approved) {
                mat.setBaseColorTexture(textures.getValue(kind+if(approved) "_lens" else "_neutral"))
                binding.approved=approved
            }
            mat.setBaseColor(if(approved) Color4(1f,1f,1f,1f) else c)
            mat.setEmissiveColor(Color4(c.red*.10f,c.green*.10f,c.blue*.10f,1f))
        }
    }
    private fun mount(kind:String,parent:Entity,p:Vector3,light:Light,scale:Float=1f) {
        val index=used[kind]?:0
        val model=models.getValue(kind)[index]
        used[kind]=index+1
        var found=0
        fun visit(e:Entity) {
            if(kind=="edge" && e.getName()=="OakBase") e.enabled=false
            if(e.getName()=="FrostedLens") {
                val component=e.components[ModelComponent::class.java]
                if(component!=null) {
                    val mat=PhysicallyBasedMaterial.create().apply {
                        setBaseColorTexture(textures.getValue(kind+"_lens"))
                        setBaseColor(Color4(1f,1f,1f,1f));setMetallic(0f);setRoughness(.38f)
                    }
                    e.components.set(ModelComponent(component.mesh,mat))
                    light.bindings.add(Binding(kind,mat));found++
                }
            }
            e.getChildren().forEach {visit(it)}
        }
        visit(model)
        check(found==1) {"Lamp $kind expected one independent lens, got $found"}
        // Edge model was previewed in a 15mm sample rail. Remove that rail and
        // align its recessed bezel to the existing stage rail's top surface.
        model.components[TransformComponent::class.java]?.setPosition(
            Vector3(p.x,p.y-if(kind=="edge") .015f else 0f,p.z))
        model.components[TransformComponent::class.java]?.setScaleVector(Vector3(scale))
        parent.addChild(model)
        Log.i("WoodenPuppet","craftedLampMounted kind=$kind instance=$index lens=$found")
    }
    fun square(parent:Entity,p:Vector3,light:Light,scale:Float=1f)=mount("square",parent,p,light,scale)
    fun round(parent:Entity,p:Vector3,light:Light)=mount("round",parent,p,light)
    fun edge(parent:Entity,p:Vector3,light:Light)=mount("edge",parent,p,light)
}
