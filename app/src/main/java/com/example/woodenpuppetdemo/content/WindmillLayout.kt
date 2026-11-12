package com.example.woodenpuppetdemo.content
import kotlin.math.*
internal object WindmillLayout {
    const val X=-1.20f;const val Z=-.66f
    const val SCOOP_X=-.56f;const val SCOOP_Z=-.47f
    const val HUB_Y=.31f;const val HUB_Z=.18f
    const val OUTLET_LOCAL_Y=.0275f
    const val SCOOP_Y=HUB_Y-OUTLET_LOCAL_Y
    val facing=atan2(SCOOP_X-X,SCOOP_Z-Z)
    val hubX=X+HUB_Z*sin(facing);val hubZ=Z+HUB_Z*cos(facing)
    val aim=atan2(hubX-SCOOP_X,hubZ-SCOOP_Z)
    fun scoopYaw(quarter:Int)=aim+(quarter-2)*Math.PI.toFloat()/2f
}
