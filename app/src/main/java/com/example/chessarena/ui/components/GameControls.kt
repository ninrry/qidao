package com.example.chessarena.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessarena.R
import com.example.chessarena.theme.*

/**
 * 棋道 2.0 品牌化游戏对局控制面板
 * 将开局、悔棋、认输三个按钮与手绘国风 VectorDrawable 完美咬合，传递落子无悔与时光逆溯的东方哲理
 */
@Composable
fun GameControls(
    onNewGame: () -> Unit,
    onUndo: () -> Unit,
    onResign: () -> Unit,
    canUndo: Boolean,
    modifier: Modifier = Modifier
) {
    val newGameInteractionSource = remember { MutableInteractionSource() }
    val undoInteractionSource = remember { MutableInteractionSource() }
    val resignInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. 开新局按钮 (混沌重开)
        Button(
            onClick = onNewGame,
            interactionSource = newGameInteractionSource,
            modifier = Modifier
                .weight(1.0f)
                .pressScale(newGameInteractionSource, pressedScale = 0.97f)
                .semantics { contentDescription = "开始新局" },
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryRed,
                contentColor = SilkWhite
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_reset_chaos),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = SilkWhite
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("开新局", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        // 2. 悔棋按钮 (阴阳时光逆流)
        FilledTonalButton(
            onClick = onUndo,
            enabled = canUndo,
            interactionSource = undoInteractionSource,
            modifier = Modifier
                .weight(1.0f)
                .pressScale(undoInteractionSource, enabled = canUndo, pressedScale = 0.97f)
                .semantics { contentDescription = if (canUndo) "悔棋，撤销上一步" else "悔棋，当前不可用" },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Color(0x26E9C46A), // 浅金黄微光背景
                contentColor = AccentGold,
                disabledContainerColor = Color(0x08FFFFFF),
                disabledContentColor = OnSurfaceDarkSecondary.copy(alpha = 0.72f) // 大幅提高对比度，更加容易辨识
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_undo_reincarnation),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (canUndo) AccentGold else OnSurfaceDarkSecondary.copy(alpha = 0.72f) // 同步提高对比度
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("悔棋", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        // 3. 认输按钮 (折扇休兵)
        OutlinedButton(
            onClick = onResign,
            interactionSource = resignInteractionSource,
            modifier = Modifier
                .weight(1.0f)
                .pressScale(resignInteractionSource, pressedScale = 0.97f)
                .semantics { contentDescription = "认输，结束当前对局" },
            border = BorderStroke(1.dp, Color(0x80C04851)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFE29C45) // 雄黄传统暖色
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_resign_flag),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFFE29C45)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("认输", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
