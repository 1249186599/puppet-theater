package com.example.woodenpuppetdemo.content

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal data class WalkPoint(val x: Float, val z: Float)

internal data class WalkFrame(
    val position: WalkPoint,
    val yawRadians: Float,
    val phaseRadians: Float,
    val moving: Boolean,
)

/** A bounded tabletop stage and a single smooth click-to-walk destination. */
internal class ClickToWalkMotion(
    start: WalkPoint = WalkPoint(0f, -1.2f),
) {
    var position = start
        private set
    var destination = start
        private set
    private var yaw = 0f
    private var phase = 0f

    fun setDestination(point: WalkPoint): WalkPoint {
        destination = WalkPoint(
            point.x.coerceIn(-WALK_HALF_WIDTH, WALK_HALF_WIDTH),
            point.z.coerceIn(STAGE_CENTER_Z - WALK_HALF_DEPTH,
                STAGE_CENTER_Z + WALK_HALF_DEPTH),
        )
        return destination
    }

    fun advance(seconds: Float): WalkFrame {
        val dt = seconds.coerceIn(0f, 0.05f)
        val dx = destination.x - position.x
        val dz = destination.z - position.z
        val distance = sqrt(dx * dx + dz * dz)
        if (distance <= ARRIVAL_METERS) {
            position = destination
            phase = 0f
            return WalkFrame(position, yaw, phase, false)
        }
        val targetYaw = atan2(dx, dz)
        val turn = atan2(sin(targetYaw - yaw), cos(targetYaw - yaw))
        yaw += turn * min(1f, dt * 8f)
        val travel = min(distance, WALK_SPEED_METERS_PER_SECOND * dt)
        position = WalkPoint(position.x + dx / distance * travel,
            position.z + dz / distance * travel)
        phase += dt * WALK_CYCLES_PER_SECOND * (2f * Math.PI.toFloat())
        if (phase > 2f * Math.PI.toFloat()) phase -= 2f * Math.PI.toFloat()
        return WalkFrame(position, yaw, phase, true)
    }

    companion object {
        const val STAGE_CENTER_Z = -1.2f
        // Scale each side by sqrt(2) to double the board area.
        const val STAGE_HALF_WIDTH = 0.84852814f
        const val STAGE_HALF_DEPTH = 0.84852814f
        private const val EDGE_CLEARANCE = 0.18f
        const val WALK_HALF_WIDTH = STAGE_HALF_WIDTH - EDGE_CLEARANCE
        const val WALK_HALF_DEPTH = STAGE_HALF_DEPTH - EDGE_CLEARANCE
        const val WALK_SPEED_METERS_PER_SECOND = 0.18f
        const val WALK_CYCLES_PER_SECOND = 1.5f
        const val ARRIVAL_METERS = 0.012f

        fun boardIntersection(originX: Float, originY: Float, originZ: Float,
            directionX: Float, directionY: Float, directionZ: Float,
            floorY: Float): WalkPoint? {
            if (abs(directionY) < 0.02f) return null
            val t = (floorY - originY) / directionY
            if (t <= 0f || t > 4f) return null
            val x = originX + directionX * t
            val z = originZ + directionZ * t
            return if (abs(x) <= STAGE_HALF_WIDTH &&
                abs(z - STAGE_CENTER_Z) <= STAGE_HALF_DEPTH)
                WalkPoint(x, z) else null
        }
    }
}
