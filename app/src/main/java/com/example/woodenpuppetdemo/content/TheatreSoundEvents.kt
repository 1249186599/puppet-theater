package com.example.woodenpuppetdemo.content

internal class TheatreSoundEvents {
    private val emitted=mutableSetOf<AdventureCue>()
    fun reset()=emitted.clear()
    fun collect(level: TheatreLevel): List<AdventureCue> = buildList {
        fun edge(on: Boolean,cue: AdventureCue) { if(on && emitted.add(cue)) add(cue) }
        edge(level.hasGear,AdventureCue.GEAR_PICKUP)
        edge(level.installed,AdventureCue.GEAR_SEATED)
        edge(level.powered,AdventureCue.DRIVE_START)
        edge(level.rising,AdventureCue.LIFT_RUN)
        edge(level.upstairs,AdventureCue.LIFT_ARRIVE)
        edge(level.completed,AdventureCue.VICTORY)
    }
}
