package com.example.woodenpuppetdemo.content

import kotlin.math.*

/** Chapter four owns safe routes, ferry boarding and generous wind windows. */
internal class WindLevel {
    var position=START; private set
    private val route=ArrayDeque<WalkPoint>()
    private val cues=ArrayDeque<AdventureCue>()
    private var intent=""
    private var yaw=0f
    private var phase=0f
    var time=0f; private set
    var scoopQuarter=0; private set
    var windReady=false; private set
    var power=0; private set // 1 ferry; 2 wind corridor
    var riding=false; private set
    var rideProgress=0f; private set
    var island=false; private set
    var hasChimes=false; private set
    var crossed=0; private set
    var waiting=0; private set
    var crossing=0; private set
    var hung=false; private set
    var notes=0; private set
    var tuned=false; private set
    var completed=false; private set
    private var lastNote=-1f
    var idleSeconds=0f; private set
    private var nextHintAt=0f
    private var pendingHint: Int?=null
    val helpVisible get()=idleSeconds>=12f && !busy && !completed
    fun consumeHintNote():Int? {val result=pendingHint;pendingHint=null;return result}
    val taskLine get()=when {
        completed -> "风铃响起，工坊重新苏醒"
        tuned -> "推开通往剧场的大门"
        hung -> "听着风铃，奏出旋律"
        crossed==2 -> "让风铃回到木架上"
        waiting>0 -> "等风停，再向前"
        crossing>0 -> "穿过风道"
        riding -> "随平台渡过水道"
        island && hasChimes -> "带着风铃返回"
        island -> "找到遗落的风铃"
        hasChimes && power!=2 -> "把动力送往风道"
        hasChimes || crossed>0 -> "借挡风板穿过风道"
        power==1 -> "去小岛，把风铃带回来"
        power==2 -> "先把动力送往摆渡平台"
        windReady -> "把动力送往摆渡平台"
        else -> "让风车转起来"
    }
    private val previousSafe=booleanArrayOf(false,false)
    var message="1/5 点击木制风筒，让窄喷口朝向扇叶"; private set
    val stage get()=when { !windReady->1;power==0->2;!hasChimes || island || riding->3;crossed<2->4;else->5 }
    val busy get()=riding || route.isNotEmpty() || intent.isNotEmpty() || waiting>0 || crossing>0
    val ferryZ get(): Float {
        val p=rideProgress;val e=p*p*(3f-2f*p)
        return if(riding) {if(island) FAR.z+(NEAR.z-FAR.z)*e else NEAR.z+(FAR.z-NEAR.z)*e}
        else if(island) FAR.z else NEAR.z
    }
    val elevation get(): Float=when {
        riding || island -> .025f
        position.z< -1.84f -> .16f
        position.x>.36f && position.z<-.94f -> ((-.94f-position.z)/.90f).coerceIn(0f,1f)*.16f
        position.x<-.60f && position.z<-.88f -> ((-.88f-position.z)/.16f).coerceIn(0f,1f)*.025f
        else -> 0f
    }
    fun safe(gate:Int): Boolean = windReady && power==2 && (((time+(gate-1)*3.1f)%8f)<4.8f || crossing==gate || crossed>=gate)
    fun reset() {
        position=START;route.clear();cues.clear();intent="";yaw=0f;phase=0f;time=0f;scoopQuarter=0;windReady=false;power=0
        riding=false;rideProgress=0f;island=false;hasChimes=false;crossed=0;waiting=0;crossing=0;hung=false;notes=0;tuned=false;completed=false;lastNote=-1f
        previousSafe.fill(false);idleSeconds=0f;nextHintAt=0f;pendingHint=null;message=instruction()
    }
    fun drainCues(): List<AdventureCue> {val result=cues.toList();cues.clear();return result}
    private fun cue(c:AdventureCue) {cues.addLast(c)}
    private fun routeTo(action:String,vararg points:WalkPoint) {intent=action;route.clear();route.addAll(points.toList())}
    fun select(input:String,raw:WalkPoint?=null):Boolean {
        val action=if(input.startsWith("note") && !hung) "chimes" else input
        if(completed || busy) return false
        when(action) {
            "scoop" -> {
                if(windReady)return false
                scoopQuarter=(scoopQuarter+1)%4;cue(AdventureCue.POWER_SWITCH)
                if(scoopQuarter==2) {windReady=true;cue(AdventureCue.BRIDGE_READY)}
            }
            "power" -> {
                if(!windReady || island || crossed>0) return false
                power=if(power==1)2 else 1;cue(AdventureCue.POWER_SWITCH)
            }
            "ferry" -> {
                if(!windReady || power!=1 || crossed>0)return false
                if(island)routeTo("ride",ISLAND,FAR)
                else routeTo("ride",WalkPoint(position.x,-.69f),ENTRY,NEAR)
            }
            "chimes" -> {
                if(!island || hasChimes)return false
                routeTo("pickup",ISLAND)
            }
            "wind1","wind2" -> {
                val gate=if(action=="wind1")1 else 2
                if(!hasChimes || island || power!=2 || crossed!=gate-1) {message="先乘摆渡平台取回风铃，再把动力切换到右侧风道";return false}
                if(gate==1)routeTo("wait1",WalkPoint(position.x,-.69f),WalkPoint(.62f,-.69f),WAIT1)
                else routeTo("wait2",WAIT2)
            }
            "hang" -> {
                if(crossed<2 || !hasChimes || hung)return false
                routeTo("hang",WalkPoint(.62f,-1.89f),HANG)
            }
            "note0","note1","note2" -> {
                if(!hung || tuned || time-lastNote<.3f)return false
                lastNote=time;nextHintAt=time+3f;pendingHint=null;idleSeconds=0f
                val note=action.last()-'0';cue(listOf(AdventureCue.CHIME_LOW,AdventureCue.CHIME_MID,AdventureCue.CHIME_HIGH)[note])
                if(note==ORDER[notes]) {notes++;if(notes==3) {tuned=true;cue(AdventureCue.CHIME_SUCCESS)}}
                else {notes=0;message="顺序不对：按标记 1 → 2 → 3 敲响，已挂好的风铃会保留";return true}
            }
            "exit" -> {
                if(!tuned)return false
                routeTo("exit",WalkPoint(position.x,-1.89f),EXIT)
            }
            "floor" -> {
                val q=raw?:return false
                if(!q.x.isFinite() || !q.z.isFinite())return false
                if(crossed==2) {
                    // Both wind segments are complete: the rear deck remains walkable while
                    // hanging, playing the melody and approaching the exit. Keep a clear strip
                    // in front of the frame/door and a body margin at the deck edges.
                    if(q.x !in DECK_MIN_X..DECK_MAX_X || q.z !in DECK_MIN_Z..DECK_MAX_Z)return false
                    routeTo("floor",WalkPoint(q.x.coerceIn(DECK_MIN_X+.12f,DECK_MAX_X-.12f),
                        q.z.coerceIn(-1.99f,DECK_MAX_Z-.12f)))
                } else {
                    // Before reaching the rear deck, keep the original water/wind restrictions.
                    if(island || crossed>0 || q.z<-.83f)return false
                    routeTo("floor",WalkPoint(q.x.coerceIn(-.55f,1.1f),q.z.coerceIn(-.79f,-.55f)))
                }
            }
            else -> return false
        }
        idleSeconds=0f
        message=if(route.isNotEmpty()) "木偶正在前往目标…\n请等它停下，再按提示操作。" else instruction();return true
    }
    fun advance(seconds:Float):WalkFrame {
        val dt=seconds.coerceIn(0f,.05f);time+=dt
        idleSeconds=if(busy || completed)0f else idleSeconds+dt
        if(hung && !tuned && !busy && time>=nextHintAt) {
            pendingHint=ORDER[notes];nextHintAt=time+6f
        }
        for(i in 0..1) {
            val now=safe(i+1)
            if(power==2 && !completed && now!=previousSafe[i]) cue(if(now)AdventureCue.SHIELD_CLOSE else AdventureCue.SHIELD_OPEN)
            previousSafe[i]=now
        }
        if(riding) {
            rideProgress=(rideProgress+dt/4.5f).coerceAtMost(1f)
            position=WalkPoint(-.85f,ferryZ)
            if(rideProgress>=1f) {
                riding=false;island=!island;cue(AdventureCue.FERRY_ARRIVE)
                routeTo("disembark",if(island) ISLAND else ENTRY)
                message=instruction()
            }
            return WalkFrame(position,yaw,0f,false)
        }
        if(waiting>0) {
            val g=waiting;val cycle=(time+(g-1)*3.1f)%8f
            if(cycle<1.5f) {
                waiting=0;crossing=g
                routeTo("cross$g",if(g==1) WAIT2 else LANDING)
                message="4/5 挡风板已合拢，木偶正在通过第 ${g} 段风道…"
            }
        }
        var moving=false
        if(route.isNotEmpty()) {
            val dest=route.first();val dx=dest.x-position.x;val dz=dest.z-position.z;val distance=hypot(dx,dz)
            if(distance<.006f){position=dest;route.removeFirst()}
            else {
                moving=true;val a=atan2(dx,dz);yaw+=atan2(sin(a-yaw),cos(a-yaw))*min(1f,dt*8f)
                val step=min(distance,.18f*dt);position=WalkPoint(position.x+dx/distance*step,position.z+dz/distance*step)
                phase=(phase+dt*1.5f*2f*Math.PI.toFloat())%(2f*Math.PI.toFloat())
            }
        }
        if(route.isEmpty() && intent.isNotEmpty()) {
            val arrived=intent;intent="";idleSeconds=0f
            when(arrived) {
                "ride"->{riding=true;rideProgress=0f}
                "pickup"->{hasChimes=true;cue(AdventureCue.CHIME_PICKUP)}
                "wait1","wait2"->{waiting=if(arrived=="wait1")1 else 2}
                "cross1","cross2"->{crossed=crossing;crossing=0;cue(AdventureCue.BRIDGE_READY)}
                "hang"->{hung=true;nextHintAt=time+3f;cue(AdventureCue.CHIME_HANG)}
                "exit"->{completed=true;cue(AdventureCue.VICTORY)}
            }
            message=instruction()
        }
        if(!moving)phase=0f
        return WalkFrame(position,yaw,phase,moving)
    }
    val suggestedAction: String? get()=when {
        completed || busy -> null
        tuned -> "exit"
        hung -> "note${ORDER[notes]}"
        crossed==2 -> "hang"
        crossed==1 -> "wind2"
        island && hasChimes -> "ferry"
        island -> "chimes"
        hasChimes && power!=2 -> "power"
        hasChimes -> "wind1"
        power==2 -> "power"
        power==1 -> "ferry"
        windReady -> "power"
        else -> "scoop"
    }
    private fun instruction():String=when {
        completed->"第四关完成！风铃已响起，大门已打开。"
        tuned->"5/5 打开出口\n点击后方木门。"
        hung->"5/5 敲响风铃（已完成 $notes/3）\n点击${listOf("左侧","中间","右侧")[ORDER[notes]]}的铃管，或它正下方的按钮。\n点击平台空地可移动木偶。"
        crossed==2->"5/5 挂上风铃\n点击后方空的木架，木偶会把风铃挂上去。"
        waiting>0->"4/5 正在等待安全通行\n不用继续点击。挡风板合拢后，木偶会自动通过第 $waiting 段。"
        crossing>0->"4/5 正在通过风道\n请等木偶走到安全落脚点。"
        crossed==1->"4/5 通过第二段风道\n点击右侧靠后的按钮。木偶会等待安全时机再走。"
        riding->"3/5 摆渡平台正在移动\n请等木偶到岸并走下平台，再进行下一步。"
        island && hasChimes->"3/5 带风铃回去\n点击左侧摆渡平台，木偶会乘它返回。"
        island->"3/5 拾取风铃\n点击小岛上的三根金色铃管，或铃管下面的木底座。"
        hasChimes && power!=2->"4/5 给风道供动力\n点击前方分流手柄一次，把动力从摆渡切到右侧风道。"
        hasChimes->"4/5 通过第一段风道\n点击右侧靠前的按钮。木偶会等待安全时机再走。"
        power==2->"3/5 先去取风铃\n点击前方分流手柄一次，切回摆渡平台。"
        power==1->"3/5 乘平台去小岛\n点击左侧停靠岸边的木平台，木偶会自动登上并乘坐。"
        windReady->"2/5 启动摆渡平台\n点击前方分流手柄一次，把风车动力送给左侧平台。"
        else->"1/5 启动风车\n点击木制风筒使它转动，让窄喷口朝向扇叶。布条飘起时，风车就会启动。"
    }
    companion object {
        // Authored wind_floor rear platform: center (.4, -2.07), size (1.8, .62).
        const val DECK_MIN_X=-.5f;const val DECK_MAX_X=1.3f
        const val DECK_MIN_Z=-2.38f;const val DECK_MAX_Z=-1.76f
        const val DECK_HEIGHT=.16f
        val START=WalkPoint(0f,-.62f);val ENTRY=WalkPoint(-.85f,-.78f)
        const val FERRY_HALF_LENGTH=.21f
        const val NEAR_BANK_EDGE=-.95f
        const val ISLAND_BANK_EDGE=-1.58f
        private const val DOCK_GAP=.002f
        val NEAR=WalkPoint(-.85f,NEAR_BANK_EDGE-FERRY_HALF_LENGTH-DOCK_GAP)
        val FAR=WalkPoint(-.85f,ISLAND_BANK_EDGE+FERRY_HALF_LENGTH+DOCK_GAP)
        val ISLAND=WalkPoint(-.85f,-1.83f)
        val WAIT1=WalkPoint(.62f,-.97f);val WAIT2=WalkPoint(.62f,-1.41f);val LANDING=WalkPoint(.62f,-1.89f)
        val HANG=WalkPoint(.05f,-1.89f);val EXIT=WalkPoint(1.13f,-1.89f)
        val ORDER=listOf(0,2,1)
    }
}
