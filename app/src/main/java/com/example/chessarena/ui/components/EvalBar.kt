package com.example.chessarena.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessarena.theme.*
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * 引擎评估条 (Evaluation Bar)
 *
 * 显示当前局势分析。黑方在上（暗色），红方在下（深红/金），平局在中间（白色）。
 * @param score 当前分数 (以百兵为单位，正值表示红方/玩家优势，负值表示黑方/AI优势)
 * @param isThinking AI是否正在思考中
 */
@Composable
fun EvalBar(
    score: Int,
    isThinking: Boolean,
    modifier: Modifier = Modifier
) {
    val extendedColors = ChessArenaColors.extendedColors
    
    // 限制分数范围在 -1000 到 +1000 (约10个兵的优势)
    val maxScore = 1000f
    val clampedScore = score.coerceIn(-1000, 1000).toFloat()
    
    // 计算红方占的比例 (分数 +1000 -> 比例 1.0; 分数 -1000 -> 比例 0.0)
    val redRatio = (clampedScore + maxScore) / (2 * maxScore)
    val animatedRatio by animateFloatAsState(targetValue = redRatio, label = "red_ratio")

    Box(
        modifier = modifier
            .width(24.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // 背景是黑方优势 (上方黑色)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            extendedColors.evalNeutral,
                            extendedColors.evalNegative
                        )
                    )
                )
        )

        // 前景是红方优势 (下方红色，从底部网上填)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedRatio)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            extendedColors.evalPositive,
                            extendedColors.evalNeutral
                        )
                    )
                )
        )

        // 中间分割线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.Center)
                .background(Color.White.copy(alpha = 0.5f))
        )

        // 文字指示分值
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 黑方优势分数
            if (score < -50) {
                Text(
                    text = String.format(Locale.ROOT, "%.1f", (score.absoluteValue / 100f)),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Spacer(modifier = Modifier.size(1.dp))
            }

            // 红方优势分数
            if (score > 50) {
                Text(
                    text = String.format(Locale.ROOT, "%.1f", (score / 100f)),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Spacer(modifier = Modifier.size(1.dp))
            }
        }
    }
}
