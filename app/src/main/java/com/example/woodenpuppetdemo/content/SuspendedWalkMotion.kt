package com.example.woodenpuppetdemo.content

import kotlin.math.min
import kotlin.math.sqrt

internal data class SuspendedMotionFrame(
    val x: Float,
    val y: Float,
    val z: Float,
    val rollRadians: Float,
    val pitchRadians: Float,
)

/** Small spring-driven body motion: the opposite foot bears weight while a leg swings. */
internal class SuspendedWalkMotion {
    private var x = 0f
    private var z = 0f
    private var targetX = 0f
    private var targetZ = 0f
    private var velocityX = 0f
    private var velocityZ = 0f
    private var lift = 0f
    private var lean = 0f

    fun update(swing: StepVector?, supportLean: Float, seconds: Float): SuspendedMotionFrame {
        if (swing != null) {
            val length = sqrt(swing.x * swing.x + swing.z * swing.z)
            if (length >= 0.001f) {
                val stride = (length * 1.4f).coerceIn(0.035f, 0.075f)
                targetX = (targetX + swing.x / length * stride).coerceIn(-0.9f, 0.9f)
                targetZ = (targetZ + swing.z / length * stride).coerceIn(-1.3f, 0.45f)
            }
        }
        val dt = seconds.coerceIn(0f, 0.05f)
        velocityX += ((targetX - x) * SPRING - velocityX * DAMPING) * dt
        velocityZ += ((targetZ - z) * SPRING - velocityZ * DAMPING) * dt
        x = (x + velocityX * dt).coerceIn(-0.9f, 0.9f)
        z = (z + velocityZ * dt).coerceIn(-1.3f, 0.45f)
        val blend = min(1f, dt * 5f)
        lift += ((if (supportLean != 0f) 1f else 0f) - lift) * blend
        lean += (supportLean - lean) * blend
        return SuspendedMotionFrame(
            x = x + lean * 0.006f,
            y = lift * 0.014f,
            z = z,
            rollRadians = lean * 0.045f + velocityX * 0.07f,
            pitchRadians = -velocityZ * 0.07f,
        )
    }

    private companion object {
        const val SPRING = 42f
        const val DAMPING = 12f
    }
}
