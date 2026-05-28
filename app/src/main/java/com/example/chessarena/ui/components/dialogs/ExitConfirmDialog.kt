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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chessarena.theme.*

/**
 * 古典水墨卷轴式退出对局二次防呆弹窗
 */
@Composable
fun ExitConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF1C1E2A) else Color(0xFFFCFAF2)
    val textColor = if (isDark) SilkWhite else DeepInk
    val subTextColor = if (isDark) OnSurfaceDarkSecondary else StoneGray

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            border = BorderStroke(2.dp, AccentGold.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 丹砂“警”字小盖印
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .border(BorderStroke(2.dp, ChineseRed), RoundedCornerShape(8.dp))
                        .background(ChineseRed.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "警",
                        color = ChineseRed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "局未终 · 战未了",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "少侠，棋局正酣，此时抽身离去将被判定为弃子落败，战绩印记亦有缺憾。确定要离去吗？",
                    fontSize = 14.sp,
                    color = subTextColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 按钮 1：且慢 (重回棋局)
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "且慢",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // 按钮 2：断然离去 (强退对局并自动判负)
                    OutlinedButton(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, if (isDark) OnSurfaceDarkSecondary.copy(alpha = 0.4f) else StoneGray.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                    ) {
                        Text(
                            text = "断然离去",
                            color = Error,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
