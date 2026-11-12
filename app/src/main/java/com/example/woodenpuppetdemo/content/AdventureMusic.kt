package com.example.woodenpuppetdemo.content

import android.content.Context
import android.media.MediaPlayer
import android.os.SystemClock
import android.util.Log
import com.example.woodenpuppetdemo.R

/** Quiet continuous bed; mechanism cues temporarily take precedence. */
internal class AdventureMusic(context: Context) {
    private var player: MediaPlayer? = MediaPlayer.create(context.applicationContext, R.raw.theatre_bgm)
    private var duckUntil = 0L
    private var volume = 0f
    private var lastMs = SystemClock.uptimeMillis()
    init {
        player?.apply {
            isLooping = true
            setVolume(0f, 0f)
            setOnErrorListener { _, what, extra ->
                Log.e("WoodenPuppet", "bgmError what=$what extra=$extra")
                true
            }
            start()
        }
        Log.i("WoodenPuppet", "bgmStarted ready=${player != null} loop=true gain=0.28")
    }
    fun cue(cue: AdventureCue) {
        val duration = when (cue) {
            AdventureCue.PLATE -> 700L
            AdventureCue.LOCKED -> 480L
            AdventureCue.BRIDGE_TURN -> 1200L
            AdventureCue.BRIDGE_READY -> 720L
            AdventureCue.KEY -> 900L
            AdventureCue.VICTORY -> 2600L
            AdventureCue.GEAR_PICKUP, AdventureCue.GEAR_SEATED -> 850L
            AdventureCue.DRIVE_START -> 1800L
            AdventureCue.LIFT_RUN -> 5000L
            AdventureCue.LIFT_ARRIVE -> 1100L
            AdventureCue.TRACK_SWITCH -> 1150L
            AdventureCue.CORE_RUN -> 2000L
            AdventureCue.POWER_SWITCH, AdventureCue.FERRY_ARRIVE -> 1100L
            AdventureCue.CHIME_PICKUP, AdventureCue.CHIME_HANG -> 2200L
            AdventureCue.CHIME_LOW, AdventureCue.CHIME_MID, AdventureCue.CHIME_HIGH -> 800L
            AdventureCue.CHIME_SUCCESS -> 4200L
            AdventureCue.FINALE_OVATION -> 8000L
            else -> return
        }
        duckUntil = maxOf(duckUntil, SystemClock.uptimeMillis() + duration)
    }
    fun tick() {
        val now = SystemClock.uptimeMillis()
        val dt = ((now-lastMs)/1000f).coerceIn(0f, 0.1f)
        lastMs = now
        val target = if (now < duckUntil) 0.07f else 0.28f
        val speed = if (target < volume) 2.1f else 0.24f
        volume += (target-volume).coerceIn(-speed*dt, speed*dt)
        player?.setVolume(volume, volume)
    }
    fun reset() {
        duckUntil = 0L
        volume = 0f
        player?.setVolume(0f, 0f)
        player?.seekTo(0)
    }
    fun release() { player?.release(); player = null }
}
