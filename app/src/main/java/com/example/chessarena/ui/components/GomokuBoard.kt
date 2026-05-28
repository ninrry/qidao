package com.example.chessarena.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessarena.theme.*
import com.example.chessarena.viewmodel.AnimationSpeed
import com.example.chessarena.viewmodel.BoardTheme
import kotlin.math.roundToInt

// ── 数据模型 ──────────────────────────────────────────────

/** 五子棋棋子颜色 */
enum class StoneColor { BLACK, WHITE }

/** 五子棋棋子 */
data class GomokuStone(
    val row: Int,
    val col: Int,
    val color: StoneColor
)

/** 五子棋棋盘状态 */
data class GomokuBoardState(
    val boardSize: Int = 15,
    val stones: List<GomokuStone> = emptyList(),
    val lastMove: GomokuStone? = null,
    val forbiddenMoves: List<Pair<Int, Int>> = emptyList(),
    val winningLine: List<Pair<Int, Int>> = emptyList(),
)

/**
 * 五子棋棋盘组件
 *
 * 使用 Canvas 绘制完整的 15×15 五子棋棋盘，包括：
 * - 木纹渐变背景
 * - 15×15 网格线
 * - 天元及四角星位
 * - 3D 立体棋子（黑子暗渐变 + 高光，白子玻璃效果）
 * - 最后落子红点标记
 * - 禁手 X 标记
 * - 获胜连线高亮
 * - 触摸落子（点击最近交叉点）
 */
