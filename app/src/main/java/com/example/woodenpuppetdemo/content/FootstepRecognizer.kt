package com.example.woodenpuppetdemo.content

import kotlin.math.sqrt

internal data class StepVector(val x: Float, val z: Float)

/** One outward leg swing starts a step; both legs must return to rest before rearming. */
internal class FootstepRecognizer {
    private enum class Side { LEFT, RIGHT }
    private var leftRest: StepVector? = null
    private var rightRest: StepVector? = null
    private var previousLeft: StepVector? = null
    private var previousRight: StepVector? = null
    private var lastUpdateMs: Long? = null
    private var calibratingUntilMs = 0L
    private var armed = false
    private var restSinceMs: Long? = null
    private var candidate: Side? = null
    private var candidateSinceMs: Long? = null
    private var activeSide: Side? = null
    private var lastStepMs = Long.MIN_VALUE

    /** -1 leans toward the right support foot; +1 leans toward the left support foot. */
    val supportLean: Float
        get() = when (activeSide) { Side.LEFT -> -1f; Side.RIGHT -> 1f; null -> 0f }

    fun update(leftPosition: StepVector?, rightPosition: StepVector?, nowMs: Long): StepVector? {
        if (leftPosition == null || rightPosition == null ||
            !finite(leftPosition) || !finite(rightPosition)) {
            armed = false
            candidate = null
            candidateSinceMs = null
            restSinceMs = null
            activeSide = null
            lastUpdateMs = null
            return null
        }
        if (leftRest == null || rightRest == null) {
            leftRest = leftPosition
            rightRest = rightPosition
            calibratingUntilMs = nowMs + CALIBRATION_MS
            previousLeft = leftPosition
            previousRight = rightPosition
            lastUpdateMs = nowMs
            return null
        }
        val gap = lastUpdateMs?.let { nowMs - it > TRACKING_GAP_MS } ?: false
        val jump = distance(leftPosition, previousLeft ?: leftPosition) > TRACKING_JUMP_METERS ||
            distance(rightPosition, previousRight ?: rightPosition) > TRACKING_JUMP_METERS
        previousLeft = leftPosition
        previousRight = rightPosition
        lastUpdateMs = nowMs
        if (gap || jump) {
            armed = false
            candidate = null
            candidateSinceMs = null
            restSinceMs = null
            activeSide = null
            return null
        }
        if (nowMs < calibratingUntilMs) {
            leftRest = leftPosition
            rightRest = rightPosition
            return null
        }
        val leftDelta = subtract(leftPosition, leftRest!!)
        val rightDelta = subtract(rightPosition, rightRest!!)
        val leftLength = length(leftDelta)
        val rightLength = length(rightDelta)

        if (!armed) {
            if (leftLength <= REST_METERS && rightLength <= REST_METERS) {
                if (restSinceMs == null) restSinceMs = nowMs
                if (nowMs - (restSinceMs ?: nowMs) >= REST_DWELL_MS) {
                    armed = true
                    activeSide = null
                    candidate = null
                    candidateSinceMs = null
                    // Adjust the baseline only when both fingers are back at rest.
                    leftRest = leftPosition
                    rightRest = rightPosition
                }
            } else {
                restSinceMs = null
            }
            return null
        }
        if (leftLength >= OTHER_FOOT_MOTION_METERS &&
            rightLength >= OTHER_FOOT_MOTION_METERS) {
            armed = false
            candidate = null
            candidateSinceMs = null
            restSinceMs = null
            activeSide = null
            return null
        }
        val next = when {
            leftLength >= SWING_METERS && rightLength < OTHER_FOOT_MOTION_METERS -> Side.LEFT
            rightLength >= SWING_METERS && leftLength < OTHER_FOOT_MOTION_METERS -> Side.RIGHT
            else -> null
        }
        if (next != candidate) {
            candidate = next
            candidateSinceMs = if (next == null) null else nowMs
        }
        if (next == null || nowMs - (candidateSinceMs ?: nowMs) < SWING_DWELL_MS ||
            (lastStepMs != Long.MIN_VALUE && nowMs - lastStepMs < STEP_INTERVAL_MS)) return null
        val swing = if (next == Side.LEFT) leftDelta else rightDelta
        activeSide = next
        armed = false
        restSinceMs = null
        candidate = null
        candidateSinceMs = null
        lastStepMs = nowMs
        return swing
    }

    fun debugState(nowMs: Long): String {
        val l = previousLeft?.let { p -> leftRest?.let { distance(p, it) } }
        val r = previousRight?.let { p -> rightRest?.let { distance(p, it) } }
        return "armed=$armed active=$activeSide leftOffset=$l rightOffset=$r " +
            "calibrating=${nowMs < calibratingUntilMs}"
    }

    fun reset() {
        leftRest = null
        rightRest = null
        previousLeft = null
        previousRight = null
        lastUpdateMs = null
        calibratingUntilMs = 0L
        armed = false
        restSinceMs = null
        candidate = null
        candidateSinceMs = null
        activeSide = null
        lastStepMs = Long.MIN_VALUE
    }

    private fun finite(v: StepVector) = v.x.isFinite() && v.z.isFinite()
    private fun subtract(a: StepVector, b: StepVector) = StepVector(a.x - b.x, a.z - b.z)
    private fun length(v: StepVector) = sqrt(v.x * v.x + v.z * v.z)
    private fun distance(a: StepVector, b: StepVector) = length(subtract(a, b))

    private companion object {
        const val CALIBRATION_MS = 400L
        const val TRACKING_GAP_MS = 350L
        const val TRACKING_JUMP_METERS = 0.060f
        const val SWING_METERS = 0.025f
        const val OTHER_FOOT_MOTION_METERS = 0.018f
        const val REST_METERS = 0.012f
        const val REST_DWELL_MS = 160L
        const val SWING_DWELL_MS = 100L
        const val STEP_INTERVAL_MS = 300L
    }
}
