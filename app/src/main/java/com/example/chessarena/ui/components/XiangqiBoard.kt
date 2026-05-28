package com.example.chessarena.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessarena.theme.*
import com.example.chessarena.viewmodel.AnimationSpeed
import com.example.chessarena.viewmodel.BoardTheme
import kotlin.math.roundToInt

// ── 数据模型 ──────────────────────────────────────────────
data class BoardPosition(val row: Int, val col: Int)
enum class PieceSide { RED, BLACK }
enum class XiangqiPieceType { JIANG, SHI, XIANG, MA, JU, PAO, BING }

data class XiangqiPieceData(
    val type: XiangqiPieceType,
    val side: PieceSide,
    val position: BoardPosition
)

data class XiangqiMoveData(
    val from: BoardPosition,
    val to: BoardPosition,
    val notation: String = ""
)

data class XiangqiBoardState(
    val pieces: List<XiangqiPieceData> = emptyList(),
    val selectedPosition: BoardPosition? = null,
    val validMoves: List<BoardPosition> = emptyList(),
    val lastMove: XiangqiMoveData? = null,
    val isRedTurn: Boolean = true,
    val isInCheck: Boolean = false
)

/**
 * 棋道 2.0 专业级象棋棋盘
 * 引入高级材质渲染与细腻交互反馈
 */
