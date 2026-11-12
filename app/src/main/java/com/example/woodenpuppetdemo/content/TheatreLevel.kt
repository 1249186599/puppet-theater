package com.example.woodenpuppetdemo.content

import kotlin.math.abs
import kotlin.math.hypot

/** Second-level rules. Elevation is explicit; floor clicks cannot bypass the lift. */
internal class TheatreLevel {
    private var walk = ClickToWalkMotion(START)
    private val route = ArrayDeque<WalkPoint>()
    private var intent = ""
    private var readyAge = 0f
    private var boardingAge = 0f
    private var liftAge = 0f
    var hasGear = false; private set
    var installed = false; private set
    var powered = false; private set
    var rising = false; private set
    var upstairs = false; private set
    var completed = false; private set
    val position get() = walk.position
    var target = START; private set
    var message = "① 点击左侧木齿轮，让木偶拾取它"; private set
    val liftHeight get(): Float {
        val p=(liftAge/LIFT_SECONDS).coerceIn(0f,1f)
        return BASE_HEIGHT + RISE*(p*p*(3f-2f*p))
    }
    fun reset() {
        walk=ClickToWalkMotion(START); route.clear(); intent=""; readyAge=0f; boardingAge=0f; liftAge=0f
        hasGear=false; installed=false; powered=false; rising=false; upstairs=false; completed=false
        target=START; message="① 点击左侧木齿轮，让木偶拾取它"
    }
    fun select(action: String, raw: WalkPoint? = null): Boolean {
        if(completed) return false
        if(rising) { message="升降台正在上升，请稍等…"; return false }
        if(action=="lift" && !powered) { message=if(installed) "动力正在启动，请稍等…" else "先拾取木齿轮，再点击动力箱安装"; return false }
        if(action=="box" && !hasGear) { message="先点击左侧木齿轮，把它带到动力箱"; return false }
        if(action=="exit" && !upstairs) { message="先修复动力箱，再乘升降台到上层"; return false }
        if(upstairs && action !in listOf("floor","exit","lift")) return false
        if(action=="gear" && hasGear) return false
        if(action=="box" && installed) { message=instruction(); return false }
        val p=when(action) {
            "gear" -> GEAR
            "box" -> BOX_APPROACH
            "lift" -> if(upstairs) LANDING else LIFT
            "exit" -> EXIT
            "floor" -> {
                val q=raw ?: return false
                if(!q.x.isFinite() || !q.z.isFinite()) return false
                if(upstairs) {
                    if(q.z > -1.44f) { message="升降台已停在上层，请在上层平台活动"; return false }
                    WalkPoint(q.x.coerceIn(-.65f,.65f),q.z.coerceIn(-1.65f,-1.57f))
                } else {
                    if(q.z < -.94f) { message="请点击升降台乘坐，不能直接走到上层"; return false }
                    WalkPoint(q.x.coerceIn(-.65f,.65f),q.z.coerceIn(if(q.x>.30f) -.70f else -.88f,-.54f))
                }
            }
            else -> return false
        }
        route.clear(); boardingAge=0f; intent=action
        // Always leave/enter the lift through the open end, never across a railing.
        if(!upstairs && (action=="lift" || position.z < -.94f)) route.addLast(ENTRY)
        if(upstairs && position.z > -1.56f) route.addLast(LANDING)
        route.addLast(p); target=p
        walk.setDestination(route.removeFirst()); message=instruction()
        return true
    }
    fun heightAt(p: WalkPoint): Float = when {
        upstairs -> TOP_HEIGHT
        rising -> liftHeight
        abs(p.x)<.15f && p.z < -.84f -> BASE_HEIGHT*((-.84f-p.z)/.12f).coerceIn(0f,1f)
        else -> 0f
    }
    fun advance(seconds: Float): WalkFrame {
        val dt=seconds.coerceIn(0f,.05f)
        if(installed && !powered) {
            readyAge+=dt
            if(readyAge>=1f) { powered=true; message=instruction() }
        }
        if(rising) {
            liftAge=(liftAge+dt).coerceAtMost(LIFT_SECONDS)
            if(liftAge>=LIFT_SECONDS) { rising=false; upstairs=true; intent=""; message=instruction() }
            return walk.advance(0f)
        }
        var frame=walk.advance(dt)
        if(!frame.moving && route.isNotEmpty()) { walk.setDestination(route.removeFirst()); frame=walk.advance(dt) }
        val arrived=!frame.moving && route.isEmpty()
        if(arrived) when(intent) {
            "gear" -> if(!hasGear && near(position,GEAR)) { hasGear=true; intent=""; message=instruction() }
            "box" -> if(hasGear && !installed && near(position,BOX_APPROACH)) { installed=true; intent=""; message=instruction() }
            "lift" -> if(powered && !upstairs && near(position,LIFT)) {
                boardingAge+=dt
                if(boardingAge>=.35f) { rising=true; message="升降台正在上升，请稍等…" }
            }
            "exit" -> if(upstairs && near(position,EXIT)) { completed=true; intent=""; message=instruction() }
        }
        return frame
    }
    private fun instruction()=when {
        completed -> "第二关完成！你修好了剧场，帮助木偶到达上层出口"
        upstairs -> "④ 已到上层！点击大门，让木偶走出升降台"
        powered -> "③ 动力已恢复！点击升降台，木偶站稳后自动上升"
        installed -> "齿轮安装完成，动力正在启动…"
        hasGear -> "② 带上齿轮，点击右侧动力箱安装"
        else -> "① 点击左侧木齿轮，让木偶拾取它"
    }
    companion object {
        val START=WalkPoint(0f,-.62f)
        val GEAR=WalkPoint(-.48f,-.73f)
        val BOX_APPROACH=WalkPoint(.50f,-.65f)
        val ENTRY=WalkPoint(0f,-.84f)
        val LIFT=WalkPoint(0f,-1.20f)
        val LANDING=WalkPoint(0f,-1.58f)
        val EXIT=WalkPoint(.60f,-1.54f)
        const val BASE_HEIGHT=.032f
        const val RISE=.32f
        const val TOP_HEIGHT=BASE_HEIGHT+RISE
        const val LIFT_SECONDS=4.5f
        private fun near(a: WalkPoint,b: WalkPoint)=hypot(a.x-b.x,a.z-b.z)<.02f
    }
}
