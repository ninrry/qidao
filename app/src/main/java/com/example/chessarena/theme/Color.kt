package com.example.chessarena.theme

import androidx.compose.ui.graphics.Color

/**
 * 棋道 2.0 高级色彩系统
 * 采用中国传统色系，增强视觉深度与文化底蕴
 */

// ── 核心调色板 (Core Palette) ──────────────────────────────────
val ChineseRed = Color(0xFFC04851)           // 玉赭
val DarkRed = Color(0xFF6B2B1F)              // 檀
val DeepInk = Color(0xFF161823)              // 鸦青
val StoneGray = Color(0xFF3B393C)            // 黝
val SilkWhite = Color(0xFFF7F4ED)            // 缟
val GoldenLeaf = Color(0xFFD4B106)           // 琥珀

// ── 语义化颜色 (Semantic Colors) ───────────────────────────────

// 主色：红方（热情、博弈、力量）
val PrimaryRed = ChineseRed
val PrimaryRedContainer = Color(0xFF4D1A1E)
val OnPrimaryRed = Color(0xFFFFFFFF)

// 次色：黑方（沉稳、深邃、智慧）
val SecondaryInk = Color(0xFF2B2E3D)
val SecondaryInkContainer = Color(0xFF1C1E2A)
val OnSecondaryInk = Color(0xFFDCDCDC)

// 强调色：金色（成就、高亮、尊贵）
val AccentGold = Color(0xFFE9C46A)
val AccentGoldVariant = Color(0xFFB08D3E)
val GlowGold = Color(0x40E9C46A)

// ── 棋盘专色 (Board & Pieces) ───────────────────────────────

// 棋盘：木质纹理感
val BoardBase = Color(0xFFE3C598)            // 浅木色
val BoardVein = Color(0xFF6B4226)            // 木纹/线条
val BoardRiver = Color(0x1A6B4226)           // 河界背景

// 棋子：陶瓷/玉石感
val PieceRedSurface = Color(0xFF912C33)
val PieceRedText = Color(0xFFF4E0A1)
val PieceBlackSurface = Color(0xFF1A1A1A)
val PieceBlackText = Color(0xFFE0E0E0)

// ── 表面颜色 (Surfaces) ──────────────────────────────────────

// 深色模式 (Dark Mode) - 极致深邃
val SurfaceDarkBase = Color(0xFF0F1014)
val SurfaceDarkElevated = Color(0xFF1A1C22)
val SurfaceDarkVariant = Color(0xFF25272F)
val OnSurfaceDarkPrimary = Color(0xFFE2E2E6)
val OnSurfaceDarkSecondary = Color(0xFFA1A2A9)

// 浅色模式 (Light Mode) - 纸墨雅致
val SurfaceLightBase = Color(0xFFFCFAF2)
val SurfaceLightElevated = Color(0xFFFFFFFF)
val SurfaceLightVariant = Color(0xFFF0EDE5)
val OnSurfaceLightPrimary = Color(0xFF2C2E35)
val OnSurfaceLightSecondary = Color(0xFF62646C)

// ── 功能色 (Utility) ──────────────────────────────────────────
val Success = Color(0xFF529B72)              // 翡翠
val Warning = Color(0xFFE29C45)              // 雄黄
val Error = Color(0xFFB3424A)                // 胭脂
val Info = Color(0xFF4C8DAE)                 // 远山蓝

// ── 交互指示 (Interaction) ────────────────────────────────────
val SelectionHighlight = Color(0x66E9C46A)
val ValidTarget = Color(0x40529B72)
val LastMoveMark = Color(0x33B08D3E)

// ── 新设计系统颜色 (New Design System) ─────────────────────────
// 首页卡片
val XiangqiCardGreen = Color(0xFF5A9A6E)
val XiangqiCardGreenDark = Color(0xFF3D7A52)
val GomokuCardBlue = Color(0xFF5B8DB8)
val GomokuCardBlueDark = Color(0xFF3E6F9A)

// 纸墨背景
val PaperBg = Color(0xFFF5F0E8)
val PaperBgDark = Color(0xFF1A1A1E)

// 玩家头像色
val PlayerAvatarGreen = Color(0xFF5A9A6E)
val AiAvatarRed = Color(0xFFC04851)
val AiAvatarBlue = Color(0xFF5B8DB8)

// 新表面色
val SurfaceCardLight = Color(0xFFFFFFFF)
val SurfaceCardDark = Color(0xFF25252B)
val DividerLight = Color(0xFFE8E4DC)
val DividerDark = Color(0xFF33333A)