@Composable
fun GomokuBoard(
    state: GomokuBoardState,
    onPositionClick: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier,
    showCoordinates: Boolean = false,
    animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    boardTheme: BoardTheme = BoardTheme.WOOD,
) {
    val boardSize = state.boardSize
    val extendedColors = ChessArenaColors.extendedColors
    val palette = gomokuBoardPalette(boardTheme, extendedColors)
    val textMeasurer = rememberTextMeasurer()

    // 1. 新落子弹性物理缩放入场动画
    val stoneScaleAnim = remember { Animatable(0f) }
    LaunchedEffect(state.lastMove, animationSpeed) {
        val durationMs = motionDurationMs(animationSpeed, 260)
        if (state.lastMove != null) {
            if (durationMs == 0) {
                stoneScaleAnim.snapTo(1f)
            } else {
                stoneScaleAnim.snapTo(0f)
                stoneScaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
                )
            }
        } else {
            stoneScaleAnim.snapTo(1f)
        }
    }

    // 2. 宣纸水墨涟漪晕染扩散动画
    val rippleAnim = remember { Animatable(0f) }
    LaunchedEffect(state.lastMove, animationSpeed) {
        val durationMs = motionDurationMs(animationSpeed, 600)
        if (state.lastMove != null) {
            if (durationMs == 0) {
                rippleAnim.snapTo(1f)
            } else {
                rippleAnim.snapTo(0f)
                rippleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = durationMs, easing = EaseOutQuad)
                )
            }
        } else {
            rippleAnim.snapTo(1f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(state) {
                detectTapGestures { offset ->
                    val w = size.width.toFloat()
                    val padding = w * 0.05f
                    val cellSize = (w - 2 * padding) / (boardSize - 1)

                    val col = ((offset.x - padding) / cellSize).roundToInt()
                    val row = ((offset.y - padding) / cellSize).roundToInt()
                    if (row in 0 until boardSize && col in 0 until boardSize) {
                        onPositionClick(row, col)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val padding = w * 0.05f
        val cellSize = (w - 2 * padding) / (boardSize - 1)
        val stoneRadius = cellSize * 0.44f

        // 1. 木纹背景
        drawGomokuWoodBackground(w, h, palette.background, palette.line)

        // 2. 网格线
        drawGomokuGrid(padding, cellSize, boardSize, palette.line)

        // 3. 星位
        drawGomokuStarPoints(padding, cellSize, boardSize, palette.line)

        // 3.5 坐标标注
        if (showCoordinates) {
            drawGomokuCoordinates(textMeasurer, padding, cellSize, boardSize, palette.line)
        }

        // 4. 获胜连线
        if (state.winningLine.size >= 2) {
            val first = state.winningLine.first()
            val last = state.winningLine.last()
            drawLine(
                color = AccentGold,
                start = Offset(padding + first.second * cellSize, padding + first.first * cellSize),
                end = Offset(padding + last.second * cellSize, padding + last.first * cellSize),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }

        // 5. 禁手标记
        state.forbiddenMoves.forEach { (r, c) ->
            val cx = padding + c * cellSize
            val cy = padding + r * cellSize
            drawForbiddenMark(cx, cy, stoneRadius * 0.5f, extendedColors.evalNegative)
        }

        // 6. 棋子与水墨涟漪
        state.stones.forEach { stone ->
            val cx = padding + stone.col * cellSize
            val cy = padding + stone.row * cellSize
            val isLast = state.lastMove == stone

            val scale = if (isLast) stoneScaleAnim.value else 1f
            val currentRadius = stoneRadius * scale

            if (currentRadius > 0.1f) {
                if (stone.color == StoneColor.BLACK) {
                    drawBlackStone(Offset(cx, cy), currentRadius, isLast, extendedColors)
                } else {
                    drawWhiteStone(Offset(cx, cy), currentRadius, isLast, extendedColors)
                }
            }
        }

        // 7. 渲染宣纸水墨同心圆扩散涟漪与飞散墨滴微粒子
        state.lastMove?.let { lastStone ->
            val cx = padding + lastStone.col * cellSize
            val cy = padding + lastStone.row * cellSize
            val rVal = rippleAnim.value
            if (rVal < 1f) {
                // ── 1. 绘制 3 圈带有时间差的水墨波纹涟漪 ──
                
                // 第一圈：主波纹 (无延迟)
                val t1 = rVal
                val rippleRadius1 = stoneRadius * (1f + t1 * 2.8f)
                val rippleAlpha1 = (1f - t1) * 0.45f
                drawCircle(
                    color = palette.line.copy(alpha = rippleAlpha1),
                    radius = rippleRadius1,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.dp.toPx())
                )

                // 第二圈：中波纹 (微延迟，从 0.15f 开始)
                val t2 = ((rVal - 0.15f).coerceAtLeast(0f) / 0.85f)
                if (t2 > 0f) {
                    val rippleRadius2 = stoneRadius * (1f + t2 * 2.2f)
                    val rippleAlpha2 = (1f - t2) * 0.30f
                    drawCircle(
                        color = palette.line.copy(alpha = rippleAlpha2),
                        radius = rippleRadius2,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // 第三圈：内波纹 (较大延迟，从 0.3f 开始)
                val t3 = ((rVal - 0.3f).coerceAtLeast(0f) / 0.7f)
                if (t3 > 0f) {
                    val rippleRadius3 = stoneRadius * (1f + t3 * 1.6f)
                    val rippleAlpha3 = (1f - t3) * 0.18f
                    drawCircle(
                        color = palette.line.copy(alpha = rippleAlpha3),
                        radius = rippleRadius3,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // ── 2. 绘制指尖落子爆开的 8 颗水墨微粒子 ──
                // 为了让每次落子的粒子偏角和速度相对不同但对同一个位置稳定，
                // 我们基于 lastStone.row 和 lastStone.col 生成“伪随机”的速度与方向偏角因子。
                for (i in 0 until 8) {
                    // 粒子初始角度
                    val angleOffset = ((lastStone.row * 7 + lastStone.col * 13 + i * 45) % 360) * (Math.PI / 180f)
                    val baseAngle = i * (Math.PI / 4.0) + angleOffset
                    
                    // 粒子速度因子 (伪随机：0.6 到 1.4)
                    val speedFactor = 0.6f + ((lastStone.row * 3 + lastStone.col * 5 + i * 7) % 9) * 0.1f
                    
                    // 当前飞散距离 (非线性，先快后慢)
                    val distProgress = 1f - (1f - rVal) * (1f - rVal) // 缓动曲线
                    val dist = stoneRadius * (1.2f + distProgress * 2.8f * speedFactor)
                    
                    // 当前墨滴大小
                    val size = (stoneRadius * 0.22f) * (1f - rVal)
                    
                    // 墨滴透明度
                    val particleAlpha = (1f - rVal) * 0.55f
                    
                    val px = cx + kotlin.math.cos(baseAngle).toFloat() * dist
                    val py = cy + kotlin.math.sin(baseAngle).toFloat() * dist
                    
                    // 绘制墨滴点
                    if (size > 0.1f) {
                        drawCircle(
                            color = palette.line.copy(alpha = particleAlpha),
                            radius = size,
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }
    }
}

// ── 木纹背景 ──────────────────────────────────────────────
private fun DrawScope.drawGomokuWoodBackground(
    width: Float,
    height: Float,
    backgroundColor: Color,
    lineColor: Color
) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                backgroundColor,
                backgroundColor.copy(alpha = 0.9f),
                backgroundColor,
            )
        ),
        size = Size(width, height)
    )
    // 纹理
    val stripeCount = 35
    for (i in 0 until stripeCount) {
        val y = height * i / stripeCount
        drawLine(
            color = lineColor.copy(alpha = 0.04f),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
    }
    // 边框
    drawRect(
        color = lineColor,
        size = Size(width, height),
        style = Stroke(width = 3f)
    )
}

// ── 网格线 ────────────────────────────────────────────────
private fun DrawScope.drawGomokuGrid(
    padding: Float, cellSize: Float, boardSize: Int, lineColor: Color
) {
    val end = boardSize - 1

    for (i in 0 until boardSize) {
        val pos = padding + i * cellSize
        // 横线
        drawLine(lineColor, Offset(padding, pos), Offset(padding + end * cellSize, pos), 1.5f)
        // 竖线
        drawLine(lineColor, Offset(pos, padding), Offset(pos, padding + end * cellSize), 1.5f)
    }
}

// ── 星位 ──────────────────────────────────────────────────
private fun DrawScope.drawGomokuStarPoints(
    padding: Float, cellSize: Float, boardSize: Int, starColor: Color
) {
    val starRadius = cellSize * 0.1f

    val starPositions = if (boardSize == 15) {
        listOf(
            Pair(7, 7),   // 天元
            Pair(3, 3), Pair(3, 11),  // 四角星
            Pair(11, 3), Pair(11, 11),
        )
    } else {
        listOf(Pair(boardSize / 2, boardSize / 2))
    }

    starPositions.forEach { (r, c) ->
        drawCircle(
            color = starColor,
            radius = starRadius,
            center = Offset(padding + c * cellSize, padding + r * cellSize)
        )
    }
}

// ── 黑子 ──────────────────────────────────────────────────
private fun DrawScope.drawBlackStone(center: Offset, radius: Float, isLast: Boolean, colors: ChessArenaExtendedColors) {
    // 1. 双层柔和微物理阴影
    drawCircle(
        color = Color.Black.copy(alpha = 0.20f),
        radius = radius * 1.05f,
        center = center + Offset(radius * 0.08f, radius * 0.11f)
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.42f),
        radius = radius * 0.95f,
        center = center + Offset(radius * 0.03f, radius * 0.04f)
    )

    // 2. 玄石多色径向渐变
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF3E3F45),
                Color(0xFF18181C),
                Color(0xFF060608)
            ),
            center = center - Offset(radius * 0.22f, radius * 0.22f),
            radius = radius * 1.55f
        ),
        radius = radius,
        center = center
    )

    // 3. 高光反射亮弧
    drawPath(
        path = Path().apply {
            addArc(
                androidx.compose.ui.geometry.Rect(
                    center.x - radius * 0.7f,
                    center.y - radius * 0.7f,
                    center.x + radius * 0.1f,
                    center.y + radius * 0.1f
                ),
                180f,
                90f
            )
        },
        color = Color.White.copy(alpha = 0.16f),
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    )

    // 4. 国风古金双层水墨指示星环
    if (isLast) {
        drawCircle(
            color = Color(0xFFD4AF37),
            radius = radius * 0.28f,
            center = center,
            style = Stroke(width = 1.6.dp.toPx())
        )
        drawCircle(
            color = Color(0xFFD4AF37),
            radius = radius * 0.08f,
            center = center
        )
    }
}

// ── 白子 ──────────────────────────────────────────────────
private fun DrawScope.drawWhiteStone(center: Offset, radius: Float, isLast: Boolean, colors: ChessArenaExtendedColors) {
    // 1. 双层柔和微物理阴影
    drawCircle(
        color = Color(0xFF1A150D).copy(alpha = 0.15f),
        radius = radius * 1.02f,
        center = center + Offset(radius * 0.06f, radius * 0.09f)
    )
    drawCircle(
        color = Color(0xFF1A150D).copy(alpha = 0.26f),
        radius = radius * 0.94f,
        center = center + Offset(radius * 0.02f, radius * 0.03f)
    )

    // 2. 白玛瑙温润多色渐变
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFAF9F4),
                Color(0xFFDFDCD2)
            ),
            center = center - Offset(radius * 0.18f, radius * 0.18f),
            radius = radius * 1.5f
        ),
        radius = radius,
        center = center
    )

    // 3. 立体玉石微压纹描边
    drawCircle(
        color = Color(0xFFD0CDC2).copy(alpha = 0.6f),
        radius = radius * 0.98f,
        center = center,
        style = Stroke(width = 0.8.dp.toPx())
    )

    // 4. 玻璃质感反射高光
    drawPath(
        path = Path().apply {
            addArc(
                androidx.compose.ui.geometry.Rect(
                    center.x - radius * 0.65f,
                    center.y - radius * 0.65f,
                    center.x + radius * 0.05f,
                    center.y + radius * 0.05f
                ),
                180f,
                90f
            )
        },
        color = Color.White.copy(alpha = 0.72f),
        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
    )

    // 5. 国风古金双层水墨指示星环
    if (isLast) {
        drawCircle(
            color = Color(0xFFD4AF37),
            radius = radius * 0.28f,
            center = center,
            style = Stroke(width = 1.6.dp.toPx())
        )
        drawCircle(
            color = Color(0xFFD4AF37),
            radius = radius * 0.08f,
            center = center
        )
    }
}

