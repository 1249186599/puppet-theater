package com.example.woodenpuppetdemo.content

/** Follow the existing animation phase, including wrap, instead of a second timer. */
internal class WalkSoundTiming {
    private var previous = 0f
    fun reset() { previous = 0f }
    fun update(moving: Boolean, phase: Float): AdventureCue? {
        if (!moving) { reset(); return null }
        val cue = when {
            phase < previous -> AdventureCue.STEP_RIGHT
            previous < Math.PI.toFloat() && phase >= Math.PI.toFloat() -> AdventureCue.STEP_LEFT
            else -> null
        }
        previous = phase
        return cue
    }
}
