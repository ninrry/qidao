package com.example.chessarena.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/**
 * 棋道 2.0 Material 3 深色主题配置
 */
private val ChessArenaDarkColorScheme = darkColorScheme(
    primary = PrimaryRed,
    onPrimary = OnPrimaryRed,
    primaryContainer = PrimaryRedContainer,
    onPrimaryContainer = SilkWhite,
    secondary = SecondaryInk,
    onSecondary = OnSecondaryInk,
    secondaryContainer = SecondaryInkContainer,
    onSecondaryContainer = OnSecondaryInk,
    tertiary = AccentGold,
    onTertiary = SurfaceDarkBase,
    tertiaryContainer = AccentGoldVariant,
    onTertiaryContainer = SilkWhite,
    background = SurfaceDarkBase,
    onBackground = OnSurfaceDarkPrimary,
    surface = SurfaceDarkElevated,
    onSurface = OnSurfaceDarkPrimary,
    surfaceVariant = SurfaceDarkVariant,
    onSurfaceVariant = OnSurfaceDarkSecondary,
    outline = StoneGray,
    error = Error,
    onError = Color.White
)

/**
 * 棋道 2.0 Material 3 浅色主题配置
 */
private val ChessArenaLightColorScheme = lightColorScheme(
    primary = ChineseRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF9E8E8),
    onPrimaryContainer = DarkRed,
    secondary = SecondaryInk,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E9EF),
    onSecondaryContainer = SecondaryInk,
    tertiary = AccentGoldVariant,
    onTertiary = Color.White,
    background = SurfaceLightBase,
    onBackground = OnSurfaceLightPrimary,
    surface = SurfaceLightElevated,
    onSurface = OnSurfaceLightPrimary,
    surfaceVariant = SurfaceLightVariant,
    onSurfaceVariant = OnSurfaceLightSecondary,
    outline = Color(0xFFD1D1D6),
    error = Error,
    onError = Color.White
)

/**
 * 棋道 2.0 专业扩展颜色集
 * 涵盖游戏核心交互、材质表现与视觉引导
 */
@Immutable
data class ChessArenaExtendedColors(
    // ── 棋盘材质 (Board Texture) ──
    val boardBackground: Color,
    val boardLine: Color,
    val boardRiver: Color,
    
    // ── 象棋材质 (Xiangqi Material) ──
    val pieceRedSurface: Color,
    val pieceRedText: Color,
    val pieceBlackSurface: Color,
    val pieceBlackText: Color,
    
    // ── 五子棋材质 (Gomoku Stone) ──
    val stoneBlack: Color,
    val stoneWhite: Color,
    val stoneShadow: Color,
    
    // ── 竞技场反馈 (Arena Feedback) ──
    val selection: Color,
    val selectionGlow: Color,
    val moveHint: Color,
    val lastMoveMark: Color,
    val winGlow: Color,
    
    // ── 算力评估 (Evaluation) ──
    val evalPositive: Color,
    val evalNeutral: Color,
    val evalNegative: Color
)

private val DarkExtendedColors = ChessArenaExtendedColors(
    boardBackground = Color(0xFFBC9A6C),
    boardLine = Color(0xFF423126),
    boardRiver = Color(0x26423126),
    pieceRedSurface = PieceRedSurface,
    pieceRedText = PieceRedText,
    pieceBlackSurface = PieceBlackSurface,
    pieceBlackText = PieceBlackText,
    stoneBlack = Color(0xFF1A1A1A),
    stoneWhite = Color(0xFFE8E8E8),
    stoneShadow = Color(0x4D000000),
    selection = SelectionHighlight,
    selectionGlow = GlowGold,
    moveHint = ValidTarget,
    lastMoveMark = LastMoveMark,
    winGlow = Color(0x4D529B72),
    evalPositive = Success,
    evalNeutral = Color(0xFF70727B),
    evalNegative = PrimaryRed
)

private val LightExtendedColors = ChessArenaExtendedColors(
    boardBackground = BoardBase,
    boardLine = BoardVein,
    boardRiver = BoardRiver,
    pieceRedSurface = Color(0xFFA53D45),
    pieceRedText = Color(0xFFFBEBC0),
    pieceBlackSurface = Color(0xFF2C2C2C),
    pieceBlackText = Color(0xFFE8E8E8),
    stoneBlack = Color(0xFF262626),
    stoneWhite = Color(0xFFFDFDFD),
    stoneShadow = Color(0x33000000),
    selection = SelectionHighlight,
    selectionGlow = GlowGold,
    moveHint = Color(0x4D529B72),
    lastMoveMark = LastMoveMark,
    winGlow = Color(0x33529B72),
    evalPositive = Color(0xFF3B8158),
    evalNeutral = Color(0xFFD1D1D6),
    evalNegative = Color(0xFFB3424A)
)

val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }

@Composable
fun ChessArenaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) ChessArenaDarkColorScheme else ChessArenaLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * 向下兼容
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    ChessArenaTheme(darkTheme = darkTheme, content = content)
}

object ChessArenaColors {
    val extendedColors: ChessArenaExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}

