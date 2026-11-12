package com.example.woodenpuppetdemo.content

import com.pico.spatial.core.math.Quat
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.tracking.hand.HandJoint
import com.pico.spatial.tracking.hand.HandPose

/** Repeatable input sequence for emulator rendering; never used on the Swan hardware. */
internal class EmulatorGaitPreview {
    private var startedAtMs: Long? = null
    private val directions = listOf(
        StepVector(0f, -0.05f), StepVector(0f, -0.05f),
        StepVector(-0.05f, 0f), StepVector(-0.05f, 0f),
        StepVector(0f, 0.05f), StepVector(0f, 0.05f),
        StepVector(0.05f, 0f), StepVector(0.05f, 0f),
    )

    fun sample(nowMs: Long): Pair<HandPose, HandPose> {
        val started = startedAtMs ?: nowMs.also { startedAtMs = it }
        val elapsed = (nowMs - started) % CYCLE_MS
        val gaitTime = elapsed - NEUTRAL_MS
        val segment = if (gaitTime >= 0) (gaitTime / STEP_MS).toInt() else -1
        val within = if (segment in directions.indices) gaitTime % STEP_MS else 0L
        val amount = when {
            segment !in directions.indices -> 0f
            within < 300L -> within / 300f
            within < 650L -> 1f
            within < 900L -> (900L - within) / 250f
            else -> 0f
        }
        val move = directions.getOrNull(segment) ?: StepVector(0f, 0f)
        val leftMove = if (segment % 2 == 0) move else StepVector(0f, 0f)
        val rightMove = if (segment % 2 == 1) move else StepVector(0f, 0f)
        return hand(true, leftMove, amount) to hand(false, rightMove, amount)
    }

    private fun hand(left: Boolean, move: StepVector, amount: Float): HandPose {
        val palm = Vector3(if (left) 0.18f else -0.18f, 1.36f, -0.74f)
        val index = Vector3(palm.x, palm.y - 0.08f, palm.z - 0.06f)
        val middle = Vector3(palm.x + move.x * amount,
            palm.y - 0.08f, palm.z - 0.07f + move.z * amount)
        return HandPose(listOf(
            HandJoint(palm, Quat(), HandJoint.Index.PALM),
            HandJoint(index, Quat(), HandJoint.Index.INDEX_TIP),
            HandJoint(middle, Quat(), HandJoint.Index.MIDDLE_TIP),
        ))
    }

    private companion object {
        const val NEUTRAL_MS = 1200L
        const val STEP_MS = 1200L
        const val CYCLE_MS = NEUTRAL_MS + STEP_MS * 8L + 1200L
    }
}
