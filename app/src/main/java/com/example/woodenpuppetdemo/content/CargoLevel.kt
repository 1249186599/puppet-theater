package com.example.woodenpuppetdemo.content

import kotlin.math.*

/** Five independent challenges, reversible branch choice, no Android dependencies. */
internal class CargoLevel {
    var position=START; private set
    var cart=ORIGIN; private set
    var cartYaw=Math.PI.toFloat()/2f; private set
    var unlocked=false; private set
    var loaded=false; private set
    var trackReady=false; private set
    var trackQuarter=1; private set
    var correctBranch=false; private set
    var atBranch=false; private set
    var atJunction=false; private set
    var docked=false; private set
    var seated=false; private set
    var coreQuarter=1; private set
    var powered=false; private set
    var completed=false; private set
    var transfer=0f; private set
    var transferKind=""; private set
    var target=START; private set
    var message="1/5 配对解锁：观察车锁图案，点击相同图案的按钮"; private set
    private val cues=ArrayDeque<AdventureCue>()
    private val walkRoute=ArrayDeque<WalkPoint>()
    private var intent=""
    private var yaw=0f
    private var phase=0f
    private var drivePath=emptyList<WalkPoint>()
    private var driveAt=0f
    private var driveEnd=0f
    private var driveResult=""
    var pushing=false; private set
    var rewinding=false; private set
    val busy get()=pushing || rewinding || transferKind.isNotEmpty() || intent.isNotEmpty()
    val canLoad get()=unlocked && !loaded && !busy && near(cart,LOAD)
    val stage get()=when { !unlocked->1;!loaded->2;!trackReady->3;!docked->4;else->5 }
    fun drainCues(): List<AdventureCue> = buildList { while(cues.isNotEmpty()) add(cues.removeFirst()) }
    fun reset() {
        position=START;cart=ORIGIN;cartYaw=Math.PI.toFloat()/2f;unlocked=false;loaded=false
        trackReady=false;trackQuarter=1;correctBranch=false;atBranch=false;atJunction=false;docked=false
        seated=false;coreQuarter=1;powered=false;completed=false;transfer=0f;transferKind=""
        target=START;intent="";yaw=0f;phase=0f;drivePath=emptyList();driveAt=0f;driveEnd=0f;driveResult=""
        pushing=false;rewinding=false;walkRoute.clear();cues.clear();message=instruction()
    }
    private fun reject(text: String): Boolean {message=text;cues.add(AdventureCue.LOCKED);return false}
    fun select(action: String,raw: WalkPoint?=null): Boolean {
        if(completed) return false
        if(busy) {message="机关或木偶正在移动，请稍等…";return false}
        when {
            action.startsWith("lock") -> {
                if(unlocked) return false
                if(action!="lockCircle") return reject("1/5 图案不匹配：观察车锁上的圆环，再试一次")
                approach(LOCK_APPROACH,"unlock")
            }
            action=="cart" -> {
                if(!unlocked) return reject("1/5 先匹配车锁图案，解除锁扣")
                if(atBranch) return reject("4/5 这里是维修支线，请点击回退机关")
                if(docked) return reject("5/5 小车已到位，请点击动力座接驳")
                val end=when {
                    !loaded && near(cart,ORIGIN) -> LOAD
                    !loaded -> return reject("2/5 小车已到位：点击架子上的核心，或前排发亮的蓝色圆环按钮")
                    !trackReady -> SAFE_LINE
                    !atJunction -> JUNCTION
                    correctBranch -> DOCK
                    else -> BRANCH
                }
                if(near(cart,end)) return reject("3/5 前方断轨：点击旋转轨道盘，把两条轨道接通")
                val result=when(end) {LOAD->"load";SAFE_LINE->"safe";JUNCTION->"junction";DOCK->"dock";else->"branch"}
                driveResult=result
                approach(WalkPoint(cart.x-.28f,cart.z),"push")
            }
            action=="load" -> {
                if(!unlocked || !near(cart,LOAD)) return reject("2/5 先点击小车，把它推到装载标记处")
                if(loaded) return false
                transfer=0f;transferKind="load";message="2/5 装载托架正在把核心送入车斗…"
            }
            action=="track" -> {
                if(!loaded) return reject("先完成核心装载，再修复断轨")
                if(trackReady) return false
                trackQuarter=(trackQuarter+1)%4;cues.add(AdventureCue.TRACK_SWITCH)
                if(trackQuarter%2==0) {trackReady=true;cues.add(AdventureCue.BRIDGE_READY)}
                message=instruction()
            }
            action=="switch" -> {
                if(!trackReady || docked) return reject("先修复断轨，再选择运输路线")
                if(atBranch) return reject("先把小车退回岔口，再切换路线")
                correctBranch=!correctBranch;cues.add(AdventureCue.TRACK_SWITCH);message=instruction()
            }
            action=="rewind" -> {
                if(!atBranch) return reject("只有进入维修支线后才需要回退")
                drivePath=listOf(cart,JUNCTION);driveAt=0f;driveEnd=length(drivePath);rewinding=true
                message="4/5 回退机关正在把小车拉回岔口…"
            }
            action=="dock" -> {
                if(!docked) return reject("先把小车沿圆环标记的路线送到动力座旁")
                if(seated) return false
                transfer=0f;transferKind="dock";message="5/5 接驳托架正在把核心送入动力座…"
            }
            action=="dial" -> {
                if(!seated) return reject("5/5 先点击动力座，完成核心接驳")
                if(powered) return false
                coreQuarter=(coreQuarter+1)%4;cues.add(AdventureCue.GEAR_SEATED)
                if(coreQuarter==0) {powered=true;cues.add(AdventureCue.CORE_RUN);cues.add(AdventureCue.BRIDGE_READY)}
                message=instruction()
            }
            action=="exit" -> {
                if(!powered) return reject("完成五项任务并接通动力后，大门才能打开")
                approach(EXIT,"exit")
            }
            action=="floor" -> {
                val p=raw ?: return false
                if(!p.x.isFinite() || !p.z.isFinite()) return false
                if(p.z<-.86f) return reject("轨道区域请直接点击小车或机关；前方木地板可以行走")
                walkRoute.clear();target=WalkPoint(p.x.coerceIn(-1.10f,1.10f),p.z.coerceIn(-.76f,-.54f))
                walkRoute.add(WalkPoint(position.x,-.66f));walkRoute.add(target);message=instruction()
            }
            else -> return false
        }
        return true
    }
    private fun approach(p: WalkPoint,action: String) {
        walkRoute.clear()
        if(action=="push") {
            walkRoute.addAll(CartApproach.route(position,cart,p))
        } else {
        val sideX=if(position.z < -1.30f) position.x-.28f else position.x
        if(position.z < -1.30f) walkRoute.add(WalkPoint(sideX,position.z))
        walkRoute.add(WalkPoint(sideX,-.66f))
        if(action=="exit") { walkRoute.add(WalkPoint(1.20f,-.66f));walkRoute.add(WalkPoint(1.20f,p.z)) }
        else walkRoute.add(WalkPoint(p.x,-.66f))
        walkRoute.add(p)
        }
        target=p;intent=action
        message=when(action) {"unlock"->"1/5 木偶正在走向锁扣…";"push"->"木偶正在走到车后…";else->"五项任务完成，木偶正在走向大门…"}
    }
    private fun startDrive() {
        val end=when(driveResult) {"load"->LOAD;"safe"->SAFE_LINE;"junction"->JUNCTION;"dock"->DOCK;else->BRANCH}
        drivePath=listOf(position,cart,end);driveAt=distance(position,cart);driveEnd=length(drivePath)
        pushing=true;message="${stage}/5 木偶正在沿轨道推车…"
    }
    fun advance(seconds: Float): WalkFrame {
        val dt=seconds.coerceIn(0f,.05f)
        var moving=false
        if(transferKind.isNotEmpty()) {
            transfer=(transfer+dt/1.4f).coerceAtMost(1f)
            if(transfer>=1f) {
                if(transferKind=="load") loaded=true else seated=true
                transferKind="";cues.add(AdventureCue.GEAR_SEATED);message=instruction()
            }
        } else if(pushing || rewinding) {
            driveAt=(driveAt+dt*.12f).coerceAtMost(driveEnd)
            val old=cart;cart=sample(drivePath,driveAt)
            if(distance(old,cart)>.00001f) cartYaw=atan2(cart.x-old.x,cart.z-old.z)
            if(pushing) {
                val oldP=position;position=sample(drivePath,(driveAt-.28f).coerceAtLeast(0f))
                if(distance(oldP,position)>.00001f) yaw=atan2(position.x-oldP.x,position.z-oldP.z)
                moving=true
            }
            if(driveAt>=driveEnd) {
                if(rewinding) {atBranch=false;atJunction=true;cartYaw=Math.PI.toFloat()/2f}
                else when(driveResult) {"junction"->atJunction=true;"branch"->{atBranch=true;atJunction=false};"dock"->{docked=true;atJunction=false}}
                pushing=false;rewinding=false;moving=false;message=instruction()
            }
        } else if(walkRoute.isNotEmpty()) {
            val p=walkRoute.first();val dx=p.x-position.x;val dz=p.z-position.z;val d=hypot(dx,dz)
            if(d<=.012f) {position=p;walkRoute.removeFirst()}
            else {
                val travel=min(d,.18f*dt);position=WalkPoint(position.x+dx/d*travel,position.z+dz/d*travel)
                val turn=atan2(sin(atan2(dx,dz)-yaw),cos(atan2(dx,dz)-yaw));yaw+=turn*min(1f,dt*8f);moving=true
            }
            if(walkRoute.isEmpty()) {
                val done=intent;intent=""
                when(done) {
                    "unlock"->{unlocked=true;cues.add(AdventureCue.PLATE);message=instruction()}
                    "push"->startDrive()
                    "exit"->{completed=true;cues.add(AdventureCue.VICTORY);message=instruction()}
                }
            }
        }
        phase=if(moving) (phase+dt*1.5f*2f*Math.PI.toFloat())%(2f*Math.PI.toFloat()) else 0f
        return WalkFrame(position,yaw,phase,moving)
    }
    private fun instruction()=when {
        completed->"第三关完成！你成功把发条核心送到了剧场深处"
        powered->"5/5 动力已接通！点击右后方大门完成第三关"
        seated->"5/5 点击前排右侧旋钮，让核心端面的标记对准上方刻线"
        docked->"5/5 接驳动力：点击动力座，把核心送入轴承座"
        atBranch->"4/5 进入了维修支线：点击回退机关，再重新选择路线"
        atJunction->"4/5 核对道岔与动力座的圆环图案，再点击小车继续"
        trackReady->"4/5 选择路线：点击前排中间的圆环按钮切换道岔，再点击小车；岔口会自动停下"
        loaded->"3/5 修复断轨：点击中央轨道盘，让轨道两端接通"
        near(cart,LOAD)->"2/5 点击架子上的核心，或前排发亮的蓝色圆环按钮装载"
        unlocked->"2/5 装载核心：点击小车，把空车推到装载标记处"
        else->"1/5 配对解锁：观察车锁图案，点击相同图案的按钮"
    }
    companion object {
        val START=WalkPoint(-.98f,-.62f)
        val ORIGIN=WalkPoint(-.84f,-1.20f)
        val LOAD=WalkPoint(-.55f,-1.20f)
        val SAFE_LINE=WalkPoint(-.28f,-1.20f)
        val JUNCTION=WalkPoint(.35f,-1.20f)
        val BRANCH=WalkPoint(.35f,-1.78f)
        val DOCK=WalkPoint(.88f,-1.20f)
        val CRADLE=WalkPoint(.88f,-1.72f)
        val EXIT=WalkPoint(1.10f,-1.54f)
        val LOCK_APPROACH=WalkPoint(-1.12f,-1.20f)
        fun distance(a: WalkPoint,b: WalkPoint)=hypot(a.x-b.x,a.z-b.z)
        private fun near(a: WalkPoint,b: WalkPoint)=distance(a,b)<.02f
        private fun length(p: List<WalkPoint>)=(1 until p.size).sumOf { distance(p[it-1],p[it]).toDouble() }.toFloat()
        private fun sample(p: List<WalkPoint>,at: Float): WalkPoint {
            var rest=at
            for(i in 1 until p.size) {
                val a=p[i-1];val b=p[i];val d=distance(a,b)
                if(rest<=d && d>0f) return WalkPoint(a.x+(b.x-a.x)*rest/d,a.z+(b.z-a.z)*rest/d)
                rest-=d
            };return p.last()
        }
    }
}