@Composable
fun XiangqiBoard(
    state: XiangqiBoardState,
    onPositionClick: (BoardPosition) -> Unit,
    modifier: Modifier = Modifier,
    isFlipped: Boolean = false,
    showCoordinates: Boolean = false,
    animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    boardTheme: BoardTheme = BoardTheme.WOOD,
) {
    val textMeasurer = rememberTextMeasurer()
    val extendedColors = ChessArenaColors.extendedColors
    val palette = xiangqiBoardPalette(boardTheme, extendedColors)

    // 选中动画：呼吸光晕
    val infiniteTransition = rememberInfiniteTransition(label = "selection")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    // 字符映射
    val pieceChars = remember {
        mapOf(
            Pair(XiangqiPieceType.JIANG, PieceSide.RED) to "帥",
            Pair(XiangqiPieceType.SHI, PieceSide.RED) to "仕",
            Pair(XiangqiPieceType.XIANG, PieceSide.RED) to "相",
            Pair(XiangqiPieceType.MA, PieceSide.RED) to "馬",
            Pair(XiangqiPieceType.JU, PieceSide.RED) to "車",
            Pair(XiangqiPieceType.PAO, PieceSide.RED) to "砲",
            Pair(XiangqiPieceType.BING, PieceSide.RED) to "兵",
            Pair(XiangqiPieceType.JIANG, PieceSide.BLACK) to "將",
            Pair(XiangqiPieceType.SHI, PieceSide.BLACK) to "士",
            Pair(XiangqiPieceType.XIANG, PieceSide.BLACK) to "象",
            Pair(XiangqiPieceType.MA, PieceSide.BLACK) to "馬",
            Pair(XiangqiPieceType.JU, PieceSide.BLACK) to "車",
            Pair(XiangqiPieceType.PAO, PieceSide.BLACK) to "砲",
            Pair(XiangqiPieceType.BING, PieceSide.BLACK) to "卒",
        )
    }

    // 走棋平移物理动画与水墨涟漪同步状态重置机制 (防止首帧闪烁)
    var currentLastMove by remember { mutableStateOf<XiangqiMoveData?>(null) }
    var animValueState by remember { mutableStateOf(1f) }
    var rippleAnimValueState by remember { mutableStateOf(1f) }

    if (state.lastMove != currentLastMove) {
        currentLastMove = state.lastMove
        if (state.lastMove != null) {
            animValueState = 0f
            rippleAnimValueState = 0f
        }
    }

    LaunchedEffect(state.lastMove, animationSpeed) {
        val durationMs = motionDurationMs(animationSpeed, 280)
        if (state.lastMove != null) {
            if (durationMs == 0) {
                animValueState = 1f
            } else {
                animate(
                    initialValue = animValueState,
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
                ) { value, _ ->
                    animValueState = value
                }
            }
        } else {
            animValueState = 1f
        }
    }

    LaunchedEffect(state.lastMove, animationSpeed) {
        val durationMs = motionDurationMs(animationSpeed, 600)
        if (state.lastMove != null) {
            if (durationMs == 0) {
                rippleAnimValueState = 1f
            } else {
                animate(
                    initialValue = rippleAnimValueState,
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = durationMs, easing = EaseOutQuad)
                ) { value, _ ->
                    rippleAnimValueState = value
                }
            }
        } else {
            rippleAnimValueState = 1f
        }
    }

    val selectAnimScale by animateFloatAsState(
        targetValue = if (state.selectedPosition != null) 1.06f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "selectAnimScale"
    )

    // 捕获被吃的棋子
    var prevPieces by remember { mutableStateOf<List<XiangqiPieceData>>(emptyList()) }
    var capturedPiece by remember { mutableStateOf<XiangqiPieceData?>(null) }
    LaunchedEffect(state.pieces) {
        val lastMove = state.lastMove
        if (lastMove != null) {
            // 在旧的 pieces 列表中寻找上一次落在 to 处的棋子（即被吃掉的棋子）
            val oldPieceAtTo = prevPieces.find { it.position == lastMove.to }
            capturedPiece = oldPieceAtTo
        } else {
            capturedPiece = null
        }
        prevPieces = state.pieces
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.92f) // 优化棋盘比例
            .pointerInput(state, isFlipped) {
                detectTapGestures { offset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val px = w * 0.05f
                    val py = h * 0.05f
                    val cw = (w - 2 * px) / 8f
                    val ch = (h - 2 * py) / 9f
                    val col = ((offset.x - px + cw / 2) / cw).toInt()
                    val row = ((offset.y - py + ch / 2) / ch).toInt()
                    if (row in 0..9 && col in 0..8) {
                        val finalRow = if (isFlipped) 9 - row else row
                        val finalCol = if (isFlipped) 8 - col else col
                        onPositionClick(BoardPosition(finalRow, finalCol))
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val px = w * 0.05f
        val py = h * 0.05f
        val cw = (w - 2 * px) / 8f
        val ch = (h - 2 * py) / 9f
        val radius = (minOf(cw, ch) * 0.44f)

        // 1. 材质底座渲染
        drawBoardMaterial(w, h, palette.background, palette.line)

        // 2. 交互层：上一步记录
        state.lastMove?.let { move ->
            drawMoveHighlight(move, px, py, cw, ch, radius, extendedColors.lastMoveMark, isFlipped)
        }

        // 3. 骨架：线条与标记
        drawBoardSkeleton(px, py, cw, ch, palette.line)
        drawRiverCalligraphy(textMeasurer, px, py, cw, ch, palette.line, isFlipped)

        if (showCoordinates) {
            drawXiangqiCoordinates(textMeasurer, px, py, cw, ch, palette.line, isFlipped)
        }

        // 4. 指引：有效落子点（仅在空地上绘制）
        state.validMoves.forEach { pos ->
            val hasPiece = state.pieces.any { it.position == pos }
            if (!hasPiece) {
                val displayRow = if (isFlipped) 9 - pos.row else pos.row
                val displayCol = if (isFlipped) 8 - pos.col else pos.col
                val cx = px + displayCol * cw
                val cy = py + displayRow * ch
                drawCircle(
                    color = extendedColors.moveHint,
                    radius = radius * 0.25f,
                    center = Offset(cx, cy)
                )
            }
        }

        // 5. 核心：棋子渲染
        val animValue = animValueState
        val isAnimating = animValue < 1f
        val lastMove = state.lastMove

        state.pieces.forEach { piece ->
            val movingMove = lastMove?.takeIf { isAnimating && piece.position == it.to }
            
            val cx: Float
            val cy: Float
            if (movingMove != null) {
                val startRow = if (isFlipped) 9 - movingMove.from.row else movingMove.from.row
                val startCol = if (isFlipped) 8 - movingMove.from.col else movingMove.from.col
                val endRow = if (isFlipped) 9 - movingMove.to.row else movingMove.to.row
                val endCol = if (isFlipped) 8 - movingMove.to.col else movingMove.to.col
                val startCx = px + startCol * cw
                val startCy = py + startRow * ch
                val endCx = px + endCol * cw
                val endCy = py + endRow * ch
                cx = startCx + (endCx - startCx) * animValue
                cy = startCy + (endCy - startCy) * animValue
            } else {
                val displayRow = if (isFlipped) 9 - piece.position.row else piece.position.row
                val displayCol = if (isFlipped) 8 - piece.position.col else piece.position.col
                cx = px + displayCol * cw
                cy = py + displayRow * ch
            }

            val isSelected = state.selectedPosition == piece.position
            val char = pieceChars[Pair(piece.type, piece.side)] ?: ""
            val isTurnGeneralInCheck = state.isInCheck && 
                piece.type == XiangqiPieceType.JIANG && 
                ((piece.side == PieceSide.RED && state.isRedTurn) || (piece.side == PieceSide.BLACK && !state.isRedTurn))

            drawPremiumPiece(
                center = Offset(cx, cy),
                radius = if (isSelected) radius * selectAnimScale else radius,
                piece = piece,
                char = char,
                isSelected = isSelected,
                isGeneralInCheck = isTurnGeneralInCheck,
                glowScale = if (isSelected || isTurnGeneralInCheck) glowScale else 1f,
                colors = extendedColors,
                textMeasurer = textMeasurer
            )
        }

        // 6. 顶层吃子平隐动画渲染
        val disappearingPiece = capturedPiece
        if (isAnimating && disappearingPiece != null && lastMove != null) {
            val displayRow = if (isFlipped) 9 - lastMove.to.row else lastMove.to.row
            val displayCol = if (isFlipped) 8 - lastMove.to.col else lastMove.to.col
            val cx = px + displayCol * cw
            val cy = py + displayRow * ch
            val alpha = 1f - animValue
            val scale = 1f - animValue * 0.4f
            val char = pieceChars[Pair(disappearingPiece.type, disappearingPiece.side)] ?: ""

            drawPremiumPiece(
                center = Offset(cx, cy),
                radius = radius * scale,
                piece = disappearingPiece,
                char = char,
                isSelected = false,
                isGeneralInCheck = false,
                glowScale = 1f,
                colors = extendedColors,
                textMeasurer = textMeasurer,
                alpha = alpha
            )
        }

        // 7. 顶层：吃子高亮瞄准边框
        state.validMoves.forEach { pos ->
            val enemySide = if (state.isRedTurn) PieceSide.BLACK else PieceSide.RED
            val hasEnemyPiece = state.pieces.any { it.position == pos && it.side == enemySide }
            if (hasEnemyPiece) {
                val displayRow = if (isFlipped) 9 - pos.row else pos.row
                val displayCol = if (isFlipped) 8 - pos.col else pos.col
                val cx = px + displayCol * cw
                val cy = py + displayRow * ch
                drawEatTargetHighlight(
                    center = Offset(cx, cy),
                    radius = radius,
                    color = extendedColors.evalNegative
                )
            }
        }

        // 8. 顶层：渲染指尖落子/吃子爆开的 3 圈同心圆水墨涟漪与 8 颗微粒子飞散
        state.lastMove?.let { lastMove ->
            val displayRow = if (isFlipped) 9 - lastMove.to.row else lastMove.to.row
            val displayCol = if (isFlipped) 8 - lastMove.to.col else lastMove.to.col
            val cx = px + displayCol * cw
            val cy = py + displayRow * ch
            val rVal = rippleAnimValueState
            if (rVal < 1f) {
                // ── 1. 绘制 3 圈带有时间差的水墨波纹涟漪 ──
                
                // 第一圈：主波纹 (无延迟)
                val t1 = rVal
                val rippleRadius1 = radius * (1f + t1 * 2.6f)
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
                    val rippleRadius2 = radius * (1f + t2 * 2.0f)
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
                    val rippleRadius3 = radius * (1f + t3 * 1.4f)
                    val rippleAlpha3 = (1f - t3) * 0.18f
                    drawCircle(
                        color = palette.line.copy(alpha = rippleAlpha3),
                        radius = rippleRadius3,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // ── 2. 绘制 8 颗水墨墨滴微粒子飞散 ──
                for (i in 0 until 8) {
                    val angleOffset = ((lastMove.to.row * 7 + lastMove.to.col * 13 + i * 45) % 360) * (Math.PI / 180f)
                    val baseAngle = i * (Math.PI / 4.0) + angleOffset
                    val speedFactor = 0.6f + ((lastMove.to.row * 3 + lastMove.to.col * 5 + i * 7) % 9) * 0.1f
                    
                    val distProgress = 1f - (1f - rVal) * (1f - rVal) // 缓动
                    val dist = radius * (1.2f + distProgress * 2.6f * speedFactor)
                    val size = (radius * 0.22f) * (1f - rVal)
                    val particleAlpha = (1f - rVal) * 0.55f
                    
                    val pX = cx + kotlin.math.cos(baseAngle).toFloat() * dist
                    val pY = cy + kotlin.math.sin(baseAngle).toFloat() * dist
                    
                    if (size > 0.1f) {
                        drawCircle(
                            color = palette.line.copy(alpha = particleAlpha),
                            radius = size,
                            center = Offset(pX, pY)
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawBoardMaterial(w: Float, h: Float, backgroundColor: Color, lineColor: Color) {
    // 渐变木质纹理
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(backgroundColor.copy(alpha = 0.9f), backgroundColor),
            center = Offset(w / 2, h / 2),
            radius = w
        ),
        size = Size(w, h)
    )
    
    // 边框质感
    drawRect(
        color = lineColor.copy(alpha = 0.8f),
        size = Size(w, h),
        style = Stroke(width = 4.dp.toPx())
    )
}

private fun DrawScope.drawBoardSkeleton(px: Float, py: Float, cw: Float, ch: Float, lineColor: Color) {
    val lineAlpha = 0.7f
    val strokeColor = lineColor.copy(alpha = lineAlpha)
    val stroke = Stroke(width = 1.2.dp.toPx())

    // 横线
    for (i in 0..9) {
        drawLine(strokeColor, Offset(px, py + i * ch), Offset(px + 8 * cw, py + i * ch), stroke.width)
    }
    // 纵线
    for (i in 0..8) {
        val x = px + i * cw
        if (i == 0 || i == 8) {
            drawLine(strokeColor, Offset(x, py), Offset(x, py + 9 * ch), stroke.width)
        } else {
            drawLine(strokeColor, Offset(x, py), Offset(x, py + 4 * ch), stroke.width)
            drawLine(strokeColor, Offset(x, py + 5 * ch), Offset(x, py + 9 * ch), stroke.width)
        }
    }
    
    // 九宫格
    val palaceStroke = stroke.width * 0.8f
    drawLine(strokeColor, Offset(px + 3 * cw, py), Offset(px + 5 * cw, py + 2 * ch), palaceStroke)
    drawLine(strokeColor, Offset(px + 5 * cw, py), Offset(px + 3 * cw, py + 2 * ch), palaceStroke)
    drawLine(strokeColor, Offset(px + 3 * cw, py + 7 * ch), Offset(px + 5 * cw, py + 9 * ch), palaceStroke)
    drawLine(strokeColor, Offset(px + 5 * cw, py + 7 * ch), Offset(px + 3 * cw, py + 9 * ch), palaceStroke)
}

private fun DrawScope.drawRiverCalligraphy(textMeasurer: TextMeasurer, px: Float, py: Float, cw: Float, ch: Float, color: Color, isFlipped: Boolean) {
    val style = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = color.copy(alpha = 0.5f),
        letterSpacing = 12.sp
    )
    val y = py + 4.5f * ch
    
    val leftText = if (isFlipped) "汉界" else "楚河"
    val rightText = if (isFlipped) "楚河" else "汉界"
    
    val leftLayout = textMeasurer.measure(leftText, style)
    drawText(textLayoutResult = leftLayout, topLeft = Offset(px + 1.2f * cw, y - leftLayout.size.height / 2))
    
    val rightLayout = textMeasurer.measure(rightText, style)
    drawText(textLayoutResult = rightLayout, topLeft = Offset(px + 5.2f * cw, y - rightLayout.size.height / 2))
}

private fun DrawScope.drawMoveHighlight(move: XiangqiMoveData, px: Float, py: Float, cw: Float, ch: Float, radius: Float, color: Color, isFlipped: Boolean) {
    listOf(move.from, move.to).forEach { pos ->
        val displayRow = if (isFlipped) 9 - pos.row else pos.row
        val displayCol = if (isFlipped) 8 - pos.col else pos.col
        drawCircle(
            color = color,
            radius = radius * 1.1f,
            center = Offset(px + displayCol * cw, py + displayRow * ch)
        )
    }
}

private fun DrawScope.drawPremiumPiece(
    center: Offset,
    radius: Float,
    piece: XiangqiPieceData,
    char: String,
    isSelected: Boolean,
    isGeneralInCheck: Boolean,
    glowScale: Float,
    colors: ChessArenaExtendedColors,
    textMeasurer: TextMeasurer,
    alpha: Float = 1f
) {
    if (alpha <= 0f) return

    val paint = Paint().apply {
        this.alpha = alpha
    }

    drawContext.canvas.saveLayer(
        Rect(center.x - radius * 2.5f, center.y - radius * 2.5f, center.x + radius * 2.5f, center.y + radius * 2.5f),
        paint
    )

    val isRed = piece.side == PieceSide.RED
    val surface = if (isRed) colors.pieceRedSurface else colors.pieceBlackSurface
    val textColor = if (isRed) colors.pieceRedText else colors.pieceBlackText

    // 1. 动态光晕
    if (isSelected) {
        drawCircle(
            color = colors.selectionGlow,
            radius = radius * glowScale,
            center = center
        )
    } else if (isGeneralInCheck) {
        drawCircle(
            color = colors.evalNegative.copy(alpha = 0.45f),
            radius = radius * glowScale,
            center = center
        )
    }

    // 2. 阴影
    drawCircle(
        color = Color.Black.copy(alpha = 0.4f),
        radius = radius,
        center = center + Offset(2f, 3f)
    )

    // 3. 棋子本体：玉石渐变
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(surface.copy(alpha = 0.9f), surface),
            center = center - Offset(radius * 0.2f, radius * 0.2f),
            radius = radius * 1.5f
        ),
        radius = radius,
        center = center
    )

    // 4. 装饰压纹
    drawCircle(
        color = textColor.copy(alpha = 0.2f),
        radius = radius * 0.85f,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )

    // 5. 文字渲染
    val textStyle = ChessArtStyles.xiangqiPieceLarge.copy(
        color = textColor,
        fontSize = (radius * 1.1f / density / fontScale).sp
    )
    val tr = textMeasurer.measure(char, textStyle)
    drawText(textLayoutResult = tr, topLeft = center - Offset(tr.size.width / 2f, tr.size.height / 2f))
    
    // 6. 高光反射
    drawPath(
        path = Path().apply {
            addArc(Rect(center.x - radius * 0.7f, center.y - radius * 0.7f, center.x + radius * 0.1f, center.y + radius * 0.1f), 180f, 90f)
        },
        color = Color.White.copy(alpha = 0.15f),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    drawContext.canvas.restore()
}

private fun DrawScope.drawEatTargetHighlight(center: Offset, radius: Float, color: Color) {
    val r = radius * 1.05f
    val len = radius * 0.35f
    val strokeWidth = 2.5.dp.toPx()
    
    drawCircle(
        color = color.copy(alpha = 0.12f),
        radius = r,
        center = center
    )
    
    // 左上
    drawLine(color, center + Offset(-r, -r), center + Offset(-r + len, -r), strokeWidth, cap = StrokeCap.Round)
    drawLine(color, center + Offset(-r, -r), center + Offset(-r, -r + len), strokeWidth, cap = StrokeCap.Round)
    // 右上
    drawLine(color, center + Offset(r, -r), center + Offset(r - len, -r), strokeWidth, cap = StrokeCap.Round)
    drawLine(color, center + Offset(r, -r), center + Offset(r, -r + len), strokeWidth, cap = StrokeCap.Round)
    // 左下
    drawLine(color, center + Offset(-r, r), center + Offset(-r + len, r), strokeWidth, cap = StrokeCap.Round)
    drawLine(color, center + Offset(-r, r), center + Offset(-r, r - len), strokeWidth, cap = StrokeCap.Round)
    // 右下
    drawLine(color, center + Offset(r, r), center + Offset(r - len, r), strokeWidth, cap = StrokeCap.Round)
    drawLine(color, center + Offset(r, r), center + Offset(r, r - len), strokeWidth, cap = StrokeCap.Round)
}

private fun DrawScope.drawXiangqiCoordinates(
    textMeasurer: TextMeasurer,
    px: Float,
    py: Float,
    cw: Float,
    ch: Float,
    color: Color,
    isFlipped: Boolean
) {
    val style = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        color = color.copy(alpha = 0.55f)
    )
    val topYBase = (py * 0.5f).coerceAtLeast(0f)
    val bottomYBase = (size.height - py * 0.5f).coerceAtLeast(0f)
    val leftXBase = (px * 0.5f).coerceAtLeast(0f)
    val rightXBase = (size.width - px * 0.5f).coerceAtLeast(0f)

    for (displayCol in 0..8) {
        val labelIndex = if (isFlipped) 8 - displayCol else displayCol
        val label = ('A' + labelIndex).toString()
        val layout = textMeasurer.measure(label, style)
        val x = px + displayCol * cw - layout.size.width / 2f
        val topY = (topYBase - layout.size.height / 2f).coerceAtLeast(0f)
        val bottomY = (bottomYBase - layout.size.height / 2f).coerceAtLeast(0f)
        drawText(textLayoutResult = layout, topLeft = Offset(x, topY))
        drawText(textLayoutResult = layout, topLeft = Offset(x, bottomY))
    }

    for (displayRow in 0..9) {
        val labelNumber = if (isFlipped) 10 - displayRow else displayRow + 1
        val label = labelNumber.toString()
        val layout = textMeasurer.measure(label, style)
        val y = py + displayRow * ch - layout.size.height / 2f
        val leftX = (leftXBase - layout.size.width / 2f).coerceAtLeast(0f)
        val rightX = (rightXBase - layout.size.width / 2f).coerceAtLeast(0f)
        drawText(textLayoutResult = layout, topLeft = Offset(leftX, y))
        drawText(textLayoutResult = layout, topLeft = Offset(rightX, y))
    }
}

private fun motionDurationMs(speed: AnimationSpeed, baseMs: Int): Int {
    if (speed == AnimationSpeed.INSTANT) return 0
    val ratio = speed.durationMs.toFloat() / AnimationSpeed.NORMAL.durationMs.toFloat()
    return (baseMs * ratio).roundToInt().coerceAtLeast(1)
}

private data class XiangqiPalette(
    val background: Color,
    val line: Color
)

private fun xiangqiBoardPalette(theme: BoardTheme, colors: ChessArenaExtendedColors): XiangqiPalette {
    return when (theme) {
        BoardTheme.WOOD -> XiangqiPalette(colors.boardBackground, colors.boardLine)
        BoardTheme.DARK -> XiangqiPalette(Color(0xFF23242A), Color(0xFF6F7179))
        BoardTheme.MARBLE -> XiangqiPalette(Color(0xFFE7E5DF), Color(0xFF8B8D94))
    }
}
