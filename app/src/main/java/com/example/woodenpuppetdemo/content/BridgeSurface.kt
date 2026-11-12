package com.example.woodenpuppetdemo.content

import kotlin.math.abs

/** Matches the fitted bridge deck and its two attached landing ramps. */
internal object BridgeSurface {
    const val LIFT = 0.065f
    const val DECK_END = 0.215f
    const val RAMP_END = 0.365f
    fun height(x: Float, z: Float, ready: Boolean): Float {
        if (!ready || abs(x) > 0.14f) return 0f
        val distance = abs(z - AdventureLevel.CENTER_Z)
        if (distance <= DECK_END) return LIFT
        if (distance >= RAMP_END) return 0f
        return LIFT * (RAMP_END - distance) / (RAMP_END - DECK_END)
    }
}
