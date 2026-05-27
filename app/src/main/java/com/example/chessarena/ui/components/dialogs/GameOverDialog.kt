package com.example.chessarena.ui.components.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chessarena.theme.*

/**
 * 游戏结束对话框 - 统一的中国风水墨卷轴印章设计
 *
 * @param message 游戏结束消息
 * @param gameType 游戏类型 ("xiangqi" 或 "gomoku")
 * @param onDismiss 关闭对话框回调
 * @param onPlayAgain 再来一局回调
 */
@Composable
fun GameOverDialog(
    message: String,
    gameType: String,
    onDismiss: () -> Unit,
    onPlayAgain: () -> Unit
) {
    // 判定胜负
    val gameResult = when (gameType) {
        "xiangqi" -> parseXiangqiResult(message)
        "gomoku" -> parseGomokuResult(message)
        else -> GameResult.UNKNOWN
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFFF9F6EE),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, Color(0xFFD4B106).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6B4226),
                                Color.Transparent,
                                Color.Transparent,
                                Color(0xFF6B4226)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 印章文字
                    val sealText = when (gameResult) {
                        GameResult.WIN -> "捷"
                        GameResult.DRAW -> "和"
                        GameResult.LOSS -> "憾"
                        GameResult.UNKNOWN -> "?"
                    }
                    val sealColor = when (gameResult) {
                        GameResult.WIN -> ChineseRed
                        GameResult.DRAW -> Success
                        GameResult.LOSS -> SecondaryInk
                        GameResult.UNKNOWN -> StoneGray
                    }

                    // 朱砂红大篆艺术盖印印章
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.5.dp, sealColor, RoundedCornerShape(8.dp))
                            .background(sealColor.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = sealText,
                            color = sealColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp,
                            letterSpacing = 0.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 宣判内容
                    Text(
                        text = "弈局已定",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurfaceLightPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceLightSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 底部交互
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("确定", color = OnSurfaceLightSecondary, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onPlayAgain,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("再来一局", color = SilkWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private enum class GameResult {
    WIN, LOSS, DRAW, UNKNOWN
}

private fun parseXiangqiResult(message: String): GameResult {
    return when {
        message.contains("红方获胜") || message.contains("红方胜利") || message.contains("红胜") -> GameResult.WIN
        message.contains("黑方获胜") || message.contains("黑方胜利") || message.contains("黑胜") -> GameResult.LOSS
        message.contains("和棋") || message.contains("作和") || message.contains("平局") -> GameResult.DRAW
        else -> GameResult.UNKNOWN
    }
}

private fun parseGomokuResult(message: String): GameResult {
    return when {
        message.contains("黑方获胜") || message.contains("黑子获胜") || message.contains("黑方胜利") || message.contains("黑胜") -> GameResult.WIN
        message.contains("白方获胜") || message.contains("白子获胜") || message.contains("白方胜利") || message.contains("白胜") -> GameResult.LOSS
        message.contains("和棋") || message.contains("作和") || message.contains("平局") -> GameResult.DRAW
        else -> GameResult.UNKNOWN
    }
}
