package com.example.chessarena.theme

import androidx.compose.ui.unit.dp

/**
 * 棋道 2.0 设计系统常量
 * 
 * 集中管理所有设计令牌（Design Tokens），确保全局一致性
 */
object DesignSystem {
    
    // ── 间距系统 (Spacing) ──────────────────────────────────
    object Spacing {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 20.dp
        val xxl = 24.dp
        val xxxl = 32.dp
    }
    
    // ── 圆角系统 (Corner Radius) ───────────────────────────
    object CornerRadius {
        val sm = 4.dp
        val md = 8.dp
        val lg = 12.dp
        val xl = 16.dp
        val xxl = 20.dp
        val round = 50.dp
    }
    
    // ── 阴影系统 (Elevation) ───────────────────────────────
    object Elevation {
        val none = 0.dp
        val low = 2.dp
        val medium = 4.dp
        val high = 8.dp
        val highest = 12.dp
    }
    
    // ── 动画时长 (Animation Duration) ──────────────────────
    object AnimationDuration {
        const val instant = 0
        const val fast = 150
        const val normal = 300
        const val slow = 500
        const val verySlow = 800
    }
    
    // ── 透明度 (Opacity) ───────────────────────────────────
    object Opacity {
        const val disabled = 0.38f
        const val medium = 0.60f
        const val high = 0.87f
        const val full = 1.0f
    }
    
    // ── 边框宽度 (Border Width) ────────────────────────────
    object BorderWidth {
        val thin = 1.dp
        val medium = 2.dp
        val thick = 3.dp
    }
    
    // ── 图标尺寸 (Icon Size) ───────────────────────────────
    object IconSize {
        val sm = 16.dp
        val md = 20.dp
        val lg = 24.dp
        val xl = 32.dp
        val xxl = 40.dp
    }
    
    // ── 按钮尺寸 (Button Size) ─────────────────────────────
    object ButtonSize {
        val height = 48.dp
        val minWidth = 64.dp
        val iconPadding = 8.dp
    }
    
    // ── 棋盘相关 (Board Specific) ──────────────────────────
    object Board {
        const val padding = 0.05f  // 5% padding
        const val pieceRadiusRatio = 0.44f
        const val gridLineWidth = 1.5f
        const val borderLineWidth = 3f
    }
}

/**
 * 棋盘主题调色板
 * 
 * 定义不同棋盘主题的配色方案
 */
data class BoardPalette(
    val background: Long,
    val line: Long,
    val pieceRed: Long,
    val pieceBlack: Long
) {
    companion object {
        val Wood = BoardPalette(
            background = 0xFFD4A574,
            line = 0xFF3E2723,
            pieceRed = 0xFFC62828,
            pieceBlack = 0xFF212121
        )
        
        val Dark = BoardPalette(
            background = 0xFF2D2D2D,
            line = 0xFFE0E0E0,
            pieceRed = 0xFFEF5350,
            pieceBlack = 0xFF424242
        )
        
        val Marble = BoardPalette(
            background = 0xFFF5F5F5,
            line = 0xFF424242,
            pieceRed = 0xFFD32F2F,
            pieceBlack = 0xFF1A1A1A
        )
    }
}
