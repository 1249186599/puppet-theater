package com.example.woodenpuppetdemo.content

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/** Pure level rules and waypoint routing; no device or rendering dependencies. */
internal class AdventureLevel {
    private var walk = ClickToWalkMotion(START)
    private val route = ArrayDeque<WalkPoint>()
    var unlocked = false; private set
    var bridgeTurning = false; private set
    var bridgeProgress = 0f; private set
    val bridgeReady get() = bridgeProgress >= 1f
    var hasKey = false; private set
    var completed = false; private set
    val position get() = walk.position
    var target = START; private set
    var message = "① 点击蓝色踏板，让木偶踩上去"; private set

    fun reset() {
        walk = ClickToWalkMotion(START); route.clear(); target = START
        unlocked = false; bridgeTurning = false; bridgeProgress = 0f
        hasKey = false; completed = false
        message = "① 点击蓝色踏板，让木偶踩上去"
    }

    fun turnBridge() {
        if (completed || bridgeTurning || bridgeReady) return
        if (!unlocked) { message = "先让木偶踩上蓝色踏板，解锁橙色机关"; return }
        bridgeTurning = true
        message = "桥梁正在转动，请稍等…"
    }

    fun select(raw: WalkPoint): Boolean {
        if (completed || !raw.x.isFinite() || !raw.z.isFinite()) return false
        var p = WalkPoint(raw.x.coerceIn(-ClickToWalkMotion.WALK_HALF_WIDTH,
            ClickToWalkMotion.WALK_HALF_WIDTH), raw.z.coerceIn(-1.868528f, -0.531472f))
        if (inRiver(p)) {
            if (!bridgeReady || abs(p.x) > 0.15f) {
                message = "这里是河道，请选择陆地或接好的桥"; return false
            }
            p = if (position.z > CENTER_Z) FAR_GATE else NEAR_GATE
        }
        val currentFront = position.z > CENTER_Z
        val targetFront = p.z > CENTER_Z
        if (currentFront != targetFront && !bridgeReady) {
            message = "桥还没接好：踩蓝色踏板，再点击橙色机关"; return false
        }
        route.clear()
        if (inRiver(position)) {
            route.addLast(if (targetFront) NEAR_GATE else FAR_GATE)
        } else if (currentFront != targetFront) {
            route.addLast(if (currentFront) NEAR_GATE else FAR_GATE)
            route.addLast(if (currentFront) FAR_GATE else NEAR_GATE)
        }
        route.addLast(p); target = p
        walk.setDestination(route.removeFirst())
        message = instruction()
        return true
    }

    fun advance(seconds: Float): WalkFrame {
        val dt = seconds.coerceIn(0f, 0.05f)
        if (bridgeTurning) {
            bridgeProgress = min(1f, bridgeProgress + dt / 1.2f)
            if (bridgeReady) { bridgeTurning = false; message = instruction() }
        }
        var frame = walk.advance(dt)
        if (!frame.moving && route.isNotEmpty()) {
            walk.setDestination(route.removeFirst())
            frame = walk.advance(dt)
        }
        if (!unlocked && near(position, PAD, 0.10f)) {
            unlocked = true; message = "② 踏板已解锁！点击右侧橙色机关转桥"
        }
        if (bridgeReady && !hasKey && near(position, KEY, 0.12f)) {
            hasKey = true; message = "④ 找到钥匙了！点击右后方的大门"
        }
        if (hasKey && !completed && near(position, EXIT, 0.035f)) {
            completed = true; route.clear(); walk.setDestination(position)
            message = "冒险完成！你帮助木偶打开了剧院大门"
            frame = walk.advance(0f)
        }
        return frame
    }

    private fun instruction() = when {
        completed -> "冒险完成！你帮助木偶打开了剧院大门"
        hasKey -> "④ 找到钥匙了！点击右后方的大门"
        bridgeReady -> "③ 桥已锁定，点击对岸的金色钥匙"
        bridgeTurning -> "桥梁正在转动，请稍等…"
        unlocked -> "② 踏板已解锁！点击右侧橙色机关转桥"
        else -> "① 点击蓝色踏板，让木偶踩上去"
    }

    companion object {
        const val CENTER_Z = -1.2f
        val START = WalkPoint(0f, -0.65f)
        val PAD = WalkPoint(-0.48f, -0.75f)
        val KEY = WalkPoint(-0.45f, -1.70f)
        val EXIT = WalkPoint(0.60f, -1.45f)
        val NEAR_GATE = WalkPoint(0f, CENTER_Z + 0.385f)
        val FAR_GATE = WalkPoint(0f, CENTER_Z - 0.385f)
        fun inRiver(p: WalkPoint) = p.z < -1.06f && p.z > -1.34f
        private fun near(a: WalkPoint, b: WalkPoint, radius: Float) =
            hypot(a.x - b.x, a.z - b.z) < radius
    }
}
