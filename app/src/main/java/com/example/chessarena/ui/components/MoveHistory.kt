package com.example.chessarena.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessarena.theme.*

/**
 * 古代竹简兵书式对局历史记谱板
 */
@Composable
fun MoveHistory(
    moves: List<String>,
    currentMoveIndex: Int,
    onMoveClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    firstMoveLabel: String = "红",
    secondMoveLabel: String = "黑"
) {
    // 竹简与挂轴背景绘制
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp, max = 98.dp) // 自适应弹性高度，最大 98dp，为棋盘反哺空间
            .animateContentSize()
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                val w = size.width
                val h = size.height

                // 1. 竹简木纹基色 (#EFE6D5)
                drawRect(color = Color(0xFFEFE6D5), size = size)

                // 2. 绘制立体竹简缝隙与光影 (竹简宽度约为 24.dp)
                val slipW = 24.dp.toPx()
                var x = slipW
                val headerH = 10.dp.toPx()
                val footerH = 10.dp.toPx()
                
                while (x < w) {
                    // 纵向缝隙阴影
                    drawLine(
                        color = Color(0xFFC8BBA4),
                        start = Offset(x, headerH),
                        end = Offset(x, h - footerH),
                        strokeWidth = 1.2.dp.toPx()
                    )
                    // 纵向缝隙左侧高光 (制造浮雕感)
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(x - 1f, headerH),
                        end = Offset(x - 1f, h - footerH),
                        strokeWidth = 0.8.dp.toPx()
                    )
                    x += slipW
                }

                // 3. 上下红木挂轴包边 (檀木红渐变)
                val redwoodBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF421617), // 极暗红
                        Color(0xFF5C2223), // 檀木红
                        Color(0xFF421617)
                    )
                )
                drawRect(
                    brush = redwoodBrush,
                    topLeft = Offset(0f, 0f),
                    size = Size(w, headerH)
                )
                drawRect(
                    brush = redwoodBrush,
                    topLeft = Offset(0f, h - footerH),
                    size = Size(w, footerH)
                )

                // 4. 缝合竹简的黑色编织绳索 (线装虚线)
                val ropeStrokeWidth = 1.2.dp.toPx()
                val ropePathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                // 上绑绳 (在红木挂轴边缘下方 5.dp 处)
                drawLine(
                    color = Color(0xFF2C251C),
                    start = Offset(0f, headerH + 5.dp.toPx()),
                    end = Offset(w, headerH + 5.dp.toPx()),
                    strokeWidth = ropeStrokeWidth,
                    pathEffect = ropePathEffect
                )
                // 下绑绳 (在红木挂轴边缘上方 5.dp 处)
                drawLine(
                    color = Color(0xFF2C251C),
                    start = Offset(0f, h - footerH - 5.dp.toPx()),
                    end = Offset(w, h - footerH - 5.dp.toPx()),
                    strokeWidth = ropeStrokeWidth,
                    pathEffect = ropePathEffect
                )
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "📜 旷世兵书战策",
                color = Color(0xFF5C2223), // 檀红字，代表古雅兵书标题
                fontSize = 11.sp, // 略微缩小字体以适配紧凑布局
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            )

            if (moves.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "落子无言 · 虚席以待",
                        color = Color(0xFF7E725F), // 古典暗金灰
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 分组渲染，两步一回合
                    val roundsCount = (moves.size + 1) / 2
                    items(count = roundsCount, key = { it }) { roundIdx ->
                        val redMoveIdx = roundIdx * 2
                        val blackMoveIdx = roundIdx * 2 + 1
                        
                        RoundItem(
                            roundNumber = roundIdx + 1,
                            redMove = moves[redMoveIdx],
                            redMoveIdx = redMoveIdx,
                            blackMove = if (blackMoveIdx < moves.size) moves[blackMoveIdx] else null,
                            blackMoveIdx = blackMoveIdx,
                            currentMoveIndex = currentMoveIndex,
                            onMoveClick = onMoveClick,
                            firstMoveLabel = firstMoveLabel,
                            secondMoveLabel = secondMoveLabel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundItem(
    modifier: Modifier = Modifier,
    roundNumber: Int,
    redMove: String,
    redMoveIdx: Int,
    blackMove: String?,
    blackMoveIdx: Int,
    currentMoveIndex: Int,
    onMoveClick: (Int) -> Unit,
    firstMoveLabel: String,
    secondMoveLabel: String
) {
    // 每一个回合占竹简的列容器，完全透明以便露出精细竹缝底纹
    Column(
        modifier = modifier
            .width(100.dp)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "第 $roundNumber 策",
            color = Color(0xFF6B583E), // 古雅深竹褐
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )
        Spacer(modifier = Modifier.height(2.dp))

        // 先手走棋 (红方印章)
        val isRedCurrent = currentMoveIndex == redMoveIdx
        val redText = formatMove(firstMoveLabel, redMove)
        
        Text(
            text = redText,
            color = if (isRedCurrent) Color(0xFFFFFFFF) else Color(0xFF912C33), // 选中为印章白字，未选中为朱砂暗红
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, // 记谱对齐 Monospace 字体
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .let {
                    if (isRedCurrent) {
                        // 朱砂艺术印章高亮底色
                        it.background(Color(0xFFC04851))
                          .border(BorderStroke(1.dp, Color(0xFFD4B106).copy(alpha = 0.8f)), RoundedCornerShape(4.dp))
                    } else {
                        it
                    }
                }
                .clickable { onMoveClick(redMoveIdx) }
                .padding(vertical = 2.dp, horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 后手走棋 (黑方/白方印章)
        if (blackMove != null) {
            val isBlackCurrent = currentMoveIndex == blackMoveIdx
            val blackText = formatMove(secondMoveLabel, blackMove)
            
            Text(
                text = blackText,
                color = if (isBlackCurrent) Color(0xFFFFFFFF) else Color(0xFF2C2E35), // 选中为印章白字，未选中为漆黑墨色
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .let {
                        if (isBlackCurrent) {
                            // 鸦青墨色印章底色
                            it.background(Color(0xFF2B2E3D))
                              .border(BorderStroke(1.dp, Color(0xFFD4B106).copy(alpha = 0.8f)), RoundedCornerShape(4.dp))
                        } else {
                            it
                        }
                    }
                    .clickable { onMoveClick(blackMoveIdx) }
                    .padding(vertical = 2.dp, horizontal = 4.dp)
            )
        } else {
            Text(
                text = "$secondMoveLabel: ...",
                color = Color(0xFF8C7F6E).copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)
            )
        }
    }
}

private fun formatMove(label: String, move: String): String {
    val trimmed = move.trim()
    val embeddedLabel = listOf("红", "黑", "白").firstOrNull { trimmed.startsWith("$it ") }
    return if (embeddedLabel != null) {
        "$embeddedLabel: ${trimmed.removePrefix("$embeddedLabel ").trimStart()}"
    } else {
        "$label: $trimmed"
    }
}
