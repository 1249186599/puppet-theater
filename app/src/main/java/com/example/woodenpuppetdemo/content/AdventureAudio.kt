package com.example.woodenpuppetdemo.content

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.example.woodenpuppetdemo.R

/** Short, preloaded one-shots. No looping or delayed playback after reset. */
internal class AdventureAudio(context: Context) {
    private val music = AdventureMusic(context)
    fun tick() = music.tick()
    private val pool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val loaded = mutableSetOf<Int>()
    private val samples = mutableMapOf<AdventureCue, Int>()
    private val streams = mutableMapOf<AdventureCue, Int>()
    private var released = false

    init {
        pool.setOnLoadCompleteListener { _, id, status ->
            synchronized(this) {
                if (!released && status == 0) loaded.add(id)
                Log.i("WoodenPuppet", "sfxLoaded id=$id status=$status loaded=${loaded.size}/${samples.size}")
            }
        }
        mapOf(AdventureCue.PLATE to R.raw.plate_unlock,
            AdventureCue.LOCKED to R.raw.mechanism_locked,
            AdventureCue.BRIDGE_TURN to R.raw.bridge_turning,
            AdventureCue.BRIDGE_READY to R.raw.bridge_ready,
            AdventureCue.KEY to R.raw.key_pickup,
            AdventureCue.VICTORY to R.raw.door_victory,
            AdventureCue.STEP_LEFT to R.raw.wood_step_left,
            AdventureCue.STEP_RIGHT to R.raw.wood_step_right,
            AdventureCue.GEAR_PICKUP to R.raw.gear_pickup,
            AdventureCue.GEAR_SEATED to R.raw.gear_seated,
            AdventureCue.DRIVE_START to R.raw.drive_start,
            AdventureCue.LIFT_RUN to R.raw.lift_running,
            AdventureCue.LIFT_ARRIVE to R.raw.lift_arrival,
            AdventureCue.CART_ROLL to R.raw.cart_rolling,
            AdventureCue.TRACK_SWITCH to R.raw.track_switch,
            AdventureCue.CORE_RUN to R.raw.core_running,
            AdventureCue.WIND_RUN to R.raw.wind_running,
            AdventureCue.POWER_SWITCH to R.raw.power_switch,
            AdventureCue.FERRY_RUN to R.raw.ferry_running,
            AdventureCue.FERRY_ARRIVE to R.raw.ferry_arrival,
            AdventureCue.SHIELD_OPEN to R.raw.shield_open,
            AdventureCue.SHIELD_CLOSE to R.raw.shield_close,
            AdventureCue.CHIME_PICKUP to R.raw.chime_pickup,
            AdventureCue.CHIME_HANG to R.raw.chime_hang,
            AdventureCue.CHIME_LOW to R.raw.chime_low,
            AdventureCue.CHIME_MID to R.raw.chime_mid,
            AdventureCue.CHIME_HIGH to R.raw.chime_high,
            AdventureCue.CHIME_SUCCESS to R.raw.chime_success,
            AdventureCue.FINALE_OVATION to R.raw.finale_ovation).forEach { (cue, res) ->
            samples[cue] = pool.load(context.applicationContext, res, 1)
        }
    }

    @Synchronized fun play(cue: AdventureCue, gain:Float?=null) {
        if (released) return
        val sample = samples[cue] ?: return
        if (sample !in loaded) {
            Log.w("WoodenPuppet", "sfxNotReady cue=$cue")
            return
        }
        if (cue == AdventureCue.BRIDGE_READY) {
            streams.remove(AdventureCue.BRIDGE_TURN)?.let { pool.stop(it) }
        }
        if (cue == AdventureCue.LIFT_ARRIVE) stopLift()
        streams.remove(cue)?.let { pool.stop(it) }
        val volume = gain ?: when (cue) {
            AdventureCue.STEP_LEFT, AdventureCue.STEP_RIGHT -> 0.315f
            AdventureCue.LOCKED -> 0.55f
            AdventureCue.LIFT_RUN -> 0.55f
            AdventureCue.CART_ROLL -> 0.45f
            AdventureCue.CORE_RUN -> 0.30f
            AdventureCue.WIND_RUN -> 0.22f
            AdventureCue.FERRY_RUN -> 0.42f
            AdventureCue.SHIELD_OPEN, AdventureCue.SHIELD_CLOSE -> 0.45f
            else -> 0.8f
        }
        val stream = pool.play(sample, volume, volume, 1, if(cue in listOf(AdventureCue.LIFT_RUN,AdventureCue.CART_ROLL,AdventureCue.CORE_RUN,AdventureCue.WIND_RUN,AdventureCue.FERRY_RUN)) -1 else 0, 1f)
        if (stream != 0) { streams[cue] = stream; music.cue(cue) }
        Log.i("WoodenPuppet", "sfxPlay cue=$cue stream=$stream")
    }

    @Synchronized fun cargoLoops(rolling: Boolean, powered: Boolean) {
        listOf(AdventureCue.CART_ROLL to rolling,AdventureCue.CORE_RUN to powered).forEach { (cue,on) ->
            if(on && cue !in streams) play(cue)
            else if(!on) streams.remove(cue)?.let { pool.stop(it) }
        }
    }

    @Synchronized fun windLoops(wind: Boolean, ferry: Boolean) {
        listOf(AdventureCue.WIND_RUN to wind,AdventureCue.FERRY_RUN to ferry).forEach { (cue,on) ->
            if(on && cue !in streams) play(cue)
            else if(!on) streams.remove(cue)?.let { pool.stop(it) }
        }
    }

    @Synchronized fun stopLift() {
        streams.remove(AdventureCue.LIFT_RUN)?.let { pool.stop(it) }
    }

    @Synchronized fun stopSteps() {
        if (released) return
        listOf(AdventureCue.STEP_LEFT, AdventureCue.STEP_RIGHT).forEach { cue ->
            streams.remove(cue)?.let { pool.stop(it) }
        }
    }

    @Synchronized fun stopAll() {
        if (released) return
        streams.values.forEach { pool.stop(it) }
        streams.clear()
        music.reset()
    }

    @Synchronized fun release() {
        if (released) return
        stopAll()
        released = true
        music.release()
        pool.setOnLoadCompleteListener(null)
        pool.release()
        loaded.clear()
    }
}
