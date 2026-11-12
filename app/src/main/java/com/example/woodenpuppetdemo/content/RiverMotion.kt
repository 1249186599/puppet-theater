package com.example.woodenpuppetdemo.content
import android.util.Log
import com.pico.spatial.core.ecs.*
import kotlin.math.*
internal class RiverMotion(root:Entity) {
    private val controllers=mutableListOf<BlendShapeControllerComponent>()
    init {
        fun visit(entity:Entity) {
            val names=entity.components[ModelComponent::class.java]?.mesh?.getBlendShapeNames()
            if(names!=null && names.size==4) {
                val controller=entity.components[BlendShapeControllerComponent::class.java]
                    ?: BlendShapeControllerComponent().also {entity.components.set(it)}
                controllers.add(controller)
            }
            entity.getChildren().forEach {visit(it)}
        }
        visit(root)
        Log.i("WoodenPuppet","riverWaveControllers=${controllers.size}")
    }
    fun update(time:Float) {
        val phase=time*.70f
        val weights=floatArrayOf(max(0f,cos(phase)),max(0f,sin(phase)),max(0f,-cos(phase)),max(0f,-sin(phase)))
        for(c in controllers) for(i in 0..3)c.setBlendShapeWeight(i,weights[i])
    }
}
