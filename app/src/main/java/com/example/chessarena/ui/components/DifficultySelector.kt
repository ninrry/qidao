package com.example.chessarena.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessarena.R
import com.example.chessarena.engine.Difficulty
import com.example.chessarena.theme.*

/**
 * 棋道 2.0 品牌化难度选择器
 * 完全采用底层的 Difficulty 大师核心领域模型，彻底替代旧版“练习一段”等断裂称号
 */
@Composable
fun DifficultySelector(
    gameType: String, // "xiangqi" or "gomoku"
    selectedDifficulty: Difficulty,
    onSelect: (Difficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = if (gameType == "xiangqi") {
        Difficulty.xiangqiDifficulties()
    } else {
        Difficulty.gomokuDifficulties()
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items) { tier ->
            val isSelected = tier == selectedDifficulty
            val borderDp by animateDpAsState(targetValue = if (isSelected) 2.dp else 1.dp, label = "border_dp")
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) AccentGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                label = "border_color"
            )
            val containerColor = if (isSelected) Color(0x33B08D3E) else Color(0xEE1C1E2A)

            val index = items.indexOf(tier)
            val starsCount = index + 1

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(tier) },
                colors = CardDefaults.cardColors(containerColor = containerColor),
                border = BorderStroke(borderDp, borderColor),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. 左侧：祥云难度勋章与星级
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_difficulty_cloud),
                            contentDescription = null,
                            tint = if (isSelected) AccentGold else StoneGray,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            repeat(starsCount) {
                                Text(
                                    text = "★",
                                    color = if (isSelected) AccentGold else OnSurfaceDarkSecondary,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }

                    // 2. 中间：现实大师名称与水墨描述
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tier.displayName,
                            color = if (isSelected) AccentGold else SilkWhite,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp, // 微调大标题字号
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tier.description,
                            color = if (isSelected) SilkWhite.copy(alpha = 0.8f) else OnSurfaceDarkSecondary,
                            maxLines = 2, // 确保文本在极限情况下支持多分辨率换行展示且最多2行，无裁剪
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.5.sp, // 略微收缩字号
                                lineHeight = 14.sp
                            )
                        )
                    }

                    // 3. 右侧：引擎参数（极限算力与时长）
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Surface(
                            color = if (isSelected) PrimaryRed else Color(0x33FFFFFF),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Lvl ${tier.engineParam}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${tier.maxThinkTime}ms",
                            color = if (isSelected) AccentGold else OnSurfaceDarkSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
