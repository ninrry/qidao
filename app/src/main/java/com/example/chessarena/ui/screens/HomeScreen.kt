package com.example.chessarena.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessarena.XiangqiGame
import com.example.chessarena.GomokuGame
import com.example.chessarena.Settings
import com.example.chessarena.History
import com.example.chessarena.R
import com.example.chessarena.theme.*
import kotlinx.coroutines.delay

/**
 * 棋道 3.0 首页
 * 新国风纸墨设计，太极山水意境
 */
@Composable
fun HomeScreen(
    onNavigateTo: (androidx.navigation3.runtime.NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) PaperBgDark else PaperBg)
    ) {
        InkWashBackground(isDark)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 品牌标题区（固定高度）
            BrandHeader()

            // 游戏入口卡片区
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                GameCard(
                    title = "中国象棋",
                    subtitle = "楚河汉界 · 运筹帷幄",
                    gradient = Brush.linearGradient(
                        colors = listOf(XiangqiCardGreen, XiangqiCardGreenDark),
                        start = Offset.Zero,
                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                    ),
                    iconRes = R.drawable.ic_brain_pikafish,
                    delayMillis = 100,
                    onClick = { onNavigateTo(XiangqiGame) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                GameCard(
                    title = "连珠五子棋",
                    subtitle = "黑白交锋 · 妙手连珠",
                    gradient = Brush.linearGradient(
                        colors = listOf(GomokuCardBlue, GomokuCardBlueDark),
                        start = Offset.Zero,
                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                    ),
                    iconRes = R.drawable.ic_brain_slowrenju,
                    delayMillis = 200,
                    onClick = { onNavigateTo(GomokuGame) }
                )
            }

            // 底部导航
            BottomNavBar(
                currentTab = 0,
                onTabClick = { index ->
                    when (index) {
                        1 -> onNavigateTo(History)
                        2 -> onNavigateTo(Settings)
                    }
                }
            )
        }
    }
}

@Composable
private fun BrandHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.alpha(breathAlpha).padding(top = 16.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_logo_zen),
            contentDescription = "棋道",
            tint = Color.Unspecified,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "棋 道",
            style = MaterialTheme.typography.titleLarge,
            color = if (androidx.compose.foundation.isSystemInDarkTheme()) SilkWhite else DeepInk,
            fontWeight = FontWeight.Bold,
            letterSpacing = 10.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            color = AccentGold.copy(alpha = 0.12f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = "CHESS ARENA",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = AccentGold,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "纯粹 · 深度 · 智慧",
            style = MaterialTheme.typography.bodySmall,
            color = if (androidx.compose.foundation.isSystemInDarkTheme()) OnSurfaceDarkSecondary else StoneGray,
            letterSpacing = 4.sp
        )
    }
}

@Composable
private fun GameCard(
    title: String,
    subtitle: String,
    gradient: Brush,
    iconRes: Int,
    delayMillis: Int,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = tween(400, easing = EaseOutBack),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "alpha"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .scale(scale)
            .alpha(alpha)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    currentTab: Int,
    onTabClick: (Int) -> Unit
) {
    data class NavItem(val label: String, val icon: ImageVector, val index: Int)
    val items = listOf(
        NavItem("首页", Icons.Default.Home, 0),
        NavItem("对局记录", Icons.Default.History, 1),
        NavItem("设置", Icons.Default.Settings, 2)
    )

    Surface(
        color = if (androidx.compose.foundation.isSystemInDarkTheme()) SurfaceCardDark else SurfaceCardLight,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = item.index == currentTab
                val color = if (selected) XiangqiCardGreen else
                    if (androidx.compose.foundation.isSystemInDarkTheme()) OnSurfaceDarkSecondary else StoneGray

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onTabClick(item.index) }
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                }
            }
        }
    }
}

@Composable
private fun InkWashBackground(isDark: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val inkAlpha = if (isDark) 0.08f else 0.04f

        // 左上角淡墨晕染
        drawCircle(
            color = DeepInk.copy(alpha = inkAlpha),
            radius = width * 0.6f,
            center = Offset(0f, 0f)
        )

        // 右下角淡墨晕染
        drawCircle(
            color = DeepInk.copy(alpha = inkAlpha * 0.7f),
            radius = width * 0.5f,
            center = Offset(width, height)
        )

        // 装饰线条
        val lineColor = if (isDark) Color.White.copy(alpha = 0.03f) else DeepInk.copy(alpha = 0.03f)
        drawLine(
            color = lineColor,
            start = Offset(width * 0.1f, 0f),
            end = Offset(width * 0.1f, height),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = lineColor,
            start = Offset(width * 0.9f, 0f),
            end = Offset(width * 0.9f, height),
            strokeWidth = 1.dp.toPx()
        )
        drawPath(
            path = Path().apply {
                moveTo(0f, height * 0.25f)
                quadraticTo(width * 0.5f, height * 0.28f, width, height * 0.25f)
            },
            color = lineColor,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}
