package com.example.chessarena.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chessarena.R
import com.example.chessarena.theme.AccentGold
import com.example.chessarena.theme.ChineseRed
import com.example.chessarena.theme.OnSurfaceDarkSecondary
import com.example.chessarena.theme.SilkWhite
import com.example.chessarena.theme.StoneGray
import com.example.chessarena.theme.xuanPaperBackground
import com.example.chessarena.ui.components.pressScale
import com.example.chessarena.viewmodel.AnimationSpeed
import com.example.chessarena.viewmodel.BoardTheme
import com.example.chessarena.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val resetInteractionSource = remember { MutableInteractionSource() }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .xuanPaperBackground(isDark),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "对局偏好",
                        fontWeight = FontWeight.Bold,
                        color = SilkWhite,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back_leaf),
                            contentDescription = "返回",
                            tint = SilkWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = SilkWhite
                ),
                modifier = Modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E1E24),
                            Color(0xFF121215)
                        )
                    )
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsGroup(title = "界面与古韵") {
                Text(
                    "棋盘主题",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BoardTheme.entries.forEach { theme ->
                        val isSelected = uiState.boardTheme == theme
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clickable { viewModel.setBoardTheme(theme) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0x33B08D3E) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) AccentGold else Color.Transparent
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    theme.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Text(
                                    theme.description,
                                    fontSize = 9.sp,
                                    color = if (isSelected) SilkWhite.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                SettingsSwitchRow(
                    title = "显示局面评估条",
                    description = "实时显示 AI 引擎对当前局势的量化评分分差条",
                    checked = uiState.showEvalBar,
                    onCheckedChange = { viewModel.setShowEvalBar(it) },
                    iconResId = R.drawable.ic_logo_zen
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                SettingsSwitchRow(
                    title = "显示棋盘坐标",
                    description = "在棋盘边缘显示定位字母和数字坐标标注",
                    checked = uiState.showCoordinates,
                    onCheckedChange = { viewModel.setShowCoordinates(it) },
                    iconResId = R.drawable.ic_setting_bagua
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                SettingsSwitchRow(
                    title = "显示走棋历史",
                    description = "在对局中展示中文棋谱或黑白落子序列记录板",
                    checked = uiState.showMoveHistory,
                    onCheckedChange = { viewModel.setShowMoveHistory(it) },
                    iconResId = R.drawable.ic_undo_reincarnation
                )
            }

            SettingsGroup(title = "声音与反馈") {
                SettingsSwitchRow(
                    title = "游戏音效",
                    description = "开启落子、吃子、将军与胜败宣判的古雅提示音效",
                    checked = uiState.soundEnabled,
                    onCheckedChange = { viewModel.setSoundEnabled(it) },
                    iconResId = if (uiState.soundEnabled) R.drawable.ic_sound_guqin else R.drawable.ic_sound_guqin_muted
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                SettingsSwitchRow(
                    title = "触觉振动反馈",
                    description = "每一次吃棋与落子都在玩家指尖绽放极速轻柔的物理震感",
                    checked = uiState.hapticEnabled,
                    onCheckedChange = { viewModel.setHapticEnabled(it) },
                    iconResId = if (uiState.hapticEnabled) R.drawable.ic_haptic_ripple else R.drawable.ic_haptic_ripple_disabled
                )
            }

            SettingsGroup(title = "对局与动画") {
                Text(
                    "棋子移动动画速度",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimationSpeed.entries.forEach { speed ->
                        val isSelected = uiState.animationSpeed == speed
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable { viewModel.setAnimationSpeed(speed) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0x26E9C46A) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) AccentGold else Color.Transparent
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    speed.displayName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            Button(
                onClick = { viewModel.resetToDefaults() },
                interactionSource = resetInteractionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(vertical = 4.dp)
                    .pressScale(resetInteractionSource, pressedScale = 0.95f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ChineseRed,
                                Color(0xFF8A1E25)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0x88FFD700),
                                AccentGold,
                                Color(0x88FFD700)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_reset_chaos),
                    contentDescription = "重置",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "恢复默认设置",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xEE1C1E2A)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = AccentGold,
                fontSize = 15.sp,
                letterSpacing = 1.sp
            )
            content()
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconResId: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            tint = if (checked) AccentGold else OnSurfaceDarkSecondary.copy(alpha = 0.5f),
            modifier = Modifier
                .size(32.dp)
                .padding(end = 12.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(color = SilkWhite),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall.copy(color = OnSurfaceDarkSecondary),
                lineHeight = 16.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentGold,
                checkedTrackColor = Color(0x66E9C46A),
                uncheckedThumbColor = StoneGray,
                uncheckedTrackColor = Color(0x11FFFFFF)
            )
        )
    }
}