// ── 古典碎金数据模型 ──
private data class GoldFoil(
    val xRatio: Float,
    val yRatio: Float,
    val sizeRatio: Float,
    val alpha: Float,
    val rotation: Float
)

// 25个固定种子的碎金金箔点，保证性能与每次绘制位置绝对稳定、不重复计算
private val fixedGoldFoils = listOf(
    GoldFoil(0.12f, 0.08f, 0.015f, 0.12f, 25f),
    GoldFoil(0.85f, 0.15f, 0.022f, 0.15f, -15f),
    GoldFoil(0.05f, 0.35f, 0.018f, 0.10f, 40f),
    GoldFoil(0.92f, 0.45f, 0.025f, 0.14f, -30f),
    GoldFoil(0.45f, 0.12f, 0.012f, 0.08f, 10f),
    GoldFoil(0.28f, 0.28f, 0.020f, 0.11f, -20f),
    GoldFoil(0.72f, 0.32f, 0.016f, 0.09f, 35f),
    GoldFoil(0.18f, 0.55f, 0.024f, 0.13f, 12f),
    GoldFoil(0.82f, 0.62f, 0.019f, 0.12f, -8f),
    GoldFoil(0.35f, 0.75f, 0.026f, 0.14f, 45f),
    GoldFoil(0.68f, 0.78f, 0.014f, 0.09f, -18f),
    GoldFoil(0.08f, 0.88f, 0.022f, 0.11f, 28f),
    GoldFoil(0.88f, 0.92f, 0.028f, 0.16f, -35f),
    GoldFoil(0.52f, 0.48f, 0.011f, 0.07f, 5f),
    GoldFoil(0.22f, 0.22f, 0.013f, 0.09f, -12f),
    GoldFoil(0.64f, 0.18f, 0.017f, 0.10f, 22f),
    GoldFoil(0.38f, 0.58f, 0.020f, 0.12f, -28f),
    GoldFoil(0.58f, 0.88f, 0.015f, 0.08f, 15f),
    GoldFoil(0.78f, 0.48f, 0.021f, 0.13f, -5f),
    GoldFoil(0.15f, 0.72f, 0.018f, 0.11f, 33f),
    GoldFoil(0.48f, 0.95f, 0.023f, 0.14f, -42f),
    GoldFoil(0.95f, 0.28f, 0.016f, 0.09f, 18f),
    GoldFoil(0.02f, 0.62f, 0.019f, 0.10f, -25f),
    GoldFoil(0.30f, 0.92f, 0.014f, 0.08f, 8f),
    GoldFoil(0.76f, 0.96f, 0.025f, 0.15f, -15f)
)

/**
 * 宣纸岁结碎金水墨晕染全局古典美学背景 Modifier
 */
fun Modifier.xuanPaperBackground(isDark: Boolean): Modifier = this.drawBehind {
    val width = size.width
    val height = size.height

    // 1. 纸基底色
    val paperColor = if (isDark) Color(0xFF0F1014) else Color(0xFFFCFAF2)
    drawRect(color = paperColor, size = size)

    // 2. 水墨边缘柔和晕染 (鸦青色/淡墨色)
    val inkColor = if (isDark) Color(0xFF050608) else Color(0xFF161823)
    
    // 左上水墨晕染
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(inkColor.copy(alpha = if (isDark) 0.35f else 0.06f), Color.Transparent),
            center = Offset(0f, 0f),
            radius = width * 0.7f
        ),
        size = size
    )
    // 右下水墨晕染
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(inkColor.copy(alpha = if (isDark) 0.30f else 0.05f), Color.Transparent),
            center = Offset(width, height),
            radius = width * 0.8f
        ),
        size = size
    )
    // 左下微弱晕染
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(inkColor.copy(alpha = if (isDark) 0.20f else 0.03f), Color.Transparent),
            center = Offset(0f, height),
            radius = width * 0.5f
        ),
        size = size
    )

    // 3. 岁结金箔碎金斑驳
    val goldColor = if (isDark) Color(0xFFB08D3E) else Color(0xFFD4B106)
    fixedGoldFoils.forEach { foil ->
        val cx = foil.xRatio * width
        val cy = foil.yRatio * height
        val foilSize = width * foil.sizeRatio
        
        withTransform({
            rotate(foil.rotation, Offset(cx, cy))
        }) {
            // 绘制不规则碎金块 (以 Path 绘制钻石状/三角形/平行四边形碎片)
            val path = Path().apply {
                moveTo(cx - foilSize * 0.5f, cy)
                lineTo(cx, cy - foilSize * 0.4f)
                lineTo(cx + foilSize * 0.5f, cy + foilSize * 0.1f)
                lineTo(cx + foilSize * 0.1f, cy + foilSize * 0.5f)
                lineTo(cx - foilSize * 0.3f, cy + foilSize * 0.3f)
                close()
            }
            drawPath(
                path = path,
                color = goldColor.copy(alpha = foil.alpha * (if (isDark) 0.7f else 1.0f))
            )
        }
    }
}