// ── 禁手标记 ──────────────────────────────────────────────
private fun DrawScope.drawForbiddenMark(cx: Float, cy: Float, halfSize: Float, color: Color) {
    val markColor = color.copy(alpha = 0.7f)
    drawLine(markColor, Offset(cx - halfSize, cy - halfSize), Offset(cx + halfSize, cy + halfSize), 2.5f, cap = StrokeCap.Round)
    drawLine(markColor, Offset(cx + halfSize, cy - halfSize), Offset(cx - halfSize, cy + halfSize), 2.5f, cap = StrokeCap.Round)
}

// ── 坐标标注 ─────────────────────────────────────────────
private fun DrawScope.drawGomokuCoordinates(
    textMeasurer: TextMeasurer,
    padding: Float,
    cellSize: Float,
    boardSize: Int,
    color: Color
) {
    val paint = android.graphics.Paint().apply {
        this.color = color.copy(alpha = 0.55f).toArgb()
        textSize = 10.sp.toPx()
        typeface = android.graphics.Typeface.MONOSPACE
        isAntiAlias = true
    }
    val topYBase = (padding * 0.5f).coerceAtLeast(0f)
    val bottomYBase = (size.height - padding * 0.5f).coerceAtLeast(0f)
    val leftXBase = (padding * 0.5f).coerceAtLeast(0f)
    val rightXBase = (size.width - padding * 0.5f).coerceAtLeast(0f)

    for (col in 0 until boardSize) {
        val label = ('A' + col).toString()
        val textWidth = paint.measureText(label)
        val x = padding + col * cellSize
        val topY = topYBase + paint.textSize * 0.35f
        val bottomY = bottomYBase + paint.textSize * 0.35f
        drawContext.canvas.nativeCanvas.drawText(label, x - textWidth / 2f, topY, paint)
        drawContext.canvas.nativeCanvas.drawText(label, x - textWidth / 2f, bottomY, paint)
    }

    for (row in 0 until boardSize) {
        val label = (row + 1).toString()
        val textWidth = paint.measureText(label)
        val y = padding + row * cellSize + paint.textSize * 0.35f
        val leftX = leftXBase
        val rightX = rightXBase
        drawContext.canvas.nativeCanvas.drawText(label, leftX - textWidth / 2f, y, paint)
        drawContext.canvas.nativeCanvas.drawText(label, rightX - textWidth / 2f, y, paint)
    }
}

