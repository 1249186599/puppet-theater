package com.example.woodenpuppetdemo.content

import kotlin.math.*

/** Pure chapter-five rules. Arrival commits physical tasks; gestures require a stable hold. */
internal class FinaleLevel {
    var position=START; private set
    private val route=ArrayDeque<WalkPoint>()
    private var intent=""
    private var phase=0f
    private var yaw=0f
    private var segmentTravelled=0f
    var walkStride=0f; private set
    var time=0f; private set
    var plates=0; private set
    var powered=false; private set
    val scenery=intArrayOf(1,2,3)
    var carrying=false; private set
    var delivered=false; private set
    var lampAim=0; private set
    var brightness=0; private set
    var lit=false; private set
    val locks=booleanArrayOf(false,false)
    var opening=false; private set
    var curtain=0f; private set
    var rehearsing=false; private set
    var practice=0; private set
    var performing=false; private set
    var performance=0; private set
    var completed=false; private set
    var leftPull=0f; private set
    var rightPull=0f; private set
    var gestureHold=0f; private set
    var bow=0f; private set
    var showTime=0f; private set
    var celebrationTime=0f; private set
    var feedbackVersion=0; private set
    var errorVersion=0; private set
    private var error=""
    private var errorUntil=0f
    var idle=0f; private set
    val busy get()=route.isNotEmpty() || intent.isNotEmpty() || opening || (performing && !completed)
    val sceneryReady get()=scenery.all{it==0}
    val stringsActive get()=rehearsing && !busy && !completed
    val stage get()=when { !powered->1;!delivered->2;!lit->3;curtain<1f->4;practice<3->5;else->6 }
    val task get()=when {
        completed->"木偶的演出，圆满落幕"
        performing->if(bow>0f) "向观众鞠躬谢幕" else "欣赏木偶的最后一场演出"
        busy->if(opening) "幕布正在打开" else "跟着木偶前往目标"
        stage==1->if(plates<3) "按图案踩下三个踏板" else "接通剧场电源"
        stage==2->if(!sceneryReady) "转动左侧的三个布景" else if(!carrying) "拿起星星道具" else "把星星送到舞台边"
        stage==3->"把灯光对准舞台星标"
        stage==4->if(locks.all{it}) "转动右后方卷扬机" else "打开舞台两侧的锁扣"
        stage==5->if(!rehearsing) "走上右前方练习踏垫" else "牵线练习：${poseName(practice)}"
        else->"试演完成，点击主舞台开始演出"
    }
    val message get()=when {
        time<errorUntil->error
        completed->"彩带与掌声送给小木偶，也送给帮助它的你。演出圆满结束！"
        performing->if(bow>0f) "木偶正在向你鞠躬，感谢你一路的帮助。" else "木偶会自己完成表演，无需再牵线。请欣赏它的演出。"
        busy->if(opening) "两侧锁扣已打开，幕布向两边展开。" else "木偶正在走向目标，请等它停下。"
        stage==1->if(plates<3) "左前方踏板顺序：星星 → 三角 → 圆形。点击图案，木偶走到后才算踩下。当前 ${plates}/3。" else "三个踏板已接通。点击左前方供电箱上的拨杆。"
        stage==2->if(!sceneryReady) "点击左侧布景底座，每次转四分之一圈。让三块图案朝向你，底座全部变绿。" else if(!carrying) "点击布景旁的小星星，让木偶把它拿起来。" else "点击舞台左前角的星形托座，放好道具。"
        stage==3->"点击右侧灯架下的左右箭头，将光斑移到舞台星标；点击灯头调亮。需要方向居中、亮度两格（当前 ${brightness}/2）。"
        stage==4->if(locks.all{it}) "点击右后方带摇柄的卷扬机，拉开前幕。" else "点击台框左、右两侧的锁扣。两个锁扣都变绿后，才能开幕。"
        stage==5 && !rehearsing->"点击右前方脚印踏垫。站定后，用旁边两个圆环练习控制木偶。"
        stage==6->"试演完成。点击主舞台，木偶会沿台阶走到中央，自动为你表演。"
        else->"${poseInstruction(practice)}\n手势：捏合抓线，向上提约 12 厘米；松开即放下。也可点击左、右圆环切换提起/放下。动作保持半秒。"
    }
    val suggestedActions get():Set<String> = if(busy || completed) emptySet() else when(stage) {
        1->setOf(if(plates<3) "plate$plates" else "power")
        2->if(!sceneryReady) scenery.indices.filter{scenery[it]!=0}.map{"scenery$it"}.toSet() else setOf(if(carrying)"dock" else "prop")
        3->setOf("aimLeft","aimRight","brightness")
        4->if(locks.all{it}) setOf("winch") else locks.indices.filter{!locks[it]}.map{"lock$it"}.toSet()
        5->if(rehearsing)setOf("pullLeft","pullRight") else setOf("practice")
        else->setOf("perform")
    }
    var elevation=0f; private set
    private fun supportHeight(p:WalkPoint)=max(stageHeightAt(p),PLATES.maxOf { .025f*smooth(((.12f-hypot(it.x-p.x,it.z-p.z))/.04f).coerceIn(0f,1f)) })
    fun reset() {
        position=START;route.clear();intent="";phase=0f;yaw=0f;segmentTravelled=0f;walkStride=0f;elevation=0f;time=0f;plates=0;powered=false
        scenery[0]=1;scenery[1]=2;scenery[2]=3;carrying=false;delivered=false;lampAim=0;brightness=0;lit=false
        locks.fill(false);opening=false;curtain=0f;rehearsing=false;practice=0;performing=false;performance=0;completed=false
        leftPull=0f;rightPull=0f;gestureHold=0f;bow=0f;showTime=0f;celebrationTime=0f;feedbackVersion=0;errorVersion=0;error="";errorUntil=0f;idle=0f
    }
    private fun reject(text:String):Boolean {error=text;errorUntil=time+3f;errorVersion++;return false}
    private fun go(action:String,vararg points:WalkPoint) {intent=action;route.clear();route.addAll(points.toList());segmentTravelled=0f;phase=0f;walkStride=0f}
    private fun smooth(t:Float)=t*t*(3f-2f*t)
    private fun face(target:Float,dt:Float):Boolean {
        val delta=atan2(sin(target-yaw),cos(target-yaw));val step=8f*dt
        if(abs(delta)<=step+.0001f){yaw=target;return true}
        yaw+=delta.coerceIn(-step,step)
        yaw=atan2(sin(yaw),cos(yaw));return false
    }
    private fun success(){feedbackVersion++;errorUntil=0f;idle=0f}
    fun select(action:String,raw:WalkPoint?=null):Boolean {
        if(completed || busy)return false
        when(action) {
            "plate0","plate1","plate2"->{
                if(stage!=1 || plates==3)return false
                val i=action.last()-'0'
                if(i!=plates)return reject("按供电箱上的顺序：星星 → 三角 → 圆形。已踩对的踏板保留。")
                go(action,PLATES[i])
            }
            "power"->{if(plates!=3 || powered)return reject("先按星星、三角、圆形的顺序踩好三个踏板。")
                go(action,WalkPoint(-.86f,-.92f))}
            "scenery0","scenery1","scenery2"->{if(stage!=2 || carrying)return false
                val i=action.last()-'0';if(scenery[i]==0)return false
                scenery[i]=(scenery[i]+1)%4;success()}
            "prop"->{if(stage!=2 || !sceneryReady || carrying)return reject("先把三块布景转正，再搬运星星。")
                go(action,PROP_APPROACH)}
            "dock"->{if(stage!=2 || !carrying)return false
                go(action,DOCK_APPROACH)}
            "aimLeft","aimRight","brightness"->{if(stage!=3)return false
                if(action=="brightness")brightness=(brightness+1)%3
                else lampAim=(lampAim+if(action=="aimLeft")-1 else 1).coerceIn(0,4)
                lit=lampAim==2 && brightness==2;success()}
            "lock0","lock1"->{if(stage!=4)return false
                val i=action.last()-'0';if(locks[i])return false;locks[i]=true;success()}
            "winch"->{if(stage!=4 || !locks.all{it})return reject("先打开台框左右两个锁扣。")
                opening=true;success()}
            "practice"->{if(stage!=5 || rehearsing)return false
                go(action,PRACTICE)}
            "perform"->{if(stage!=6 || performing)return false
                rehearsing=false;leftPull=0f;rightPull=0f
                // Use the stair entrance, then one straight ascent to the star.
                go(action,WalkPoint(0f,-1.04f),PERFORM)}
            "pullLeft","pullRight"->{if(!stringsActive)return false
                if(action=="pullLeft")leftPull=if(leftPull>.5f)0f else 1f else rightPull=if(rightPull>.5f)0f else 1f
                idle=0f;errorUntil=0f}
            "floor"->{val q=raw?:return false
                if(stage==6 && stageHeightAt(q)>.1f)return select("perform")
                if(stringsActive || stage>=5 || !q.x.isFinite() || !q.z.isFinite() || q.x !in -.8f.. .75f || q.z !in -1.10f..-.48f)return false
                go(action,q)}
            else->return false
        }
        idle=0f;return true
    }
    fun setTrackedPull(left:Float,right:Float) {
        if(!stringsActive)return
        leftPull=if(left.isFinite())left.coerceIn(0f,1f) else 0f
        rightPull=if(right.isFinite())right.coerceIn(0f,1f) else 0f
    }
    fun advance(seconds:Float):WalkFrame {
        val dt=if(seconds.isFinite())seconds.coerceIn(0f,.05f) else 0f;time+=dt;idle+=dt
        if(opening){curtain=(curtain+dt/1.8f).coerceAtMost(1f);if(curtain>=1f){opening=false;success()}}
        var moving=false;walkStride=0f
        if(route.isNotEmpty()) {
            val dest=route.first();val dx=dest.x-position.x;val dz=dest.z-position.z;val distance=hypot(dx,dz)
            if(distance<.00001f){position=dest;route.removeFirst();segmentTravelled=0f;phase=0f}
            else if(face(atan2(dx,dz),dt)) {
                // Translation starts only once facing the segment; heading stays fixed while walking.
                val step=min(distance,.18f*dt);moving=step>0f
                position=if(step>=distance)dest else WalkPoint(position.x+dx/distance*step,position.z+dz/distance*step)
                segmentTravelled+=step
                phase=(phase+step/.12f*2f*Math.PI.toFloat())%(2f*Math.PI.toFloat())
                val gain=smooth(min(segmentTravelled/.06f,(distance-step)/.06f).coerceIn(0f,1f))
                walkStride=sin(phase)*gain
                if(step>=distance){route.removeFirst();segmentTravelled=0f;phase=0f;walkStride=0f}
            }
        }
        val support=supportHeight(position)
        // Lift a little before an ascending edge; bound vertical motion instead of teleporting the root.
        val anticipated=if(moving && route.isNotEmpty()) {
            val dest=route.first();val d=hypot(dest.x-position.x,dest.z-position.z)
            val lead=min(d,.025f)
            if(d>.00001f)stageHeightAt(WalkPoint(position.x+(dest.x-position.x)/d*lead,position.z+(dest.z-position.z)/d*lead)) else support
        } else support
        val heightTarget=max(support,anticipated)
        elevation+=(heightTarget-elevation).coerceIn(-.20f*dt,.20f*dt)
        val facingStation=if(route.isEmpty() && intent in listOf("practice","perform")) !moving && face(0f,dt) else true
        if(route.isEmpty() && intent.isNotEmpty() && facingStation && abs(elevation-support)<.0001f) {
            when(intent) {
                "plate0","plate1","plate2"->plates++
                "power"->powered=true
                "prop"->carrying=true
                "dock"->{carrying=false;delivered=true}
                "practice"->{rehearsing=true;leftPull=0f;rightPull=0f}
                "perform"->{performing=true;showTime=0f;leftPull=0f;rightPull=0f}
            }
            intent="";success()
        }
        if(stringsActive) {
            val index=practice
            val match=when(index){0->leftPull>.72f && rightPull<.2f;1->rightPull>.72f && leftPull<.2f;2->leftPull>.72f && rightPull>.72f;3->leftPull<.15f && rightPull<.15f;else->false}
            gestureHold=if(match)gestureHold+dt else 0f
            if(gestureHold>=.65f){gestureHold=0f;success()
                practice++;if(practice==3){rehearsing=false;leftPull=0f;rightPull=0f}}
        }
        if(completed)celebrationTime=(celebrationTime+dt).coerceAtMost(FinaleShow.CELEBRATION_DURATION)
        else if(performing) {
            showTime=(showTime+dt).coerceAtMost(FinaleShow.DURATION)
            val pose=FinaleShow.pose(showTime)
            leftPull=pose.left;rightPull=pose.right;walkStride=pose.stride;yaw=pose.yaw;bow=pose.bow
            performance=when{showTime<4f->1;showTime<10f->2;showTime<FinaleShow.DURATION->3;else->4}
            if(showTime>=FinaleShow.DURATION){completed=true;celebrationTime=0f;success()}
        }
        return WalkFrame(position,yaw,if(moving)phase else 0f,moving)
    }
    companion object {
        val START=WalkPoint(.1f,-.92f)
        val PLATES=listOf(WalkPoint(-.61f,-.65f),WalkPoint(-.28f,-.65f),WalkPoint(.05f,-.65f))
        val PROP_APPROACH=WalkPoint(-.68f,-1.10f)
        val DOCK_APPROACH=WalkPoint(-.55f,-1.27f)
        val PRACTICE=WalkPoint(.60f,-.77f)
        const val MODEL_FLOOR=.100f
        const val MODEL_CENTER_Z=-1.350f
        val PERFORM=WalkPoint(0f,MODEL_CENTER_Z-.290f)
        /** Actual deck/step footprints from the approved stage exporter; heights above its ground. */
        fun stageHeightAt(p:WalkPoint):Float {
            val z=p.z-MODEL_CENTER_Z
            if(abs(p.x)<=.99f && z>=-.79f && z<=-.23f+.235f*sqrt((1f-(p.x/.99f).pow(2)).coerceAtLeast(0f)))return .145f
            for(i in 2 downTo 0) {
                val half=(.86f-i*.045f)/2f
                val front=.215f-i*.065f-.035f+.035f*sqrt((1f-(p.x/half).pow(2)).coerceAtLeast(0f))
                if(abs(p.x)<=half && z>=-.045f && z<=front)return (i+1)*.145f/3f
            }
            return 0f
        }
        fun poseName(i:Int)=when(i){0->"提起左线";1->"提起右线";2->"两线一起提起";else->"松开两线"}
        fun poseInstruction(i:Int)=when(i){0->"只提左线，右线放下。";1->"放下左线，只提右线。";2->"保持右线提起，再提起左线。";else->"把左右两根线都放下。"}
    }
}
