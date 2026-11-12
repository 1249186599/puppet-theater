package com.example.woodenpuppetdemo.content

import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.pico.spatial.ui.foundation.gesture.detectSpatialDragGesture
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.onSizeChanged
import com.pico.spatial.ui.platform.LocalPhysicalLengthConverter
import com.pico.spatial.ui.platform.LengthUnit
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import com.example.woodenpuppetdemo.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.ButtonDefaults
import com.pico.spatial.ui.design.Text

// App-owned programme-paper palette; native PicoTheme roles remain unchanged.
private val programPaper = Color(0xFFF4E8D2) // design-style: fixed-figma-color accepted warm-paper concept A
private val programInk = Color(0xFF51331F) // design-style: fixed-figma-color accepted warm-paper concept A
private val programMuted = Color(0xFF84674C) // design-style: fixed-figma-color accepted warm-paper concept A
private val programBrass = Color(0xFFB39051) // design-style: fixed-figma-color accepted warm-paper concept A
private val programFold = Color(0xFFE5D3B4) // design-style: fixed-figma-color accepted warm-paper concept A

@Composable
internal fun ProgramPanel(chapter:Int, task:String, detail:String, step:Int, total:Int,
    completed:Boolean, autoHelp:Boolean, grabbed:Boolean=false, onMeasured:(Float,Float)->Unit={_,_->}, emulatorInput:Boolean=false, onPreviewDrag:(Int,GrabV)->Unit={_,_->}, onReset:()->Unit, onNext:()->Unit, onReturnToFirst:()->Unit) {
    val context=LocalContext.current
    val converter=LocalPhysicalLengthConverter.current
    var manualHelp by remember(chapter,task) { mutableStateOf<Boolean?>(null) }
    val showHelp=manualHelp ?: autoHelp
    val chapterName=listOf("第一幕 · 剧院大门","第二幕 · 八音盒剧场","第三幕 · 发条之旅","第四幕 · 风之工坊","第五幕 · 木偶的最后一幕")[chapter-1]
    // This bounded AttachmentPanel is a scene prop, not a window's root background.
    Box(Modifier.width(480.dp).height(640.dp).onSizeChanged { size ->
        onMeasured(converter.dpToLength(with(converter){size.width.toDp()},LengthUnit.Meters),
            converter.dpToLength(with(converter){size.height.toDp()},LengthUnit.Meters))
    }) {
        Image(painterResource(R.drawable.program_backboard),contentDescription=null,
            modifier=Modifier.matchParentSize(),contentScale=ContentScale.FillBounds)
        Column(Modifier.fillMaxSize().padding(start=64.dp,end=64.dp,top=76.dp,bottom=52.dp),
            horizontalAlignment=Alignment.CenterHorizontally) {
                Text(chapterName,color=programInk,fontSize=23.sp,letterSpacing=1.sp,fontFamily=FontFamily.Serif)
                Spacer(Modifier.height(12.dp))
                ProgrammeRule()
                Spacer(Modifier.height(18.dp))
                Image(painterResource(R.drawable.program_windmill),contentDescription=null,
                    modifier=Modifier.width(180.dp).height(72.dp),contentScale=ContentScale.Fit)
                Spacer(Modifier.height(22.dp))
                Text(task,color=programInk,fontSize=34.sp,lineHeight=44.sp,fontFamily=FontFamily.Serif,
                    fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),contentAlignment=Alignment.TopCenter) {
                    val explanation=if(completed) "谢谢你，陪木偶走完这一段冒险。" else if(showHelp)
                        detail.replace(Regex("^[①②③④⑤⑥]\\s*|^[1-6]/[56]\\s*"),"")
                        else if(emulatorInput) "指向纸面上部，捏合拖动；松开放置" else "握把键拿起节目单；徒手捏住纸面上部移动"
                    Text(explanation,color=programInk,fontSize=21.sp,lineHeight=30.sp,fontFamily=FontFamily.Serif,textAlign=TextAlign.Center)
                }
                Canvas(Modifier.width(240.dp).height(28.dp)) {
                    val y=size.height/2;val gap=(size.width-16.dp.toPx())/(total-1)
                    drawLine(programBrass.copy(alpha=.5f),Offset(8.dp.toPx(),y),Offset(size.width-8.dp.toPx(),y),1.dp.toPx())
                    repeat(total){i->val c=Offset(8.dp.toPx()+gap*i,y)
                        drawCircle(programPaper,7.dp.toPx(),c)
                        if(i<step-1 || completed) drawCircle(programInk,6.dp.toPx(),c)
                        else if(i==step-1) {drawCircle(programInk,7.dp.toPx(),c,style=Stroke(1.5.dp.toPx()));drawCircle(programBrass,3.dp.toPx(),c)}
                        else drawCircle(programBrass,5.dp.toPx(),c,style=Stroke(1.dp.toPx()))
                    }
                }
                Text(if(completed) "本幕完成" else "$step / $total",color=programInk,fontSize=20.sp,fontFamily=FontFamily.Serif)
                Spacer(Modifier.height(18.dp))
                ProgrammeRule()
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                    Button(enabled=!grabbed,onClick={manualHelp=null;onReset()},colors=ButtonDefaults.buttonColors(
                        containerColor=Color.Transparent,contentColor=programInk)) { Text("重新开始",fontSize=21.sp,fontFamily=FontFamily.Serif) }
                    if(completed) {
                        val finalChapter = chapter == 5
                        Button(enabled=!grabbed,onClick=if(finalChapter) onReturnToFirst else onNext,
                            colors=ButtonDefaults.buttonColors(containerColor=programInk,contentColor=programPaper)) {
                            Text(if(finalChapter) "返回第一关" else "下一关",fontSize=21.sp,fontFamily=FontFamily.Serif)
                        }
                    } else {
                        Button(enabled=!grabbed,onClick={manualHelp=!showHelp},colors=ButtonDefaults.buttonColors(
                            containerColor=Color.Transparent,contentColor=programInk)) {Text(if(showHelp) "收起提示" else "提示",fontSize=21.sp,fontFamily=FontFamily.Serif)}
                    }
                }
            }
        // Simulator preview uses system gaze/pinch dragging; footer buttons remain separate.
        if(emulatorInput) Box(Modifier.fillMaxWidth().height(350.dp).pointerInput(Unit) {
            detectSpatialDragGesture(context,
                onDragStart={ _, _ -> onPreviewDrag(0,GrabV(0f,0f,0f)) },
                onDragEnd={ onPreviewDrag(2,GrabV(0f,0f,0f)) },
                onDragCancel={ onPreviewDrag(2,GrabV(0f,0f,0f)) }
            ) { value ->
                fun meters(px:Float)=converter.dpToLength(with(converter){px.toDp()},LengthUnit.Meters)
                onPreviewDrag(1,GrabV(meters(value.dragAmount.x),-meters(value.dragAmount.y),meters(value.dragAmount.z)))
            }
        })
    }
}

@Composable
private fun ProgrammeRule() {
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(programBrass.copy(alpha=.55f),Offset(0f,0f),Offset(size.width,0f),.7.dp.toPx())
    }
}