private fun motionDurationMs(speed: AnimationSpeed, baseMs: Int): Int {
    if (speed == AnimationSpeed.INSTANT) return 0
    val ratio = speed.durationMs.toFloat() / AnimationSpeed.NORMAL.durationMs.toFloat()
    return (baseMs * ratio).roundToInt().coerceAtLeast(1)
}

private data class GomokuPalette(
    val background: Color,
    val line: Color
)

private fun gomokuBoardPalette(theme: BoardTheme, colors: ChessArenaExtendedColors): GomokuPalette {
    return when (theme) {
        BoardTheme.WOOD -> GomokuPalette(colors.boardBackground, colors.boardLine)
        BoardTheme.DARK -> GomokuPalette(Color(0xFF23242A), Color(0xFF6F7179))
        BoardTheme.MARBLE -> GomokuPalette(Color(0xFFE7E5DF), Color(0xFF8B8D94))
    }
}

// ── 预览 ──────────────────────────────────────────────────
@Preview(showBackground = true, widthDp = 360, heightDp = 360)
@Composable
private fun GomokuBoardPreview() {
    val stones = listOf(
        GomokuStone(7, 7, StoneColor.BLACK),
        GomokuStone(7, 8, StoneColor.WHITE),
        GomokuStone(8, 7, StoneColor.BLACK),
        GomokuStone(8, 8, StoneColor.WHITE),
        GomokuStone(6, 6, StoneColor.BLACK),
    )
    ChessArenaTheme(darkTheme = true) {
        GomokuBoard(
            state = GomokuBoardState(
                stones = stones,
                lastMove = stones.last(),
                forbiddenMoves = listOf(Pair(5, 5)),
            ),
            onPositionClick = { _, _ -> }
        )
    }
}
