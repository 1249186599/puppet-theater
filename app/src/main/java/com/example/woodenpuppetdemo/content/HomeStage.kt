package com.example.woodenpuppetdemo.content

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.ViewCoordinateSpace
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.ui.foundation.gesture.detectSpatialPointerEvent
import com.pico.spatial.core.ecs.DirectionalLightComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelEntity
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.animation.AnimationPlayConfig
import com.pico.spatial.core.ecs.animation.AnimationPlaybackController
import com.pico.spatial.core.ecs.animation.AnimationTransitionMode
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.foundation.content.SpatialView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

private const val TAG = "WoodenPuppet"
private const val PUPPET_SCALE = 0.55f
private const val FLOOR_Y = 0.531f
private const val LOOP_MS = 33L
private const val ARM_REST_DEGREES = -74f
private const val ARM_WALK_SWING_DEGREES = 6f

private class PuppetStageScene(private val context: android.content.Context) {
    val programGrabber = ProgramGrabber()
    var programHeld by mutableStateOf(false)
        private set
    private val audio = AdventureAudio(context)
    private val soundEvents = AdventureSoundEvents()
    private val walkSounds = WalkSoundTiming()
    private val level = AdventureLevel()
    private val theatre = TheatreLevel()
    private val cargo = CargoLevel()
    private val wind = WindLevel()
    private val finale = FinaleLevel()
    private val finaleStrings = FinaleStrings()
    var finaleProps: FinaleProps? = null
    var fifthRoot: Entity? = null
    private var finaleFeedback=0
    private var finaleErrors=0
    private var finaleWasComplete=false
    var finaleTask by mutableStateOf("按图案踩下三个踏板")
        private set
    var windProps: WindProps? = null
    var fourthRoot: Entity? = null
    var cargoProps: CargoProps? = null
    var thirdRoot: Entity? = null
    private val theatreSounds = TheatreSoundEvents()
    var theatreProps: TheatreProps? = null
    var firstRoot: Entity? = null
    var secondRoot: Entity? = null
    var instructions: Entity? = null
    private var riverMotion: RiverMotion? = null
    fun attachRiver(entity:Entity) {riverMotion=RiverMotion(entity)}
    var chapter by mutableStateOf(1)
        private set
    var completed by mutableStateOf(false)
        private set
    val title get() = when(chapter) {1->"第一关 · 打开剧院大门";2->"第二关 · 修复八音盒剧场";3->"第三关 · 运送发条核心";4->"第四关 · 风车工坊";else->"第五关 · 木偶的最后一幕"}
    var programStep by mutableStateOf(1)
        private set
    val programTask get() = if(completed) "这一幕，圆满落幕" else when(chapter) {
        1 -> listOf("踩下蓝色踏板", "转动木桥", "取回金色钥匙", "打开剧院大门")[programStep-1]
        2 -> listOf("找到木齿轮", "修复动力箱", "搭乘升降台", "打开上层大门")[programStep-1]
        3 -> listOf("解开小车锁扣", "装载发条核心", "修复断开的轨道", "选对运送路线", "唤醒发条核心")[programStep-1]
        4 -> windTask
        else -> finaleTask
    }
    private fun updateProgramStep() {
        programStep=when(chapter) {
            1 -> when { !level.unlocked->1; !level.bridgeReady->2; !level.hasKey->3; else->4 }
            2 -> when { !theatre.hasGear->1; !theatre.powered->2; !theatre.upstairs->3; else->4 }
            3 -> cargo.stage
            4 -> wind.stage
            else -> finale.stage
        }
    }
    fun placeProgramForInitialLoad() = placeProgram()
    private fun placeProgram() {
        if(programGrabber.motion.placed) return
        instructions?.components?.get(TransformComponent::class.java)?.apply {
            setPosition(Vector3(if(chapter<=2)-1.12f else -1.72f,1.10f,-1.40f))
            setScaleVector(Vector3(1.30f))
        }
    }
    fun nextLevel() {
        if(!completed || chapter>=5) return
        if(chapter==1 && theatreProps==null || chapter==2 && cargoProps==null || chapter==3 && windProps==null || chapter==4 && finaleProps==null) return
        enterChapter(chapter + 1)
    }
    fun returnToFirstLevel() {
        if (chapter != 5 || !completed || firstRoot == null) return
        finale.reset()
        finaleStrings.reset()
        enterChapter(1)
    }
    private fun enterChapter(target: Int) {
        chapter = target
        firstRoot?.enabled = target == 1
        secondRoot?.enabled = target == 2
        thirdRoot?.enabled = target == 3
        fourthRoot?.enabled = target == 4
        fifthRoot?.enabled = target == 5
        placeProgram()
        reset()
        Log.i(TAG,"chapterChanged chapter=$chapter")
    }
    // One-shot debug preview marker used for local visual QA; normal entry stays chapter one.
    fun consumeWindPreview(context: android.content.Context) {
        if(context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        val finaleMarker=java.io.File(context.filesDir,"preview-chapter5")
        if(finaleMarker.exists() && finaleProps!=null) {
            finaleMarker.delete();chapter=5
            firstRoot?.enabled=false;secondRoot?.enabled=false;thirdRoot?.enabled=false;fourthRoot?.enabled=false;fifthRoot?.enabled=true
            placeProgram();reset();Log.i(TAG,"finalePreviewReady chapter=5 stages=6");return
        }
        val thirdMarker=java.io.File(context.filesDir,"preview-chapter3")
        if(thirdMarker.exists() && cargoProps!=null) {
            thirdMarker.delete();chapter=3
            firstRoot?.enabled=false;secondRoot?.enabled=false;thirdRoot?.enabled=true;fourthRoot?.enabled=false
            placeProgram();reset();Log.i(TAG,"cargoPreviewReady chapter=3");return
        }
        val secondMarker=java.io.File(context.filesDir,"preview-chapter2")
        if(secondMarker.exists() && theatreProps!=null) {
            secondMarker.delete();chapter=2
            firstRoot?.enabled=false;secondRoot?.enabled=true;thirdRoot?.enabled=false;fourthRoot?.enabled=false
            placeProgram();reset();Log.i(TAG,"theatrePreviewReady chapter=2");return
        }
        val marker=java.io.File(context.filesDir,"preview-chapter4")
        if(!marker.exists() || windProps==null)return
        marker.delete();chapter=4
        firstRoot?.enabled=false;secondRoot?.enabled=false;thirdRoot?.enabled=false;fourthRoot?.enabled=true
        placeProgram()
        reset();Log.i(TAG,"windPreviewReady chapter=4")
    }
    val lampTextureResources=mutableListOf<com.pico.spatial.core.ecs.resource.TextureResource>()
    var props: AdventureProps? = null
    var windTask by mutableStateOf("让风车转起来")
        private set
    var windHelp by mutableStateOf(false)
        private set
    var status by mutableStateOf(level.message)
        private set
    fun applyStageLighting() {
        // Share the accepted chapter-four light across every chapter, including cold start.
        // Local -Z tilted -45 degrees lights upward boards and front-facing props together.
        val transform = light?.components?.get(TransformComponent::class.java) ?: return
        val lightPitch = -Math.PI.toFloat()/4f
        transform.setQuaternion(Quat(Vector3(1f,0f,0f),lightPitch))
        Log.i(TAG,"chapterLighting chapter=$chapter pitchRadians=$lightPitch intensity=800 shared=true")
    }
    fun reset() {
        applyStageLighting()
        audio.stopAll(); soundEvents.reset(); walkSounds.reset()
        props?.resetFeedback()
        cargoProps?.resetFeedback()
        if(chapter == 1) { level.reset(); status = level.message }
        else if(chapter==2) { theatre.reset(); theatreSounds.reset(); status=theatre.message }
        else if(chapter==3) { cargo.reset();status=cargo.message }
        else if(chapter==4) {wind.reset();status=wind.message;windTask=wind.taskLine;windHelp=false}
        else {finale.reset();finaleStrings.reset();finaleFeedback=0;finaleErrors=0;finaleWasComplete=false;status=finale.message;finaleTask=finale.task}
        completed=false
        updateProgramStep()
        moving=false
        destinationMarker?.enabled = false
        previousMs = null
        Log.i(TAG, "adventureReset")
    }
    fun click(entity: Entity?, position: Vector3) {
        if(programGrabber.blocksClicks) return
        if(chapter==5) {
            val action=finaleProps?.actions?.get(entity) ?: return
            val accepted=finale.select(action,WalkPoint(position.x,position.z))
            status=finale.message;finaleTask=finale.task
            destinationMarker?.enabled=false
            Log.i(TAG,"finaleAction action=$action accepted=$accepted stage=${finale.stage} message=$status")
            return
        }
        if(chapter==4) {
            val action=windProps?.actions?.get(entity) ?: return
            val accepted = wind.select(action,WalkPoint(position.x,position.z))
            if(accepted && (!action.startsWith("note") || wind.hung)) windProps?.press(action,wind.time)
            status=wind.message
            destinationMarker?.enabled=false
            Log.i(TAG,"windAction action=$action accepted=$accepted target=$position from=${wind.position} stage=${wind.stage} message=$status")
            return
        }
        if(chapter==3) {
            val action=cargoProps?.actions?.get(entity) ?: return
            cargoProps?.press(action,SystemClock.uptimeMillis()/1000f)
            cargo.select(action,WalkPoint(position.x,position.z))
            status=cargo.message
            destinationMarker?.enabled=false
            Log.i(TAG,"cargoAction action=$action stage=${cargo.stage} message=$status")
            return
        }
        if(chapter == 2) {
            val action=theatreProps?.actions?.get(entity) ?: return
            if(theatre.select(action,WalkPoint(position.x,position.z))) {
                destinationMarker?.apply {
                    enabled=true
                    components[TransformComponent::class.java]?.setPosition(Vector3(
                        theatre.target.x,FLOOR_Y+theatre.heightAt(theatre.target)+.012f,theatre.target.z))
                }
                Log.i(TAG,"theatreDestination action=$action target=${theatre.target}")
            }
            status=theatre.message
            return
        }
        when(props?.actions?.get(entity)) {
            "lever" -> {
                if (!level.unlocked && soundEvents.lockedClick(SystemClock.uptimeMillis())) {
                    audio.play(AdventureCue.LOCKED)
                    props?.flashLocked(SystemClock.uptimeMillis()/1000f)
                }
                level.turnBridge()
                soundEvents.collect(level).forEach { audio.play(it) }
                status = level.message
            }
            "pad" -> chooseDestination(AdventureLevel.PAD, "pad")
            "key" -> chooseDestination(AdventureLevel.KEY, "key")
            "exit" -> chooseDestination(AdventureLevel.EXIT, "exit")
            else -> if(entity == board) pointAt(position, true)
        }
    }
    fun accepts(entity: Entity?) = entity != null &&
        (if(chapter == 1) (entity == board || props?.actions?.containsKey(entity) == true)
            else if(chapter==2) theatreProps?.actions?.containsKey(entity) == true
            else if(chapter==3) cargoProps?.actions?.containsKey(entity) == true
            else if(chapter==4) windProps?.actions?.containsKey(entity) == true
            else finaleProps?.actions?.containsKey(entity) == true)
    private val clips = mutableMapOf<String, AnimationPlaybackController>()
    var root: Entity? = null
    var actor: Entity? = null
    var puppet: Entity? = null
    var board: Entity? = null
    var destinationMarker: ModelEntity? = null
    var light: Entity? = null
    private var previousMs: Long? = null
    private var moving = false

    var viewToStage: ((Vector3) -> Vector3)? = null

    fun pointAt(position: Vector3, pressed: Boolean) {
        if (!position.x.isFinite() || !position.z.isFinite()) return
        if (abs(position.x) > ClickToWalkMotion.STAGE_HALF_WIDTH + 0.01f ||
            abs(position.z - ClickToWalkMotion.STAGE_CENTER_Z) >
            ClickToWalkMotion.STAGE_HALF_DEPTH + 0.01f) {
            if (pressed) Log.w(TAG, "stagePointerOutside position=$position")
            return
        }
        if (pressed) chooseDestination(WalkPoint(position.x, position.z), "systemPointer")
    }

    private var debugCommandAt=0L
    // Debug-only local QA input. Uses the same scene click/reset/next handlers; never runs without a file.
    private fun consumeDebugCommand() {
        if(context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE == 0)return
        val now=SystemClock.uptimeMillis();if(now-debugCommandAt<250)return;debugCommandAt=now
        val file=java.io.File(context.filesDir,"finale-command");if(!file.exists())return
        val command=file.readText().trim();file.delete()
        when(command) {
            "reset"->reset()
            "next"->nextLevel()
            "return-first"->returnToFirstLevel()
            "qa-wind-hung"->{
                if(chapter==4) {
                    // Prepare the reported state through the normal rules, without changing assets.
                    wind.reset()
                    fun action(a:String){check(wind.select(a));repeat(3000){wind.advance(.033f)}}
                    for(a in listOf("scoop","scoop","power","ferry","chimes","ferry",
                        "power","wind1","wind2","hang"))action(a)
                    check(wind.hung && !wind.busy)
                    Log.i(TAG,"windQA preparedHung=true position=${wind.position}")
                }
            }
            "qa-transition-four-five"->{
                // Prepare a completed chapter-four rules state, then test the real Next transition.
                chapter=4;wind.reset()
                fun action(a:String){check(wind.select(a)){"QA wind rejected $a"};repeat(3000){wind.advance(.033f)}}
                action("scoop");action("scoop");action("power");action("ferry");action("chimes");action("ferry")
                action("power");action("wind1");action("wind2");action("hang")
                for(note in WindLevel.ORDER)action("note$note")
                action("exit");check(wind.completed);completed=true
                nextLevel()
                check(chapter==5 && fifthRoot?.enabled==true && fourthRoot?.enabled==false)
                Log.i(TAG,"finaleTransitionQA PASS from=4 to=5 preparedRulesState=true")
            }
            else->if(chapter==5) {
                val entity=finaleProps?.actions?.entries?.firstOrNull{it.value==command}?.key
                if(entity!=null)click(entity,Vector3(0f,FLOOR_Y,-.92f))
            } else if(chapter==4) {
                val parts=command.split(" ")
                val action=parts.first()
                if(action=="state") {
                    Log.i(TAG,"windQA state position=${wind.position} busy=${wind.busy} hung=${wind.hung} notes=${wind.notes} tuned=${wind.tuned} completed=${wind.completed}")
                } else {
                    val entity=windProps?.actions?.entries?.firstOrNull{it.value==action}?.key
                    val x=parts.getOrNull(1)?.toFloatOrNull()?:.05f
                    val z=parts.getOrNull(2)?.toFloatOrNull()?:-1.89f
                    if(entity!=null)click(entity,Vector3(x,FLOOR_Y+WindLevel.DECK_HEIGHT,z))
                }
            } else if(chapter==3) {
                val entity=cargoProps?.actions?.entries?.firstOrNull{it.value==command}?.key
                if(entity!=null)click(entity,Vector3(-.55f,FLOOR_Y,-1.20f))
                Log.i(TAG,"cargoQA command=$command loaded=${cargo.loaded} transfer=${cargo.transferKind} stage=${cargo.stage}")
            }
        }
        Log.i(TAG,"finaleQA command=$command chapter=$chapter stage=${finale.stage} busy=${finale.busy} practice=${finale.practice} performance=${finale.performance} completed=$completed")
    }
    fun tick() {
        consumeDebugCommand()
        root?.let { r -> instructions?.let { p -> programGrabber.update(r,p) } }
        programHeld=programGrabber.blocksClicks
        audio.tick()
        val actor = actor ?: return
        val now = SystemClock.uptimeMillis()
        val seconds = previousMs?.let { (now - it) / 1000f } ?: 0f
        previousMs = now
        if(chapter==5 && finale.stringsActive && !programGrabber.blocksClicks) {
            root?.let { finaleStrings.update(programGrabber.stringHandSamples(it),seconds) }?.let { finale.setTrackedPull(it.first,it.second) }
        } else finaleStrings.reset()
        val frame = when(chapter) {1->level.advance(seconds);2->theatre.advance(seconds);3->cargo.advance(seconds);4->wind.advance(seconds);else->finale.advance(seconds)}
        if(chapter == 1) {
            riverMotion?.update(now/1000f)
            soundEvents.collect(level).forEach { audio.play(it) }
            props?.update(level,now/1000f)
        } else if(chapter==2) {
            theatreSounds.collect(theatre).forEach { audio.play(it) }
            theatreProps?.update(theatre,now/1000f)
            if(!theatre.rising) audio.stopLift()
        } else if(chapter==3) {
            cargo.drainCues().forEach { audio.play(it) }
            audio.cargoLoops(cargo.pushing || cargo.rewinding,cargo.powered && !cargo.completed)
            cargoProps?.update(cargo,now/1000f)
        } else if(chapter==4) {
            wind.drainCues().forEach {audio.play(it)}
            windProps?.update(wind,now/1000f)
            audio.windLoops(windProps?.airflowOn==true,wind.riding)
            wind.consumeHintNote()?.let { audio.play(listOf(AdventureCue.CHIME_LOW,AdventureCue.CHIME_MID,AdventureCue.CHIME_HIGH)[it],.20f) }
            windTask=wind.taskLine;windHelp=wind.helpVisible
        } else {
            finaleProps?.update(finale,now/1000f)
            finaleTask=finale.task
            if(finale.feedbackVersion!=finaleFeedback){finaleFeedback=finale.feedbackVersion;audio.play(AdventureCue.BRIDGE_READY,.40f)}
            if(finale.errorVersion!=finaleErrors){finaleErrors=finale.errorVersion;audio.play(AdventureCue.LOCKED,.35f)}
            if(finale.completed && !finaleWasComplete)audio.play(AdventureCue.FINALE_OVATION,.85f)
            finaleWasComplete=finale.completed
        }
        updateProgramStep()
        val newStatus=when(chapter) {1->level.message;2->theatre.message;3->cargo.message;4->wind.message;else->finale.message}
        completed=when(chapter) {1->level.completed;2->theatre.completed;3->cargo.completed;4->wind.completed;else->finale.completed}
        if(status != newStatus) {
            status=newStatus
            Log.i(TAG,"adventureState chapter=$chapter won=$completed message=$status")
        }
        if(completed || (chapter == 2 && theatre.rising)) destinationMarker?.enabled=false
        val elevation=if(chapter == 1) BridgeSurface.height(frame.position.x,frame.position.z,level.bridgeReady)+
            (props?.plateHeight(frame.position.x,frame.position.z) ?: 0f) else if(chapter==2) theatre.heightAt(frame.position) else if(chapter==4) wind.elevation else if(chapter==5) finale.elevation else 0f
        walkSounds.update(frame.moving, frame.phaseRadians)?.let { audio.play(it) }
        if (!frame.moving) audio.stopSteps()
        val stride = if(chapter==5) finale.walkStride else if (frame.moving) sin(frame.phaseRadians) else 0f
        actor.components[TransformComponent::class.java]?.apply {
            setPosition(Vector3(frame.position.x,
                FLOOR_Y + elevation +
                    (if (frame.moving) abs(stride) * 0.006f else 0f),
                frame.position.z))
            setQuaternion(if(chapter==5 && finale.bow>0f) Quat(Vector3(1f,0f,0f),finale.bow*.24f)
                else Quat(Vector3(0f, 1f, 0f), frame.yawRadians))
        }
        setClip("LeftUpLegSwing", stride * 22f, 55f)
        setClip("RightUpLegSwing", -stride * 22f, 55f)
        setClip("LeftUpLegFlex", max(0f, -stride) * 32f, 110f)
        setClip("RightUpLegFlex", max(0f, stride) * 32f, 110f)
        setClip("LeftArmSwing", ARM_REST_DEGREES - stride * ARM_WALK_SWING_DEGREES, 110f)
        setClip("RightArmSwing", ARM_REST_DEGREES + stride * ARM_WALK_SWING_DEGREES, 110f)
        // Pull clips raise the wrist vertically using shoulder + elbow articulation.
        // Keep the accepted front/back walking swing neutral during string control.
        val pulling = chapter==5 && (finale.stringsActive || (finale.performing && !finale.completed))
        setClip("LeftArmPull",if(pulling) finale.leftPull else 0f,1f)
        setClip("RightArmPull",if(pulling) finale.rightPull else 0f,1f)
        if (moving && !frame.moving) Log.i(TAG, "arrived x=${frame.position.x} z=${frame.position.z}")
        moving = frame.moving
    }

    fun chooseDestination(point: WalkPoint, source: String) {
        if(!level.select(point)) { status = level.message; return }
        status = level.message
        val target = level.target
        destinationMarker?.apply {
            enabled = true
            components[TransformComponent::class.java]?.setPosition(
                Vector3(target.x, FLOOR_Y + 0.01f, target.z))
        }
        Log.i(TAG, "destination x=${target.x} z=${target.z} source=$source")
    }

    private fun setClip(name: String, degrees: Float, limit: Float) {
        clips[name]?.takeIf { it.valid }?.setTime(1f + degrees / limit * 0.98f)
    }

    fun loadClips(model: Entity) {
        val mesh = model.findSkinnedMeshEntity().firstOrNull()
        val resources = mesh?.getAnimationResources().orEmpty()
        Log.i(TAG, "animations=${resources.joinToString { it.getName() }}")
        val config = AnimationPlayConfig(transitionMode = AnimationTransitionMode.COMPOSE)
        for (name in listOf("LeftUpLegSwing", "RightUpLegSwing", "LeftUpLegFlex",
            "RightUpLegFlex", "LeftArmSwing", "RightArmSwing", "LeftArmPull", "RightArmPull")) {
            val resource = resources.firstOrNull { it.getName() == name }
            if (mesh != null && resource != null) {
                clips[name] = mesh.playAnimation(resource, config).apply {
                    pause()
                    setTime(if (name.endsWith("ArmSwing"))
                        1f + ARM_REST_DEGREES / 110f * 0.98f else 1f)
                }
            }
            Log.i(TAG, "walkClip $name valid=${clips[name]?.valid}")
        }
    }

    fun destroy() {
        programGrabber.stop()
        audio.release()
        clips.values.forEach { it.close() }
        windProps?.release()
        viewToStage = null
        root?.destroy(true)
        lampTextureResources.forEach { it.close() }
        lampTextureResources.clear()
        light?.destroy(true)
    }
}

@Composable
fun HomeStage() {
    val context = LocalContext.current
    val scene = remember { PuppetStageScene(context.applicationContext) }
    DisposableEffect(scene) {
        scene.programGrabber.start()
        onDispose { scene.destroy() }
    }
    LaunchedEffect(scene) {
        while (isActive) {
            if (scene.actor != null) scene.tick()
            delay(LOOP_MS)
        }
    }

    SpatialView(
        modifier = Modifier.pointerInput(scene, context) {
            detectSpatialPointerEvent(context = context) { events ->
                var handled = false
                for (event in events) {
                    if (scene.accepts(event.targetedEntity)) {
                        val position = scene.viewToStage?.invoke(
                            Vector3(event.x, event.y, event.z))
                        if (position != null) {
                            if(event.isDownEvent()) scene.click(event.targetedEntity, position)
                            if (event.isDownEvent()) Log.i(TAG,
                                "stagePointer kind=${event.kind} view=${event.position3D} stage=$position")
                            handled = true
                        }
                    }
                }
                handled
            }
        },
        attachments = {
            AttachmentPanel("instructions") {
                ProgramPanel(
                    chapter = scene.chapter,
                    task = scene.programTask,
                    detail = scene.status,
                    step = scene.programStep,
                    total = if(scene.chapter <= 2) 4 else if(scene.chapter==5) 6 else 5,
                    completed = scene.completed,
                    autoHelp = scene.chapter == 5 || (scene.chapter == 4 && scene.windHelp),
                    grabbed = scene.programHeld,
                    onMeasured = { w,h -> scene.programGrabber.measure(w,h) },
                    emulatorInput = scene.programGrabber.emulatorInput,
                    onPreviewDrag = { phase, delta -> scene.instructions?.let { scene.programGrabber.previewDrag(it,phase,delta) } },
                    onReset = { if(!scene.programGrabber.blocksClicks) scene.reset() },
                    onNext = { if(!scene.programGrabber.blocksClicks) scene.nextLevel() },
                    onReturnToFirst = { if(!scene.programGrabber.blocksClicks) scene.returnToFirstLevel() }
                )
            }
        },
        initial = { content, attachments ->
            val root = Entity()
            val actor = Entity()
            root.addChild(actor)
            val puppet = withContext(Dispatchers.IO) { Entity.load("asset://puppet.glb") }
            actor.addChild(puppet)
            content.addEntity(root)
            scene.viewToStage = { position ->
                content.convertPosition(position, ViewCoordinateSpace.Local,
                    content.localSpatialCoordinateSpace)
            }
            scene.root = root
            scene.actor = actor
            scene.puppet = puppet
            val light = Entity().apply {
                components.set(DirectionalLightComponent(Color4.WHITE, 800f))
            }
            content.addEntity(light)
            scene.light = light
            scene.applyStageLighting()
            val mesh = puppet.findSkinnedMeshEntity().firstOrNull()
            val bounds = mesh?.getVisualBounds(puppet)
            if (bounds != null) {
                puppet.components[TransformComponent::class.java]?.apply {
                    setScaleVector(Vector3(PUPPET_SCALE))
                    setPosition(Vector3(-bounds.center.x * PUPPET_SCALE,
                        -bounds.min.y * PUPPET_SCALE,
                        -bounds.center.z * PUPPET_SCALE))
                }
                Log.i(TAG, "modelBounds min=${bounds.min} max=${bounds.max} scale=$PUPPET_SCALE")
            } else Log.e(TAG, "puppetVisualBoundsMissing")
            actor.components[TransformComponent::class.java]?.setPosition(
                Vector3(AdventureLevel.START.x, FLOOR_Y, AdventureLevel.START.z))
            scene.loadClips(puppet)
            val firstRoot=Entity()
            root.addChild(firstRoot);scene.firstRoot=firstRoot
            val targetMaterial = UnlitMaterial.create().apply {
                setBaseColor(Color4(1f, 0.72f, 0.28f, 1f))
            }
            // Keep the accepted floor hit area, without the old teal visual slab filling the new recess.
            scene.board = Entity().apply {
                components.set(CollisionComponent(
                    collisionShape = listOf(ShapeResource.createBox(Vector3(
                        ClickToWalkMotion.STAGE_HALF_WIDTH * 2f, 0.012f,
                        ClickToWalkMotion.STAGE_HALF_DEPTH * 2f))),
                    physicsMaterial = PhysicsMaterialResource()))
                components.set(InteractableComponent())
                components[TransformComponent::class.java]?.setPosition(
                    Vector3(0f, FLOOR_Y - 0.008f, ClickToWalkMotion.STAGE_CENTER_Z))
                firstRoot.addChild(this)
            }
            scene.destinationMarker = ModelEntity(
                mesh = MeshResource.createCylinder(0.025f, 0.004f),
                material = targetMaterial).apply {
                enabled = false
                root.addChild(this)
            }
            val keyModel = withContext(Dispatchers.IO) { Entity.load("asset://key.glb") }
            val bridgeModel = withContext(Dispatchers.IO) { Entity.load("asset://bridge.glb") }
            val leverModel = withContext(Dispatchers.IO) { Entity.load("asset://lever_base.glb") }
            val leverHandleModel = withContext(Dispatchers.IO) { Entity.load("asset://lever_handle.glb") }
            val plateModel = withContext(Dispatchers.IO) { Entity.load("asset://plate.glb") }
            val doorFrameModel = withContext(Dispatchers.IO) { Entity.load("asset://door_frame.glb") }
            val doorPanelModel = withContext(Dispatchers.IO) { Entity.load("asset://door_panel.glb") }
            val surfaces = withContext(Dispatchers.IO) { Entity.load("asset://chapter1_river_fitted.glb") }
            firstRoot.addChild(surfaces)
            val riverSource = surfaces.getVisualBounds(firstRoot, recursive = true, enabledOnly = false)
            check(!riverSource.isEmpty()) { "Chapter1 river bounds missing" }
            // Native geometry is fitted to the channel; keep the animated mesh at unit scale.
            surfaces.components[TransformComponent::class.java]?.setPosition(Vector3(0f,FLOOR_Y-.012f,AdventureLevel.CENTER_Z))
            val riverPlaced = surfaces.getVisualBounds(firstRoot, recursive = true, enabledOnly = false)
            Log.i(TAG,"chapter1RiverMeasured min=${riverPlaced.min} max=${riverPlaced.max}")
            check(kotlin.math.abs(riverPlaced.center.z-AdventureLevel.CENTER_Z)<.002f &&
                riverPlaced.min.x >= -.778f && riverPlaced.max.x <= .778f &&
                riverPlaced.min.z >= AdventureLevel.CENTER_Z-.152f &&
                riverPlaced.max.z <= AdventureLevel.CENTER_Z+.152f)
            scene.attachRiver(surfaces)
            Log.i(TAG, "chapter1RiverFitReady separateAsset=true min=${riverPlaced.min} max=${riverPlaced.max}")
            val groundModel=withContext(Dispatchers.IO) {Entity.load("asset://chapter1_ground_dry_pbr.glb")}
            val lampModels=mutableMapOf<String,List<Entity>>()
            val lampTextures=mutableMapOf<String,com.pico.spatial.core.ecs.resource.TextureResource>()
            for((kind,count) in listOf("square" to 7,"round" to 2,"edge" to 12)) {
                lampModels[kind]=withContext(Dispatchers.IO) {List(count) {Entity.load("asset://lamp_$kind.glb")}}
                for(suffix in listOf("lens","neutral")) {
                    val name="${kind}_$suffix"
                    lampTextures[name]=withContext(Dispatchers.IO) {com.pico.spatial.core.ecs.resource.TextureResource.load("lamp_$name.png").also {it.toGlobal()}}
                    scene.lampTextureResources.add(lampTextures.getValue(name))
                }
            }
            val lamps=TheatreLamps(lampModels,lampTextures)
            scene.props = AdventureProps(firstRoot, FLOOR_Y, keyModel, bridgeModel, leverModel, plateModel,
                doorFrameModel, doorPanelModel, groundModel, leverHandleModel, lamps)
            Log.i(TAG, "doorModelsLoaded frameHeight=0.70 panelHeight=0.55 pivot=left frameFixed=true")
            Log.i(TAG, "plateModelLoaded width=0.26 thickness=0.020 press=0.006 statusRim=true")
            Log.i(TAG, "leverModelLoaded approvedShared=true base=0.24x0.045x0.116 hingeY=0.061 movingHandle=true pivotZ=true")
            Log.i(TAG, "bridgeModelLoaded length=0.50 width=0.351 deckLift=0.065 ramps=true flattened=true")
            Log.i(TAG, "ornateKeyLoaded height=0.147 headUp=true")
            scene.props?.update(AdventureLevel(), 0f)
            val secondRoot=Entity().apply { enabled=false }
            root.addChild(secondRoot);scene.secondRoot=secondRoot
            val gearModel=withContext(Dispatchers.IO) { Entity.load("asset://theatre_gear.glb") }
            val boxModel=withContext(Dispatchers.IO) { Entity.load("asset://theatre_gearbox.glb") }
            val liftModel=withContext(Dispatchers.IO) { Entity.load("asset://theatre_lift.glb") }
            val secondFrame=withContext(Dispatchers.IO) { Entity.load("asset://door_frame.glb") }
            val secondLeaf=withContext(Dispatchers.IO) { Entity.load("asset://door_panel.glb") }
            val secondSurfaces=withContext(Dispatchers.IO) { Entity.load("asset://theatre_surfaces.glb") }
            val secondRamp=withContext(Dispatchers.IO) { Entity.load("asset://theatre_ramp.glb") }
            val secondPerimeter=withContext(Dispatchers.IO) { Entity.load("asset://theatre_perimeter.glb") }
            val secondGateBar=withContext(Dispatchers.IO) { Entity.load("asset://theatre_gate_bar.glb") }
            val secondGateMounts=withContext(Dispatchers.IO) { Entity.load("asset://theatre_gate_mounts.glb") }
            val secondRails=mutableMapOf<String,Entity>()
            for(name in listOf("moving_left","moving_right","fixed_guides","landing_left","landing_right")) {
                secondRails[name]=withContext(Dispatchers.IO) { Entity.load("asset://theatre_rail_$name.glb") }
            }
            scene.theatreProps=TheatreProps(secondRoot,FLOOR_Y,gearModel,boxModel,liftModel,
                secondFrame,secondLeaf,secondSurfaces,secondRamp,secondPerimeter,secondGateBar,secondGateMounts,secondRails,lamps)
            scene.theatreProps?.update(TheatreLevel(),0f)
            Log.i(TAG,"theatreReady gear=true gearbox=true lift=true rise=0.32 craftedRailings=5 craftedPerimeter=true hingedGate=true")
            val thirdRoot=Entity().apply {enabled=false}
            root.addChild(thirdRoot);scene.thirdRoot=thirdRoot
            val cartModel=withContext(Dispatchers.IO) {Entity.load("asset://cargo_cart.glb")}
            val coreModel=withContext(Dispatchers.IO) {Entity.load("asset://cargo_core.glb")}
            val cradleModel=withContext(Dispatchers.IO) {Entity.load("asset://cargo_cradle.glb")}
            val cargoFrame=withContext(Dispatchers.IO) {Entity.load("asset://door_frame.glb")}
            val cargoLeaf=withContext(Dispatchers.IO) {Entity.load("asset://door_panel.glb")}
            val cargoFloor=withContext(Dispatchers.IO) {Entity.load("asset://chapter3_ground_pbr.glb")}
            val cargoTracks=withContext(Dispatchers.IO) {Entity.load("asset://cargo_tracks.glb")}
            val cargoTrackDisc=withContext(Dispatchers.IO) {Entity.load("asset://cargo_track_disc.glb")}
            val cargoSupportBox=withContext(Dispatchers.IO) {Entity.load("asset://chapter3_support_box_pbr.glb")}
            val cargoRotorBase=withContext(Dispatchers.IO) {Entity.load("asset://chapter3_rotor_base_pbr.glb")}
            val cargoArtModels=mutableMapOf<String,Entity>()
            for(name in listOf("01_symbol_keys","02_track_lock","03_load_return","04_route_selector","05_alignment_dial","06_progress_lamps","07_loading_clamps","08_dock_alignment","09_floor_marks")) {
                cargoArtModels[name]=withContext(Dispatchers.IO) {Entity.load("asset://cargo_art_$name.glb")}
            }
            val cargoArt=CargoArt(cargoArtModels,thirdRoot)
            scene.cargoProps=CargoProps(thirdRoot,FLOOR_Y,cartModel,coreModel,cradleModel,cargoFrame,cargoLeaf,cargoFloor,cargoTracks,cargoTrackDisc,cargoSupportBox,cargoRotorBase,cargoArt)
            scene.cargoProps?.update(CargoLevel(),0f)
            Log.i(TAG,"cargoReady stages=5 cart=true core=true cradle=true texturedTracks=true trackDisc=true")
            val fourthRoot=Entity().apply {enabled=false};root.addChild(fourthRoot);scene.fourthRoot=fourthRoot
            val windAssets=mapOf("floor" to "wind_floor", "walkway" to "wind_walkway", "river" to "wind_river", "housing" to "wind_housing", "rotor" to "wind_rotor",
                "scoop" to "wind_scoop_nozzle", "scoopBase" to "wind_scoop_base", "scoopRibbon" to "wind_scoop_ribbon",
                "ferry" to "wind_ferry", "shield0" to "wind_shield", "shield1" to "wind_shield",
                "chimeBase" to "wind_chime_base", "frame" to "wind_frame", "tube0" to "wind_tube", "tube1" to "wind_tube", "tube2" to "wind_tube",
                "lever" to "lever_base", "handle" to "lever_handle", "doorframe" to "door_frame", "doorleaf" to "door_panel",
                "powerPanel" to "wind_control_power", "ferryLights" to "wind_control_ferry_lights",
                "dockNear" to "wind_control_dock", "dockIsland" to "wind_control_dock",
                "gateKey0" to "wind_control_gate1", "gateKey1" to "wind_control_gate2",
                "noteKey0" to "wind_control_note1", "noteKey1" to "wind_control_note3", "noteKey2" to "wind_control_note2")
            val windModels=mutableMapOf<String,Entity>()
            for((name,asset) in windAssets) windModels[name]=withContext(Dispatchers.IO) {Entity.load("asset://$asset.glb")}
            scene.windProps=WindProps(fourthRoot,FLOOR_Y,windModels)
            scene.windProps?.update(WindLevel(),0f)
            Log.i(TAG,"windReady stages=5 models=${windModels.size} ferry=true shields=2 chimes=3 craftedScoop=true craftedRiver=true craftedControls=true")
            val fifthRoot=Entity().apply { enabled=false };root.addChild(fifthRoot);scene.fifthRoot=fifthRoot
            val finaleStageModel=withContext(Dispatchers.IO) {Entity.load("asset://final_theatre_stage_pbr_v2.glb")}
            val finaleArt=withContext(Dispatchers.IO) { Entity.load("asset://chapter5_approved_props.glb") }
            scene.finaleProps=FinaleProps(fifthRoot,FLOOR_Y,finaleStageModel,finaleArt)
            Log.i(TAG,"finaleReady stages=6 stagePbrV2=true approvedMechanisms=true permanentBackdrop=true strings=true")
            attachments.entity("instructions")?.let { panel ->
                scene.instructions=panel
                scene.placeProgramForInitialLoad()
                root.addChild(panel)
            }
            scene.consumeWindPreview(context)
            Log.i(TAG, "adventureReady version=1")
            Log.i(TAG, "stageReady floorY=$FLOOR_Y centerZ=${ClickToWalkMotion.STAGE_CENTER_Z} halfWidth=${ClickToWalkMotion.STAGE_HALF_WIDTH} halfDepth=${ClickToWalkMotion.STAGE_HALF_DEPTH}")
        },
        update = { _, _ -> }
    )
}
