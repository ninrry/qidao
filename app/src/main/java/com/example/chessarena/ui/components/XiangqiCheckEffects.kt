package com.example.chessarena.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.example.chessarena.theme.*

/**
 * 象棋将军特效 - 红色边框脉冲 + 太极八卦残影
 */
@Composable
fun XiangqiCheckEffects(
    isInCheck: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isInCheck) return

    // 呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "checkPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val borderRed = Color(0xFFD32F2F).copy(alpha = pulseAlpha * 0.28f)
            val strokeW = 24.dp.toPx()

            // 上
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(borderRed, Color.Transparent),
                    startY = 0f,
                    endY = strokeW
                ),
                size = Size(size.width, strokeW)
            )
            // 下
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, borderRed),
                    startY = size.height - strokeW,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height - strokeW),
                size = Size(size.width, strokeW)
            )
            // 左
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(borderRed, Color.Transparent),
                    startX = 0f,
                    endX = strokeW
                ),
                size = Size(strokeW, size.height)
            )
            // 右
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, borderRed),
                    startX = size.width - strokeW,
                    endX = size.width
                ),
                topLeft = Offset(size.width - strokeW, 0f),
                size = Size(strokeW, size.height)
            )

            // 四角太极八卦
            val sealColor = Color(0xFFC04851).copy(alpha = pulseAlpha * 0.35f)
            val baguaRadius = 56.dp.toPx()

            drawBagua(Offset(baguaRadius, baguaRadius), baguaRadius, sealColor, rotationAngle)
            drawBagua(Offset(size.width - baguaRadius, baguaRadius), baguaRadius, sealColor, -rotationAngle)
            drawBagua(Offset(baguaRadius, size.height - baguaRadius), baguaRadius, sealColor, -rotationAngle)
            drawBagua(Offset(size.width - baguaRadius, size.height - baguaRadius), baguaRadius, sealColor, rotationAngle)
        }
    }
}

private fun DrawScope.drawBagua(
    center: Offset,
    radius: Float,
    color: Color,
    rotationAngle: Float
) {
    withTransform({ rotate(rotationAngle, center) }) {
        // 外圆
        drawCircle(color = color, radius = radius, center = center, style = Stroke(width = 1.5.dp.toPx()))
        // 内圆
        drawCircle(color = color, radius = radius * 0.58f, center = center, style = Stroke(width = 1.dp.toPx()))

        // S 型分割
        val path = Path().apply {
            arcTo(
                rect = Rect(center.x - radius * 0.29f, center.y - radius * 0.58f, center.x + radius * 0.29f, center.y),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = true
            )
            arcTo(
                rect = Rect(center.x - radius * 0.29f, center.y, center.x + radius * 0.29f, center.y + radius * 0.58f),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
        }
        drawPath(path = path, color = color, style = Stroke(width = 1.dp.toPx()))

        // 鱼眼
        drawCircle(color = color, radius = radius * 0.07f, center = Offset(center.x, center.y - radius * 0.29f))
        drawCircle(color = color, radius = radius * 0.07f, center = Offset(center.x, center.y + radius * 0.29f))

        // 八卦卦象
        val trigrams = listOf(
            listOf(true, true, true),
            listOf(true, false, false),
            listOf(false, true, false),
            listOf(false, false, true),
            listOf(false, false, false),
            listOf(false, true, true),
            listOf(true, false, true),
            listOf(true, true, false)
        )

        trigrams.forEachIndexed { index, lines ->
            val angle = index * 45f
            withTransform({ rotate(angle, center) }) {
                for (level in 0..2) {
                    val rLevel = radius * (0.68f + level * 0.08f)
                    val lineW = radius * 0.22f
                    val yPos = center.y - rLevel
                    val isSolid = lines[level]

                    if (isSolid) {
                        drawLine(
                            color = color,
                            start = Offset(center.x - lineW / 2, yPos),
                            end = Offset(center.x + lineW / 2, yPos),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    } else {
                        val segmentW = lineW * 0.4f
                        drawLine(
                            color = color,
                            start = Offset(center.x - lineW / 2, yPos),
                            end = Offset(center.x - lineW / 2 + segmentW, yPos),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawLine(
                            color = color,
                            start = Offset(center.x + lineW / 2 - segmentW, yPos),
                            end = Offset(center.x + lineW / 2, yPos),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }
            }
        }
    }
}
