package com.example.woodenpuppetdemo.content

internal enum class AdventureCue { PLATE, LOCKED, BRIDGE_TURN, BRIDGE_READY, KEY, VICTORY, STEP_LEFT, STEP_RIGHT, GEAR_PICKUP, GEAR_SEATED, DRIVE_START, LIFT_RUN, LIFT_ARRIVE, CART_ROLL, TRACK_SWITCH, CORE_RUN, WIND_RUN, POWER_SWITCH, FERRY_RUN, FERRY_ARRIVE, SHIELD_OPEN, SHIELD_CLOSE, CHIME_PICKUP, CHIME_HANG, CHIME_LOW, CHIME_MID, CHIME_HIGH, CHIME_SUCCESS, FINALE_OVATION }

/** Emits only on state edges; reset starts a fresh, silent run. */
internal class AdventureSoundEvents {
    private val emitted = mutableSetOf<AdventureCue>()
    private var lastLockedMs: Long? = null

    fun reset() { emitted.clear(); lastLockedMs = null }
    fun lockedClick(nowMs: Long): Boolean {
        if (lastLockedMs?.let { nowMs - it < 800L } == true) return false
        lastLockedMs = nowMs
        return true
    }
    fun collect(level: AdventureLevel): List<AdventureCue> = buildList {
        fun edge(active: Boolean, cue: AdventureCue) {
            if (active && emitted.add(cue)) add(cue)
        }
        edge(level.unlocked, AdventureCue.PLATE)
        edge(level.bridgeTurning, AdventureCue.BRIDGE_TURN)
        edge(level.bridgeReady, AdventureCue.BRIDGE_READY)
        edge(level.hasKey, AdventureCue.KEY)
        edge(level.completed, AdventureCue.VICTORY)
    }
}
